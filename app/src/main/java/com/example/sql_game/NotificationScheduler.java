package com.example.sql_game; // Замените на имя вашего пакета

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

/**
 * Статический класс-утилита для создания канала уведомлений,
 * планирования и отмены ежедневных повторяющихся напоминаний.
 */
public class NotificationScheduler {

    public static final String CHANNEL_ID = "lesson_reminder_channel";
    private static final int BASE_REQUEST_CODE = 1000; // Базовый код для создания уникальных кодов

    /**
     * Создает канал уведомлений (требуется для Android 8.0+).
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Уроки SQL Напоминания";
            String description = "Напоминания о необходимости вернуться к изучению SQL.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Регистрируем канал в системе
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("Scheduler", "Notification Channel Created");
            }
        }
    }

    /**
     * Планирует повторяющиеся напоминания для списка заданных времен.
     * @param context Контекст приложения.
     * @param times Список строк в формате "HH:MM" (например, ["08:30", "12:00", "14:00"]).
     */
    public static void scheduleMultipleReminders(Context context, List<String> times) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e("Scheduler", "AlarmManager is null.");
            return;
        }

        // Мы используем размер списка times для определения того, сколько напоминаний нужно отменить
        // в будущем, поэтому передаем его в Shared Preferences (или другое хранилище)
        // Но для упрощения тут его просто планируем.

        for (int i = 0; i < times.size(); i++) {
            String timeString = times.get(i);
            String[] parts = timeString.split(":");

            if (parts.length == 2) {
                try {
                    int hourOfDay = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());
                    int requestCode = BASE_REQUEST_CODE + i; // Уникальный код для каждого времени

                    // 1. Создание PendingIntent
                    Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
                    // Передаем CHANNEL_ID, чтобы Receiver мог его использовать
                    intent.putExtra("channelId", CHANNEL_ID);

                    // Упрощение флагов: IMMUTABLE обязателен для современных версий
                    // (используем его без проверки, так как он поддерживается с API 23)
                    int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode, // Используем уникальный код
                            intent,
                            flags
                    );

                    // 2. Установка времени срабатывания (сегодня или завтра)
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    // Если время уже прошло сегодня, устанавливаем на завтра
                    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    // 3. Планирование повторяющегося будильника (каждый день)
                    // AlarmManager.RTC_WAKEUP разбудит устройство для срабатывания
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY, // Повторять ежедневно
                            pendingIntent
                    );

                    Log.d("Scheduler", "Reminder scheduled for " + timeString + " daily with code " + requestCode);

                } catch (NumberFormatException e) {
                    Log.e("Scheduler", "Invalid time format: " + timeString, e);
                }
            }
        }
    }

    /**
     * Отменяет все ранее запланированные напоминания.
     * @param context Контекст приложения.
     * @param numberOfReminders Количество запланированных напоминаний, которые нужно отменить.
     */
    public static void cancelAllReminders(Context context, int numberOfReminders) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < numberOfReminders; i++) {
            int requestCode = BASE_REQUEST_CODE + i;
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);

            // Флаги должны быть такими же, как при создании, за исключением FLAG_NO_CREATE
            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE;

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    flags
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("Scheduler", "Reminder cancelled for request code " + requestCode);
            }
        }
    }
}