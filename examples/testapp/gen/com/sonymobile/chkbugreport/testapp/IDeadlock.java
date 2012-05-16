/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/CORPUSERS/23052993/workspace/ChkBugReport/examples/testapp/src/com/sonymobile/chkbugreport/testapp/IDeadlock.aidl
 */
package com.sonymobile.chkbugreport.testapp;
public interface IDeadlock extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.sonymobile.chkbugreport.testapp.IDeadlock
{
private static final java.lang.String DESCRIPTOR = "com.sonymobile.chkbugreport.testapp.IDeadlock";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.sonymobile.chkbugreport.testapp.IDeadlock interface,
 * generating a proxy if needed.
 */
public static com.sonymobile.chkbugreport.testapp.IDeadlock asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.sonymobile.chkbugreport.testapp.IDeadlock))) {
return ((com.sonymobile.chkbugreport.testapp.IDeadlock)iin);
}
return new com.sonymobile.chkbugreport.testapp.IDeadlock.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setCallback:
{
data.enforceInterface(DESCRIPTOR);
com.sonymobile.chkbugreport.testapp.IDeadlock _arg0;
_arg0 = com.sonymobile.chkbugreport.testapp.IDeadlock.Stub.asInterface(data.readStrongBinder());
this.setCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_doStep1:
{
data.enforceInterface(DESCRIPTOR);
this.doStep1();
reply.writeNoException();
return true;
}
case TRANSACTION_doStep2:
{
data.enforceInterface(DESCRIPTOR);
this.doStep2();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.sonymobile.chkbugreport.testapp.IDeadlock
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void setCallback(com.sonymobile.chkbugreport.testapp.IDeadlock cb) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void doStep1() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_doStep1, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void doStep2() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_doStep2, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_doStep1 = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_doStep2 = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void setCallback(com.sonymobile.chkbugreport.testapp.IDeadlock cb) throws android.os.RemoteException;
public void doStep1() throws android.os.RemoteException;
public void doStep2() throws android.os.RemoteException;
}
