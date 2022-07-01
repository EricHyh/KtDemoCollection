package com.example.lib_tabs

import com.hyh.list.internal.utils.IElementDiff
import com.hyh.list.internal.utils.ListUpdate
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

        println("start")

        /*val oldList = listOf<TestData>(
            TestData(0, "0"),
            TestData(1, "1"),
            TestData(2, "2"),
            TestData(3, "3"),
            TestData(4, "4"),
            TestData(5, "5"),
            TestData(6, "6"),
        )
        val newList = listOf<TestData>(
            TestData(0, "00"),
            TestData(1, "1"),
            TestData(2, "2"),
            TestData(3, "3"),
            TestData(4, "4"),
            TestData(5, "5"),
            TestData(6, "6"),
            TestData(7, "7"),
            TestData(8, "8"),
            TestData(9, "9"),
            TestData(10, "10"),
            TestData(11, "11"),
        )*/

        val oldList = listOf<TestData>(
            TestData(0, "0"),
            TestData(0, "1"),
            TestData(0, "2"),
            TestData(1, "0"),
            TestData(0, "0"),
            TestData(0, "0"),
            TestData(2, "0"),
            TestData(0, "3")
        )
        val newList = listOf<TestData>(
            TestData(0, "0"),
            TestData(0, "0"),
            TestData(0, "2"),
            TestData(0, "3")
        )


        val result = ListUpdate.calculateDiff(oldList, newList, object : IElementDiff<TestData> {
            override fun isSupportUpdate(oldElement: TestData, newElement: TestData): Boolean {
                return false
            }

            override fun areItemsTheSame(oldElement: TestData, newElement: TestData): Boolean {
                return oldElement.id == newElement.id
            }

            override fun areContentsTheSame(oldElement: TestData, newElement: TestData): Boolean {
                return oldElement.text == newElement.text
            }

            override fun getChangePayload(oldElement: TestData, newElement: TestData): Any? {
                return "change"
            }
        })

        print("")


        /*val diffResult = TestDiffUtil.calculateDiff(object : TestDiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val areContentsTheSame = oldList[oldItemPosition].id == newList[newItemPosition].id
                println("areItemsTheSame:$oldItemPosition&$newItemPosition = $areContentsTheSame")
                return areContentsTheSame
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val areContentsTheSame = oldList[oldItemPosition].text == newList[newItemPosition].text
                //println("areContentsTheSame:$oldItemPosition&$newItemPosition = $areContentsTheSame")
                return areContentsTheSame
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                return newList[newItemPosition].text
            }
        })


        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
                println("onChanged: position=$position, count=$count, payload=$payload")
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                println("onMoved: fromPosition=$fromPosition, toPosition=$toPosition")
            }

            override fun onInserted(position: Int, count: Int) {
                println("onInserted: position=$position, count=$count")
            }

            override fun onRemoved(position: Int, count: Int) {
                println("onRemoved: position=$position, count=$count")
            }
        })*/

        println("end")

        assertTrue(true)
    }


    class TestData(
        val id: Int,
        val text: String
    )
}