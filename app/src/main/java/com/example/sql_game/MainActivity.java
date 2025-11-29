package com.example.sql_game;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sql_game.ui.viewmodel.UserViewModel;

// MainActivity теперь выступает в качестве "шлюза" для проверки аутентификации
public class MainActivity extends AppCompatActivity {

    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Не устанавливаем setContentView, так как сразу переходим к другой активности

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Наблюдение за статусом входа
        userViewModel.getIsLoggedIn().observe(this, isLoggedIn -> {
            if (isLoggedIn != null) {
                // Выполняем переход только один раз
                if (isLoggedIn) {
                    // Пользователь вошел, переходим к урокам
                    startActivity(new Intent(MainActivity.this, LessonsActivity.class));
                } else {
                    // Пользователь не вошел, переходим к экрану входа/регистрации
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
                finish(); // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
            }
        });
    }
}
