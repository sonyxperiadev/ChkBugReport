/*
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
package com.sonyericsson.chkbugreport.webserver.engine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class implements the socket part of the web server.
 * It listens on the specified socket and when a client connect, it hands the connection
 * over to the WebServer.
 */
public class WebServerSocket implements Runnable {

    private static final boolean DEBUG = false;

    private WebServer mServer;
    private int mPort = 8080;
    private Thread thread;
    private ServerSocket ss;

    public WebServerSocket(WebServer server) {
        mServer = server;
    }

    public void setPort(int port) {
        this.mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            ss = new ServerSocket(mPort);
            while (thread != null) {
                if (DEBUG) System.out.println("Waiting for connection...");
                Socket sock = ss.accept();
                if (DEBUG) System.out.println("Client connected!");
                ClientThread ct = new ClientThread(mServer, sock);
                ct.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        try {
            if (ss != null) {
                ss.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ClientThread extends Thread {

        private static final boolean ENABLE_THREADS = true;

        private Socket mSock;
        private WebServer mServer;

        public ClientThread(WebServer server, Socket sock) {
            mServer = server;
            mSock = sock;
        }

        @Override
        public synchronized void start() {
            if (ENABLE_THREADS) {
                super.start();
            } else {
                run();
            }
        }

        @Override
        public void run() {
            try {
                mServer.process(mSock.getInputStream(), mSock.getOutputStream());
                mSock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
