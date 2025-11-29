package com.example.sql_game.data.model;

import java.util.List;

/**
 * Модель данных для одного урока SQL.
 * Объединяет метаданные урока (для списка) и SQL-сценарии (для выполнения).
 */
public class LessonModel {
    // --- Поля из Вашей версии (Метаданные и Прогресс) ---
    private String id; // Изменено на String для гибкости (Firebase/Room)
    private String title;
    private String description;
    private int requiredExperience;
    private List<TaskModel> tasks; // Список заданий в уроке

    // --- НОВОЕ ПОЛЕ: Явный статус завершения урока (для репозитория) ---
    private boolean isExplicitlyCompleted = false;

    // --- Поля из Моей версии (Сценарии для выполнения) ---
    private String setupSql;        // Скрипт для создания и заполнения таблиц
    private String expectedQuery;   // Ожидаемый SQL-запрос для проверки
    private String initialCode;     // Начальный код в редакторе

    // Обязательный публичный конструктор без аргументов (требуется для десериализации)
    public LessonModel() {}

    // Конструктор для всех полей
    public LessonModel(String id, String title, String description, int requiredExperience, List<TaskModel> tasks, String setupSql, String expectedQuery, String initialCode) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.requiredExperience = requiredExperience;
        this.tasks = tasks;
        this.setupSql = setupSql;
        this.expectedQuery = expectedQuery;
        this.initialCode = initialCode;
    }

    // --- НОВЫЙ СЕТТЕР, УСТРАНЯЮЩИЙ ОШИБКУ LessonRepository ---
    /**
     * Явно отмечает урок как завершенный в репозитории.
     * @param completed true, если урок завершен.
     */
    public void setCompleted(boolean completed) {
        this.isExplicitlyCompleted = completed;
    }
    // ------------------------------------------

    // --- ГЕТТЕРЫ И СЕТТЕРЫ (для десериализации) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRequiredExperience() {
        return requiredExperience;
    }

    public void setRequiredExperience(int requiredExperience) {
        this.requiredExperience = requiredExperience;
    }

    public List<TaskModel> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskModel> tasks) {
        this.tasks = tasks;
    }

    // --- Добавленные Геттеры для LessonPlayActivity ---

    public String getSetupSql() {
        return setupSql;
    }

    public void setSetupSql(String setupSql) {
        this.setupSql = setupSql;
    }

    public String getExpectedQuery() {
        return expectedQuery;
    }

    public void setExpectedQuery(String expectedQuery) {
        this.expectedQuery = expectedQuery;
    }

    public String getInitialCode() {
        return initialCode;
    }

    public void setInitialCode(String initialCode) {
        this.initialCode = initialCode;
    }

    // --- МЕТОДЫ ДЛЯ РАСЧЕТА ПРОГРЕССА (Ваша логика) ---

    /**
     * Возвращает общее количество заданий в уроке.
     */
    public int getTotalTasks() {
        return tasks != null ? tasks.size() : 0;
    }

    /**
     * Возвращает количество выполненных заданий.
     * Требует, чтобы TaskModel имел метод isCompleted().
     */
    public int getProgress() {
        if (tasks == null) {
            return 0;
        }
        int completedCount = 0;
        for (TaskModel task : tasks) {
            // Предполагается, что TaskModel имеет isCompleted()
            if (task != null && task.isCompleted()) {
                completedCount++;
            }
        }
        return completedCount;
    }

    /**
     * Возвращает статус завершения урока.
     * Он завершен, если:
     * 1. Репозиторий явно отметил его (isExplicitlyCompleted) ИЛИ
     * 2. Все задания выполнены.
     */
    public boolean isCompleted() {
        return this.isExplicitlyCompleted || (getTotalTasks() > 0 && getProgress() == getTotalTasks());
    }

    /**
     * Возвращает процент выполнения урока.
     */
    public int getProgressPercentage() {
        int total = getTotalTasks();
        int completed = getProgress();
        if (total == 0) return 0;
        // Улучшение: используем double для расчета, чтобы избежать потерь точности
        return (int) ((completed / (double) total) * 100);
    }

    /**
     * Возвращает задание по его индексу в списке.
     */
    public TaskModel getTaskByIndex(int index) {
        if (tasks != null && index >= 0 && index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }
}