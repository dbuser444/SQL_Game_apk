package com.example.sql_game;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.model.UserModel;
import com.example.sql_game.ui.adapter.LessonsAdapter;
import com.example.sql_game.ui.viewmodel.LessonsViewModel;
import com.example.sql_game.ui.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный экран приложения, отображающий информацию о профиле пользователя
 * и список доступных уроков (LessonModel), а также управляющий напоминаниями.
 */
public class LessonsActivity extends AppCompatActivity implements LessonsAdapter.OnLessonClickListener {

    // --- Поля для ViewModel и UI ---
    private UserViewModel userViewModel;
    private LessonsViewModel lessonsViewModel;
    private TextView textUsername, textLevel, textXp, textCrystals;
    private Button btnLogout;
    private View loadingLayout;
    private RecyclerView recyclerViewLessons;
    private LessonsAdapter lessonsAdapter;
    private UserModel currentUser;
    private final List<LessonModel> lessonList = new ArrayList<>();

    // --- Поля для Уведомлений ---
    private Button btnSetReminder;
    // Лаунчер для запроса разрешения POST_NOTIFICATIONS (для Android 13+)
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // ФИКСИРОВАННЫЙ СПИСОК ВРЕМЕНИ ДЛЯ ПЛАНИРОВАНИЯ
    private static final List<String> REMINDER_TIMES = List.of("08:00", "12:30", "19:00");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Предполагается, что в res/layout есть activity_lessons.xml
        setContentView(R.layout.activity_lessons);

        // --- Инициализация ViewModels и UI ---
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        lessonsViewModel = new ViewModelProvider(this).get(LessonsViewModel.class);

        textUsername = findViewById(R.id.text_username);
        textLevel = findViewById(R.id.text_level);
        textXp = findViewById(R.id.text_xp);
        textCrystals = findViewById(R.id.text_crystals);
        btnLogout = findViewById(R.id.btn_logout);
        loadingLayout = findViewById(R.id.loading_overlay);
        recyclerViewLessons = findViewById(R.id.recycler_lessons);
        btnSetReminder = findViewById(R.id.btn_set_reminder); // Кнопка для напоминаний


        // Настройка RecyclerView и Адаптера
        recyclerViewLessons.setLayoutManager(new LinearLayoutManager(this));
        // Обратите внимание: lessonsAdapter передается пустой lessonList, который будет заполнен позже
        lessonsAdapter = new LessonsAdapter(this, lessonList, currentUser, this);
        recyclerViewLessons.setAdapter(lessonsAdapter);

        // --- ЛОГИКА УВЕДОМЛЕНИЙ: Настройка и Канал ---
        setupNotificationPermissionLauncher();
        // Создаем канал уведомлений при запуске, используя статический метод
        NotificationScheduler.createNotificationChannel(this);

        // Наблюдение за LiveData
        observeViewModel();

        // Установка слушателей
        btnLogout.setOnClickListener(v -> logoutUser());

        // --- Логика Уведомлений: Слушатель для кнопки ---
        btnSetReminder.setOnClickListener(v -> checkAndRequestNotificationPermission());
    }

    // ---------------------------------------------------------------------------------------------
    // --- ЛОГИКА УВЕДОМЛЕНИЙ ---
    // ---------------------------------------------------------------------------------------------

    /**
     * Инициализирует ActivityResultLauncher для обработки результата запроса разрешения.
     */
    private void setupNotificationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Разрешение получено, планируем напоминание
                        setReminders();
                    } else {
                        // Разрешение не получено
                        Toast.makeText(this, "Не удалось установить напоминание. Нет разрешения на уведомления.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Проверяет наличие разрешения на уведомления и запрашивает его, если необходимо.
     */
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Разрешение уже есть, планируем напоминание
                setReminders();
            } else {
                // Разрешения нет, запрашиваем его
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Разрешение не требуется (Android < 13), сразу планируем напоминание
            setReminders();
        }
    }

    /**
     * Отменяет все предыдущие напоминания и устанавливает новый набор.
     * Использует класс NotificationScheduler.
     */
    private void setReminders() {
        // 1. Отменяем предыдущие напоминания.
        // Передаем размер текущего списка, чтобы отменить все, что было установлено ранее.
        NotificationScheduler.cancelAllReminders(this, REMINDER_TIMES.size());

        // 2. Планируем новый набор напоминаний.
        NotificationScheduler.scheduleMultipleReminders(this, REMINDER_TIMES);

        Toast.makeText(this,
                "Ежедневные напоминания установлены на 08:00, 12:30 и 19:00!",
                Toast.LENGTH_LONG).show();
    }


    // ---------------------------------------------------------------------------------------------
    // --- СУЩЕСТВУЮЩАЯ ЛОГИКА ---
    // ---------------------------------------------------------------------------------------------

    private void observeViewModel() {
        // 1. Наблюдение за данными пользователя (UserModel)
        userViewModel.getCurrentUserData().observe(this, user -> {
            if (user != null) {
                currentUser = user;

                // Обновление UI профиля
                int currentLevel = calculateLevel(user.getXp());

                textUsername.setText(user.getUsername());
                textLevel.setText(getString(R.string.profile_level, currentLevel));
                textXp.setText(getString(R.string.profile_xp, user.getXp()));
                textCrystals.setText(getString(R.string.profile_crystals, user.getCrystals()));

                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.GONE);
                }

                lessonsAdapter.updateCurrentUser(currentUser);

            } else {
                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // 2. Наблюдение за списком уроков (LessonModel List)
        lessonsViewModel.getAllLessons().observe(this, lessons -> {
            if (lessons != null) {
                lessonsAdapter.updateLessons(lessons);
            }
        });

        // 3. Наблюдение за статусом входа
        userViewModel.getIsLoggedIn().observe(this, isLoggedIn -> {
            if (Boolean.FALSE.equals(isLoggedIn)) {
                Intent intent = new Intent(LessonsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // 4. Наблюдение за сообщениями об ошибках
        userViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(LessonsActivity.this, message, Toast.LENGTH_LONG).show();
                userViewModel.clearAuthMessage();
            }
        });
    }

    private void logoutUser() {
        userViewModel.logout();
        Toast.makeText(this, "Выход...", Toast.LENGTH_SHORT).show();
    }

    private int calculateLevel(int xp) {
        if (xp < 50) return 1;
        if (xp < 150) return 2;
        if (xp < 300) return 3;
        return 4;
    }

    @Override
    public void onLessonClick(LessonModel lesson) {
        if (currentUser == null) {
            Toast.makeText(this, "Подождите, данные пользователя загружаются.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lesson.getTasks() == null || lesson.getTasks().isEmpty()) {
            Toast.makeText(this, "Урок пуст. Заданий не найдено.", Toast.LENGTH_LONG).show();
            return;
        }

        // *** ДОБАВЛЕНА ПРОВЕРКА НАЛИЧИЯ ID ПЕРЕД ЗАПУСКОМ ***
        String lessonId = lesson.getId();
        if (lessonId == null || lessonId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID урока отсутствует в объекте LessonModel.", Toast.LENGTH_LONG).show();
            return; // Прерываем действие, если ID не найден
        }

        Intent intent = new Intent(this, LessonPlayActivity.class);
        intent.putExtra("lessonId", lessonId); // Используем проверенный ID
        startActivity(intent);
    }
}