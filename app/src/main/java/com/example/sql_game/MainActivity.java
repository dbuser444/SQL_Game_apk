package com.example.sql_game;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаём объект Intent для перехода на другой экран
                Intent intent = new Intent(MainActivity.this, LessonsActivity.class);
// Запускаем новое Activity
                startActivity(intent);
// Опционально: закрываем текущий экран, чтобы пользователь не мог вернуться назад по кнопке "Назад"
                finish();
            }
        });
    }
}