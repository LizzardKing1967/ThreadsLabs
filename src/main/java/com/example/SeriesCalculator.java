package com.example;

public class SeriesCalculator {

    private final double epsilon;

    public SeriesCalculator(double epsilon) {
        this.epsilon = epsilon;
    }

    public double calculateSum() {
        double sum = 0.0;
        double term;
        int n = 1;

        int progressUpdateFrequency = 100;
        int maxIterations = 100000;
        double progress;
        do {
            term = term(n);
            sum += term;

            if (n % (maxIterations / progressUpdateFrequency) == 0) {
                progress = ((double) n / maxIterations) * 100;
                System.out.printf("Прогресс: %.2f%% выполнено%n", progress);
            }

            n++;
        } while (Math.abs(term) > epsilon && n <= maxIterations);

        System.out.println("Прогресс: 100% выполнено");

        return sum;
    }

    private double term(int x) {
        return Math.sin(x) / x;
    }

    public double getEpsilon() {
        return epsilon;
    }
}
