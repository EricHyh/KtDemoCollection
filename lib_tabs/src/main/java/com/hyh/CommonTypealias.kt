package com.hyh

/**
 * 公共类型别名定义
 *
 * @author eriche
 * @data 2021/6/18
 */


typealias Invoke = () -> Unit
typealias SuspendInvoke = (suspend () -> Unit)
typealias OnEventReceived = Invoke
typealias RefreshActuator = Invoke