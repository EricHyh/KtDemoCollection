package com.hyh.kt_demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.thread


data class Test(
    val i: Int, val any: Any
)


interface ItemType

class NoMoneyException(msg: kotlin.String) : RuntimeException(msg)


fun tryJoinCrazyThursday() {
    throw NoMoneyException("KFC.kt Crazy Thursday whoever gives me \$50, I will thank him.")
}

fun formatValueWithPlaces(
    value: BigDecimal,
    minDecimalPlaces: Int,
    maxDecimalPlaces: Int = minDecimalPlaces
): String {

    fun createPattern(): String {
        if (maxDecimalPlaces <= 0) {
            return "##,###,##0"
        }
        val diff = if (maxDecimalPlaces < minDecimalPlaces) {
            0
        } else {
            maxDecimalPlaces - minDecimalPlaces
        }
        return "##,###,##0.${("0".repeat(minDecimalPlaces))}${("#".repeat(diff))}"
    }

    val decimalFormat = DecimalFormat(createPattern(), DecimalFormatSymbols(Locale.CHINA))
    decimalFormat.roundingMode = RoundingMode.FLOOR
    return decimalFormat.format(value)
}

private fun getKeepDecimalSizedNumber(price: String, keepDecimalSize: Int): String {
    if (price.contains(".")) {
        val num: Int = price.subSequence(price.indexOf(".") + 1, price.length).length
        if (num > keepDecimalSize) {
            val removeLength = num - keepDecimalSize
            return price.subSequence(0, price.length - removeLength).toString()
        }
    }
    return price
}


fun main() {

    var numStr = "99999999999"

    val keepDecimalSizedNumber = getKeepDecimalSizedNumber("10.0", 0)

    val toPlainString = keepDecimalSizedNumber.toBigDecimal().stripTrailingZeros().toPlainString()

    val str = "xxx{{t11p}}} good"
    val regex = "\\{\\{.*}}"
    val compile = Pattern.compile(regex)
    val matcher = compile.matcher(str)
    var start = -1
    var end = -1
    while (matcher.find()) {
        //需要找到最后一个
        start = matcher.start()
        end = matcher.end()
        break
    }
    val substring = str.substring(start + 2, end - 3)
    println("index = $start, $end, $substring")



    val formatValue1 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 0, 0
    )
    val formatValue2 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 1, 1
    )
    val formatValue3 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 1, 2
    )
    val formatValue4 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 1, 3
    )
    val formatValue5 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 2, 2
    )
    val formatValue6 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 2, 3
    )
    val formatValue7 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 3, 3
    )
    val formatValue8 = formatValueWithPlaces(
        BigDecimal("111111111230.116"), 3, 4
    )

    println("formatValue1 $formatValue1")
    println("formatValue2 $formatValue2")
    println("formatValue3 $formatValue3")
    println("formatValue4 $formatValue4")
    println("formatValue5 $formatValue5")
    println("formatValue6 $formatValue6")
    println("formatValue7 $formatValue7")
    println("formatValue8 $formatValue8")


    val bigDecimal = BigDecimal("0.0000000000000")
    val toString = bigDecimal.toString()
    val toBigDecimalOrNull = toString.toBigDecimalOrNull()


    val instance = Calendar.getInstance()
    instance.timeInMillis = System.currentTimeMillis()
    instance.timeZone = TimeZone.getTimeZone("America/New_York")
    //instance.set(Calendar.HOUR_OF_DAY, 25)
    //instance.set(Calendar.MINUTE, 60)

    val hour = instance.get(Calendar.HOUR_OF_DAY)
    val minute = instance.get(Calendar.MINUTE)
    val WEDNESDAY = instance.get(Calendar.WEDNESDAY)
    val DAY_OF_WEEK = instance.get(Calendar.DAY_OF_WEEK)
    val DAY_OF_WEEK_IN_MONTH = instance.get(Calendar.DAY_OF_WEEK_IN_MONTH)


    val quantity = "124300".toBigDecimal()

    val countFormat = DecimalFormat("###,###,###.#########", DecimalFormatSymbols(Locale.CHINA))
    val format = countFormat.format(quantity.stripTrailingZeros())

    println(format)


    val decimalFormat = DecimalFormat()
    decimalFormat.decimalFormatSymbols = DecimalFormatSymbols(Locale.CHINA)
    decimalFormat.isGroupingUsed = true
    val format1 = decimalFormat.format(1000)
    println(format1)




    listOf(1, 2, 3).onEach {
        println("onEach: $it")
    }

    println("-".toBigDecimalOrNull())

    val zero = BigDecimal.ZERO
    val toBigDecimal = "-1.00005500".toBigDecimal()

    println(zero.toPlainString())
    println(zero.negate().toPlainString())
    println(zero.negate().toPlainString())

    println(toBigDecimal.toPlainString())
    println(toBigDecimal.negate().toPlainString())

    println(zero.divide(BigDecimal(100), 5, RoundingMode.DOWN).stripTrailingZeros().toPlainString())
    println(toBigDecimal.divide(BigDecimal(100), 5, RoundingMode.DOWN).stripTrailingZeros().toPlainString())

    //tryJoinCrazyThursday()

    val newProxyInstance1 = Proxy.newProxyInstance(
        ItemType::class.java.classLoader, arrayOf(ItemType::class.java)
    ) { proxy, method, args ->

    }
    val newProxyInstance2 = Proxy.newProxyInstance(
        ItemType::class.java.classLoader, arrayOf(ItemType::class.java)
    ) { proxy, method, args ->

    }

    val kClass1 = newProxyInstance1::class
    val kClass2 = newProxyInstance2::class

    if (kClass1 == kClass2) {
        println("")
    }

    println("")


    val any = Any()
    val test1 = Test(1, any)
    val test2 = test1.copy(i = 2)

    println("test1=$test1")
    println("test2=$test2")


    val s1 = "1"

    val sortedBy = listOf<String>("3", "1", "2", "1").sortedBy {
        if (it == s1) {
            0
        } else {
            1
        }
    }.filter {
        it != "2"
    }

    println("")

    val x: BigDecimal

    //x.rem()


    thread {

        runBlocking {
            launch {
                repeat(100) {
                    println("index = $it")
                    delay(400)
                }
            }


            val nums = (1..3).asFlow().onEach { delay(300) } // 发射数字 1..3，间隔 300 毫秒
            val strs = flowOf("one", "two", "three").onEach { delay(400) } // 每 400 毫秒发射一次字符串
            val startTime = System.currentTimeMillis() // 记录开始的时间

            //nums.combineTransform()

            nums.combine(strs) { a, b -> "$a -> $b" } // 使用“combine”组合单个字符串
                .collect { value ->
                    // 收集并打印
                    println("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }



            flow<String> {

            }.collect() {

            }


            val xx: Flow<Int> = flowOf(1)

            xx.collect() {

            }

            fun getNum(): Int {
                return 1
            }

            ::getNum.asFlow()
                .collect() { a ->

                }



            flowOf("")
                .filter {
                    true
                }.map {
                    1
                }.flatMapConcat {
                    flowOf(1)
                }.onEmpty {
                    2
                }.onCompletion {

                }.onEach {

                }.collect {

                }
        }
    }
    Thread.sleep(200000)
}