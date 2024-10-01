package com.example;

class CalculationTask implements Runnable {

    private final SeriesCalculator calculator;

    public CalculationTask(SeriesCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();

        double sum = calculator.calculateSum();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;  // Время в миллисекундах

        System.out.println("Сумма ряда: " + sum);
        System.out.println("Время выполнения: " + duration + " мс");
    }
}