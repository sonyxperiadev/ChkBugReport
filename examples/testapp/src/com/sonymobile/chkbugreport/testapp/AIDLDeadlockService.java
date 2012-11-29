/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
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
