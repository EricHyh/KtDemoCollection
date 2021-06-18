package com.hyh

/**
 * 公共类型别名定义
 *
 * @author eriche
 * @data 2021/6/18
 */


typealias OnEventReceived = () -> Unit
typealias RefreshActuator<Param> = (Param) -> Unit
typealias Invoke = (() -> Unit)
typealias SuspendInvoke = (suspend () -> Unit)