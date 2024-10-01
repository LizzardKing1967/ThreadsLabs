package com.example;

class CalculationTask implements Runnable {

    private final SeriesCalculator calculator;
    private final int taskId;

    public CalculationTask(SeriesCalculator calculator, int taskId) {
        this.calculator = calculator;
        this.taskId = taskId;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();

        double sum = calculator.calculateSum();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        System.out.println("Задача " + taskId + " завершена. Результат: " + sum + ", Время: " + duration + " мс");
    }
}