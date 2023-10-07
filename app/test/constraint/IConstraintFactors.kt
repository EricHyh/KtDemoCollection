package cn.futu.trade.test.constraint

/**
 * TODO
 *
 * @author eriche 2023/9/24
 */
interface IConstraintFactors<Target>


class ConstraintContext<Factors : IConstraintFactors<*>> constructor(
    val factors: Factors
)