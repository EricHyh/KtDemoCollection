package lang

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import java.math.BigDecimal
import kotlin.concurrent.thread


data class Test(
    val i: Int, val any: Any
)


interface ItemType

internal class NoMoneyException(message: String?) : RuntimeException(message)


fun tryJoinCrazyThursday() {
    throw NoMoneyException("KFC Crazy Thursday whoever gives me \$50, I will thank him.")
}

fun main() {

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