package com.example.sql_game; // Замените на имя вашего пакета

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    // ВАЖНО: Убедитесь, что этот ID совпадает с ID, используемым в LessonsActivity.java
    // при создании канала уведомлений (createNotificationChannel).
    private static final String CHANNEL_ID = "lesson_reminder_channel";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Reminder", "Alarm triggered! Showing notification.");
        showNotification(context);
    }

    private void showNotification(Context context) {

        // --- 1. Создание PendingIntent для запуска LessonsActivity ---
        // Это намерение запустит LessonsActivity, когда пользователь нажмет на уведомление.
        Intent activityIntent = new Intent(context, LessonsActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0, // Request code
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        // --- Конец создания PendingIntent ---


        // 2. Создание уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon) // Убедитесь, что 'icon' существует
                .setContentTitle("Время для SQL-урока!")
                .setContentText("Напоминание: Сейчас лучшее время, чтобы зайти в приложение и продолжить обучение.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent) // <-- ДОБАВЛЕНО: Привязываем Intent к уведомлению
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // 3. *** ПРОВЕРКА РАЗРЕШЕНИЯ ДЛЯ ANDROID 13+ (API 33) ***
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Reminder", "Failed to show notification: POST_NOTIFICATIONS permission denied.");
                return;
            }
        }

        // 4. Отображение уведомления (только если разрешение есть или API < 33)
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}