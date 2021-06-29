package com.hyh

/**
 * 公共类型别名定义
 *
 * @author eriche
 * @data 2021/6/18
 */


typealias Invoke = () -> Unit
typealias SuspendInvoke = (suspend () -> Unit)

typealias InvokeWithParam<T> = (T.() -> Unit)
typealias SuspendInvokeWithParam<T> = (suspend T.() -> Unit)

typealias OnEventReceived = (suspend () -> Unit)
typealias RefreshActuator = InvokeWithParam<Boolean>

typealias RunWith<T> = (InvokeWithParam<T>) -> Unit
typealias SuspendRunWith<T> = (SuspendInvokeWithParam<T>) -> Unit