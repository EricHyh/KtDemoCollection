package cn.futu.trade.test.dimensional

import cn.futu.nndc.trade.base.TradeAccount
import cn.futu.trade.api.model.TradeStockInfo
import java.math.BigDecimal

/**
 * 维度处理
 *
 * @author eriche 2023/9/24
 */
interface IDimensionHandler<Dimensional, Param, Target> {

    val dimensional: Dimensional

    fun getResult(param: Param): Target

}

interface IChildDimensionHandler<ParentDimensional, ChildDimensional, Param, Target>
    : IDimensionHandler<ChildDimensional, Param, Target> {

    fun isAffect(
        parentDimensional: ParentDimensional,
        param: Param
    ): Boolean

}


class ChildDimensionHandler<Param, Target> constructor(
    private val parent: IDimensionHandler<*, Param, Target>,
    private val children: List<IChildDimensionHandler<*, *, Param, Target>>,
) : IChildDimensionHandler<Any?, List<Any?>, Param, Target> {

    constructor(
        parent: IDimensionHandler<*, Param, Target>,
        child: IChildDimensionHandler<*, *, Param, Target>
    ) : this(parent, listOf(child))

    override val dimensional: List<Any?> = buildList {
        this + parent.dimensional
        this + children.map { it.dimensional }
    }

    @Suppress("UNCHECKED_CAST")
    override fun isAffect(parentDimensional: Any?, param: Param): Boolean {
        return if (parent is IChildDimensionHandler<*, *, *, *>) {
            (parent as IChildDimensionHandler<Any?, *, Param, Target>).isAffect(parentDimensional, param)
        } else {
            true
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getResult(param: Param): Target {
        val child = children.find {
            (it as IChildDimensionHandler<Any?, Any?, Param, Target>).isAffect(
                parent.dimensional,
                param
            )
        }
        return if (child != null) {
            child.getResult(param)
        } else {
            parent.getResult(param)
        }
    }
}


fun <Param, Target> IDimensionHandler<*, Param, Target>.withChild(
    child: IChildDimensionHandler<*, *, Param, Target>,
): IChildDimensionHandler<*, *, Param, Target> {
    return ChildDimensionHandler(this, child)
}

fun <Param, Target> IDimensionHandler<*, Param, Target>.withChild(
    children: List<IChildDimensionHandler<*, *, Param, Target>>,
): IChildDimensionHandler<*, *, Param, Target> {
    return ChildDimensionHandler(this, children)
}


//fun <Param, Target> combine(
//
//): IDimensionHandler<*, Param, Target>


class MaxQuantityMarketDimension : IDimensionHandler<TradeAccount.EnableMarket, TradeStockInfo?, BigDecimal> {

    override val dimensional: TradeAccount.EnableMarket
        get() = TODO("Not yet implemented")

    override fun getResult(param: TradeStockInfo?): BigDecimal {
        TODO("Not yet implemented")
    }

}


class MaxQuantityBrokerDimension(
    override val dimensional: Int
) : IChildDimensionHandler<TradeAccount.EnableMarket, Int, TradeStockInfo?, BigDecimal> {

    override fun isAffect(
        parentDimensional: TradeAccount.EnableMarket,
        param: TradeStockInfo?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getResult(param: TradeStockInfo?): BigDecimal {
        TODO("Not yet implemented")
    }


    fun getMaxPrice(): BigDecimal {
        TODO("Not yet implemented")
    }


    fun getMinPrice(): BigDecimal {
        TODO("Not yet implemented")
    }
}


class MaxQuantityBrokerXXXDimension(
    override val dimensional: String
) : IChildDimensionHandler<TradeAccount.EnableMarket, String, TradeStockInfo?, BigDecimal> {

    override fun isAffect(
        parentDimensional: TradeAccount.EnableMarket,
        param: TradeStockInfo?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getResult(param: TradeStockInfo?): BigDecimal {
        TODO("Not yet implemented")
    }
}


fun getMaxQuantity(
    brokerId: Int,
    market: TradeAccount.EnableMarket,
    xxx: String,
    stock: TradeStockInfo?
): BigDecimal {
    val brokerDimension = MaxQuantityBrokerDimension(brokerId).withChild(MaxQuantityBrokerXXXDimension(xxx))
    return MaxQuantityMarketDimension().withChild(brokerDimension).getResult(stock)
}