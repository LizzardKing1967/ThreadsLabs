package com.example;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CustomCountDownLatchTest {
    @Test
    void testCountDownLatchReachesZero() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(3);

        latch.countDown();
        latch.countDown();
        latch.countDown();

        assertTrue(latch.tryAwait(), "Latch should be zero after three countDown calls");
    }

    @Test
    void testCountDownLatchAwait() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(2);

        Thread thread = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread.start();
        Thread.sleep(100); // Ensuring the thread is waiting

        latch.countDown();
        latch.countDown();

        thread.join();
        assertTrue(latch.tryAwait(), "Latch should reach zero and allow await to proceed");
    }

    @Test
    void testCountDownLatchDoesNotAwaitOnNonZero() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(2);
        assertFalse(latch.tryAwait(), "Latch should not allow await to proceed when count > 0");
    }

    @Test
    void testCountDownLatchCountCannotGoNegative() {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        latch.countDown();
        latch.countDown();

        assertTrue(latch.tryAwait(), "Latch should not go below zero count");
    }

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

    @Test
    void testCountDownLatchConcurrentCountdown() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(5);
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(latch::countDown);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(latch.tryAwait(), "Latch should reach zero after 5 concurrent countDown calls");
    }

    @Test
    void testCountDownLatchAwaitInterruption() throws InterruptedException {
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        final boolean[] wasInterrupted = {false}; // флаг для отслеживания прерывания

        Thread thread = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                wasInterrupted[0] = true; // отмечаем, что прерывание произошло
            }
        });

        thread.start();

        // Подождем немного, чтобы убедиться, что поток зашел в метод await()
        Thread.sleep(50);

        thread.interrupt(); // Прерываем поток

        thread.join(); // Ждем завершения потока

        // Проверка, что поток был корректно прерван
        assertTrue(wasInterrupted[0], "Thread should be interrupted during await");
    }

    @Test
    void testCustomCountDownLatchRaceCondition() throws InterruptedException {
        int threadCount = 10;
        CustomCountDownLatch latch = new CustomCountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        final int[] count = {0}; // Отслеживаем завершение потоков

        // Создаем несколько потоков, которые будут вызывать countDown
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                latch.countDown();
                synchronized (count) {
                    count[0]++;
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // Ожидание завершения всех потоков
        latch.await();

        // Проверка, что все потоки корректно уменьшили счётчик до нуля
        assertEquals(threadCount, count[0], "All threads should have completed countdown");
    }
}