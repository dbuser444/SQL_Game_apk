package com.example.sql_game.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.R;
import com.example.sql_game.data.model.TaskModel;
import com.example.sql_game.data.model.TaskModel.TaskType;

import java.util.Locale; // Импортируем Locale для явного указания в String.format

/**
 * Адаптер для отображения списка заданий в уроке.
 * Отображает статус выполнения и порядковый номер задания.
 */
public class TaskPromptAdapter extends ListAdapter<TaskModel, TaskPromptAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    private final OnTaskClickListener listener;
    private int currentActiveTaskIndex = -1;
    private static final int DEFAULT_CARD_COLOR = Color.WHITE;

    public TaskPromptAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Устанавливает индекс текущего активного задания для подсветки.
     * @param index Индекс активного задания (начиная с 0).
     */
    public void setCurrentActiveTaskIndex(int index) {
        if (currentActiveTaskIndex != index) {
            int oldIndex = currentActiveTaskIndex;
            currentActiveTaskIndex = index;
            // Обновляем только измененные элементы для оптимизации
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex);
            }
            if (currentActiveTaskIndex != -1) {
                notifyItemChanged(currentActiveTaskIndex);
            }
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = getItem(position);
        holder.bind(task, position);

        // Обработка клика
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        // Визуальная индикация текущего активного задания
        CardView cardView = (CardView) holder.itemView;
        Context context = holder.itemView.getContext();

        if (position == currentActiveTaskIndex) {
            // Подсветка активного задания
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
        } else {
            // Установка цвета по умолчанию для неактивного задания
            cardView.setCardBackgroundColor(DEFAULT_CARD_COLOR);
        }
    }

    /**
     * ViewHolder для отдельной карточки задания.
     */
    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final ImageView statusIcon;
        private final TextView instructionText;
        private final TextView rewardCrystals;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            // Привязка элементов из item_task_card.xml
            statusIcon = itemView.findViewById(R.id.image_task_status_icon);
            instructionText = itemView.findViewById(R.id.text_task_prompt);
            rewardCrystals = itemView.findViewById(R.id.text_task_score);
        }

        public void bind(TaskModel task, int position) {
            Context context = itemView.getContext();

            // ИСПРАВЛЕНИЕ 1: Добавляем Locale.getDefault() для устранения предупреждения String.format
            String taskPromptWithNumber = String.format(Locale.getDefault(), "%d. %s", position + 1, task.getInstruction());
            instructionText.setText(taskPromptWithNumber);

            // Устанавливаем награду
            rewardCrystals.setText("+" + task.getCrystalReward());

            // Установка статуса
            if (task.isCompleted()) {
                // Задание выполнено
                statusIcon.setImageResource(R.drawable.ic_check_circle);
                // Предполагаем, что R.color.gren - это цвет успеха
                statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.gren));
                // Делаем текст менее заметным
                instructionText.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                rewardCrystals.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            } else {
                // Задание не выполнено
                int iconColor;
                int textColor = ContextCompat.getColor(context, R.color.primary_dark);

                // ИСПРАВЛЕНИЕ 2: Используем .equals() для сравнения Enum'ов, чтобы избежать предупреждения Suspicious equality check
                if (TaskType.THEORY.equals(task.getType())) {
                    // Теоретический шаг
                    statusIcon.setImageResource(R.drawable.ic_book);
                    iconColor = ContextCompat.getColor(context, R.color.primary);
                } else {
                    // Практическое задание
                    statusIcon.setImageResource(R.drawable.ic_pending_circle);
                    iconColor = ContextCompat.getColor(context, R.color.light_gray);
                }

                statusIcon.setColorFilter(iconColor);
                instructionText.setTextColor(textColor);
                rewardCrystals.setTextColor(textColor);
            }
        }
    }

    // DiffUtil для эффективного обновления списка
    private static final DiffUtil.ItemCallback<TaskModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskModel oldItem, @NonNull TaskModel newItem) {
            // Сравнение примитивов (int), оператор == обязателен.
            return oldItem.getTaskId() == newItem.getTaskId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskModel oldItem, @NonNull TaskModel newItem) {
            // Сравниваем только те поля, изменение которых должно вызвать перерисовку
            // Для примитивов (boolean, int) используем ==, для объектов (String, Enum) используем .equals()
            return oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getInstruction().equals(newItem.getInstruction()) &&
                    oldItem.getCrystalReward() == newItem.getCrystalReward() &&
                    // Для сравнения Enum'ов (TaskType) лучше использовать .equals()
                    oldItem.getType().equals(newItem.getType());
        }
    };
}