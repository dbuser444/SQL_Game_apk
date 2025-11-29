package com.example.sql_game;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.model.TaskModel;
import com.example.sql_game.data.model.TaskModel.TaskType;
import com.example.sql_game.data.repository.LessonRepository;
import com.example.sql_game.ui.adapter.TableDataAdapter;
import com.example.sql_game.ui.adapter.TaskPromptAdapter;
import com.example.sql_game.util.SqlExecutor;
import com.example.sql_game.util.SqlExecutor.ExecutionResult;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для управления игровым прогрессом, использует SharedPreferences для сохранения.
 * Хранит количество кристаллов, опыт (XP) и статус завершения уроков.
 */
class GameProgressManager {
    private final SharedPreferences prefs;
    private static final String PREFS_FILE = "SQL_GAME_PROGRESS";
    private static final String KEY_CRYSTALS = "CRYSTAL_COUNT";
    private static final String KEY_XP = "XP_COUNT";
    private static final String KEY_LESSON_PREFIX = "LESSON_COMPLETED_";

    public GameProgressManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        Log.d("ProgressManager", "Менеджер инициализирован. Кристаллов: " + getCrystalCount() +
                ", Опыта (XP): " + getXPCount());
    }

    /**
     * Начисляет кристаллы и опыт (XP) **одним атомарным обновлением**.
     * Использует commit() для немедленного синхронного сохранения.
     */
    public void addRewards(int crystalAmount, int xpAmount) {
        int currentCrystals = getCrystalCount();
        int newCrystals = currentCrystals + crystalAmount;

        int currentXP = getXPCount();
        int newXP = currentXP + xpAmount;

        // КРИТИЧЕСКОЕ ИЗМЕНЕНИЕ: Используем commit() для немедленного сохранения.
        boolean saved = prefs.edit()
                .putInt(KEY_CRYSTALS, newCrystals)
                .putInt(KEY_XP, newXP)
                .commit();

        // Проверка сразу после сохранения
        if (saved) {
            Log.i("ProgressManager",
                    String.format("СОХРАНЕНИЕ УСПЕШНО (commit). Кристаллов: +%d (Всего: %d), Опыта: +%d (Всего: %d)",
                            crystalAmount, newCrystals, xpAmount, newXP));
            // Дополнительная проверка: читаем только что сохраненное значение
            Log.i("ProgressManager",
                    String.format("ПРОВЕРКА ЧТЕНИЯ: Текущие кристаллы: %d, Текущий XP: %d",
                            getCrystalCount(), getXPCount()));
        } else {
            Log.e("ProgressManager", "СОХРАНЕНИЕ НЕ УДАЛОСЬ (commit вернул false).");
        }
    }

    /**
     * Возвращает текущее количество кристаллов.
     */
    public int getCrystalCount() {
        return prefs.getInt(KEY_CRYSTALS, 0);
    }

    /**
     * Возвращает текущее количество опыта (XP).
     */
    public int getXPCount() {
        return prefs.getInt(KEY_XP, 0);
    }


    /**
     * Отмечает урок как завершенный.
     */
    public void advanceLessonProgress(String lessonId) {
        String key = KEY_LESSON_PREFIX + lessonId;
        prefs.edit().putBoolean(key, true).apply();
        Log.i("ProgressManager", "Урок " + lessonId + " отмечен как завершенный.");
    }

    /**
     * Проверяет, завершен ли урок.
     */
    public boolean isLessonCompleted(String lessonId) {
        String key = KEY_LESSON_PREFIX + lessonId;
        return prefs.getBoolean(key, false);
    }
}


/**
 * Основная активность для прохождения уроков.
 * Здесь происходит отображение заданий, ввод и выполнение SQL-запросов.
 */
public class LessonPlayActivity extends AppCompatActivity implements TaskPromptAdapter.OnTaskClickListener {

    private static final String TAG = "LessonPlayActivity";
    // Награда за задание: 25 кристаллов и 50 XP
    private static final int CRYSTAL_REWARD_PER_TASK = 10;
    private static final int XP_REWARD_PER_TASK = 10;

    // Бонус за урок
    private static final int CRYSTAL_BONUS_FOR_LESSON = 50;
    private static final int XP_BONUS_FOR_LESSON = 100;

    // --- Общие элементы UI ---
    private TextView lessonTitle;
    private Button checkButton;
    private LinearLayout statusLayout;
    private TextView statusMessage;
    private TextView resultTitle;
    private RecyclerView resultTable;

    // --- Контейнеры для переключения режимов ---
    private LinearLayout theoryContainer;
    private LinearLayout practiceContainer;

    // --- Элементы ТЕОРИИ ---
    private TextView lessonDescription;
    private TextView textSyntaxExample;

    // --- Элементы ПРАКТИКИ ---
    private RecyclerView recyclerViewTasks;
    private TextView sourceTableTitle;
    private RecyclerView recyclerViewSourceTable;
    private EditText sqlInput;
    private Button buttonExecuteQuery;
    private Button buttonHint;

    // Ресурсы данных
    private LessonModel currentLesson;
    private final LessonRepository repository = LessonRepository.getInstance();
    private final SqlExecutor sqlExecutor = new SqlExecutor();
    private TaskPromptAdapter taskAdapter;
    private TaskModel currentTask;

    public static final String EXTRA_LESSON_ID = "lessonId";

    private GameProgressManager progressManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_play);

        // Инициализация GameProgressManager с контекстом
        progressManager = new GameProgressManager(getApplicationContext());

        initViews();
        setupTasksRecyclerView();
        updateToolbarStats(); // Обновляем статистику при старте

        String lessonId = getIntent().getStringExtra(EXTRA_LESSON_ID);
        if (lessonId != null) {
            loadLessonData(lessonId);
        } else {
            Toast.makeText(this, "Ошибка: ID урока не найден.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Обновляет статистику пользователя.
     * Использует Toast для визуального подтверждения начисления.
     */
    private void updateToolbarStats() {
        int crystals = progressManager.getCrystalCount();
        int xp = progressManager.getXPCount();
        Log.i(TAG, "Статистика пользователя обновлена. Кристаллы: " + crystals + ", XP: " + xp);

        // Визуальное подтверждение изменения XP/Кристаллов
        // Toast.makeText(this, "Кристаллы: " + crystals + " | XP: " + xp, Toast.LENGTH_SHORT).show();
    }

    /**
     * Инициализация всех UI элементов по ID из макета.
     */
    private void initViews() {
        // Общие элементы
        lessonTitle = findViewById(R.id.lesson_title);
        statusMessage = findViewById(R.id.status_message);
        statusLayout = findViewById(R.id.status_layout);
        resultTitle = findViewById(R.id.text_result_title);
        resultTable = findViewById(R.id.result_table);
        checkButton = findViewById(R.id.check_button);

        // Контейнеры
        theoryContainer = findViewById(R.id.theory_container);
        practiceContainer = findViewById(R.id.practice_container);

        // Элементы Теории
        lessonDescription = findViewById(R.id.lesson_description);
        textSyntaxExample = findViewById(R.id.text_syntax_example);

        // Элементы Практики
        recyclerViewTasks = findViewById(R.id.recycler_view_tasks);
        recyclerViewSourceTable = findViewById(R.id.recycler_view_source_table);
        sourceTableTitle = findViewById(R.id.text_source_table_title);
        sqlInput = findViewById(R.id.sql_input);
        buttonExecuteQuery = findViewById(R.id.button_execute_query);
        buttonHint = findViewById(R.id.button_hint);

        // Обработчики кликов
        buttonExecuteQuery.setOnClickListener(v -> executeUserQuery());
        buttonHint.setOnClickListener(v -> showHint());

        // Кнопка Назад из Тулбара
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
    }

    /**
     * Настройка RecyclerView для списка заданий.
     */
    private void setupTasksRecyclerView() {
        taskAdapter = new TaskPromptAdapter(this);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    /**
     * Загрузка данных урока и инициализация первого задания.
     */
    private void loadLessonData(String lessonId) {
        currentLesson = repository.getLessonById(lessonId);
        if (currentLesson == null) {
            Toast.makeText(this, "Ошибка: Урок не найден.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        lessonTitle.setText(currentLesson.getTitle());

        List<TaskModel> tasks = currentLesson.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            Toast.makeText(this, "Ошибка: В уроке нет заданий.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        taskAdapter.submitList(tasks);

        int initialTaskIndex = findFirstUncompletedTask(tasks);
        selectTask(initialTaskIndex);
    }

    private int findFirstUncompletedTask(List<TaskModel> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            if (!tasks.get(i).isCompleted()) {
                return i;
            }
        }
        return 0; // Если все выполнены, возвращаем первое
    }

    /**
     * Выбирает и отображает данные задания по его индексу.
     */
    private void selectTask(int index) {
        if (currentLesson == null || index < 0 || index >= currentLesson.getTasks().size()) {
            return;
        }

        currentTask = currentLesson.getTasks().get(index);
        taskAdapter.setCurrentActiveTaskIndex(index);

        Log.d(TAG, "Выбрано задание: " + currentTask.getInstruction() +
                ", Тип: " + currentTask.getType().name());


        // 1. Настройка базы данных для текущего задания
        String setupSql = currentTask.getDatabaseSetupSql();
        setupDatabase(setupSql);

        // 2. Обновление UI в зависимости от типа задания
        updateTaskUI(currentTask);

        // 3. Обновление редактора (начальный код)
        if (currentTask.getType() == TaskType.PRACTICE) {
            sqlInput.setText(currentTask.getInitialCode() != null ? currentTask.getInitialCode() : "");
            sqlInput.setSelection(sqlInput.getText().length());

            // 4. Загрузка и отображение исходной таблицы
            loadInitialTableData(setupSql);
        }

        // 5. Сброс результатов
        resetStatusAndResults();

        // 6. Обновление заголовка задания
        lessonTitle.setText(currentTask.getInstruction());

        // 7. Проверка, является ли текущее задание выполненным, и настройка кнопки навигации
        if (currentTask.isCompleted()) {
            setupNavigationButtonAfterCompletion(currentTask);
        } else if (currentTask.getType() == TaskType.THEORY) {
            checkButton.setText("Перейти к следующему заданию");
            checkButton.setVisibility(View.VISIBLE);
            checkButton.setOnClickListener(v -> handleTheoryNextClick());
        } else {
            checkButton.setVisibility(View.GONE);
        }
    }

    /**
     * Обновляет все элементы UI в зависимости от типа задания (Теория/Практика).
     */
    private void updateTaskUI(TaskModel task) {
        if (task.getType() == TaskType.THEORY) {
            // ===================================
            //         РЕЖИМ ТЕОРИИ
            // ===================================
            theoryContainer.setVisibility(View.VISIBLE);
            practiceContainer.setVisibility(View.GONE);
            recyclerViewSourceTable.setVisibility(View.GONE);
            if (sourceTableTitle != null) {
                sourceTableTitle.setVisibility(View.GONE);
            }

            // Заполняем теоретический контент
            lessonDescription.setText(task.getTheoryContent() != null ? task.getTheoryContent() : "Нет описания.");
            textSyntaxExample.setText(task.getSyntaxExample() != null ? task.getSyntaxExample() : "Нет примера синтаксиса.");

        } else {
            // ===================================
            //         РЕЖИМ ПРАКТИКИ
            // ===================================
            theoryContainer.setVisibility(View.GONE);
            practiceContainer.setVisibility(View.VISIBLE);
            if (sourceTableTitle != null) {
                sourceTableTitle.setVisibility(View.VISIBLE);
            }

            // Кнопка навигации скрыта до выполнения задания
            checkButton.setVisibility(View.GONE);

            // Управление кнопкой подсказки
            buttonHint.setEnabled(task.getHint() != null && !task.getHint().isEmpty());

            // Включаем кнопку выполнения запроса
            buttonExecuteQuery.setEnabled(true);

            // Настройка LayoutManager для исходной таблицы
            recyclerViewSourceTable.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        }
    }


    /**
     * Сбрасывает область статуса и результатов.
     */
    private void resetStatusAndResults() {
        statusLayout.setVisibility(View.GONE);
        statusMessage.setText("Введите запрос и нажмите 'Выполнить'.");
        resultTitle.setVisibility(View.GONE);
        clearResultsTable();
    }

    /**
     * Настраивает in-memory базу данных с помощью скрипта задания.
     */
    private void setupDatabase(String setupSql) {
        if (setupSql == null) return;
        ExecutionResult setupResult = sqlExecutor.executeSetup(setupSql);
        if (!setupResult.isSuccess) {
            Log.e(TAG, "Настройка БД не удалась: " + setupResult.errorMessage);
        } else {
            Log.d(TAG, "Настройка БД прошла успешно.");
        }
    }

    /**
     * Загружает данные исходной таблицы и отображает их.
     */
    private void loadInitialTableData(String setupSql) {
        // 1. Извлекаем имя таблицы из SQL-скрипта настройки
        String tableName = extractTableNameFromSetup(setupSql);

        // 2. Обновляем заголовок с именем таблицы
        if (sourceTableTitle != null) {
            String titleText = "Исходные данные";
            if (tableName != null) {
                titleText = "Исходные данные (Текущая таблица - \"" + tableName + "\")";
            }
            sourceTableTitle.setText(titleText);
        }

        if (tableName == null) {
            Log.e(TAG, "Не удалось извлечь имя таблицы из setup SQL. Отображаю пустую таблицу.");
            displaySourceTable(java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return;
        }

        // 3. Выполняем SELECT * на всю таблицу
        String initialQuery = "SELECT * FROM " + tableName.trim() + ";";
        ExecutionResult result = sqlExecutor.executeQuery(initialQuery);

        if (result.isSuccess && !result.resultData.isEmpty()) {
            displaySourceTable(result.resultColumns, result.resultData);
        } else {
            // Если SELECT не сработал или таблица пуста, выводим ошибку и пустую таблицу
            Log.e(TAG, "Ошибка загрузки исходной таблицы. Запрос: " + initialQuery +
                    ", Ошибка: " + (result.errorMessage != null ? result.errorMessage : "Нет данных"));
            displaySourceTable(java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }
    }

    /**
     * Попытка извлечь первое имя таблицы после CREATE TABLE из скрипта.
     */
    private String extractTableNameFromSetup(String setupSql) {
        if (setupSql == null || setupSql.isEmpty()) {
            Log.w(TAG, "Setup SQL пуст, не удается извлечь имя таблицы.");
            return null;
        }

        Log.d(TAG, "Попытка извлечь имя таблицы из SQL...");

        // Регулярное выражение для поиска 'CREATE TABLE [IF NOT EXISTS] <Имя_Таблицы>'
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\"'`\\[]?[\\p{L}0-9_]+[\"`'\\]]?)\\s*?\\(",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(setupSql);

        if (matcher.find()) {
            String rawTableName = matcher.group(1).trim();
            Log.d(TAG, "Извлечено имя таблицы: " + rawTableName);

            // Очищаем имя от возможных кавычек или скобок
            if (rawTableName.startsWith("\"") && rawTableName.endsWith("\"") ||
                    rawTableName.startsWith("'") && rawTableName.endsWith("'") ||
                    rawTableName.startsWith("`") && rawTableName.endsWith("`")) {
                return rawTableName.substring(1, rawTableName.length() - 1);
            } else if (rawTableName.startsWith("[") && rawTableName.endsWith("]")) {
                return rawTableName.substring(1, rawTableName.length() - 1);
            }

            return rawTableName;
        }

        Log.w(TAG, "Имя таблицы не найдено с помощью regex.");
        return null;
    }

    /**
     * Обработка клика по заданию в списке.
     */
    @Override
    public void onTaskClick(TaskModel task) {
        int index = currentLesson.getTasks().indexOf(task);
        if (index != -1) {
            selectTask(index);
        }
    }

    /**
     * Выполняет SQL-запрос, введенный пользователем, и отображает результат.
     */
    private void executeUserQuery() {
        if (currentTask == null || currentTask.getType() != TaskType.PRACTICE) {
            Toast.makeText(this, "Нет активного задания для практики.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userQuery = sqlInput.getText().toString().trim();
        if (userQuery.isEmpty()) {
            Toast.makeText(this, "Введите SQL-запрос.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Выполняем запрос
        ExecutionResult result = sqlExecutor.executeQuery(userQuery);
        displayExecutionResult(result);

        // Если запрос был успешен и это SELECT, переходим к проверке
        if (result.isSuccess && userQuery.toUpperCase().startsWith("SELECT")) {
            checkUserQuery(userQuery, result.resultColumns, result.resultData);
        }
    }

    /**
     * Отображает результат выполнения запроса (успех/ошибка и данные).
     */
    private void displayExecutionResult(ExecutionResult result) {
        // Предполагается, что эти цвета определены в resources
        int colorSuccess = ContextCompat.getColor(this, R.color.color_success);
        int colorError = ContextCompat.getColor(this, R.color.colorError);
        int colorInfo = ContextCompat.getColor(this, R.color.primary_dark);

        statusLayout.setVisibility(View.VISIBLE);
        resultTitle.setVisibility(View.GONE);
        clearResultsTable();

        if (result.isSuccess) {
            if (result.resultColumns != null && !result.resultColumns.isEmpty()) {
                // Успешный SELECT запрос
                statusMessage.setText(getString(R.string.query_result_success_rows, result.resultData.size()));
                statusLayout.setBackgroundColor(colorSuccess);
                updateTableDisplay(resultTable, result.resultColumns, result.resultData);
                resultTitle.setVisibility(View.VISIBLE); // Показываем заголовок "Результат"
            } else {
                // Успешная DML/DDL команда
                statusMessage.setText(result.errorMessage != null ? result.errorMessage : "Запрос успешно выполнен.");
                statusLayout.setBackgroundColor(colorInfo);
            }
        } else {
            // Ошибка выполнения
            statusMessage.setText(getString(R.string.query_result_error, result.errorMessage));
            statusLayout.setBackgroundColor(colorError);
        }
    }

    /**
     * Универсальный метод для отображения данных в RecyclerView.
     */
    private void updateTableDisplay(RecyclerView targetRecyclerView, List<String> columns, List<List<String>> data) {
        TableDataAdapter dataAdapter = new TableDataAdapter();
        targetRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        dataAdapter.updateData(columns, data);

        targetRecyclerView.setAdapter(dataAdapter);
        targetRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Отображает данные в recyclerViewSourceTable для исходных данных.
     */
    private void displaySourceTable(List<String> columns, List<List<String>> data) {
        if (columns == null || columns.isEmpty() || data.isEmpty()) {
            recyclerViewSourceTable.setVisibility(View.GONE);
            return;
        }

        updateTableDisplay(recyclerViewSourceTable, columns, data);
        recyclerViewSourceTable.setVisibility(View.VISIBLE);
    }

    /**
     * Очищает таблицу результатов.
     */
    private void clearResultsTable() {
        if (resultTable.getAdapter() instanceof TableDataAdapter) {
            ((TableDataAdapter) resultTable.getAdapter()).updateData(null, null);
        }
        resultTable.setVisibility(View.GONE);
    }


    private void checkUserQuery(String userQuery, List<String> userColumns, List<List<String>> userData) {
        String expectedQuery = currentTask.getExpectedResult();
        int crystalReward = currentTask.getCrystalReward();

        if (expectedQuery == null || expectedQuery.isEmpty()) {
            Log.w(TAG, "У задания нет ожидаемого результата. Проверка пропущена.");
            return;
        }

        ExecutionResult expectedResult = sqlExecutor.executeQuery(expectedQuery);

        if (!expectedResult.isSuccess) {
            Log.e(TAG, "Ожидаемый запрос не удалось выполнить: " + expectedResult.errorMessage);
            return;
        }

        boolean isCorrect = compareResults(userColumns, userData, expectedResult.resultColumns, expectedResult.resultData);

        int colorSuccess = ContextCompat.getColor(this, R.color.color_success);
        int colorError = ContextCompat.getColor(this, R.color.colorError);

        if (isCorrect) {
            // Задание выполнено!
            int actualCrystalReward = crystalReward > 0 ? crystalReward : CRYSTAL_REWARD_PER_TASK;
            int actualXpReward = XP_REWARD_PER_TASK;

            // 1. Обновление статуса и начисление награды
            handleTaskCompletion(currentTask, actualCrystalReward, actualXpReward);

            // 2. Обновление статуса UI: Правильный ответ
            statusMessage.setText(String.format("Задание выполнено верно! Награда: %d кристаллов, %d XP.",
                    actualCrystalReward, actualXpReward));
            statusLayout.setBackgroundColor(colorSuccess);

            // 3. Настройка кнопки навигации
            setupNavigationButtonAfterCompletion(currentTask);


        } else {
            // Задание не выполнено
            statusMessage.setText(getString(R.string.query_result_incorrect));
            statusLayout.setBackgroundColor(colorError);
            Log.d(TAG, "Запрос неверный. Пользовательских строк: " + userData.size() +
                    ", Ожидаемых строк: " + expectedResult.resultData.size());
        }
    }

    private boolean compareResults(List<String> userColumns, List<List<String>> userData,
                                   List<String> expectedColumns, List<List<String>> expectedData) {
        // Проверка заголовков
        if (!userColumns.equals(expectedColumns)) {
            Log.d(TAG, "Заголовки столбцов не совпадают.");
            return false;
        }

        // Проверка количества строк
        if (userData.size() != expectedData.size()) {
            Log.d(TAG, "Количество строк не совпадает.");
            return false;
        }

        // Проверка содержимого строк
        for (int i = 0; i < userData.size(); i++) {
            if (!userData.get(i).equals(expectedData.get(i))) {
                Log.d(TAG, "Содержимое строки не совпадает в индексе " + i);
                return false;
            }
        }

        return true;
    }


    /**
     * Обрабатывает завершение задания (обновляет статус, начисляет награду и XP).
     */
    private void handleTaskCompletion(TaskModel completedTask, int crystalReward, int xpReward) {
        // 1. Обновляем статус в модели и репозитории
        completedTask.setCompleted(true);
        repository.saveTaskStatus(completedTask);

        // 2. Начисление награды (объединенный вызов с commit)
        progressManager.addRewards(crystalReward, xpReward);

        // 3. Обновление UI статистики
        updateToolbarStats();

        // 4. Обновляем UI списка заданий
        taskAdapter.notifyItemChanged(currentLesson.getTasks().indexOf(completedTask));
    }

    /**
     * Настраивает универсальную кнопку навигации после завершения задания.
     */
    private void setupNavigationButtonAfterCompletion(TaskModel completedTask) {
        int nextIndex = currentLesson.getTasks().indexOf(completedTask) + 1;

        checkButton.setVisibility(View.VISIBLE);
        if (nextIndex < currentLesson.getTasks().size()) {
            checkButton.setText("Перейти к следующему заданию");
            checkButton.setOnClickListener(v -> selectTask(nextIndex));
        } else {
            // Последнее задание выполнено
            checkButton.setText("Завершить урок");
            checkButton.setOnClickListener(v -> handleFinishLessonClick());
        }
    }


    /**
     * Обрабатывает клик по кнопке "Закончить" (урок).
     */
    private void handleFinishLessonClick() {
        if (allTasksCompleted()) {
            // Начисляем бонус за урок
            progressManager.addRewards(CRYSTAL_BONUS_FOR_LESSON, XP_BONUS_FOR_LESSON);

            // Продвигаем уровень/прогресс (отмечаем урок как завершенный)
            progressManager.advanceLessonProgress(currentLesson.getId());
            updateToolbarStats(); // Обновление UI статистики после бонуса

            Toast.makeText(this, String.format("Урок завершен! Начислено %d бонусных кристаллов и %d XP.",
                    CRYSTAL_BONUS_FOR_LESSON, XP_BONUS_FOR_LESSON), Toast.LENGTH_LONG).show();

            finish();
        } else {
            Toast.makeText(this, "Необходимо выполнить все задания урока.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обрабатывает клик по кнопке "Далее" в режиме Теории.
     * Явно завершает текущее теоретическое задание перед переходом.
     */
    private void handleTheoryNextClick() {
        if (currentTask == null) return;

        if (currentTask.getType() == TaskType.THEORY && !currentTask.isCompleted()) {
            // Используем константы для награды
            int crystalReward = currentTask.getCrystalReward() > 0 ? currentTask.getCrystalReward() : CRYSTAL_REWARD_PER_TASK;
            int xpReward = XP_REWARD_PER_TASK;

            handleTaskCompletion(currentTask, crystalReward, xpReward);

            Toast.makeText(this, String.format("Теория пройдена! Награда: %d кристаллов, %d XP.",
                    crystalReward, xpReward), Toast.LENGTH_SHORT).show();
            // Сброс сообщений о статусе
            resetStatusAndResults();
        }

        // Навигация к следующему заданию
        int currentIndex = currentLesson.getTasks().indexOf(currentTask);
        int nextIndex = currentIndex + 1;

        if (nextIndex < currentLesson.getTasks().size()) {
            selectTask(nextIndex); // Переход к следующему заданию
        } else {
            // Если теоретическое задание было последним
            handleFinishLessonClick();
        }
    }

    /**
     * Обработка клика по кнопке "Получить подсказку".
     */
    private void showHint() {
        if (currentTask != null && currentTask.getHint() != null && !currentTask.getHint().isEmpty()) {
            Toast.makeText(this, "Подсказка: " + currentTask.getHint(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Для этого задания нет подсказки.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверяет, выполнены ли все задания в текущем уроке.
     */
    private boolean allTasksCompleted() {
        if (currentLesson == null || currentLesson.getTasks() == null) return false;
        for (TaskModel task : currentLesson.getTasks()) {
            if (!task.isCompleted()) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        sqlExecutor.closeDatabase();
    }
}