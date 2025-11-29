package com.example.sql_game.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import com.example.sql_game.data.model.UserModel;
import com.example.sql_game.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel для предоставления данных пользователя (профиль, статус авторизации)
 * пользовательскому интерфейсу.
 * Использует UserRepository (Singleton) для работы с Firebase.
 */
public class UserViewModel extends ViewModel {

    private final UserRepository userRepository;

    // LiveData, полученные из репозитория:
    private final LiveData<FirebaseUser> firebaseUserLiveData;
    private final LiveData<UserModel> currentUserData;

    // MutableLiveData для сообщений об аутентификации.
    // Используется MutableLiveData, чтобы ViewModel могла очистить сообщение после отображения.
    private final MutableLiveData<String> authMessage;

    // LiveData для статуса входа, полученная через Transformations.map
    private final LiveData<Boolean> isLoggedInLiveData;

    public UserViewModel() {
        // Использование статического метода getInstance() для получения единственного экземпляра репозитория
        userRepository = UserRepository.getInstance();

        // Инициализация LiveData из репозитория.
        firebaseUserLiveData = userRepository.getFirebaseUserLiveData();
        currentUserData = userRepository.getCurrentUserData();

        // Получаем ссылку на MutableLiveData из репозитория для управления состоянием сообщения.
        // Выполняем явное приведение типа, предполагая, что репозиторий возвращает MutableLiveData.
        // Это необходимо для последующего вызова authMessage.setValue(null) в clearAuthMessage().
        @SuppressWarnings("unchecked")
        MutableLiveData<String> message = (MutableLiveData<String>) userRepository.getAuthMessage();
        this.authMessage = message;

        // Создание LiveData для проверки статуса входа: true, если FirebaseUser не null.
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

    /** Возвращает LiveData с булевым значением статуса входа (true/false). */
    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedInLiveData;
    }

    // --- МЕТОД ОЧИСТКИ ---

    /**
     * Очищает текущее сообщение об аутентификации (устанавливает null).
     * Вызывается из Activity/Fragment после отображения Toast, чтобы предотвратить
     * его повторное появление при пересоздании View (например, при смене ориентации).
     */
    public void clearAuthMessage() {
        // Установка null на MutableLiveData
        authMessage.setValue(null);
    }

    // --- МЕТОДЫ АУТЕНТИФИКАЦИИ ---

    /**
     * Регистрация нового пользователя.
     */
    public void register(String email, String password, String username) {
        userRepository.register(email, password, username);
    }

    /**
     * Вход существующего пользователя.
     */
    public void login(String email, String password) {
        userRepository.login(email, password);
    }

    /**
     * Псевдоним для метода login (более интуитивное название для UI).
     */
    public void signIn(String email, String password) {
        login(email, password);
    }

    /**
     * Псевдоним для метода register (более интуитивное название для UI).
     */
    public void signUp(String email, String password, String username) {
        register(email, password, username);
    }

    /**
     * Выход пользователя из системы.
     */
    public void logout() {
        userRepository.logout();
    }

    // --- МЕТОДЫ ПРОФИЛЯ/ПРОГРЕССА ---

    /**
     * Обновление данных пользователя в Firestore (XP, Level, прогресс).
     * @param user Обновленная модель пользователя.
     */
    public void updateUserData(UserModel user) {
        userRepository.updateUserData(user);
    }

    /**
     * Обновляет ID выбранной аватарки пользователя.
     * @param avatarId ID новой аватарки (например, "avatar_1").
     */
    public void updateAvatar(String avatarId) {
        userRepository.updateAvatar(avatarId);
    }
}