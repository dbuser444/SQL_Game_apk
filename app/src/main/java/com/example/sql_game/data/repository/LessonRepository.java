package com.example.sql_game.data.repository;

import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.model.TaskModel;
import com.example.sql_game.data.model.TaskModel.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// В реальном Android-проекте здесь должны быть импорты Firebase SDK:
// import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.firestore.DocumentReference;
// и т.д.

/**
 * Репозиторий для получения данных об уроках SQL с поддержкой
 * сохранения прогресса пользователя в постоянном хранилище (Firebase Firestore).
 * Использует шаблон Singleton.
 */
public class LessonRepository {

    // 1. Статический экземпляр класса (Singleton)
    private static LessonRepository instance;

    // 2. Статические данные уроков (Основной контент) - храним в Map для быстрого доступа
    private final Map<String, LessonModel> lessonMap;

    // 3. Динамические данные прогресса пользователя (Загружаются из Firebase)
    // Ключ: lessonId, Значение: true (завершено)
    private final Map<String, Boolean> completedLessonIds = new ConcurrentHashMap<>();

    // 4. Firebase (замените на реальный экземпляр в Android)
    // private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // --- Общие SQL-скрипты для урока 2 ---
    private static final String PRODUCTS_SETUP_SQL_CREATE = "CREATE TABLE Продукты (ID INTEGER, Название TEXT, Категория TEXT, Цена INTEGER, КоличествоНаСкладе INTEGER);";
    private static final String PRODUCTS_SETUP_SQL_INSERT = "INSERT INTO Продукты VALUES " +
            "(1, 'Ноутбук A', 'Электроника', 120000, 15), " +
            "(2, 'Мышь X', 'Электроника', 2500, 50), " +
            "(3, 'Стол Бюро', 'Мебель', 45000, 5), " +
            "(4, 'Кресло Офис', 'Мебель', 18000, 22), " +
            "(5, 'Монитор 4K', 'Электроника', 65000, 10);";
    private static final String PRODUCTS_FULL_SETUP_SQL = PRODUCTS_SETUP_SQL_CREATE + " " + PRODUCTS_SETUP_SQL_INSERT;

    // --- Общие SQL-скрипты для уроков 3, 4 и 5 ---
    private static final String EMPLOYEES_SETUP_SQL_CREATE = "CREATE TABLE Сотрудники (Id INTEGER PRIMARY KEY, Имя TEXT, Отдел TEXT, Зарплата INTEGER, Стаж INTEGER, Email TEXT);";
    private static final String EMPLOYEES_SETUP_SQL_INSERT = "INSERT INTO Сотрудники (Id, Имя, Отдел, Зарплата, Стаж, Email) VALUES " +
            "(1, 'Анна', 'Продажи', 55000, 3, 'anna@corp.com'), " +
            "(2, 'Борис', 'IT', 92000, 8, 'boris@corp.com'), " +
            "(3, 'Виктор', 'Продажи', 60000, 5, 'viktor@corp.com'), " +
            "(4, 'Галина', 'IT', 80000, 4, 'galina@corp.com'), " +
            "(5, 'Денис', 'Маркетинг', 45000, 2, NULL), " +
            "(6, 'Елена', 'IT', 105000, 10, 'elena@corp.com'), " +
            "(7, 'Андрей', 'IT', 95000, 7, 'andrei@corp.com')," +
            "(8, 'Никита', 'IT', 98000, 6, 'nikita@corp.com');"; // Добавлено 8 сотрудников
    private static final String EMPLOYEES_FULL_SETUP_SQL = EMPLOYEES_SETUP_SQL_CREATE + " " + EMPLOYEES_SETUP_SQL_INSERT;


    /**
     * Закрытый конструктор. Инициализирует жестко закодированные данные уроков.
     */
    private LessonRepository() {
        lessonMap = new HashMap<>();

        // Инициализация уроков (ваш статический контент)
        List<LessonModel> hardcodedLessons = Arrays.asList(
                // Урок 1: Введение в SELECT
                new LessonModel(
                        "1", // id
                        "Основы SELECT: Выборка данных", // title
                        "Изучите, как использовать оператор SELECT для получения данных из таблицы.", // description
                        0, // requiredExperience: Урок 1 всегда 0
                        // Список заданий (Tasks)
                        Arrays.asList(
                                new TaskModel(1, 1, TaskType.THEORY, "Введение в SQL и оператор SELECT", "Команда SELECT используется для извлечения данных из базы данных. Это начало любого взаимодействия с SQL. Без SELECT мы не можем увидеть, что хранится в таблицах. \n\n1. SELECT * : Выбирает все столбцы. Это удобно для быстрого просмотра, но неэффективно для больших систем. \n2. SELECT column1, column2 : Выбирает только указанные столбцы. Это улучшает производительность и уменьшает нагрузку.", "SELECT column1, column2 FROM table_name;", null, null, null, "SELECT - это сердце SQL.", 5, null),
                                new TaskModel(2, 2, TaskType.PRACTICE, "Выберите все данные", null, null, "SELECT id, имя, город FROM Клиенты;", "CREATE TABLE Клиенты (id INTEGER, имя TEXT, город TEXT); INSERT INTO Клиенты VALUES (1, 'Иван', 'Москва'), (2, 'Мария', 'СПб');", "Клиенты", "Используйте символ * или перечислите все столбцы.", 10, "SELECT "),
                                new TaskModel(3, 3, TaskType.PRACTICE, "Выберите только имена", null, null, "SELECT имя FROM Клиенты;", "CREATE TABLE Клиенты (id INTEGER, имя TEXT, город TEXT); INSERT INTO Клиенты VALUES (1, 'Иван', 'Москва'), (2, 'Мария', 'СПб');", "Клиенты", "Укажите имя нужного столбца.", 10, "SELECT ")
                        ),
                        // Общие поля LessonModel
                        "CREATE TABLE Клиенты (id INTEGER, имя TEXT, город TEXT);",
                        "SELECT id, имя, город FROM Клиенты;",
                        "SELECT "
                ),

                // Урок 2: Фильтрация WHERE
                new LessonModel(
                        "2", // id
                        "Фильтрация: Оператор WHERE и логика", // title
                        "Научитесь использовать WHERE для ограничения строк, а также комбинировать условия с AND и OR.", // description
                        0, // requiredExperience: *** ИЗМЕНЕНО с 50 на 0 для отладки ***
                        // Список заданий (Tasks)
                        Arrays.asList(
                                new TaskModel(4, 1, TaskType.THEORY, "Операторы сравнения в WHERE", "Оператор WHERE позволяет указать условие. Для проверки точного совпадения используется знак равенства (=). \n\nДля числовых полей можно использовать операторы сравнения: > (больше), < (меньше), >= (больше или равно), <= (меньше или равно). \n\nОператор != (или <>) используется для выбора строк, которые НЕ соответствуют заданному значению.", "SELECT * FROM table WHERE column > 100;", null, null, null, "Сравнение строк всегда в кавычках!", 5, null),
                                new TaskModel(5, 2, TaskType.PRACTICE, "Найдите дорогие товары", "Выберите только `Название` и `Цена` тех продуктов, чья цена (`Цена`) **больше** 50000.", null, "SELECT Название, Цена FROM Продукты WHERE Цена > 50000;", PRODUCTS_FULL_SETUP_SQL, "Продукты", "Используйте оператор `>`.", 15, "SELECT Название, Цена FROM Продукты "),
                                new TaskModel(6, 3, TaskType.PRACTICE, "Товары из категории 'Мебель'", "Выведите все столбцы (`*`) для продуктов, которые относятся к категории 'Мебель'.", null, "SELECT * FROM Продукты WHERE Категория = 'Мебель';", PRODUCTS_FULL_SETUP_SQL, "Продукты", "Не забудьте одинарные кавычки для текстовых значений.", 15, "SELECT * FROM Продукты "),
                                new TaskModel(7, 4, TaskType.THEORY, "Операторы AND и OR", "Операторы AND и OR в SQL — логические операторы, которые используются для соединения нескольких условий при фильтрации данных. \n\nОписание: «AND». Все условия должны быть выполнены одновременно. \nОписание: «OR». Достаточно, чтобы выполнено было хотя бы одно условие. ", "SELECT * FROM table WHERE condition1 AND condition2;", null, null, null, "AND более строгий, чем OR.", 5, null),
                                new TaskModel(8, 5, TaskType.PRACTICE, "Электроника и в наличии", "Выберите все продукты (`*`), которые являются 'Электроникой' **И** которых на складе (`КоличествоНаСкладе`) **больше** 10.", null, "SELECT * FROM Продукты WHERE Категория = 'Электроника' AND КоличествоНаСкладе > 10;", PRODUCTS_FULL_SETUP_SQL, "Продукты", "Вам нужно два условия, соединенных `AND`.", 20, "SELECT * FROM Продукты "),
                                new TaskModel(9, 6, TaskType.PRACTICE, "Остатки или VIP-товары", "Выберите все продукты (`*`), которые либо имеют цену **больше** 100000, **ЛИБО** имеют количество на складе **меньше** 10.", null, "SELECT * FROM Продукты WHERE Цена > 100000 OR КоличествоНаСкладе < 10;", PRODUCTS_FULL_SETUP_SQL, "Продукты", "Используйте `OR` для объединения двух не связанных условий.", 20, "SELECT * FROM Продукты ")
                        ),
                        // Общие поля LessonModel
                        PRODUCTS_SETUP_SQL_CREATE,
                        "SELECT * FROM Продукты WHERE Цена > 50000;",
                        "SELECT "
                ),

                // Урок 3: Расширенная Фильтрация
                new LessonModel(
                        "3", // id
                        "Расширенная Фильтрация: BETWEEN, IN, LIKE, NULL", // title
                        "Используйте `AND`, `OR`, `NOT`, `BETWEEN`, `IN`, `LIKE` и `IS NULL` для точного отбора данных.", // description
                        0, // requiredExperience
                        // Список заданий (Tasks)
                        Arrays.asList(
                                new TaskModel(10, 1, TaskType.THEORY, "BETWEEN и IN", "Оператор **BETWEEN** выбирает значения в заданном диапазоне (включительно). \nОператор **IN** выбирает значения из списка.\n\nПример BETWEEN: `WHERE Возраст BETWEEN 20 AND 30`.\nПример IN: `WHERE Отдел IN ('IT', 'Продажи')`.", "SELECT * FROM table WHERE column BETWEEN 10 AND 20;", null, null, null, "BETWEEN включает границы диапазона.", 5, null),
                                new TaskModel(11, 2, TaskType.PRACTICE, "Фильтрация по диапазону (BETWEEN)", "Выберите `Имя`, `Отдел` и `Стаж` сотрудников, чей стаж (`Стаж`) находится **между 5 и 8 годами** (включительно).", null, "SELECT Имя, Отдел, Стаж FROM Сотрудники WHERE Стаж BETWEEN 5 AND 8;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `BETWEEN Значение1 AND Значение2`.", 20, "SELECT Имя, Отдел, Стаж FROM Сотрудники "),
                                new TaskModel(12, 3, TaskType.PRACTICE, "Фильтрация по списку (IN)", "Выберите все столбцы (`*`) для сотрудников, работающих в отделах **'Продажи' или 'Маркетинг'**.", null, "SELECT * FROM Сотрудники WHERE Отдел IN ('Продажи', 'Маркетинг');", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `IN` для списка допустимых значений.", 20, "SELECT * FROM Сотрудники "),
                                new TaskModel(13, 4, TaskType.THEORY, "Операторы LIKE и NOT", "Оператор **LIKE** используется для поиска по шаблонам в текстовых данных.\nОператор **%** заменяет любое количество символов, \nОператор **_** заменяет один символ. \nОператор **NOT** инвертирует любое условие.", "SELECT * FROM table WHERE name LIKE 'A%';", null, null, null, "Не забывайте про `%` и `_` в LIKE.", 5, null),
                                new TaskModel(14, 5, TaskType.PRACTICE, "Поиск по шаблону (LIKE)", "Выберите `Имя` сотрудников, чье имя **начинается на букву 'А'** (`А%`).", null, "SELECT Имя FROM Сотрудники WHERE Имя LIKE 'А%';", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Вам нужен `LIKE` и символ-заменитель `%`.", 20, "SELECT Имя FROM Сотрудники "),
                                new TaskModel(15, 6, TaskType.THEORY, "Проверка на NULL", "`NULL` означает отсутствие данных. Для его проверки используются только `IS NULL` или `IS NOT NULL`.", "SELECT * FROM table WHERE column IS NULL;", null, null, null, "НЕ используйте `= NULL`.", 5, null),
                                new TaskModel(16, 7, TaskType.PRACTICE, "Проверка на NULL", "Выберите `Имя` и `Email` сотрудников, у которых **не указан** адрес электронной почты (`Email IS NULL`).", null, "SELECT Имя, Email FROM Сотрудники WHERE Email IS NULL;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `IS NULL` для поиска отсутствующих значений.", 20, "SELECT Имя, Email FROM Сотрудники ")
                        ),
                        // Общие поля LessonModel
                        EMPLOYEES_SETUP_SQL_CREATE,
                        "SELECT Имя, Отдел, Зарплата FROM Сотрудники WHERE Зарплата > 70000;",
                        "SELECT "
                ),

                // Урок 4: Агрегатные Функции и Группировка
                new LessonModel(
                        "4", // id
                        "Агрегатные Функции и GROUP BY", // title
                        "Научитесь использовать `COUNT`, `SUM`, `AVG`, `MIN`, `MAX` для расчетов и `GROUP BY` для анализа данных по категориям.", // description
                        0, // requiredExperience (для отладки)
                        // Список заданий (Tasks)
                        Arrays.asList(
                                new TaskModel(17, 1, TaskType.THEORY, "COUNT, SUM, AVG, MIN, MAX", "Агрегатные функции выполняют вычисления над набором строк и возвращают единственное значение. \n- **COUNT()**: Считает количество строк или значений.\n- **SUM()**: Считает сумму значений.\n- **AVG()**: Вычисляет среднее значение.\n- **MIN()/MAX()**: Находит минимальное/максимальное значение.", "SELECT AVG(column_name) FROM table;", null, null, null, "Агрегатные функции работают со столбцами.", 5, null),
                                new TaskModel(18, 2, TaskType.PRACTICE, "Общее количество сотрудников", "Подсчитайте **общее количество** сотрудников (`COUNT()`) в таблице.", null, "SELECT COUNT(Id) FROM Сотрудники;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `COUNT(Id)` или `COUNT(*)`.", 20, "SELECT "),
                                new TaskModel(19, 3, TaskType.PRACTICE, "Средняя и максимальная зарплата", "Найдите **среднюю** (`AVG()`) и **максимальную** (`MAX()`) зарплату среди всех сотрудников. Выведите оба значения в одном запросе.", null, "SELECT AVG(Зарплата), MAX(Зарплата) FROM Сотрудники;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте две функции через запятую.", 20, "SELECT "),
                                new TaskModel(20, 4, TaskType.THEORY, "Группировка данных (GROUP BY)", "Предложение **GROUP BY** используется для группировки строк, имеющих одинаковые значения, в итоговые строки. Оно часто используется с агрегатными функциями для вычисления итоговых значений для каждой группы (например, средняя зарплата по каждому отделу).", "SELECT category, COUNT(*) FROM table GROUP BY category;", null, null, null, "Все неагрегированные столбцы в SELECT должны быть в GROUP BY.", 5, null),
                                new TaskModel(21, 5, TaskType.PRACTICE, "Сотрудники по отделам", "Выведите название **Отдела** и **количество** сотрудников (`COUNT()`) в каждом отделе.", null, "SELECT Отдел, COUNT(Id) FROM Сотрудники GROUP BY Отдел;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Сгруппируйте результат по столбцу `Отдел`.", 25, "SELECT Отдел, COUNT(Id) FROM Сотрудники "),
                                new TaskModel(22, 6, TaskType.PRACTICE, "Средняя зарплата по отделам", "Выведите название **Отдела** и **среднюю зарплату** (`AVG()`) сотрудников в этом отделе.", null, "SELECT Отдел, AVG(Зарплата) FROM Сотрудники GROUP BY Отдел;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Группируйте по `Отдел` и примените `AVG(Зарплата)`.", 25, "SELECT Отдел, AVG(Зарплата) FROM Сотрудники "),
                                new TaskModel(23, 7, TaskType.THEORY, "Фильтрация групп (HAVING)", "Предложение **HAVING** используется для фильтрации результатов группировки, устанавливая условия для агрегатных функций. **WHERE** фильтрует строки ДО группировки, **HAVING** — группы ПОСЛЕ.", "SELECT dept, COUNT(*) FROM table GROUP BY dept HAVING COUNT(*) > 5;", null, null, null, "HAVING всегда идет после GROUP BY.", 5, null),
                                new TaskModel(24, 8, TaskType.PRACTICE, "Отделы с высоким стажем", "Найдите **Отделы**, в которых **средний стаж** (`AVG(Стаж)`) сотрудников **больше 4 лет**. Выведите Отдел и средний стаж.", null, "SELECT Отдел, AVG(Стаж) FROM Сотрудники GROUP BY Отдел HAVING AVG(Стаж) > 4;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `GROUP BY`, а затем `HAVING` для фильтрации результата `AVG(Стаж)`.", 30, "SELECT Отдел, AVG(Стаж) FROM Сотрудники ")
                        ),
                        // Общие поля LessonModel
                        EMPLOYEES_SETUP_SQL_CREATE,
                        "SELECT Отдел, AVG(Зарплата) FROM Сотрудники GROUP BY Отдел;",
                        "SELECT "
                ),

                // Урок 5: Сортировка и Ограничение
                new LessonModel(
                        "5", // id
                        "Сортировка (ORDER BY) и Лимит (LIMIT)", // title
                        "Научитесь упорядочивать результаты с помощью `ORDER BY` и ограничивать количество возвращаемых строк с помощью `LIMIT`.", // description
                        0, // requiredExperience (для отладки)
                        // Список заданий (Tasks)
                        Arrays.asList(
                                new TaskModel(25, 1, TaskType.THEORY, "Сортировка ORDER BY", "Предложение **ORDER BY** используется для сортировки результирующего набора по одному или нескольким столбцам. \n- **ASC** (Ascending): По возрастанию (по умолчанию).\n- **DESC** (Descending): По убыванию.", "SELECT * FROM table ORDER BY column DESC;", null, null, null, "ORDER BY всегда идет последним в SELECT-запросе.", 5, null),
                                new TaskModel(26, 2, TaskType.PRACTICE, "Сортировка по зарплате (убывание)", "Выберите `Имя` и `Зарплата`. Отсортируйте результат по полю **Зарплата** по **убыванию** (`DESC`).", null, "SELECT Имя, Зарплата FROM Сотрудники ORDER BY Зарплата DESC;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `ORDER BY` и ключевое слово `DESC`.", 20, "SELECT Имя, Зарплата FROM Сотрудники "),
                                new TaskModel(27, 3, TaskType.PRACTICE, "Сортировка по стажу (возрастание)", "Выберите `Имя` и `Стаж`. Отсортируйте результат по полю **Стаж** по **возрастанию** (`ASC` или по умолчанию).", null, "SELECT Имя, Стаж FROM Сотрудники ORDER BY Стаж ASC;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `ORDER BY` с `ASC` (или без него).", 20, "SELECT Имя, Стаж FROM Сотрудники "),
                                new TaskModel(28, 4, TaskType.THEORY, "Множественная сортировка", "Можно сортировать по нескольким столбцам. Сортировка применяется последовательно: сначала по первому столбцу, затем по второму среди одинаковых значений первого.", "SELECT * FROM table ORDER BY column1 DESC, column2 ASC;", null, null, null, "Порядок столбцов в ORDER BY имеет значение.", 5, null),
                                new TaskModel(29, 5, TaskType.PRACTICE, "Сортировка по отделу и зарплате", "Выберите `Отдел`, `Имя` и `Зарплата`. Отсортируйте сначала по **Отделу** (по возрастанию), а затем по **Зарплате** (по убыванию) внутри каждого отдела.", null, "SELECT Отдел, Имя, Зарплата FROM Сотрудники ORDER BY Отдел ASC, Зарплата DESC;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте два столбца в `ORDER BY` через запятую.", 25, "SELECT Отдел, Имя, Зарплата FROM Сотрудники "),
                                new TaskModel(30, 6, TaskType.THEORY, "Ограничение LIMIT", "Предложение **LIMIT** используется для ограничения количества строк, возвращаемых запросом. Оно часто используется вместе с `ORDER BY` для получения, например, топ-3 самых дорогих товаров.", "SELECT * FROM table ORDER BY price DESC LIMIT 5;", null, null, null, "LIMIT всегда идет самым последним.", 5, null),
                                new TaskModel(31, 7, TaskType.PRACTICE, "Топ-3 самых высокооплачиваемых", "Выберите `Имя` и `Зарплата`. Отсортируйте по зарплате по убыванию и ограничьте результат первыми **тремя** строками.", null, "SELECT Имя, Зарплата FROM Сотрудники ORDER BY Зарплата DESC LIMIT 3;", EMPLOYEES_FULL_SETUP_SQL, "Сотрудники", "Используйте `ORDER BY` для сортировки и `LIMIT 3` для ограничения.", 30, "SELECT Имя, Зарплата FROM Сотрудники ")
                        ),
                        // Общие поля LessonModel
                        EMPLOYEES_SETUP_SQL_CREATE,
                        "SELECT Имя, Зарплата FROM Сотрудники ORDER BY Зарплата DESC LIMIT 3;",
                        "SELECT "
                )
        );

        // Заполнение lessonMap для быстрого доступа
        for (LessonModel lesson : hardcodedLessons) {
            lessonMap.put(lesson.getId(), lesson);
        }
    }

    /**
     * Публичный статический метод для получения единственного экземпляра LessonRepository (Singleton).
     */
    public static synchronized LessonRepository getInstance() {
        if (instance == null) {
            instance = new LessonRepository();
        }
        return instance;
    }

    // =========================================================================
    //                            ЛОГИКА ПРОГРЕССА (FIREBASE)
    // =========================================================================

    /**
     * Асинхронно загружает статус завершения уроков для данного пользователя из Firestore.
     * Этот метод должен быть вызван при запуске приложения, прежде чем отображать список уроков.
     *
     * @param userId ID текущего аутентифицированного пользователя.
     * @param callback Колбэк для уведомления о завершении загрузки.
     */
    public void loadProgress(String userId, Callback<Void> callback) {
        if (userId == null || userId.isEmpty()) {
            System.err.println("Firebase Error: User ID is null or empty. Cannot load progress.");
            callback.onFailure(new Exception("Invalid User ID"));
            return;
        }

        // --- ЗАГЛУШКА ДЛЯ КОМПИЛЯЦИИ (ВРЕМЕННОЕ РЕШЕНИЕ) ---
        // ИМИТАЦИЯ ЗАГРУЗКИ:
        System.out.println("WARNING: Progress loading stub used. ALL lessons are marked as completed and requiredExperience is 0 for debugging.");

        // 1. Помечаем ВСЕ уроки как завершенные в локальном хранилище прогресса
        completedLessonIds.clear(); // Очищаем старые данные
        for (LessonModel lesson : lessonMap.values()) {
            // Устанавливаем статус completed=true для ВСЕХ уроков
            lesson.setCompleted(true);
            completedLessonIds.put(lesson.getId(), true);
            System.out.println("DEBUG: Lesson " + lesson.getId() + " (" + lesson.getTitle() + ") completion status set to: TRUE");
        }

        // 2. Завершаем асинхронную операцию
        System.out.println("DEBUG: loadProgress stub finished. Calling onSuccess.");
        callback.onSuccess(null);
        // -----------------------------------------------------
    }

    /**
     * Отмечает весь урок как завершенный и сохраняет статус в Firestore.
     *
     * @param lessonId ID урока, который нужно пометить.
     * @param userId ID текущего аутентифицированного пользователя.
     */
    public void markLessonCompleted(String lessonId, String userId) {
        LessonModel lesson = lessonMap.get(lessonId);
        if (lesson == null) {
            System.err.println("Error: Lesson with ID " + lessonId + " not found.");
            return;
        }

        // 1. Обновляем локальный статус в памяти
        lesson.setCompleted(true);
        completedLessonIds.put(lessonId, true);
        System.out.println("Lesson " + lessonId + " marked as COMPLETED locally.");


        // 2. Сохраняем в Firestore
        if (userId == null || userId.isEmpty()) {
            System.err.println("Firebase Error: Cannot save progress. User ID is missing.");
            return;
        }

        // -----------------------------------------------------------
        //              ВСТАВИТЬ КОД FIREBASE SDK ЗДЕСЬ
        // (Сохранение в базу)
        // -----------------------------------------------------------
    }

    // =========================================================================
    //                            СИНХРОННЫЙ ДОСТУП К СТАТИЧЕСКИМ ДАННЫМ
    // =========================================================================

    /**
     * Возвращает полный список уроков (неизменяемый).
     */
    public List<LessonModel> getAllLessons() {
        // Возвращаем неизменяемый список, чтобы предотвратить случайное изменение данных извне.
        return Collections.unmodifiableList(new ArrayList<>(lessonMap.values()));
    }

    /**
     * Получает урок по его ID.
     */
    public LessonModel getLessonById(String id) {
        return lessonMap.get(id);
    }

    /**
     * Получает задание по его уникальному глобальному целочисленному ID.
     */
    public TaskModel getTaskByUniqueId(int taskId) {
        for (LessonModel lesson : lessonMap.values()) {
            for (TaskModel task : lesson.getTasks()) {
                if (task.getTaskId() == taskId) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * Получает LessonModel, содержащий задание с указанным глобальным ID.
     */
    public LessonModel getLessonByTaskId(int taskId) {
        for (LessonModel lesson : lessonMap.values()) {
            for (TaskModel task : lesson.getTasks()) {
                if (task.getTaskId() == taskId) {
                    return lesson; // Возвращаем родительский LessonModel
                }
            }
        }
        return null;
    }

    /**
     * Сохраняет статус выполнения задания (локально в памяти).
     * При завершении урока необходимо вызвать markLessonCompleted().
     *
     * @param taskToUpdate Объект TaskModel с обновленным статусом.
     */
    public void saveTaskStatus(TaskModel taskToUpdate) {
        LessonModel lesson = getLessonByTaskId(taskToUpdate.getTaskId());
        if (lesson != null) {
            boolean allTasksCompleted = true;
            boolean lessonWasCompleted = lesson.isCompleted();

            // Сначала обновляем текущее задание
            for (TaskModel task : lesson.getTasks()) {
                if (task.getTaskId() == taskToUpdate.getTaskId()) {
                    task.setCompleted(taskToUpdate.isCompleted());
                    System.out.println("Task ID: " + taskToUpdate.getTaskId() + " status updated locally.");
                }
            }

            // Затем проверяем статус всех заданий в уроке
            for (TaskModel task : lesson.getTasks()) {
                if (!task.isCompleted()) {
                    allTasksCompleted = false;
                    break;
                }
            }

            // Проверяем, завершен ли урок ПОСЛЕ обновления
            if (allTasksCompleted && !lessonWasCompleted) {
                // Если все задания завершены и урок не был завершен, вызываем сохранение в БД
                // ВАЖНО: Здесь нужен реальный userId!
                // markLessonCompleted(lesson.getId(), currentUserId);
                System.out.println("Lesson " + lesson.getId() + " is now fully completed. Needs persistence save to Firebase.");
            }
        }
    }

    /**
     * @deprecated Используйте {@link #markLessonCompleted(String, String)} с ID пользователя для сохранения в Firebase.
     */
    @Deprecated
    public void markLessonCompleted(String lessonId) {
        LessonModel lesson = lessonMap.get(lessonId);
        if (lesson != null) {
            lesson.setCompleted(true);
            System.out.println("Lesson " + lessonId + " marked as COMPLETED in repository (LOCAL ONLY). Use the method with userId to save to Firebase.");
        }
    }
}