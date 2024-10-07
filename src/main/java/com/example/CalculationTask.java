package com.example;

class CalculationTask implements Runnable {

    private final SeriesCalculator calculator;
    private final int taskId;
    private final int totalSteps;
    private final ProgressStorage storage;

    public CalculationTask(SeriesCalculator calculator, int taskId, int totalSteps, ProgressStorage storage) {
        this.calculator = calculator;
        this.taskId = taskId;
        this.totalSteps = totalSteps;
        this.storage = storage;
    }

    @Override
    public void run() {
        double sum = 0;

        try {
            for (int i = 1; i <= totalSteps; i++) {
                sum += calculator.calculateStep(i);

                if (i % 100 == 0) {
                    double progress = (i * 100.0) / totalSteps;
                    System.out.println("Задача " + taskId + ": результат = " + sum + ", прогресс = " + progress + "%");

                    storage.updateTaskProgress(taskId, sum, progress);
                }

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Вычисления были прерваны.");
                    throw new InterruptedException("Поток был прерван");
                }
            }

        } catch (Exception e) {
            System.out.println("Ошибка в задаче " + taskId + ": " + e.getMessage());
        }


        System.out.println("Задача " + taskId + " завершена. Результат: " + sum);
    }
}
