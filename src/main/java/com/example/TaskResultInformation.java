package com.example;

public class TaskResultInformation {
    private final double result;
    private final long completionTime;
    private final int taskId;

    public TaskResultInformation(double result, long completionTime, int taskId) {
        this.result = result;
        this.completionTime = completionTime;
        this.taskId = taskId;
    }

    public double getResult() {
        return result;
    }

    public long getCompletionTime() {
        return completionTime;
    }
    public int getTaskId() {
        return taskId;
    }
}
