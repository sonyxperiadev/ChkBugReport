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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class AIDLDeadlock extends Activity {

    protected IDeadlock mService;
    private Object LOCK1 = new Object();
    private Object LOCK2 = new Object();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(">>> onCreate");

        // Connect to the service
        Intent service = new Intent(this, AIDLDeadlockService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                // NOP
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IDeadlock.Stub.asInterface(service);
                System.out.println(">>> onService Connected");
                try {
                    synchronized (LOCK1) {
                        mService.setCallback(mCB);
                        mService.doStep1();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        bindService(service, connection, BIND_AUTO_CREATE);
    }

    public void sleepABit() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private IDeadlock mCB = new IDeadlock.Stub() {

        @Override
        public void setCallback(IDeadlock cb) throws RemoteException {
            // NOP - we don't use this
        }

        @Override
        public void doStep1() throws RemoteException {
            synchronized (LOCK2) {
                System.out.println(">>> [ACT] doStep1");
                sleepABit();
                mService.doStep2();
                System.out.println(">>> [ACT] /doStep1");
            }
        }

        @Override
        public void doStep2() throws RemoteException {
            synchronized (LOCK2) {
                System.out.println(">>> [ACT] doStep2/");
                sleepABit();
            }
        }

    };

}
