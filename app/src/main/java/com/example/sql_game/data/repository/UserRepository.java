package com.example.sql_game.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.sql_game.data.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

/**
 * Репозиторий для управления данными пользователя и аутентификацией.
 * Предоставляет LiveData для наблюдения за состоянием аутентификации.
 */
public class UserRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // LiveData для отслеживания текущего пользователя Firebase
    private final MutableLiveData<FirebaseUser> firebaseUserLiveData = new MutableLiveData<>();

    // LiveData для отслеживания данных пользователя из Firestore
    private final MutableLiveData<UserModel> currentUserData = new MutableLiveData<>();

    // LiveData для сообщений об ошибках/успехе
    private final MutableLiveData<String> authMessage = new MutableLiveData<>();

    public UserRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Запуск слушателя статуса аутентификации Firebase
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            firebaseUserLiveData.postValue(user);

            if (user != null) {
                // Если пользователь вошел, начинаем слушать его данные из Firestore
                loadUserData(user.getUid());
            } else {
                currentUserData.postValue(null);
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
     * @param email
     * @param password
     * @param username
     */
    public void register(String email, String password, String username) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // ИСПРАВЛЕНИЕ: Используем конструктор UserModel с 3 аргументами.
                            UserModel newUser = new UserModel(firebaseUser.getUid(), username, email);
                            saveNewUserToFirestore(newUser);
                        }
                        authMessage.postValue("Профиль успешно создан.");
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
                .document(user.getUserId()) // Используем getUserId(), который помечен @Exclude
                .set(user)
                .addOnFailureListener(e -> authMessage.postValue("Ошибка сохранения данных: " + e.getMessage()));
    }

    private void loadUserData(String userId) {
        db.collection("users")
                .document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        authMessage.postValue("Ошибка загрузки данных: " + e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        // Важно: установить transient userId из имени документа
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                        }
                        currentUserData.postValue(user);
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
