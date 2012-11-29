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
import android.os.IBinder;
import android.os.RemoteException;

public class HybridDeadlockService extends Service {

    private Object mLock1 = new Object();
    private Object mLock2 = new Object();

    private void sleepABit() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IDeadlock.Stub() {

            @Override
            public void setCallback(IDeadlock cb) throws RemoteException {
                // NOP
            }

            @Override
            public void doStep1() throws RemoteException {
                // Thread 1 takes lock1 first
                synchronized (mLock1) {
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
                            }
                        }
                    }.start();
                    sleepABit();
                    // Thread 1 tries to take lock2 -> start waiting for thread 2
                    synchronized (mLock2) {
                        System.out.println("This is never printed in thread#1!");
                    }
                }
            }

            @Override
            public void doStep2() throws RemoteException {
                // NOP
            }

        };
    }


}
