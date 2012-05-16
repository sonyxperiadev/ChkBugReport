package com.sonymobile.chkbugreport.testapp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

public class AIDLDeadlockService extends Service {

    protected IDeadlock mCB;

    private Object LOCK1 = new Object();
    private Object LOCK2 = new Object();

    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void sleepABit() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IDeadlock.Stub() {

            @Override
            public void setCallback(IDeadlock cb) throws RemoteException {
                mCB = cb;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (LOCK1) {
                                mCB.doStep1();
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public synchronized void doStep1() throws RemoteException {
                synchronized (LOCK2) {
                    System.out.println(">>> [SRV] doStep1");
                    sleepABit();
                    mCB.doStep2();
                    System.out.println(">>> [SRV] /doStep1");
                }
            }

            @Override
            public synchronized void doStep2() throws RemoteException {
                synchronized (LOCK2) {
                    System.out.println(">>> [SRV] doStep2/");
                    sleepABit();
                }
            }

        };
    }

}
