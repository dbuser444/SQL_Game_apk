package com.example.sql_game.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.model.TaskModel;
import com.example.sql_game.data.repository.LessonRepository;
import com.example.sql_game.data.repository.UserRepository;
import com.example.sql_game.util.SqlExecutor;
import com.example.sql_game.util.SqlExecutor.ExecutionResult;

import java.util.List;

/**
 * ViewModel для управления логикой выполнения урока и заданий.
 * Содержит текущее задание, обрабатывает проверку SQL и сохраняет прогресс.
 */
public class LessonPlayViewModel extends ViewModel {

    /**
     * Энум для отслеживания текущего режима отображения UI:
     * THEORY_VIEW: Показать блок с теорией (текст).
     * PRACTICE_VIEW: Показать редактор SQL и таблицу результатов.
     * LESSON_COMPLETED: Показать экран завершения урока.
     */
    public enum UIMode {
        THEORY_VIEW,
        PRACTICE_VIEW,
        LESSON_COMPLETED
    }

    private final LessonRepository lessonRepository = LessonRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final SqlExecutor sqlExecutor;

    // --- LiveData для UI и данных ---
    private final MutableLiveData<TaskModel> currentTask = new MutableLiveData<>();
    private final MutableLiveData<String> executionMessage = new MutableLiveData<>();
    private final MutableLiveData<ExecutionResult> executionResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<UIMode> uiMode = new MutableLiveData<>();

    // LiveData для исходной таблицы
    private final MutableLiveData<ExecutionResult> initialTableData = new MutableLiveData<>();

    // --- Локальное состояние урока ---
    private LessonModel currentLesson;
    private int currentTaskIndex = 0;
    private boolean isLessonLoaded = false;
    private String currentLessonId = null; // Добавлено для отслеживания текущего ID

    public LessonPlayViewModel() {
        sqlExecutor = new SqlExecutor();
    }

    /**
     * Загружает данные урока и инициализирует первое задание.
     * ЭТОТ МЕТОД ГАРАНТИРУЕТ, что новый урок будет загружен при смене ID.
     * @param lessonId ID урока, который нужно загрузить.
     */
    public void loadLesson(String lessonId) {
        // Проверяем, пытаемся ли мы загрузить тот же урок, который уже загружен.
        if (isLessonLoaded && lessonId.equals(currentLessonId)) {
            // Если да, просто выходим, чтобы избежать ненужной перезагрузки и сброса состояния
            return;
        }

        // --- ЛОГИКА СБРОСА И ПЕРЕЗАГРУЗКИ ---

        // 1. Сбрасываем внутреннее состояние для нового урока
        isLessonLoaded = false;
        currentTaskIndex = 0;
        currentLessonId = lessonId; // Устанавливаем новый ID

        // 2. Очищаем ресурсы БД, так как начинается новый урок
        sqlExecutor.closeDatabase();

        // 3. Загрузка новых данных урока
        currentLesson = lessonRepository.getLessonById(lessonId);

        if (currentLesson == null || currentLesson.getTasks() == null || currentLesson.getTasks().isEmpty()) {
            executionMessage.setValue("Ошибка: Урок не найден или пуст.");
            uiMode.setValue(UIMode.LESSON_COMPLETED);
            return;
        }

        isLessonLoaded = true;
        updateCurrentTask(currentTaskIndex);
    }

    /**
     * Обновляет LiveData текущего задания, настраивает БД и устанавливает UI режим.
     */
    private void updateCurrentTask(int index) {
        if (currentLesson == null || currentLesson.getTasks() == null) return;

        if (index < 0 || index >= currentLesson.getTasks().size()) {
            // Выход за границы: достигнут конец списка заданий.

            if (isLessonTrulyCompleted()) {
                // *** ИСПРАВЛЕНИЕ: Отмечаем весь урок как завершенный ***
                if (currentLesson != null) {
                    // Используем currentLessonId для сохранения статуса
                    lessonRepository.markLessonCompleted(currentLessonId);
                    // ВАЖНО: Убедитесь, что LessonRepository.markLessonCompleted()
                    // сохраняет статус в вашем постоянном хранилище данных (например, БД).
                }
                // *******************************************************

                uiMode.setValue(UIMode.LESSON_COMPLETED);
            } else {
                currentTaskIndex = currentLesson.getTasks().size() - 1;
                updateCurrentTask(currentTaskIndex);
            }
            return;
        }

        currentTaskIndex = index;
        TaskModel task = currentLesson.getTasks().get(index);

        currentTask.setValue(task);

        // 1. КЛЮЧЕВАЯ ЛОГИКА: Определение режима UI на основе TaskType
        if (task.getType() == TaskModel.TaskType.THEORY) {
            uiMode.setValue(UIMode.THEORY_VIEW);
        } else if (task.getType() == TaskModel.TaskType.PRACTICE) {
            uiMode.setValue(UIMode.PRACTICE_VIEW);
        }

        // 2. Очищаем старые результаты и сообщения
        executionResultLiveData.setValue(null);
        executionMessage.setValue(null);
        initialTableData.setValue(null); // Сбрасываем старую таблицу

        // 3. Настраиваем базу данных для этого задания
        String setupSql = task.getDatabaseSetupSql();
        if (setupSql != null && !setupSql.isEmpty()) {
            // Выполняем скрипт настройки (CREATE TABLE, INSERT INTO)
            ExecutionResult setupResult = sqlExecutor.executeSetup(setupSql);
            if (!setupResult.isSuccess) {
                executionMessage.setValue("Ошибка настройки БД: " + setupResult.errorMessage);
            }
        }

        // 4. ЗАГРУЖАЕМ ИСХОДНЫЕ ДАННЫЕ ТАБЛИЦЫ
        loadInitialTableData(task);
    }

    /**
     * Загружает исходные данные таблицы для текущего задания, если оно практическое.
     * Используется для отображения исходного состояния БД.
     */
    private void loadInitialTableData(TaskModel task) {
        if (task.getType() == TaskModel.TaskType.PRACTICE) {
            String tableName = task.getTargetTableName(); // Предполагаем, что TaskModel имеет геттер для имени таблицы
            if (tableName != null && !tableName.isEmpty()) {
                // Выполняем простой SELECT * для отображения исходных данных
                ExecutionResult initialData = sqlExecutor.executeQuery("SELECT * FROM " + tableName + ";");
                initialTableData.setValue(initialData);
            } else {
                executionMessage.setValue("Предупреждение: Не указано имя целевой таблицы для задания.");
            }
        }
    }

    /**
     * Проверяет, выполнены ли все задания в текущем уроке.
     * @return true, если все задания выполнены, иначе false.
     */
    private boolean isLessonTrulyCompleted() {
        if (currentLesson == null || currentLesson.getTasks() == null) return false;
        for (TaskModel task : currentLesson.getTasks()) {
            if (!task.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Переход к следующему заданию.
     */
    public void moveToNextTask() {
        TaskModel completedTask = currentTask.getValue();
        if (completedTask != null && completedTask.getType() == TaskModel.TaskType.THEORY) {
            // Отмечаем теорию как выполненную (это важно для прогресса)
            if (!completedTask.isCompleted()) {
                completedTask.setCompleted(true);
                lessonRepository.saveTaskStatus(completedTask);
            }
        }

        // Находим следующее невыполненное задание
        int nextIndex = currentTaskIndex + 1;
        while (nextIndex < currentLesson.getTasks().size() && currentLesson.getTasks().get(nextIndex).isCompleted()) {
            // Пропускаем уже выполненные задания
            nextIndex++;
        }

        updateCurrentTask(nextIndex);
    }

    /**
     * Проверяет SQL-запрос пользователя.
     * @param userQuery SQL-запрос, введенный пользователем.
     * @return ExecutionResult с результатами или ошибкой.
     */
    public ExecutionResult checkQuery(String userQuery) {
        TaskModel task = currentTask.getValue();
        if (task == null) {
            ExecutionResult result = new ExecutionResult("Нет активного задания.");
            executionMessage.setValue(result.errorMessage);
            return result;
        }

        if (task.getType() == TaskModel.TaskType.THEORY) {
            ExecutionResult result = new ExecutionResult("Это теоретическое задание. Нажмите 'Далее'.");
            executionMessage.setValue(result.errorMessage);
            return result;
        }

        // Если задание уже выполнено, просто возвращаем старый результат и сообщение
        if (task.isCompleted()) {
            executionMessage.setValue("Задание уже выполнено. Нажмите 'Далее' или попробуйте другой запрос.");
            // Выполняем запрос пользователя, чтобы показать результат
            ExecutionResult userResult = sqlExecutor.executeQuery(userQuery);
            executionResultLiveData.setValue(userResult);
            return userResult;
        }

        // 1. Выполняем запрос пользователя
        ExecutionResult userResult = sqlExecutor.executeQuery(userQuery);
        executionResultLiveData.setValue(userResult);

        if (!userResult.isSuccess) {
            executionMessage.setValue(userResult.errorMessage);
            return userResult;
        }

        // 2. Выполняем ожидаемый запрос для сравнения
        ExecutionResult expectedResult = sqlExecutor.executeQuery(task.getExpectedResult());

        if (!expectedResult.isSuccess) {
            executionMessage.setValue("Внутренняя ошибка: Не удалось выполнить ожидаемый запрос.");
            return expectedResult;
        }

        // 3. Сравниваем результаты
        if (compareResults(userResult, expectedResult)) {
            // Успех
            boolean wasNewlyCompleted = false;

            if (!task.isCompleted()) {
                task.setCompleted(true);
                lessonRepository.saveTaskStatus(task);
                // Начисляем кристаллы
                userRepository.addCrystals(task.getCrystalReward());
                wasNewlyCompleted = true;
            }

            // Показываем награду, только если задание было выполнено впервые.
            if (wasNewlyCompleted) {
                executionMessage.setValue("Успех! Задание выполнено. Награда: +" + task.getCrystalReward() + " кристаллов.");
            } else {
                executionMessage.setValue("Успех! Задание уже было выполнено. Нажмите 'Далее'.");
            }

        } else {
            // Неправильный результат
            executionMessage.setValue("Неправильный результат! Ваш запрос вернул не те данные, которые ожидались.");
        }

        return userResult;
    }

    /**
     * Вспомогательный метод для сравнения результатов.
     */
    private boolean compareResults(ExecutionResult userResult, ExecutionResult expectedResult) {
        // Сравнение заголовков (имен столбцов)
        if (!userResult.resultColumns.equals(expectedResult.resultColumns)) {
            return false;
        }

        // Сравнение данных
        List<List<String>> userData = userResult.resultData;
        List<List<String>> expectedData = expectedResult.resultData;

        if (userData.size() != expectedData.size()) {
            return false;
        }

        // Сравнение порядка и содержимого строк
        return userData.equals(expectedData);
    }

    // --- ГЕТТЕРЫ ДЛЯ LIVE DATA И СОСТОЯНИЯ ---

    // Геттер для LiveData исходных данных
    public LiveData<ExecutionResult> getInitialTableData() {
        return initialTableData;
    }

    public LiveData<TaskModel> getCurrentTask() {
        return currentTask;
    }

    public LiveData<String> getExecutionMessage() {
        return executionMessage;
    }

    public LiveData<ExecutionResult> getExecutionResultLiveData() {
        return executionResultLiveData;
    }

    public LiveData<UIMode> getUiMode() {
        return uiMode;
    }

    public LessonModel getCurrentLesson() {
        return currentLesson;
    }

    public int getCurrentTaskIndex() {
        return currentTaskIndex;
    }

    /**
     * Геттер для SqlExecutor, необходимый Activity для отображения исходной таблицы.
     */
    public SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Освобождаем ресурсы, связанные с базой данных
        sqlExecutor.closeDatabase();
    }
}