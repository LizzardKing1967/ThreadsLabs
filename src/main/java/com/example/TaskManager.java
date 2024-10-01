package com.example;

import java.util.HashMap;
import java.util.Map;

public class TaskManager {

    private final Map<Integer, Thread> tasks = new HashMap<>();
    private int taskIdCounter = 1;

    public void startTask(double epsilon) {
        int taskId = taskIdCounter++;  // Присваиваем уникальный идентификатор задачи
        CalculationTask task = new CalculationTask(new SeriesCalculator(epsilon), taskId);
        Thread taskThread = new Thread(task);
        tasks.put(taskId, taskThread);
        taskThread.start();
        System.out.println("Задача " + taskId + " запущена.");
    }

    public void awaitTask(int taskId) {
        Thread taskThread = tasks.get(taskId);
        if (taskThread != null) {
            try {
                taskThread.join();
                System.out.println("Задача " + taskId + " завершена.");
            } catch (InterruptedException e) {
                System.out.println("Ошибка во время ожидания завершения задачи.");
            }
        } else {
            System.out.println("Задача " + taskId + " не найдена.");
        }
    }

    public void exitProgram() {
        System.out.println("Завершение программы...");
        for (Thread taskThread : tasks.values()) {
            if (taskThread.isAlive()) {
                taskThread.interrupt();
            }
        }
        System.out.println("Программа завершена.");
    }
}
