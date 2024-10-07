package com.example;

public interface ProgressStorage {
    void updateTaskProgress(int taskId, double result, double progress);
    double getAverageProgress();
    void setObserver(ProgressObserver observer);
}
