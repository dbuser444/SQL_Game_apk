package com.example.sql_game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sql_game.ui.viewmodel.UserViewModel;

public class LoginActivity extends AppCompatActivity {

    // Режимы экрана: Вход или Регистрация
    private enum AuthMode { LOGIN, REGISTER }
    private AuthMode currentMode = AuthMode.LOGIN;

    private UserViewModel userViewModel;

    // Элементы UI
    private EditText editEmail, editPassword, editUsername;
    private Button btnMainAction, btnToggleMode;
    private TextView textTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Используем макет activity_login
        setContentView(R.layout.activity_login);

        // Инициализация ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Инициализация UI элементов
        // ИДЕНТИФИКАТОРЫ ИСПРАВЛЕНЫ: Теперь они есть в activity_login.xml
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editUsername = findViewById(R.id.edit_username);
        btnMainAction = findViewById(R.id.btn_main_action);
        btnToggleMode = findViewById(R.id.btn_toggle_mode);
        textTitle = findViewById(R.id.text_auth_title);

        // Установка слушателей
        btnMainAction.setOnClickListener(v -> performAuthAction());
        btnToggleMode.setOnClickListener(v -> toggleAuthMode());

        // Наблюдение за LiveData
        observeViewModel();

        // Устанавливаем начальный режим
        updateUI(AuthMode.LOGIN);
    }

    private void observeViewModel() {
        // Наблюдение за сообщениями об ошибках/успехе
        userViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) { // ИСПРАВЛЕНО: isEmpty() теперь доступен
                // ИСПРАВЛЕНО: Toast.makeText() должен быть доступен
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Наблюдение за статусом входа. Используем FirebaseUser для проверки авторизации.
        userViewModel.getFirebaseUserLiveData().observe(this, firebaseUser -> { // ИСПРАВЛЕНО: Вызываем getFirebaseUserLiveData()
            if (firebaseUser != null) {
                // Успешный вход или регистрация, переходим на главный экран
                Intent intent = new Intent(LoginActivity.this, LessonsActivity.class);
                // Очищаем стек, чтобы пользователь не мог вернуться назад
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void performAuthAction() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите Email и Пароль.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMode == AuthMode.LOGIN) {
            // Режим Входа
            // ИСПРАВЛЕНО: signIn() теперь доступен как псевдоним
            userViewModel.signIn(email, password);
        } else {
            // Режим Регистрации
            String username = editUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите Имя пользователя.", Toast.LENGTH_SHORT).show();
                return;
            }
            // ИСПРАВЛЕНО: signUp() теперь доступен как псевдоним
            // ИСПРАВЛЕНО: Принимает 3 аргумента (email, password, username)
            userViewModel.signUp(email, password, username);
        }
    }

    private void toggleAuthMode() {
        currentMode = (currentMode == AuthMode.LOGIN) ? AuthMode.REGISTER : AuthMode.LOGIN;
        updateUI(currentMode);
    }

    private void updateUI(AuthMode mode) {
        if (mode == AuthMode.LOGIN) {
            // Режим Вход
            textTitle.setText("Вход");
            editUsername.setVisibility(View.GONE);
            btnMainAction.setText("Войти");
            btnToggleMode.setText("Нет аккаунта? Зарегистрироваться");
        } else {
            // Режим Регистрация
            textTitle.setText("Регистрация");
            editUsername.setVisibility(View.VISIBLE);
            btnMainAction.setText("Зарегистрироваться");
            btnToggleMode.setText("Уже есть аккаунт? Войти");
        }
    }
}
