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

        do {
            term = term(n);
            sum += term;
            n++;
        } while (Math.abs(term) > epsilon);

        return sum;
    }


/*
        System.out.println("Прогресс: 100% выполнено");
*/


    private double term(int x) {
        return Math.sin(x) / x;
    }

    public double getEpsilon() {
        return epsilon;
    }
}
