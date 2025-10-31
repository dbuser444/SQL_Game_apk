package com.example.sql_game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.ui.viewmodel.UserViewModel;

// LessonsActivity должна находиться в пакете ui/activity, но оставим как есть для соответствия
public class LessonsActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextView textUsername, textLevel, textXp, textCrystals;
    private Button btnLogout;
    private View loadingOverlay;
    private RecyclerView recyclerViewLessons;
    private View loadingLayout; // Добавлено для лучшего управления загрузкой (если есть в XML)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons);

        // Инициализация ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Инициализация UI элементов
        textUsername = findViewById(R.id.text_username);
        textLevel = findViewById(R.id.text_level);
        textXp = findViewById(R.id.text_xp);
        textCrystals = findViewById(R.id.text_crystals);
        btnLogout = findViewById(R.id.btn_logout);
        loadingOverlay = findViewById(R.id.loading_overlay);
        recyclerViewLessons = findViewById(R.id.recycler_lessons);
        loadingLayout = findViewById(R.id.loading_overlay); // Используем загрузочный слой

        // Наблюдение за LiveData
        observeViewModel();

        // Установка слушателей
        btnLogout.setOnClickListener(v -> logoutUser());

        // УДАЛЕНО: userViewModel.fetchCurrentUser(); - Загрузка данных происходит автоматически
    }

    private void observeViewModel() {
        // Наблюдение за данными пользователя (UserModel)
        // ИСПРАВЛЕНО: Используем getCurrentUserData()
        userViewModel.getCurrentUserData().observe(this, user -> {
            if (user != null) {
                // ИСПРАВЛЕНО: Методы UserModel теперь разрешены
                textUsername.setText(user.getUsername());
                textLevel.setText(String.format("Уровень: %d", user.getLevel()));
                textXp.setText(String.format("XP: %d", user.getXp()));
                textCrystals.setText(String.format("Кристаллы: %d", user.getCrystals()));
                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.GONE);
                }
            } else {
                // Если пользователь null (при выходе или начальной загрузке), показываем загрузку
                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // Наблюдение за статусом входа
        // ИСПРАВЛЕНО: Метод getIsLoggedIn() теперь существует
        userViewModel.getIsLoggedIn().observe(this, isLoggedIn -> {
            // Используем безопасную проверку Boolean.FALSE.equals(isLoggedIn)
            if (Boolean.FALSE.equals(isLoggedIn)) {
                // Пользователь вышел, переходим на LoginActivity
                Intent intent = new Intent(LessonsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Закрываем LessonsActivity
            }
        });

        // Наблюдение за сообщениями об ошибках
        // ИСПРАВЛЕНО: Используем getAuthMessage()
        userViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(LessonsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutUser() {
        // Обработка выхода
        userViewModel.logout();
    }
}
