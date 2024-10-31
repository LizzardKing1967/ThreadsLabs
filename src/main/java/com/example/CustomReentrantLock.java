package com.example;

public class CustomReentrantLock {
    private final Object lock = new Object();
    private Thread ownerThread = null;
    private int holdCount = 0;

    public void lock() throws InterruptedException {
        synchronized (lock) {
            Thread currentThread = Thread.currentThread();
            while (isLocked() && ownerThread != currentThread) {
                lock.wait();
            }
            ownerThread = currentThread;
            holdCount++;
        }
    }

    public boolean tryLock() {
        synchronized (lock) {
            Thread currentThread = Thread.currentThread();
            if (!isLocked() || ownerThread == currentThread) {
                ownerThread = currentThread;
                holdCount++;
                return true;
            }
            return false;
        }
    }

    public void unlock() {
        synchronized (lock) {
            if (Thread.currentThread() == ownerThread) {
                holdCount--;
                if (holdCount == 0) {
                    ownerThread = null;
                    lock.notify();
                }
            } else {
                throw new IllegalMonitorStateException("Calling thread has not locked this lock");
            }
        }
    }

    private boolean isLocked() {
        return holdCount > 0;
    }

    public int getHoldCount() {
        return holdCount;
    }
}