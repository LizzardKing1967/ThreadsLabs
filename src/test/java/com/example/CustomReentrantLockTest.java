package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomReentrantLockTest {


    @Test
    void testReentrantLockSingleThread() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();

        // Захватываем блокировку
        lock.lock();
        // Проверяем, что текущий поток может повторно захватить ту же блокировку
        assertTrue(lock.tryLock(), "Reentrant lock should be acquired again by the same thread");

        // Освобождаем блокировку дважды, так как она была захвачена дважды
        lock.unlock();
        lock.unlock();

        // Проверяем, что после полного освобождения блокировка доступна для захвата
        assertTrue(lock.tryLock(), "Lock should be available after full release");
        lock.unlock();
    }

    @Test
    void testReentrantLockMultiThread() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        final boolean[] t2Locked = {false};

        Thread t1 = new Thread(() -> {
            try {
                lock.lock();
                latch.countDown(); // Уведомляем t2 о захвате блокировки
                Thread.sleep(100); // Удерживаем блокировку некоторое время
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock(); // Освобождаем блокировку
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                latch.await(); // Ждем, пока t1 захватит блокировку
                for (int i = 0; i < 5 && !t2Locked[0]; i++) { // Несколько попыток захватить блокировку
                    t2Locked[0] = lock.tryLock();
                    if (t2Locked[0]) {
                        lock.unlock(); // Если удалось захватить блокировку, сразу освобождаем
                    } else {
                        Thread.sleep(50); // Ждем перед новой попыткой
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Проверка, что второй поток смог захватить блокировку после освобождения первым потоком
        assertTrue(t2Locked[0], "Thread 2 should acquire the lock after it's released by Thread 1");
    }

    @Test
    void testReentrantLockUnlockThrowsException() {
        CustomReentrantLock lock = new CustomReentrantLock();
        Exception exception = assertThrows(IllegalMonitorStateException.class, lock::unlock);
        assertEquals("Calling thread has not locked this lock", exception.getMessage());
    }

    @Test
    void testReentrantLockInterruptDuringLockWait() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();
        CustomCountDownLatch latch = new CustomCountDownLatch(1);
        final boolean[] wasInterrupted = {false}; // флаг для отслеживания прерывания

        Thread t1 = new Thread(() -> {
            try {
                lock.lock();
                latch.countDown();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                lock.lock();
            } catch (InterruptedException e) {
                wasInterrupted[0] = true; // отмечаем, что прерывание произошло
            }
        });

        t1.start();
        t2.start();
        latch.await();
        Thread.sleep(50); // небольшая задержка перед прерыванием
        t2.interrupt();
        t1.join();
        t2.join();

        // Проверка, что поток t2 был корректно прерван
        assertTrue(wasInterrupted[0], "Thread should be interrupted during lock wait");
    }

    @Test
    void testReentrantLockReentrancyLimit() throws InterruptedException {
        CustomReentrantLock lock = new CustomReentrantLock();
        lock.lock();
        lock.lock();
        lock.lock();
        assertEquals(3, lock.getHoldCount(), "Reentrant lock count should match the number of lock calls");
        lock.unlock();
        lock.unlock();
        lock.unlock();
    }
}