package com.example.sql_game.data.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.sql_game.data.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction; // Добавлен для транзакций

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Репозиторий для управления данными пользователя и аутентификацией.
 * Предоставляет LiveData для наблюдения за состоянием аутентификации.
 * Реализует Singleton.
 */
public class UserRepository {

    // 1. Singleton
    private static UserRepository instance;

    // 2. Статический метод для получения экземпляра
    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser> firebaseUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserModel> currentUserData = new MutableLiveData<>();
    private final MutableLiveData<String> authMessage = new MutableLiveData<>();

    private ListenerRegistration userListenerRegistration;
    private static final String TAG = "UserRepository";
    private static final int DAILY_REWARD_CRYSTALS = 20;

    /**
     * Закрытый конструктор для реализации Singleton.
     */
    private UserRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            firebaseUserLiveData.postValue(user);

            if (user != null) {
                startListeningForUserData(user.getUid());
            } else {
                currentUserData.postValue(null);
                stopListeningForUserData();
            }
        });
    }

    // --- МЕТОДЫ АУТЕНТИФИКАЦИИ ---

    /**
     * Вход пользователя.
     */
    public void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        authMessage.postValue("Успешный вход!");
                    } else {
                        authMessage.postValue("Ошибка входа: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Регистрация нового пользователя.
     */
    public void register(String email, String password, String username) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // 1. Обновление профиля Auth (добавление username)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // 2. Создание записи в Firestore
                                            UserModel newUser = new UserModel(username);
                                            newUser.setUserId(firebaseUser.getUid());
                                            saveNewUserToFirestore(newUser);
                                            authMessage.postValue("Профиль успешно создан.");
                                        } else {
                                            authMessage.postValue("Регистрация успешна, но не удалось обновить имя пользователя.");
                                        }
                                    });
                        }
                    } else {
                        authMessage.postValue("Ошибка регистрации: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Выход пользователя.
     */
    public void logout() {
        auth.signOut();
        authMessage.postValue("Вы вышли из системы.");
    }

    // --- МЕТОДЫ РАБОТЫ С FIREBASE FIRESTORE ---

    private void saveNewUserToFirestore(UserModel user) {
        db.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnFailureListener(e -> authMessage.postValue("Ошибка сохранения данных: " + e.getMessage()));
    }

    /**
     * Запускает слушатель для данных пользователя и обрабатывает ежедневный вход.
     */
    private void startListeningForUserData(String userId) {
        stopListeningForUserData();

        userListenerRegistration = db.collection("users")
                .document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        authMessage.postValue("Ошибка загрузки данных: " + e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                            currentUserData.postValue(user);

                            checkDailyLogin(user);
                        }
                    } else {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Профиль Firestore не найден. Создание нового.");
                            UserModel newUser = new UserModel(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Новый Пользователь");
                            newUser.setUserId(firebaseUser.getUid());
                            saveNewUserToFirestore(newUser);
                        }
                    }
                });
    }

    private void stopListeningForUserData() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    /**
     * НОВЫЙ МЕТОД: Начисление кристаллов пользователю с использованием транзакции.
     * Это обеспечивает атомарность операции (кристаллы не потеряются при одновременных запросах).
     * @param amount Количество кристаллов для начисления.
     */
    public void addCrystals(int amount) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || amount <= 0) {
            Log.e(TAG, "Невозможно начислить кристаллы: Пользователь не авторизован или количество <= 0.");
            return;
        }

        final DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. Получаем текущие данные
            UserModel user = transaction.get(userRef).toObject(UserModel.class);

            if (user == null) {
                // ВАЖНОЕ ИСПРАВЛЕНИЕ: Вместо того, чтобы кидать исключение, создаем нового пользователя.
                // Это предотвращает Unhandled exception: java.lang.Exception, если документ отсутствует.
                String username = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Новый Пользователь";
                UserModel newUser = new UserModel(username);
                newUser.setUserId(firebaseUser.getUid());

                // Устанавливаем начальное количество кристаллов (0)
                newUser.setCrystals(0);

                // Обновляем количество кристаллов (0 + amount)
                int newCrystals = amount;

                // Создаем документ и обновляем данные в транзакции
                transaction.set(userRef, newUser);
                transaction.update(userRef, "crystals", newCrystals);

                Log.w(TAG, "Документ пользователя не найден, создан новый профиль и начислены кристаллы: +" + amount);
                return null;
            }

            // 2. Обновляем количество кристаллов
            int newCrystals = user.getCrystals() + amount;

            // 3. Записываем обновленные данные
            transaction.update(userRef, "crystals", newCrystals);

            return null; // Транзакция завершена
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Кристаллы успешно начислены: +" + amount);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Ошибка транзакции при начислении кристаллов: " + e.getMessage());
            // Проверяем, если это наше "ручное" исключение (хотя мы его убрали, хорошая практика)
            if (e.getMessage() != null && e.getMessage().contains("Не удалось найти данные пользователя")) {
                authMessage.postValue("Ошибка: Произошла внутренняя ошибка при попытке записи данных.");
            } else {
                authMessage.postValue("Ошибка начисления награды: " + e.getMessage());
            }
        });
    }

    /**
     * Обновление данных пользователя в Firestore.
     */
    public void updateUserData(UserModel user) {
        db.collection("users")
                .document(user.getUserId())
                .set(user, SetOptions.merge())
                .addOnFailureListener(e -> authMessage.postValue("Ошибка обновления данных: " + e.getMessage()));
    }

    /**
     * НОВЫЙ МЕТОД: Обновляет ID выбранной аватарки пользователя в Firestore.
     * @param newAvatarId ID новой аватарки (например, "avatar_1").
     */
    public void updateAvatar(String newAvatarId) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            authMessage.postValue("Ошибка: Пользователь не авторизован.");
            return;
        }

        DocumentReference userDocRef = db.collection("users").document(firebaseUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarId", newAvatarId);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    authMessage.postValue("Аватар успешно обновлен.");
                })
                .addOnFailureListener(e -> authMessage.postValue("Ошибка обновления аватара: " + e.getMessage()));
    }


    // --- ЛОГИКА ЕЖЕДНЕВНОГО ВХОДА ---

    /**
     * Проверяет, прошло ли 24 часа с момента последнего входа, и начисляет награду.
     */
    public void checkDailyLogin(UserModel user) {
        if (user.getUserId() == null || auth.getCurrentUser() == null) return;

        // Если дата последнего входа null, даем награду (первый вход)
        if (user.getLastLogin() == null) {
            giveDailyReward(user, true); // forceLogin = true
            return;
        }

        long currentTime = new Date().getTime();
        long lastLoginTime = user.getLastLogin().getTime();

        // Интервал в 24 часа
        long twentyFourHours = TimeUnit.DAYS.toMillis(1);

        if (currentTime > lastLoginTime + twentyFourHours) {
            // Награда может быть выдана
            giveDailyReward(user, false);
        }
    }

    /**
     * Выдает ежедневную награду и обновляет метку времени.
     */
    private void giveDailyReward(UserModel user, boolean forceLogin) {
        if (user.getUserId() == null) return;

        int newCrystals = user.getCrystals() + DAILY_REWARD_CRYSTALS;
        int newStreak = user.getStreakCount() + 1;

        DocumentReference userDocRef = db.collection("users").document(user.getUserId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("crystals", newCrystals);
        updates.put("lastLogin", new Date());
        updates.put("streakCount", newStreak);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = forceLogin ?
                            "Добро пожаловать! Вы получили " + DAILY_REWARD_CRYSTALS + " кристаллов." :
                            "Ежедневная награда! Вы получили " + DAILY_REWARD_CRYSTALS + " кристаллов!";
                    authMessage.postValue(message);
                })
                .addOnFailureListener(e -> authMessage.postValue("Ошибка начисления награды: " + e.getMessage()));
    }


    // --- ГЕТТЕРЫ LIVE DATA ДЛЯ ViewModel ---

    public LiveData<FirebaseUser> getFirebaseUserLiveData() {
        return firebaseUserLiveData;
    }

    public LiveData<UserModel> getCurrentUserData() {
        return currentUserData;
    }

    public LiveData<String> getAuthMessage() {
        return authMessage;
    }
}