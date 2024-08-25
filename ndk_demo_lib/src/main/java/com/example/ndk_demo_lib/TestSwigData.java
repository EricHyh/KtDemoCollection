/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (https://www.swig.org).
 * Version 4.2.1
 *
 * Do not make changes to this file unless you know what you are doing - modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.example.ndk_demo_lib;

public class TestSwigData {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TestSwigData(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TestSwigData obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected static long swigRelease(TestSwigData obj) {
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
        SwigDemoJNI.delete_TestSwigData(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setValue1(int value) {
    SwigDemoJNI.TestSwigData_value1_set(swigCPtr, this, value);
  }

  public int getValue1() {
    return SwigDemoJNI.TestSwigData_value1_get(swigCPtr, this);
  }

  public void setValue2(long value) {
    SwigDemoJNI.TestSwigData_value2_set(swigCPtr, this, value);
  }

  public long getValue2() {
    return SwigDemoJNI.TestSwigData_value2_get(swigCPtr, this);
  }

  public void setValue3(int value) {
    SwigDemoJNI.TestSwigData_value3_set(swigCPtr, this, value);
  }

  public int getValue3() {
    return SwigDemoJNI.TestSwigData_value3_get(swigCPtr, this);
  }

  public void setValue4(long value) {
    SwigDemoJNI.TestSwigData_value4_set(swigCPtr, this, value);
  }

  public long getValue4() {
    return SwigDemoJNI.TestSwigData_value4_get(swigCPtr, this);
  }

  public void setValue5(float value) {
    SwigDemoJNI.TestSwigData_value5_set(swigCPtr, this, value);
  }

  public float getValue5() {
    return SwigDemoJNI.TestSwigData_value5_get(swigCPtr, this);
  }

  public void setValue6(double value) {
    SwigDemoJNI.TestSwigData_value6_set(swigCPtr, this, value);
  }

  public double getValue6() {
    return SwigDemoJNI.TestSwigData_value6_get(swigCPtr, this);
  }

  public void setValue7(String value) {
    SwigDemoJNI.TestSwigData_value7_set(swigCPtr, this, value);
  }

  public String getValue7() {
    return SwigDemoJNI.TestSwigData_value7_get(swigCPtr, this);
  }

  public void setValues1(IntVector value) {
    SwigDemoJNI.TestSwigData_values1_set(swigCPtr, this, IntVector.getCPtr(value), value);
  }

  public IntVector getValues1() {
    long cPtr = SwigDemoJNI.TestSwigData_values1_get(swigCPtr, this);
    return (cPtr == 0) ? null : new IntVector(cPtr, false);
  }

  public void setValues2(StringVector value) {
    SwigDemoJNI.TestSwigData_values2_set(swigCPtr, this, StringVector.getCPtr(value), value);
  }

  public StringVector getValues2() {
    long cPtr = SwigDemoJNI.TestSwigData_values2_get(swigCPtr, this);
    return (cPtr == 0) ? null : new StringVector(cPtr, false);
  }

  public void setValues3(TestSwigData2Vector value) {
    SwigDemoJNI.TestSwigData_values3_set(swigCPtr, this, TestSwigData2Vector.getCPtr(value), value);
  }

  public TestSwigData2Vector getValues3() {
    long cPtr = SwigDemoJNI.TestSwigData_values3_get(swigCPtr, this);
    return (cPtr == 0) ? null : new TestSwigData2Vector(cPtr, false);
  }

  public TestSwigData() {
    this(SwigDemoJNI.new_TestSwigData(), true);
  }

}
