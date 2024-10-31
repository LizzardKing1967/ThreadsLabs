package com.example;

public class CustomCountDownLatch {
    private final Object lock = new Object();
    private int count;

    public CustomCountDownLatch(int count) {
        this.count = count;
    }

    public void countDown() {
        synchronized (lock) {
            if (count > 0) {
                count--;
                if (count == 0) {
                    lock.notifyAll();
                }
            }
        }
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while (count > 0) {
                lock.wait();
            }
        }
    }

    public boolean tryAwait() {
        synchronized (lock) {
            return count == 0;
        }
    }
}
