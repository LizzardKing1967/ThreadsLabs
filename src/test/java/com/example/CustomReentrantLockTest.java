package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomReentrantLockTest {

    @Test
    public void testReentrantLock() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();

        Thread t1 = new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread 1 acquired lock");
                lock.lock(); // Reentrant lock
                System.out.println("Thread 1 acquired lock again");
                Thread.sleep(1000);
                lock.unlock();
                System.out.println("Thread 1 released lock");
                lock.unlock();
                System.out.println("Thread 1 released lock again");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread 2 acquired lock");
                Thread.sleep(1000);
                lock.unlock();
                System.out.println("Thread 2 released lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @Test
    public void testIllegalUnlock() {
        CustomReentrantLock lock = new CustomReentrantLock();
        assertThrows(IllegalMonitorStateException.class, () -> {
            lock.unlock();
        });
    }

    @Test
    public void testTryLock() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();

        assertTrue(lock.tryLock());
        assertFalse(lock.tryLock());

        lock.unlock();

        assertTrue(lock.tryLock());
        lock.unlock();
    }
}