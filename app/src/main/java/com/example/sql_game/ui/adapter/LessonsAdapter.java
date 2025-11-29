package com.example.sql_game.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.R;
import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.model.UserModel;

import java.util.List;

/**
 * Адаптер для отображения списка уроков в RecyclerView.
 * Отвечает за проверку разблокировки уроков на основе XP пользователя и последовательности прохождения.
 */
public class LessonsAdapter extends RecyclerView.Adapter<LessonsAdapter.LessonViewHolder> {

    // 1. ИНТЕРФЕЙС: Определяем слушатель для передачи клика обратно в Activity
    public interface OnLessonClickListener {
        /**
         * Вызывается, когда пользователь нажимает на разблокированный урок.
         * @param lesson Модель урока, на который было произведено нажатие.
         */
        void onLessonClick(LessonModel lesson);
    }

    private final Context context;
    private List<LessonModel> lessons;
    private UserModel currentUser;
    // 2. ПОЛЕ: Добавляем ссылку на слушатель
    private final OnLessonClickListener listener;

    // 3. КОНСТРУКТОР: Обновляем конструктор для приема слушателя
    public LessonsAdapter(Context context, List<LessonModel> lessons, UserModel currentUser, OnLessonClickListener listener) {
        this.context = context;
        this.lessons = lessons;
        this.currentUser = currentUser;
        this.listener = listener; // Инициализируем слушатель
    }

    /**
     * Обновляет список уроков.
     */
    public void updateLessons(List<LessonModel> newLessons) {
        this.lessons = newLessons;
        notifyDataSetChanged();
    }

    /**
     * Обновляет данные пользователя для проверки разблокировки уроков.
     */
    public void updateCurrentUser(UserModel user) {
        this.currentUser = user;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Убедитесь, что R.layout.item_lesson_card существует
        View view = LayoutInflater.from(context).inflate(R.layout.item_lesson_card, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        LessonModel lesson = lessons.get(position);
        holder.bind(lesson, currentUser, listener, lessons, position); // Передаем список уроков и позицию
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    /**
     * ViewHolder для отдельного элемента урока.
     */
    public class LessonViewHolder extends RecyclerView.ViewHolder {
        final CardView cardLesson;
        final TextView textTitle;
        final TextView textDescription;
        final TextView textRequiredLevel;
        final ImageView imageLessonIcon;
        final ProgressBar progressBarLesson;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            // Инициализация views:
            // Убедитесь, что ID соответствуют вашему layout-файлу item_lesson_card
            cardLesson = itemView.findViewById(R.id.card_lesson);
            textTitle = itemView.findViewById(R.id.text_lesson_title);
            textDescription = itemView.findViewById(R.id.text_lesson_description);
            textRequiredLevel = itemView.findViewById(R.id.text_required_level);
            imageLessonIcon = itemView.findViewById(R.id.image_lesson_icon);
            progressBarLesson = itemView.findViewById(R.id.progress_bar_lesson);
        }

        /**
         * Привязывает данные урока к элементу списка, проверяя статус блокировки.
         */
        public void bind(LessonModel lesson, UserModel user, OnLessonClickListener listener, List<LessonModel> allLessons, int position) {

            String lockReason = null;

            // Проверка, разблокирован ли урок (предполагаем, что user != null)
            boolean isLocked = false;

            if (user == null) {
                isLocked = true;
                lockReason = "Ошибка данных пользователя.";
            } else {

                // --- 1. ПРОВЕРКА ПОСЛЕДОВАТЕЛЬНОСТИ (для всех уроков, кроме первого) ---
                // *** ВРЕМЕННО КОММЕНТИРУЕМ для разблокировки всех уроков в целях тестирования ***
                /*
                if (position > 0) {
                    LessonModel previousLesson = allLessons.get(position - 1);
                    // Проверяем, завершен ли предыдущий урок по его ID
                    if (!user.isLessonCompleted(previousLesson.getId())) {
                        isLocked = true;
                        lockReason = "Необходимо завершить предыдущий урок: " + previousLesson.getTitle();
                    }
                }
                */

                // --- 2. ПРОВЕРКА XP (только если урок не заблокирован по последовательности) ---
                if (!isLocked) {
                    // Т.к. requiredExperience для всех уроков установлен в 0 в LessonRepository,
                    // эта проверка всегда будет true, если у пользователя 0 XP или больше.
                    if (user.getXp() < lesson.getRequiredExperience()) {
                        isLocked = true;
                        lockReason = "Недостаточно XP. Требуется: " + lesson.getRequiredExperience();
                    }
                }
            }


            // Устанавливаем заголовок
            // Убедитесь, что R.string.lesson_title_format существует и принимает два аргумента (%s)
            textTitle.setText(context.getString(R.string.lesson_title_format, lesson.getId(), lesson.getTitle()));

            if (isLocked) {
                // --- Урок заблокирован ---
                cardLesson.setAlpha(0.6f);

                // Настраиваем клик-слушатель для заблокированного урока
                final String finalLockReason = lockReason;
                cardLesson.setOnClickListener(v -> Toast.makeText(context,
                        // Отображаем наиболее релевантную причину блокировки
                        "Урок заблокирован. " + finalLockReason,
                        Toast.LENGTH_SHORT).show());

                // Визуальные элементы для заблокированного состояния
                // Убедитесь, что R.drawable.ic_lock_closed и R.color.colorError существуют
                imageLessonIcon.setImageResource(R.drawable.ic_lock_closed);
                //imageLessonIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorError));

                // Скрываем прогресс-бар и отображаем требуемый XP
                progressBarLesson.setVisibility(View.GONE);
                textRequiredLevel.setVisibility(View.VISIBLE);
                // Убедитесь, что R.string.required_xp_format существует
                textRequiredLevel.setText(context.getString(R.string.required_xp_format, lesson.getRequiredExperience()));

                // Отображаем описание урока
                textDescription.setText(lesson.getDescription());


            } else {
                // --- Урок разблокирован ---
                cardLesson.setAlpha(1.0f);

                // Вызываем слушатель при клике
                cardLesson.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLessonClick(lesson);
                    }
                });

                // Визуальные элементы для разблокированного состояния
                // Убедитесь, что R.drawable.ic_play_arrow и R.color.primary_dark существуют
                imageLessonIcon.setImageResource(R.drawable.ic_play_arrow);
                // imageLessonIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_dark));

                // Отображаем прогресс
                textRequiredLevel.setVisibility(View.GONE);
                progressBarLesson.setVisibility(View.VISIBLE);

                // Используем методы из LessonModel
                progressBarLesson.setProgress(lesson.getProgressPercentage());

                // Отображаем статус выполнения в поле описания
                String progressText = String.format("%d из %d заданий выполнено", lesson.getProgress(), lesson.getTotalTasks());
                if (lesson.isCompleted()) { // Используем isCompleted() для проверки
                    progressText = "Завершено! 🏆";
                }
                textDescription.setText(progressText);
            }
        }
    }
}