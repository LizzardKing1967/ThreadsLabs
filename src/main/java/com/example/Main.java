package com.example;

public class Main {
    public static void main(String[] args) {

        double epsilon = 1e-14;

        SeriesCalculator calculator = new SeriesCalculator(epsilon);
        Thread calculationThread = new Thread(new CalculationTask(calculator));
        calculationThread.start();
        System.out.println("qwdqw");
    }
}