package com.example.sql_game.data.model;

/**
 * Модель данных для одного задания/шага внутри урока.
 * Содержит всю необходимую информацию для отображения задания и его проверки.
 */
public class TaskModel {

    /**
     * Тип задания: Теория или Практика.
     */
    public enum TaskType {
        THEORY,
        PRACTICE
    }

    private int taskId;              // Глобальный ID задания (из БД)
    private int lessonTaskId;        // Уникальный ID задания в контексте урока
    private TaskType type;           // Тип задания
    private String instruction;      // Инструкция или описание задания (главный заголовок)
    private String theoryContent;    // Содержание теории (для THEORY, соответствует lesson_description)
    private String syntaxExample;    // Пример синтаксиса (для THEORY, соответствует text_syntax_example)
    private String expectedResult;   // Ожидаемый SQL-запрос для проверки
    private String databaseSetupSql; // SQL-команды для настройки тестовой базы данных

    // НОВОЕ ПОЛЕ: Имя таблицы, данные которой нужно отобразить в блоке "Исходные данные"
    private String targetTableName;

    private String hint;             // Опциональная подсказка
    private int crystalReward;       // Награда за успешное выполнение
    private boolean isCompleted;     // Статус выполнения задания
    private String initialCode;      // Начальный SQL код для предзаполнения редактора

    /**
     * Обязательный публичный конструктор без аргументов.
     * Требуется для корректной десериализации (например, Firebase, Gson).
     */
    public TaskModel() {}

    /**
     * Полный конструктор для создания экземпляра TaskModel с явным указанием всех полей.
     */
    public TaskModel(
            int taskId,
            int lessonTaskId,
            TaskType type,
            String instruction,
            String theoryContent,
            String syntaxExample,
            String expectedResult,
            String databaseSetupSql,
            String targetTableName, // Добавлено в конструктор
            String hint,
            int crystalReward,
            boolean isCompleted,
            String initialCode) {
        this.taskId = taskId;
        this.lessonTaskId = lessonTaskId;
        this.type = type;
        this.instruction = instruction;
        this.theoryContent = theoryContent;
        this.syntaxExample = syntaxExample;
        this.expectedResult = expectedResult;
        this.databaseSetupSql = databaseSetupSql;
        this.targetTableName = targetTableName; // Инициализация нового поля
        this.hint = hint;
        this.crystalReward = crystalReward;
        this.isCompleted = isCompleted;
        this.initialCode = initialCode;
    }

    /**
     * Конструктор, устанавливающий isCompleted = false по умолчанию.
     */
    public TaskModel(
            int taskId,
            int lessonTaskId,
            TaskType type,
            String instruction,
            String theoryContent,
            String syntaxExample,
            String expectedResult,
            String databaseSetupSql,
            String targetTableName, // Добавлено в конструктор
            String hint,
            int crystalReward,
            String initialCode) {
        // Делегирование полному конструктору: isCompleted = false
        this(taskId, lessonTaskId, type, instruction, theoryContent, syntaxExample, expectedResult, databaseSetupSql, targetTableName, hint, crystalReward, false, initialCode);
    }

    /**
     * Удобный конструктор для создания нового задания с минимальным набором полей.
     * lessonTaskId устанавливается равным taskId, а isCompleted = false по умолчанию.
     */
    public TaskModel(
            int taskId,
            TaskType type,
            String instruction,
            String theoryContent,
            String syntaxExample,
            String expectedResult,
            String databaseSetupSql,
            String targetTableName, // Добавлено в конструктор
            String hint,
            int crystalReward,
            String initialCode) {
        // Делегирование полному конструктору: lessonTaskId = taskId, isCompleted = false
        this(taskId, taskId, type, instruction, theoryContent, syntaxExample, expectedResult, databaseSetupSql, targetTableName, hint, crystalReward, false, initialCode);
    }


    // --- ГЕТТЕРЫ И СЕТТЕРЫ ---

    // ... (Остальные геттеры и сеттеры)

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getLessonTaskId() {
        return lessonTaskId;
    }

    public void setLessonTaskId(int lessonTaskId) {
        this.lessonTaskId = lessonTaskId;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getTheoryContent() {
        return theoryContent;
    }

    public void setTheoryContent(String theoryContent) {
        this.theoryContent = theoryContent;
    }

    /**
     * Возвращает пример синтаксиса для теоретического задания.
     */
    public String getSyntaxExample() {
        return syntaxExample;
    }

    /**
     * Устанавливает пример синтаксиса.
     */
    public void setSyntaxExample(String syntaxExample) {
        this.syntaxExample = syntaxExample;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getDatabaseSetupSql() {
        return databaseSetupSql;
    }

    public void setDatabaseSetupSql(String databaseSetupSql) {
        this.databaseSetupSql = databaseSetupSql;
    }

    // НОВЫЙ ГЕТТЕР
    /**
     * Возвращает имя таблицы, с которой работает практическое задание.
     */
    public String getTargetTableName() {
        return targetTableName;
    }

    // НОВЫЙ СЕТТЕР
    /**
     * Устанавливает имя таблицы, с которой работает практическое задание.
     */
    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getCrystalReward() {
        return crystalReward;
    }

    public void setCrystalReward(int crystalReward) {
        this.crystalReward = crystalReward;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getInitialCode() {
        return initialCode;
    }

    public void setInitialCode(String initialCode) {
        this.initialCode = initialCode;
    }

    @Override
    public String toString() {
        return "TaskModel{" +
                "taskId=" + taskId +
                ", lessonTaskId=" + lessonTaskId +
                ", type=" + type +
                ", instruction='" + instruction + '\'' +
                ", isCompleted=" + isCompleted +
                ", targetTableName='" + targetTableName + '\'' + // Добавлено в toString
                '}';
    }
}