package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomCountDownLatchTest {

    @Test
    public void testCountDownLatch() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(3);

        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                latch.countDown();
                System.out.println("Thread 1 counted down");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(2000);
                latch.countDown();
                System.out.println("Thread 2 counted down");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                Thread.sleep(3000);
                latch.countDown();
                System.out.println("Thread 3 counted down");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        latch.await();
        System.out.println("All threads have counted down");
    }

    @Test
    public void testTryAwait() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);

        assertFalse(latch.tryAwait());

        latch.countDown();

        assertTrue(latch.tryAwait());
    }
}