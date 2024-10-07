package com.example;

public class SeriesCalculator {

    // Метод для вычисления очередного шага
    public double calculateStep(int n) {
        return Math.sin(n) / n;
    }
}