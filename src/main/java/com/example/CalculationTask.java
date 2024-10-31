package com.example;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

class CalculationTask implements Callable<TaskResultInformation> {

    private final SeriesCalculator calculator;
    private final int taskId;
    private final int totalSteps;
    private final int startStep;
    private final ProgressStorage storage;

    private final CustomCountDownLatch latch;

    private final CustomSemaphore semaphore;

    public CalculationTask(SeriesCalculator calculator, int taskId, int totalSteps, int startStep, ProgressStorage storage, CustomCountDownLatch latch, CustomSemaphore semaphore) {
        this.calculator = calculator;
        this.taskId = taskId;
        this.totalSteps = totalSteps;
        this.startStep = startStep;
        this.storage = storage;
        this.latch = latch;
        this.semaphore = semaphore;
    }

    @Override
    public TaskResultInformation call() {
        double sum = 0;

        try {
            semaphore.acquire();
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
        } finally {
            latch.countDown();
            semaphore.release();
        }

        System.out.println("Задача " + taskId + " завершена. Результат: " + sum);
        long completionTime = System.currentTimeMillis();
        return new TaskResultInformation(sum, completionTime, taskId);
    }
}
