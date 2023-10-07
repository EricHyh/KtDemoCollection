package cn.futu.trade.test.constraint

import cn.futu.trade.core.security.tradeorder.modulebase.model.TradeOrderTimeType

/**
 * TODO
 *
 * @author eriche 2023/9/24
 */
class MarketOrderTimeType {


    val otherMarketOrderTimeTypes: List<TradeOrderTimeType> = listOf(TradeOrderTimeType.UNSET)

    val usMarketOrderTimeTypes: List<TradeOrderTimeType> = listOf(
        TradeOrderTimeType.GENERAL,
        TradeOrderTimeType.ENABLE_BA,
        TradeOrderTimeType.ONLY_NIGHT,
    )


}



