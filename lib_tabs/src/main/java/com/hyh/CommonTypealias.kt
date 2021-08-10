package com.hyh

/**
 * 公共类型别名定义
 *
 * @author eriche
 * @data 2021/6/18
 */


/**
 * 无参的 lambda 表达式，用于定义一个动作
 */
typealias Invoke = () -> Unit
/**
 * [Invoke]的挂起形式
 */
typealias SuspendInvoke = (suspend () -> Unit)

/**
 * 带一个输入参数的 lambda 表达式，用于定义一个动作
 */
typealias InvokeWithParam<T> = (T.() -> Unit)
/**
 * [InvokeWithParam]的挂起形式
 */
typealias SuspendInvokeWithParam<T> = (suspend T.() -> Unit)

/**
 *
 */
typealias RunWith<T> = (InvokeWithParam<T>) -> Unit
/**
 *[RunWith]的挂起形式
 */
typealias SuspendRunWith<T> = (SuspendInvokeWithParam<T>) -> Unit

/**
 * 无输入参数的对象提供者
 */
typealias ObjectProvider<R> = () -> R
/**
 * 带一个输入参数的对象提供者
 */
typealias ObjectProviderWithParam<P, R> = (P) -> R


/**
 * 收到事件时的回调
 */
typealias OnEventReceived = (suspend () -> Unit)

/**
 * 刷新动作执行器
 *
 * @param important 是不是一个重要的刷新，如果是 true 那么本次刷新将不会被取消。
 *
 * 注意：important是一个参考值，能不能生效取决于刷新器的实现，目前[cn.futu.trade.common.list.ItemSource]的刷新器实现了该功能
 */
typealias RefreshActuator = ((important: Boolean) -> Unit)