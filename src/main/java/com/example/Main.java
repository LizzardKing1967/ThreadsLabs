package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите хранилище: ");
        System.out.println("1. Хранилище c ленивой инициализацией");
        System.out.println("2. Хранилище без ленивой инициализации");
        int choice = scanner.nextInt();

        ProgressStorage storage;

        if (choice == 1) {
            storage = LazyProgressStorage.getInstance();
        } else {
            storage = ImmediateProgressStorage.getInstance();
        }

        ConsoleProgressObserver observer = new ConsoleProgressObserver();
        storage.setObserver(observer);

        SeriesCalculator calculator = new SeriesCalculator();
        int totalSteps = 10000000; // Общее количество шагов
        int stepsPerTask = 1000000; // Количество шагов на одну задачу

        int numberOfTasks = (totalSteps + stepsPerTask - 1) / stepsPerTask;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfTasks);
        List<Future<Double>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Создаем и запускаем задачи динамически
        for (int i = 0; i < numberOfTasks; i++) {
            int taskId = i + 1;
            int startStep = i * stepsPerTask + 1;
            int taskSteps = Math.min(stepsPerTask, totalSteps - (i * stepsPerTask)); // Последняя задача может быть меньше
            CalculationTask task = new CalculationTask(calculator, taskId, taskSteps, startStep, storage);
            futures.add(executorService.submit(task));
        }

        double totalSumm = 0;

        for (Future<Double> future : futures) {
            try {
                totalSumm += future.get();  // Ожидаем завершения задачи и получаем результат
            } catch (InterruptedException e) {
                // Обрабатываем прерывание ожидания
                System.out.println("Ожидание задачи было прервано: " + e.getMessage());

                Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
            } catch (ExecutionException e) {
                // Обрабатываем исключение внутри задачи
                System.out.println("Ошибка в выполнении задачи: " + e.getCause());  // Получаем исходное исключение
            } catch (Exception e) {
                // Обрабатываем любые другие исключения
                System.out.println("Неизвестная ошибка: " + e.getMessage());
            }
        }

        executorService.shutdown();
        //double totalResult = storage.getTotalResult();
        long endTime = System.currentTimeMillis();
        System.out.println("Результат вычисления: " + totalSumm);
        System.out.println("Общее время выполнения: " + (endTime - startTime) + " мс.");
        System.out.println("Все задачи завершены.");
    }
}
