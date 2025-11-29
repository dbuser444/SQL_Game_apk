package com.example.sql_game.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Указываем Firestore игнорировать любые дополнительные свойства
@IgnoreExtraProperties
public class UserModel {

    // ВАЖНО: @Exclude гарантирует, что это поле НЕ будет сохранено в Firestore,
    // но будет использоваться ТОЛЬКО в приложении для хранения ID документа.
    @Exclude
    private String userId;

    // Атрибуты профиля и роли
    private String username;
    private boolean isTeacher;

    // НОВОЕ ПОЛЕ: ID выбранной аватарки (например, "avatar_1")
    private String avatarId;

    // Атрибуты игрового прогресса
    private int level;
    private int xp;
    private int crystals;
    private int streakCount;
    private Date lastLogin;

    // *** НОВОЕ ПОЛЕ: Список ID уроков, которые пользователь успешно завершил. ***
    private List<String> completedLessonIds;

    // URL фото (пустая строка, так как Storage мы не используем)
    private String fotoUrl;

    // Обязательный конструктор без аргументов для Firestore
    public UserModel() {
        // Требуется для Firebase. Инициализация списка.
        this.completedLessonIds = new ArrayList<>();
    }

    // Конструктор для создания нового пользователя (используется при регистрации)
    public UserModel(String username) {
        this.username = username;
        this.isTeacher = false; // По умолчанию пользователь не учитель

        // Инициализация игровых значений по умолчанию
        this.level = 1;
        this.xp = 0;
        this.crystals = 0;
        this.streakCount = 0;
        this.lastLogin = new Date();
        this.fotoUrl = "";

        // НОВАЯ ИНИЦИАЛИЗАЦИЯ: Аватар по умолчанию
        this.avatarId = "avatar_1";

        // Инициализация списка завершенных уроков
        this.completedLessonIds = new ArrayList<>();
    }

    // --- ГЕТТЕРЫ И СЕТТЕРЫ ---

    // Геттер и сеттер для userId с @Exclude (только для внутреннего использования)
    @Exclude
    public String getUserId() {
        return userId;
    }

    @Exclude
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // НОВЫЕ ГЕТТЕР И СЕТТЕР ДЛЯ АВАТАРА
    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public boolean isTeacher() {
        return isTeacher;
    }

    public void setTeacher(boolean teacher) {
        isTeacher = teacher;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getCrystals() {
        return crystals;
    }

    public void setCrystals(int crystals) {
        this.crystals = crystals;
    }

    public int getStreakCount() {
        return streakCount;
    }

    public void setStreakCount(int streakCount) {
        this.streakCount = streakCount;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    // *** НОВЫЕ ГЕТТЕР И СЕТТЕР ДЛЯ COMPLETED LESSONS ***
    public List<String> getCompletedLessonIds() {
        // Убедимся, что список никогда не равен null
        if (completedLessonIds == null) {
            completedLessonIds = new ArrayList<>();
        }
        return completedLessonIds;
    }

    public void setCompletedLessonIds(List<String> completedLessonIds) {
        this.completedLessonIds = completedLessonIds;
    }

    // Вспомогательный метод для проверки
    public boolean isLessonCompleted(String lessonId) {
        return getCompletedLessonIds().contains(lessonId);
    }
}