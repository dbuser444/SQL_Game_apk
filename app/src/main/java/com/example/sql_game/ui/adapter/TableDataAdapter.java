package com.example.sql_game.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sql_game.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения результатов SQL-запроса (заголовки и строки)
 * в горизонтальном RecyclerView.
 */
public class TableDataAdapter extends RecyclerView.Adapter<TableDataAdapter.ViewHolder> {

    private List<String> columns = new ArrayList<>();
    private List<List<String>> data = new ArrayList<>();

    public TableDataAdapter() {
        // Пустой конструктор для инициализации
    }

    /**
     * Обновляет данные адаптера.
     *
     * @param newColumns Заголовки столбцов.
     * @param newData Данные строк.
     */
    public void updateData(List<String> newColumns, List<List<String>> newData) {
        this.columns.clear();
        this.data.clear();
        if (newColumns != null) {
            this.columns.addAll(newColumns);
        }
        if (newData != null) {
            this.data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    /**
     * Очищает данные адаптера.
     */
    public void clearData() {
        this.columns.clear();
        this.data.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Первая строка (position 0) - это заголовки
        if (position == 0) {
            holder.bind(columns);
            holder.itemView.setBackgroundResource(R.drawable.table_row_background_header);
        } else {
            // Остальные строки - данные
            List<String> rowData = data.get(position - 1);
            holder.bind(rowData);
            holder.itemView.setBackgroundResource(R.drawable.table_row_background);
        }
    }

    // Общее количество элементов = 1 (заголовок) + количество строк данных
    @Override
    public int getItemCount() {
        return data.size() + (columns.isEmpty() ? 0 : 1);
    }

    /**
     * ViewHolder для отображения одной строки таблицы.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout rowContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_table_row должен содержать LinearLayout (или другой контейнер),
            // в который будут динамически добавляться TextView для ячеек
            rowContainer = (LinearLayout) itemView;
        }

        public void bind(List<String> rowData) {
            rowContainer.removeAllViews(); // Очищаем старые ячейки

            for (String cellValue : rowData) {
                // Создаем TextView для каждой ячейки
                TextView cellView = new TextView(rowContainer.getContext());
                cellView.setText(cellValue);
                cellView.setPadding(16, 16, 16, 16);
                cellView.setSingleLine(true);
                cellView.setBackgroundResource(R.drawable.table_cell_background); // Фон для ячейки

                // Настраиваем параметры макета
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.rightMargin = 4; // Небольшой отступ между ячейками
                cellView.setLayoutParams(params);

                rowContainer.addView(cellView);
            }
        }
    }
}