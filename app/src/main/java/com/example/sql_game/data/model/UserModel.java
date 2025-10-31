package com.example.sql_game.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Date;

// Игнорировать любые поля, которые могут быть в базе данных, но отсутствуют в этой модели.
@IgnoreExtraProperties
public class UserModel {
    // ID пользователя в Firebase Auth (не сохраняется в Firestore напрямую)
    private String userId;
    // Данные профиля
    private String username;
    private String email;
    // Игровая статистика
    private int level;
    private int xp;
    private int crystals;
    private int streakCount;
    // Служебные поля
    private Date lastLogin;
    // URL для аватара
    private String fotoUrl;

    // ОБЯЗАТЕЛЬНО: Пустой конструктор для Firestore
    public UserModel() {
        // Требуется для автоматической десериализации данных из Firestore.
    }

    // Конструктор для создания нового пользователя при регистрации
    public UserModel(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.level = 1; // Начальный уровень
        this.xp = 0;
        this.crystals = 50; // Начальные кристаллы
        this.streakCount = 0;
        this.lastLogin = new Date();
        this.fotoUrl = "https://placehold.co/100x100/A0A0A0/ffffff?text=U"; // Простой плейсхолдер
    }

    // --- Геттеры и Сеттеры (Обязательны для Firestore) ---

    // Exclude гарантирует, что мы не пытаемся записать userId в Firestore (он используется как имя документа)
    @Exclude
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
