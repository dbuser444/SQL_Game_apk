package com.example.sql_game.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.sql_game.data.model.LessonModel;
import com.example.sql_game.data.repository.LessonRepository;

import java.util.List;

/**
 * ViewModel для управления данными об уроках.
 * Предоставляет список уроков в LiveData.
 */
public class LessonsViewModel extends ViewModel {

    private final LessonRepository lessonRepository;
    private final MutableLiveData<List<LessonModel>> allLessonsLiveData = new MutableLiveData<>();

    public LessonsViewModel() {
        lessonRepository = LessonRepository.getInstance();
        loadLessons();
    }

    /**
     * Загружает все уроки из репозитория и помещает их в LiveData.
     * На данный момент уроки жестко закодированы в репозитории.
     */
    private void loadLessons() {
        List<LessonModel> lessons = lessonRepository.getAllLessons();
        allLessonsLiveData.setValue(lessons);
    }

    /**
     * Возвращает LiveData со списком всех уроков.
     */
    public LiveData<List<LessonModel>> getAllLessons() {
        return allLessonsLiveData;
    }
}