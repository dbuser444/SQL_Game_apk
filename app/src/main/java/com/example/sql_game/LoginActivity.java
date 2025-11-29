package com.example.sql_game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sql_game.ui.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity для обработки логики входа, регистрации и переключения режимов.
 * Использует UserViewModel для взаимодействия с Firebase Auth и Firestore.
 */
public class LoginActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private EditText inputEmail, inputPassword, inputUsername;
    private TextInputLayout inputEmailLayout, inputPasswordLayout, inputUsernameLayout;
    private Button btnAuthAction;
    private ProgressBar progressBar;
    private MaterialButtonToggleGroup toggleAuthMode;
    private boolean isLoginMode = true;

    // Константы для валидации
    private static final int MIN_PASSWORD_LENGTH = 6;

    // КЛЮЧЕВОЕ СЛОВО: Используется для обнаружения ошибки "Email уже используется" в сообщении от Firebase
    private static final String ERROR_EMAIL_IN_USE_KEYWORD = "already in use";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Предполагается, что в res/layout есть activity_auth.xml
        setContentView(R.layout.activity_auth);

        // Инициализация ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Инициализация UI элементов
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputUsername = findViewById(R.id.input_username);

        // Получаем TextInputLayout для валидации
        inputEmailLayout = findViewById(R.id.input_email_layout);
        inputPasswordLayout = findViewById(R.id.input_password_layout);
        inputUsernameLayout = findViewById(R.id.input_username_layout);

        btnAuthAction = findViewById(R.id.btn_auth_action);
        progressBar = findViewById(R.id.progress_bar);
        toggleAuthMode = findViewById(R.id.toggle_auth_mode);

        // Установка начального режима
        setAuthMode(true);

        // Установка слушателей
        toggleAuthMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // Переключатель MaterialButtonToggleGroup
            if (checkedId == R.id.btn_login_mode && isChecked) {
                setAuthMode(true); // Режим Входа
            } else if (checkedId == R.id.btn_register_mode && isChecked) {
                setAuthMode(false); // Режим Регистрации
            }
            // Очистка сообщений об ошибках при переключении
            clearInputErrors();
        });

        btnAuthAction.setOnClickListener(v -> handleAuthAction());

        // Наблюдение за LiveData
        observeViewModel();
    }

    private void setAuthMode(boolean isLogin) {
        this.isLoginMode = isLogin;
        if (isLogin) {
            // Устанавливаем текст кнопки для Входа
            btnAuthAction.setText(getString(R.string.action_login));
            // Скрываем поле имени пользователя
            if (inputUsernameLayout != null) {
                inputUsernameLayout.setVisibility(View.GONE);
            }
        } else {
            // Устанавливаем текст кнопки для Регистрации
            btnAuthAction.setText(getString(R.string.action_signup));
            // Показываем поле имени пользователя
            if (inputUsernameLayout != null) {
                inputUsernameLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Выполняет валидацию полей ввода и запускает соответствующее действие (Вход/Регистрация).
     */
    private void handleAuthAction() {
        // Очистка предыдущих ошибок
        clearInputErrors();

        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String username = inputUsername.getText().toString().trim();

        // 1. Общая валидация Email/Password
        if (!validateEmail(email) | !validatePassword(password)) {
            return; // Прекратить, если валидация не пройдена
        }

        setLoading(true);

        if (isLoginMode) {
            // Режим Входа
            userViewModel.login(email, password);
        } else {
            // Режим Регистрации
            if (!validateUsername(username)) {
                setLoading(false);
                return;
            }
            userViewModel.register(email, password, username);
        }
    }

    // --- МЕТОДЫ ВАЛИДАЦИИ ---

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            inputEmailLayout.setError(getString(R.string.error_empty_field));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmailLayout.setError(getString(R.string.error_invalid_email));
            return false;
        }
        inputEmailLayout.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            inputPasswordLayout.setError(getString(R.string.error_empty_field));
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            inputPasswordLayout.setError(getString(R.string.error_password_length));
            return false;
        }
        inputPasswordLayout.setError(null);
        return true;
    }

    private boolean validateUsername(String username) {
        // Проверка только в режиме регистрации
        if (username.isEmpty() && !isLoginMode) {
            inputUsernameLayout.setError(getString(R.string.error_empty_field));
            return false;
        }
        inputUsernameLayout.setError(null);
        return true;
    }

    private void clearInputErrors() {
        inputEmailLayout.setError(null);
        inputPasswordLayout.setError(null);
        inputUsernameLayout.setError(null);
    }

    // --- НАБЛЮДЕНИЕ ЗА VIEWMODEL ---

    private void observeViewModel() {
        // Наблюдение за статусом входа
        userViewModel.getIsLoggedIn().observe(this, isLoggedIn -> {
            if (Boolean.TRUE.equals(isLoggedIn)) {
                // Пользователь успешно вошел или уже был авторизован
                setLoading(false);

                // Переход на LessonsActivity
                Intent intent = new Intent(LoginActivity.this, LessonsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            // Если isLoggedIn null или false, остаемся здесь
        });

        // Наблюдение за сообщениями о статусе (ошибки/успех)
        userViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {

                // *** ОБРАБОТКА ОШИБОК РЕГИСТРАЦИИ/ВХОДА ***

                // 1. Обработка ошибки "Email уже используется"
                if (!isLoginMode && message.toLowerCase().contains(ERROR_EMAIL_IN_USE_KEYWORD)) {

                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_email_already_registered_prompt_login),
                            Toast.LENGTH_LONG).show();

                    // Автоматически переключаем режим с Регистрации на Вход
                    setAuthMode(true);
                    // Очищаем поле имени пользователя
                    inputUsername.setText("");

                } else if (isLoginMode && (message.toLowerCase().contains("invalid-credential") || message.toLowerCase().contains("user-not-found"))) {
                    // 2. Обработка ошибок входа (неверный пароль или пользователь не найден)
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_login_failed_check_credentials),
                            Toast.LENGTH_LONG).show();
                } else {
                    // 3. Общие ошибки (слабый пароль, проблемы с сетью, и т.д.)
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                // Очищаем сообщение, чтобы оно не повторялось
                userViewModel.clearAuthMessage();
                setLoading(false);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnAuthAction.setVisibility(View.GONE);
            // Отключаем ввод на время загрузки
            toggleAuthMode.setEnabled(false);
            inputEmail.setEnabled(false);
            inputPassword.setEnabled(false);
            inputUsername.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnAuthAction.setVisibility(View.VISIBLE);
            // Включаем ввод обратно
            toggleAuthMode.setEnabled(true);
            inputEmail.setEnabled(true);
            inputPassword.setEnabled(true);
            // Поле имени пользователя включаем только в режиме регистрации
            inputUsername.setEnabled(!isLoginMode);
        }
    }
}