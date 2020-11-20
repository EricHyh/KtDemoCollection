package com.hyh.dialog

import android.content.Context
import android.os.Bundle

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/10/28
 */
object WebParamConfig {


    val AAA: IWebParamHandler<ParamInfo> = object : IWebParamHandler<ParamInfo> {
        override fun appendParams(
            context: Context,
            params: Bundle,
            paramInfo: ParamInfo
        ): Bundle {
            // do something
            return params
        }
    }


    val BBB: IWebParamHandler<IntParamInfo> = object : IWebParamHandler<IntParamInfo> {
        override fun appendParams(
            context: Context,
            params: Bundle,
            paramInfo: IntParamInfo
        ): Bundle {
            // do something
            return params
        }
    }

    val CCC: IWebParamHandler<IntParamInfo> = object : BaseWebParamHandler<IntParamInfo>("123") {
        override fun appendParams(
            context: Context,
            params: Bundle,
            paramInfo: IntParamInfo
        ): Bundle {
            // do something
            return params
        }
    }


    /*//业务 A
    val AAA: IWebParamConfig<ParamInfo> = object : IWebParamConfig<ParamInfo> {
        override val id: String = "1"
        override val paramHandler = object : IWebParamHandler<ParamInfo> {
            override fun appendParams(
                id: String,
                context: Context,
                params: Bundle,
                paramInfo: ParamInfo
            ): Bundle {
                // do something
                return params
            }
        }
    }*/

    //业务 B
    /*val BBB: IWebParamConfig<IntParamInfo> = object : IWebParamConfig<IntParamInfo> {
        override val id: String = "2"
        override val paramHandler = object : IWebParamHandler<IntParamInfo> {
            override fun appendParams(
                id: String,
                context: Context,
                params: Bundle,
                paramInfo: IntParamInfo
            ): Bundle {
                // do something
                return params
            }
        }
    }*/
}

/*interface IWebParamConfig<T : ParamInfo> {

    val id: String

    val paramHandler: IWebParamHandler<T>

    fun appendParams(
        context: Context,
        params: Bundle,
        paramInfo: T
    ): Bundle {
        return paramHandler.appendParams(id, context, params, paramInfo)
    }
}*/

interface IWebParamHandler<T : ParamInfo> {

    /**
     * 处理参数拼接
     *
     * @param context 上下文
     * @param params 参数容器，只有String类型的参数值有效
     * @param paramInfo 参数 特性参数，由业务指定
     * @return 参数容器
     */
    fun appendParams(
        context: Context,
        params: Bundle,
        paramInfo: T
    ): Bundle
}

abstract class BaseWebParamHandler<T : ParamInfo>(val urlId: String) : IWebParamHandler<T>


/**
 * 特性参数基类
 *
 * @property brokerId 券商ID，可为空，根据具体业务指定
 * @property accountId 账户ID，可为空，根据具体业务指定
 */
open class ParamInfo(
    val brokerId: Int? = null,
    val accountId: Long? = null
)

open class IntParamInfo(
    brokerId: Int? = null,
    accountId: Long? = null,
    val int: Int? = null
) : ParamInfo(brokerId, accountId)

enum class AccountType {

}