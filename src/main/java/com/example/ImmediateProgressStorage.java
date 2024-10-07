package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ImmediateProgressStorage implements ProgressStorage {
    private static ImmediateProgressStorage instance = new ImmediateProgressStorage();
    private final Map<Integer, Pair<Double, Double>> taskProgressMap = new HashMap<>();
    private final List<ProgressObserver> observers = new ArrayList<>();

    // Приватный конструктор для предотвращения создания экземпляров
    private ImmediateProgressStorage() {
        System.out.println("Хранилище без ленивой инициализации создано.");
    }

    // Метод для получения единственного экземпляра (Singleton)
    public static ImmediateProgressStorage getInstance() {
        return instance;
    }

    // Синхронизированный метод для обновления прогресса задачи
    @Override
    public synchronized void updateTaskProgress(int taskId, double result, double progress) {
        taskProgressMap.put(taskId, new Pair<>(result, progress));

        // Уведомляем всех наблюдателей о новом среднем прогрессе
        notifyObservers();
    }

    // Синхронизированный метод для получения среднего прогресса по всем задачам
    @Override
    public synchronized double getAverageProgress() {
        double totalProgress = 0.0; // Инициализируем переменную для хранения общей суммы прогресса
        int numberOfTasks = taskProgressMap.size(); // Получаем количество задач

        // Суммируем прогресс каждой задачи
        for (Pair<Double, Double> progressPair : taskProgressMap.values()) {
            totalProgress += progressPair.second();
        }

        // Если нет задач, возвращаем 1
        if (numberOfTasks == 0) {
            return 1;
        }

        return totalProgress / numberOfTasks;
    }

    // Метод для добавления наблюдателя
    @Override
    public void setObserver(ProgressObserver observer) {
        observers.add(observer);
    }

    // Метод для уведомления всех наблюдателей
    private void notifyObservers() {
        double averageProgress = getAverageProgress();
        for (ProgressObserver observer : observers) {
            observer.onProgressUpdate(averageProgress);
        }
    }
}
