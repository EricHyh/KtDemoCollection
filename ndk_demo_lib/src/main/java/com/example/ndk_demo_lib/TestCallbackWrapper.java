/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (https://www.swig.org).
 * Version 4.2.1
 *
 * Do not make changes to this file unless you know what you are doing - modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.example.ndk_demo_lib;

public class TestCallbackWrapper {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TestCallbackWrapper(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TestCallbackWrapper obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected static long swigRelease(TestCallbackWrapper obj) {
    long ptr = 0;
    if (obj != null) {
      if (!obj.swigCMemOwn)
        throw new RuntimeException("Cannot release ownership as memory is not owned");
      ptr = obj.swigCPtr;
      obj.swigCMemOwn = false;
      obj.delete();
    }
    return ptr;
  }

  @SuppressWarnings({"deprecation", "removal"})
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        SwigDemoJNI.delete_TestCallbackWrapper(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  protected void swigDirectorDisconnect() {
    swigCMemOwn = false;
    delete();
  }

  public void swigReleaseOwnership() {
    swigCMemOwn = false;
    SwigDemoJNI.TestCallbackWrapper_change_ownership(this, swigCPtr, false);
  }

  public void swigTakeOwnership() {
    swigCMemOwn = true;
    SwigDemoJNI.TestCallbackWrapper_change_ownership(this, swigCPtr, true);
  }

  public void call(TestSwigData value) {
    SwigDemoJNI.TestCallbackWrapper_call(swigCPtr, this, TestSwigData.getCPtr(value), value);
  }

  public TestCallbackWrapper() {
    this(SwigDemoJNI.new_TestCallbackWrapper(), true);
    SwigDemoJNI.TestCallbackWrapper_director_connect(this, swigCPtr, true, true);
  }

}
