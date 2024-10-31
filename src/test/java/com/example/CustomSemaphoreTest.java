package com.example;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CustomSemaphoreTest {

    @Test
    void testSemaphoreWithMultipleThreads() throws InterruptedException {
        CustomSemaphore semaphore = new CustomSemaphore(2);
        AtomicInteger activePermits = new AtomicInteger(0); // To track active permits

        Runnable task = () -> {
            try {
                semaphore.acquire();
                int currentActive = activePermits.incrementAndGet(); // Increment active permit count
                // Проверка, что не более 2 потоков удерживают разрешение
                assertTrue(currentActive <= 2, "More than 2 threads are holding permits concurrently");
                Thread.sleep(500); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activePermits.decrementAndGet(); // Decrement active permit count
                semaphore.release();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        Thread t3 = new Thread(task);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // Проверка, что все потоки завершены и все разрешения освобождены
        assertTrue(activePermits.get() == 0, "All permits should be released after threads complete");
    }

    @Test
    public void testTryAcquire() throws InterruptedException {
        CustomSemaphore semaphore = new CustomSemaphore(1);

        assertTrue(semaphore.tryAcquire());
        assertFalse(semaphore.tryAcquire());

        semaphore.release();

        assertTrue(semaphore.tryAcquire());
        semaphore.release();
    }

    @Test
    void testSemaphoreAcquireReleaseInterruption() throws InterruptedException {
        CustomSemaphore semaphore = new CustomSemaphore(1);
        final boolean[] wasInterrupted = {false}; // флаг для отслеживания прерывания

        Thread thread = new Thread(() -> {
            try {
                semaphore.acquire();
                // Попробуем снова захватить семафор
                semaphore.acquire();
            } catch (InterruptedException e) {
                wasInterrupted[0] = true; // отмечаем, что прерывание произошло
            }
        });

        thread.start();

        // Подождем немного, чтобы убедиться, что поток зашел в метод acquire()
        Thread.sleep(50);

        thread.interrupt(); // Прерываем поток

        thread.join(); // Ждем завершения потока

        // Проверка, что поток был корректно прерван
        assertTrue(wasInterrupted[0], "Thread should be interrupted during semaphore acquire");
    }


    @Test
    void testSemaphoreWithZeroPermits() {
        CustomSemaphore semaphore = new CustomSemaphore(0);
        assertFalse(semaphore.tryAcquire(), "Semaphore with zero permits should not allow acquisition");
    }

    @Test
    void testCustomSemaphoreRaceCondition() throws InterruptedException {
        int permits = 3;
        CustomSemaphore semaphore = new CustomSemaphore(permits);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        final int[] activePermits = {0}; // Отслеживаем использование разрешений

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    semaphore.acquire();
                    synchronized (activePermits) {
                        activePermits[0]++;
                        assertTrue(activePermits[0] <= permits, "Active permits should not exceed the maximum permits");
                    }
                    Thread.sleep(10); // Задержка, чтобы имитировать использование разрешения
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    synchronized (activePermits) {
                        activePermits[0]--;
                    }
                    semaphore.release();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // Проверка, что все потоки корректно освободили разрешения
        assertEquals(0, activePermits[0], "All permits should be released");
    }

}