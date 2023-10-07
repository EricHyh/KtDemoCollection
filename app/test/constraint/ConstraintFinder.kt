package cn.futu.trade.test.constraint

import java.lang.reflect.Type


/**
 * TODO
 *
 * @author eriche 2023/9/24
 */
object ConstraintFinder {

    private val constraintMap: MutableMap<ConstraintKey, Constraint<*, *, *>> = mutableMapOf()


    @Suppress("UNCHECKED_CAST")
    fun <Target> findConstraints(factors: IConstraintFactors<Target>): List<Pair<*, Constraint<*, Target, *>>> {
        val factorsType = factors.javaClass
        val fields = factors.javaClass.fields
        val constraintContext = ConstraintContext(factors)
        return fields.mapNotNull { field ->
            val constraintFactor = field.getAnnotation(ConstraintFactor::class.java) ?: return@mapNotNull null
            val name = constraintFactor.name
            val constraint = constraintMap[ConstraintKey(factorsType, name)]
                    as? Constraint<*, Target, IConstraintFactors<Target>> ?: return@mapNotNull null
            val fieldValue = field.get(factors)
            (fieldValue to constraint.also { it.injectContext(constraintContext) })
        }
    }

    private data class ConstraintKey constructor(
        val factorsType: Type,
        val factorName: String
    )
}