package com.example.sql_game.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Класс, использующий In-Memory SQLite для реального выполнения и проверки SQL-запросов.
 * База данных создается в памяти (In-Memory) при вызове executeSetup().
 */
public class SqlExecutor {

    private static final String TAG = "SqlExecutor";

    // База данных, создаваемая в памяти.
    private SQLiteDatabase db;

    /**
     * Класс для хранения результата выполнения запроса.
     * Поля: успех, сообщение об ошибке, заголовки и данные.
     */
    public static class ExecutionResult {
        public final boolean isSuccess;
        public final String errorMessage;
        public final List<String> resultColumns;
        public final List<List<String>> resultData;

        // Конструктор для успешного SELECT запроса
        public ExecutionResult(List<String> columns, List<List<String>> data) {
            this.isSuccess = true;
            this.errorMessage = null;
            this.resultColumns = columns;
            this.resultData = data;
        }

        // Конструктор для ошибок
        public ExecutionResult(String error) {
            this.isSuccess = false;
            this.errorMessage = error;
            this.resultColumns = Collections.emptyList();
            this.resultData = Collections.emptyList();
        }

        // Конструктор для не-SELECT команд (INSERT, UPDATE, DELETE)
        public ExecutionResult(String message, boolean isDml) {
            this.isSuccess = true;
            this.errorMessage = message;
            this.resultColumns = Collections.emptyList();
            this.resultData = Collections.emptyList();
        }
    }

    /**
     * Создает или пересоздает In-Memory базу данных и выполняет установочный SQL-скрипт.
     * @param setupSql SQL-скрипт (CREATE TABLE, INSERT и т.д.).
     * @return ExecutionResult с сообщением об успехе или ошибке.
     */
    public ExecutionResult executeSetup(String setupSql) {
        // 1. Сначала закрываем старую базу данных, если она существует
        closeDatabase();

        try {
            // 2. Создаем новую базу данных в памяти
            // Это гарантирует чистую базу данных для каждого задания.
            db = SQLiteDatabase.openOrCreateDatabase(":memory:", null);
            Log.d(TAG, "In-Memory database created/opened.");

            // 3. Выполняем установочный скрипт
            // Используем более строгий разделитель для надежности
            String[] statements = setupSql.split(";");
            db.beginTransaction();
            try {
                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (!trimmedStatement.isEmpty()) {
                        // Для обеспечения чистоты, удаляем все пробелы, кроме разделителей
                        db.execSQL(trimmedStatement);
                    }
                }
                db.setTransactionSuccessful();
                return new ExecutionResult("База данных успешно настроена.", true);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during setup execution", e);
            closeDatabase(); // Убедиться, что база данных закрыта при ошибке
            return new ExecutionResult("Ошибка настройки базы данных: " + e.getMessage());
        }
    }

    /**
     * Выполняет произвольный SQL-запрос (SELECT, INSERT, UPDATE, DELETE, и т.д.).
     * @param sql Запрос пользователя.
     * @return ExecutionResult с данными или сообщением об ошибке.
     */
    public ExecutionResult executeQuery(String sql) {
        if (db == null) {
            return new ExecutionResult("База данных не инициализирована. Выполните настройку.");
        }

        String normalizedSql = sql.trim().toUpperCase();
        Cursor cursor = null;

        try {
            // 1. Проверка на SELECT запрос
            if (normalizedSql.startsWith("SELECT")) {
                // Если это SELECT, используем rawQuery и обрабатываем Cursor
                cursor = db.rawQuery(sql, null);
                // Заголовки
                List<String> columns = Arrays.asList(cursor.getColumnNames());
                // Данные
                List<List<String>> data = cursorToTableData(cursor);
                return new ExecutionResult(columns, data);
            }

            // 2. Обработка DML/DDL команд (INSERT, UPDATE, DELETE, CREATE, DROP)
            else {
                // Используем execSQL для всех остальных команд
                db.beginTransaction();
                try {
                    db.execSQL(sql);
                    db.setTransactionSuccessful();

                    // Определяем тип команды для сообщения
                    String commandType;
                    if (normalizedSql.startsWith("INSERT")) { commandType = "INSERT"; }
                    else if (normalizedSql.startsWith("UPDATE")) { commandType = "UPDATE"; }
                    else if (normalizedSql.startsWith("DELETE")) { commandType = "DELETE"; }
                    else if (normalizedSql.startsWith("CREATE")) { commandType = "CREATE"; }
                    else { commandType = "Команда"; }

                    return new ExecutionResult(commandType + " выполнена успешно.", true);

                } finally {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SQL Execution Error for: " + sql, e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка выполнения.";
            // Приводим ошибку к более дружелюбному виду
            if (errorMsg.contains("no such table")) {
                errorMsg = "Не найдена указанная таблица. Проверьте имена таблиц.";
            } else if (errorMsg.contains("near ")) {
                errorMsg = "Синтаксическая ошибка: " + errorMsg.substring(errorMsg.indexOf("near "));
            }
            return new ExecutionResult("Ошибка выполнения SQL: " + errorMsg);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Преобразует Cursor в List<List<String>> (только данные, без заголовков).
     */
    private List<List<String>> cursorToTableData(Cursor cursor) {
        List<List<String>> data = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    // ВАЖНО: SQLite возвращает числовые данные как String, поэтому getString(i) является безопасным.
                    // Если значение NULL, getString() вернет null, что Android SQLite API обрабатывает.
                    // Мы преобразуем null в пустую строку для консистентности отображения.
                    String value = cursor.getString(i);
                    row.add(value != null ? value : "NULL");
                }
                data.add(row);
            } while (cursor.moveToNext());
        }
        return data;
    }

    /**
     * Закрывает In-Memory базу данных, если она открыта.
     */
    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
            db = null;
            Log.d(TAG, "In-Memory database closed.");
        }
    }
}