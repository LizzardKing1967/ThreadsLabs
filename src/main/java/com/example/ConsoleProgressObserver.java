package com.example;

// Наблюдатель, который выводит средний прогресс в консоль
public class ConsoleProgressObserver implements ProgressObserver {

    @Override
    public void onProgressUpdate(double averageProgress) {
        System.out.println("Средний прогресс выполнения задач: " + averageProgress + "%");
    }
}
