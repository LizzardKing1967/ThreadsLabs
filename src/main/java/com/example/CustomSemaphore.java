package com.example;

public class CustomSemaphore {
    private final Object lock = new Object();
    private int permits;

    public CustomSemaphore(int permits) {
        this.permits = permits;
    }

    public void acquire() throws InterruptedException {
        synchronized (lock) {
            while (permits <= 0) {
                lock.wait();
            }
            permits--;
        }
    }

    public boolean tryAcquire() {
        synchronized (lock) {
            if (permits > 0) {
                permits--;
                return true;
            }
            return false;
        }
    }

    public void release() {
        synchronized (lock) {
            permits++;
            lock.notify();
        }
    }
}
