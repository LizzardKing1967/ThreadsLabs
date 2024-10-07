package com.example;

import java.util.concurrent.Callable;

class CalculationTask implements Callable<Double> {

    private final SeriesCalculator calculator;
    private final int taskId;
    private final int totalSteps;
    private final int startStep;
    private final ProgressStorage storage;

    public CalculationTask(SeriesCalculator calculator, int taskId, int totalSteps, int startStep, ProgressStorage storage) {
        this.calculator = calculator;
        this.taskId = taskId;
        this.totalSteps = totalSteps;
        this.startStep = startStep;
        this.storage = storage;
    }

    @Override
    public Double call() {
        double sum = 0;

        try {
            for (int i = 0; i < totalSteps; i++) {
                int stepNumber = startStep + i;
                sum += calculator.calculateStep(stepNumber);

                if ((i + 1) % 100 == 0) {
                    double progress = ((i + 1) * 100.0) / totalSteps;
                    System.out.println("Задача " + taskId + ": результат = " + sum + ", прогресс = " + progress + "%");
                    storage.updateTaskProgress(taskId, sum, progress);
                }
            }

        } catch (Exception e) {
            System.out.println("Ошибка в задаче " + taskId + ": " + e.getMessage());
        }

        System.out.println("Задача " + taskId + " завершена. Результат: " + sum);
        return sum;
    }
}
