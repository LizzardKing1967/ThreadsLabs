package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LazyProgressStorage implements ProgressStorage {
    private static volatile LazyProgressStorage instance;
    private final Map<Integer, Pair<Double, Double>> taskProgressMap = new HashMap<>();
    private final List<ProgressObserver> observers = new ArrayList<>();

    private final ReentrantLock taskProgressLock = new ReentrantLock();
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
    public void updateTaskProgress(int taskId, double result, double progress) {
        taskProgressLock.lock();
        try {
            taskProgressMap.put(taskId, new Pair<>(result, progress));
        } finally {
            taskProgressLock.unlock();
        }
        notifyObservers();
    }

    // Метод для получения среднего прогресса по всем задачам
    @Override
    public double getAverageProgress() {
        taskProgressLock.lock();
        try {
            double totalProgress = 0.0; // Инициализируем переменную для хранения общей суммы прогресса
            int numberOfTasks = taskProgressMap.size(); // Получаем количество задач

            // Суммируем прогресс каждой задачи
            for (Pair<Double, Double> progressPair : taskProgressMap.values()) {
                totalProgress += progressPair.second();
            }

            // Если нет задач, возвращаем 0
            if (numberOfTasks == 0) {
                return 0; // Если задач нет, средний прогресс равен 0
            }

            return totalProgress / numberOfTasks;
        } finally {
            taskProgressLock.unlock();
        }
    }

    @Override
    public double getTotalResult() {
        double totalResult = 0.0; // Инициализируем переменную для хранения общей суммы прогресса
        int numberOfTasks = taskProgressMap.size(); // Получаем количество задач

        // Суммируем прогресс каждой задачи
        for (Pair<Double, Double> progressPair : taskProgressMap.values()) {
            totalResult += progressPair.first();
        }
        return totalResult;
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
