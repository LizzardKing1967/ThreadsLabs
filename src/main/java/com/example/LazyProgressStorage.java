package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LazyProgressStorage implements ProgressStorage {
    private static volatile LazyProgressStorage instance;
    private final Map<Integer, Pair<Double, Double>> taskProgressMap = new HashMap<>();
    private final List<ProgressObserver> observers = new ArrayList<>();

    private LazyProgressStorage() {
        System.out.println("Ленивое хранилище создано.");
    }

    public static LazyProgressStorage getInstance() {
        if (instance == null) {
            synchronized (LazyProgressStorage.class) {
                if (instance == null) {
                    instance = new LazyProgressStorage();
                }
            }
        }
        return instance;
    }

    @Override
    public synchronized void updateTaskProgress(int taskId, double result, double progress) {
        taskProgressMap.put(taskId, new Pair<>(result, progress));
        // Уведомляем всех наблюдателей о новом среднем прогрессе
        notifyObservers();
    }

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

    @Override
    public void setObserver(ProgressObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        double averageProgress = getAverageProgress();
        for (ProgressObserver observer : observers) {
            observer.onProgressUpdate(averageProgress);
        }
    }
}
