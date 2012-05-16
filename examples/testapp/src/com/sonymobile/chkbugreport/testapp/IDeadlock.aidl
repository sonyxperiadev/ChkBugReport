package com.sonymobile.chkbugreport.testapp;

interface IDeadlock {

  void setCallback(in IDeadlock cb);

  void doStep1();

  void doStep2();
}
