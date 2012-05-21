package com.sonymobile.chkbugreport.testapp;

import android.app.Activity;
import android.os.Bundle;

public class DeadlockWithWait extends Activity {

    private Object mLock1 = new Object();
    private Object mLock2 = new Object();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thread 1 takes lock1 first
        synchronized (mLock1) {
            // Thread 1 takes lock2, since it will wait on this
            synchronized (mLock2) {
                // Start thread 2
                new Thread() {
                    @Override
                    public void run() {
                        // Thread 2 takes lock2 first
                        synchronized (mLock2) {
                            sleepABit();
                            // Thread 2 tries to take lock1 -> start waiting for thread 1
                            synchronized (mLock1) {
                                System.out.println("This is never printed in thread#2!");
                            }
                            mLock2.notifyAll();
                        }
                    }
                }.start();
                sleepABit();
                try {
                    mLock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sleepABit() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
