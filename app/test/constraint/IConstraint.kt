package cn.futu.trade.test.constraint

import cn.futu.nndc.trade.base.OrderTimeInForce
import cn.futu.nndc.trade.base.TradeAccount
import cn.futu.trade.api.model.TradeStockInfo
import cn.futu.trade.core.security.tradeorder.modulebase.model.TradeOrderTimeType
import cn.futu.trade.core.security.tradeorder.modulebase.model.TradeTpSlSettingType
import cn.futu.trade.core.security.tradeorder.ordertype.tradetype.model.ETradeType


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConstraintFactor(val name: String)


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ConstraintTarget(
    val subConstraint: Class
)




/**
 * 约束
 *
 * @param Factor 约束因子
 * @param Target 约束目标
 *
 * @author eriche 2023/9/24
 */
abstract class Constraint<Factor, Target, Factors : IConstraintFactors<Target>> {

    private lateinit var context: ConstraintContext<Factors>

    internal fun injectContext(context: ConstraintContext<Factors>) {
        this.context = context
    }

    protected fun getConstraintContext(): ConstraintContext<Factors> {
        return context
    }

    abstract fun apply(factor: Factor): List<Target>

}


class TradeTypeConstraintTradeOrderTime : Constraint<ETradeType, TradeOrderTimeType, TradeOrderTimeTypeFactors>() {

    override fun apply(factor: ETradeType): List<TradeOrderTimeType> {
        TODO("Not yet implemented")
    }

}

class TradeStockConstraintTradeOrderTime :
    Constraint<TradeStockInfo?, TradeOrderTimeType, TradeOrderTimeTypeFactors>() {

    override fun apply(factor: TradeStockInfo?): List<TradeOrderTimeType> {
        TODO("Not yet implemented")
    }

}


inline fun <reified Target> getSupported(factors: IConstraintFactors<Target>): List<Target> {
    val constraints = ConstraintFinder.findConstraints(factors)
    return getSupported(constraints)
}


@Suppress("UNCHECKED_CAST")
fun <Target> getSupported(constraints: List<Pair<*, Constraint<*, Target, *>>>): List<Target> {
    val targets: MutableList<Target> = mutableListOf()
    return constraints.foldIndexed(targets) { index: Int, acc: MutableList<Target>, pair: Pair<*, Constraint<*, Target, *>> ->
        val result: List<Target> =
            (pair.second as Constraint<Any?, Target, IConstraintFactors<Target>>).apply(pair.first)
        if (index == 0) {
            acc += result
        } else {
            acc.retainAll(result)
        }
        acc
    }
}


inline fun <reified Target> getTarget(
    factors: IConstraintFactors<Target>,
    crossinline prioritySorter: ((List<Target>) -> List<Target>) = { it },
    prefer: Target? = null,
): Target? {
    val supported = getSupported(factors)
    if (supported.isEmpty()) {
        return null
    }
    if (supported.size == 1) {
        return supported.firstOrNull()
    }
    if (prefer != null && supported.contains(prefer)) {
        return prefer
    }
    return prioritySorter.invoke(supported).firstOrNull()
}


data class TradeOrderTimeTypeFactors constructor(
    @ConstraintFactor("brokerId")
    val brokerId: Int?,
    val enableMarket: TradeAccount.EnableMarket,
    val tradeType: ETradeType,
    val stockInfo: TradeStockInfo?,
    val timeInForce: OrderTimeInForce,
    val tpSlSettingType: TradeTpSlSettingType,
) : IConstraintFactors<TradeOrderTimeType>


fun getTradeOrderTime(
    factors: TradeOrderTimeTypeFactors,
    prefer: TradeOrderTimeType?
): TradeOrderTimeType {
    val prioritySorter: ((List<TradeOrderTimeType>) -> List<TradeOrderTimeType>) = {
        it
    }
    return getTarget(factors, prioritySorter, prefer) ?: TradeOrderTimeType.UNSET
}


// 意外的，重要的，互相约束，组合约束


/**
 * fun getCorrectedUSOrderTimeType(
account: OrderAccountInfo,
enableMarket: EnableMarket,
tradeType: ETradeType,
stockInfo: TradeStockInfo?,
timeInForce: OrderTimeInForce,
tpSlSettingType: TradeTpSlSettingType,
selectedUSOrderTimeType: TradeOrderTimeType?,
): TradeOrderTimeType {
 */


// Target = T
// Factor = A、B

// A 是 a1，B 是 b1;
// T = t1

// A 是 a2，B 是 b2;
// T = t2


