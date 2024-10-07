package com.example;

import java.util.Scanner;

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
//        int totalsteps = 1000;

        // Запуск первой задачи
        CalculationTask task1 = new CalculationTask(calculator, 1, 5000, storage);
        Thread thread1 = new Thread(task1);

        // Запуск второй задачи
        CalculationTask task2 = new CalculationTask(calculator, 2, 3000, storage);
        Thread thread2 = new Thread(task2);

        // Старт потоков
        thread1.start();
        thread2.start();

        // Ожидание завершения
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            System.out.println("Главный поток был прерван.");
        }
    }
}
