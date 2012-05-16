package com.sonymobile.chkbugreport.testapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class HybridDeadlock extends Activity {

    protected IDeadlock mService;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println(">>> onCreate");

        // Connect to the service
        Intent service = new Intent(this, HybridDeadlockService.class);
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
                    mService.doStep1();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        bindService(service, connection, BIND_AUTO_CREATE);
    }

}
