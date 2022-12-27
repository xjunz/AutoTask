/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.hidden_api;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    final Object lock = new Object();

    @Test
    public void addition_isCorrect() throws InterruptedException {
        synchronized (lock) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }.start();
            lock.wait();
            System.out.println("Notified!");
        }
    }
}