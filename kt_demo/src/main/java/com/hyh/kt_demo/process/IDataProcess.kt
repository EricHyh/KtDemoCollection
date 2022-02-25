package com.hyh.kt_demo.process

import java.util.*

/**
 * 数据处理器
 *
 * @author eriche 2022/1/21
 */
interface IDataProcess<Input : Any, Output : Any> {

    /**
     * 处理数据，将输入值转换为输出值
     *
     * @param input 输入值
     * @return 输出值
     */
    fun process(input: Input): Output

}

/**
 * 为当前数据处理器拼接一个数据处理器
 *
 * @param Input 输入类型
 * @param Mid 第一个数据处理器的输出类型，第二个数据处理器的输入类型
 * @param Output 输出类型
 * @param next 下一个数据处理器
 * @return 拼接后的数据处理器
 */
fun <Input : Any, Mid : Any, Output : Any> IDataProcess<Input, Mid>.concat(
    next: IDataProcess<Mid, Output>
): IDataProcess<Input, Output> {
    return ConcatDataProcess(this, next)
}

/**
 * 将两个数据处理器拼接成一个数据处理器
 *
 * @param Input 输入类型
 * @param Mid 第一个数据处理器的输出类型，第二个数据处理器的输入类型
 * @param Output 输出类型
 * @property first 第一个数据处理器
 * @property second 第二个数据处理器
 */
private class ConcatDataProcess<Input : Any, Mid : Any, Output : Any>(
    private val first: IDataProcess<Input, Mid>, private val second: IDataProcess<Mid, Output>
) : IDataProcess<Input, Output> {


    override fun process(input: Input): Output {
        val process = first.process(input)
        return second.process(process)
    }
}