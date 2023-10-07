package cn.futu.trade.test.constraint2

import cn.futu.nndc.trade.base.OrderTimeInForce
import cn.futu.nndc.trade.base.TradeAccount
import cn.futu.trade.api.model.TradeStockInfo
import cn.futu.trade.core.security.tradeorder.modulebase.model.TradeOrderTimeType
import cn.futu.trade.core.security.tradeorder.modulebase.model.TradeTpSlSettingType
import cn.futu.trade.core.security.tradeorder.ordertype.tradetype.model.ETradeType
import cn.futu.trade.test.constraint.ConstraintFactor


/**
 * TODO: Add Description
 *
 * @author eriche 2023/10/7
 */


abstract class Constraint2<Factor, Factors : IConstraintFactors2> {

    private var _factor: Factor? = null

    @Suppress("UNCHECKED_CAST")
    protected val factor: Factor
        get() = _factor as Factor


    private lateinit var context: Constraint2Context<Factors>

    internal fun injectFactor(factor: Factor) {
        this._factor = factor
    }

    internal fun injectContext(context: Constraint2Context<Factors>) {
        this.context = context
    }

    protected fun getConstraintContext(): Constraint2Context<Factors> {
        return context
    }
}


interface ISubConstraint {

    fun handle(): Boolean

}


interface IConstraintFactors2


class Constraint2Context<Factors : IConstraintFactors2> constructor(
    val factors: Factors
)

interface GetTradeOrderTimeType {


    fun getTradeOrderTimeType(): List<TradeOrderTimeType>

}

class TradeTypeConstraintTradeOrderTime : Constraint2<ETradeType, TradeOrderTimeTypeFactors2>(), GetTradeOrderTimeType {

    override fun getTradeOrderTimeType(): List<TradeOrderTimeType> {
        TODO("Not yet implemented")
    }
}


class BrokerConstraintTradeOrderTime : Constraint2<Int, TradeOrderTimeTypeFactors2>(), GetTradeOrderTimeType,
    ISubConstraint {

    override fun getTradeOrderTimeType(): List<TradeOrderTimeType> {
        TODO("Not yet implemented")
    }

    override fun handle(): Boolean {
        TODO("Not yet implemented")
    }
}


data class TradeOrderTimeTypeFactors2 constructor(
    @ConstraintFactor("brokerId")
    val brokerId: Int?,
    val enableMarket: TradeAccount.EnableMarket,
    val tradeType: ETradeType,
    val stockInfo: TradeStockInfo?,
    val timeInForce: OrderTimeInForce,
    val tpSlSettingType: TradeTpSlSettingType,
) : IConstraintFactors2


interface BoolResult {

    fun isTrue(): Boolean

}

abstract class BoolConfig<Factor> : Constraint2<Factor, IConstraintFactors2>(), BoolResult





