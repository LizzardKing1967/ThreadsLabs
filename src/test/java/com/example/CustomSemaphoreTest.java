package com.example;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomSemaphoreTest {

    @Test
    public void testSemaphore() throws InterruptedException {
        CustomSemaphore semaphore = new CustomSemaphore(2);

        Thread t1 = new Thread(() -> {
            try {
                semaphore.acquire();
                System.out.println("Thread 1 acquired semaphore");
                Thread.sleep(1000);
                semaphore.release();
                System.out.println("Thread 1 released semaphore");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                semaphore.acquire();
                System.out.println("Thread 2 acquired semaphore");
                Thread.sleep(1000);
                semaphore.release();
                System.out.println("Thread 2 released semaphore");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                semaphore.acquire();
                System.out.println("Thread 3 acquired semaphore");
                Thread.sleep(1000);
                semaphore.release();
                System.out.println("Thread 3 released semaphore");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();
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
}