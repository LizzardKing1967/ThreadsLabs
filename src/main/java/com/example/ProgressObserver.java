package com.example;

// Интерфейс для паттерна Наблюдатель
public interface ProgressObserver {
    // Метод для обновления наблюдателя при изменении прогресса в хранилище
    void onProgressUpdate(double averageProgress);
}
