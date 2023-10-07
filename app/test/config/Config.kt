package cn.futu.trade.test.config

import cn.futu.nndc.trade.base.TradeAccount.EnableMarket
import java.lang.reflect.Type

/**
 * TODO: Add Description
 *
 * @author eriche 2023/10/7
 */
interface BrokerWhiteList {

    val brokerList: List<Int>

}


interface BrokerBlackList {

    val brokerList: List<Int>

}

interface MarketWhiteList {

    val marketList: List<EnableMarket>

}

interface MarketBlackList {

    val marketList: List<EnableMarket>

}


@Suppress("UNCHECKED_CAST")
object WhiteListFinder {

    private val brokerWhiteListMap: MutableMap<Type, BrokerWhiteList> = mutableMapOf()

    inline fun <reified WhiteList : BrokerWhiteList> getBrokerWhiteList(): WhiteList {
        return getBrokerWhiteList(WhiteList::class.java)
    }

    fun <WhiteList : BrokerWhiteList> getBrokerWhiteList(type: Class<WhiteList>): WhiteList {
        return brokerWhiteListMap[type] as WhiteList
    }
}


