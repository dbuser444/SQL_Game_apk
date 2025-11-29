package com.example.sql_game.data.repository;

// Интерфейс для обработки результатов асинхронных операций
public interface Callback<T> {
    /**
     * Вызывается при успешном завершении операции.
     * @param result Результат операции, или null, если результат не требуется.
     */
    void onSuccess(T result);

    /**
     * Вызывается при возникновении ошибки.
     * @param e Исключение, описывающее ошибку.
     */
    void onFailure(Exception e);
}