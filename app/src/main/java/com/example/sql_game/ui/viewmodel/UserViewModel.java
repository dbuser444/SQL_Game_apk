package com.example.sql_game.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import com.example.sql_game.data.model.UserModel;
import com.example.sql_game.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel для предоставления данных пользователя (профиль, статус авторизации)
 * пользовательскому интерфейсу.
 * Использует UserRepository для работы с Firebase.
 */
public class UserViewModel extends ViewModel {

    private final UserRepository userRepository;

    // LiveData из репозитория для отслеживания состояния входа (FirebaseUser)
    private final LiveData<FirebaseUser> firebaseUserLiveData;
    // LiveData из репозитория для отслеживания данных пользователя (из Firestore)
    private final LiveData<UserModel> currentUserData;
    // LiveData для сообщений об ошибках/успехе
    private final LiveData<String> authMessage;
    // LiveData для статуса входа (Boolean)
    private final LiveData<Boolean> isLoggedInLiveData; // <-- Добавлена LiveData для статуса

    public UserViewModel() {
        // Инициализация репозитория (предполагаем, что он инициализируется здесь)
        userRepository = new UserRepository();

        // Получение LiveData из репозитория
        firebaseUserLiveData = userRepository.getFirebaseUserLiveData();
        currentUserData = userRepository.getCurrentUserData();
        authMessage = userRepository.getAuthMessage();

        // ИСПРАВЛЕНО: Добавление LiveData для проверки статуса входа
        isLoggedInLiveData = Transformations.map(firebaseUserLiveData, user -> user != null);
    }

    // --- ГЕТТЕРЫ LIVE DATA ДЛЯ UI ---

    /** Возвращает LiveData: FirebaseUser, null если не авторизован */
    public LiveData<FirebaseUser> getFirebaseUserLiveData() {
        return firebaseUserLiveData;
    }

    /** Возвращает LiveData с данными профиля пользователя из Firestore */
    public LiveData<UserModel> getCurrentUserData() {
        return currentUserData;
    }

    /** Возвращает LiveData с сообщениями о статусе (успех/ошибка) */
    public LiveData<String> getAuthMessage() {
        return authMessage;
    }

    /** Возвращает LiveData с булевым значением статуса входа. */
    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedInLiveData;
    }

    // --- МЕТОДЫ АУТЕНТИФИКАЦИИ ---

    /**
     * Регистрация: вызывает метод register в репозитории.
     * @param email
     * @param password
     * @param username
     */
    public void register(String email, String password, String username) {
        // Предполагаем, что userRepository.register принимает 3 аргумента
        userRepository.register(email, password, username);
    }

    /**
     * Вход: вызывает метод login в репозитории.
     * @param email
     * @param password
     */
    public void login(String email, String password) {
        userRepository.login(email, password);
    }

    /**
     * Псевдоним для метода login.
     */
    public void signIn(String email, String password) {
        login(email, password);
    }

    /**
     * Псевдоним для метода register.
     */
    public void signUp(String email, String password, String username) {
        register(email, password, username);
    }

    /**
     * Выход: вызывает метод logout в репозитории.
     */
    public void logout() {
        userRepository.logout();
    }

    // --- МЕТОДЫ ПРОФИЛЯ ---

    /**
     * Обновление игрового прогресса (XP, Level и т.д.).
     * @param user Обновленная модель пользователя.
     */
    public void updateUserData(UserModel user) {
        userRepository.updateUserData(user);
    }
}