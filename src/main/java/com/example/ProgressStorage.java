package com.example;

public interface ProgressStorage {
    void updateTaskProgress(int taskId, double result, double progress) throws InterruptedException;
    double getAverageProgress() throws InterruptedException;
    void setObserver(ProgressObserver observer);

    double getTotalResult();
}
