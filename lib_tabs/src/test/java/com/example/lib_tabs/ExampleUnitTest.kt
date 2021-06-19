package com.example.lib_tabs

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
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

        val oldList = listOf<TestData>(
            TestData(0, "0"),
            TestData(1, "1"),
            TestData(2, "2"),
            TestData(3, "3"),
            TestData(4, "4"),
        )
        val newList = listOf<TestData>(
            TestData(0, "00"),
            TestData(1, "11"),
            TestData(3, "33"),
            TestData(4, "44"),
            TestData(5, "55"),
            TestData(2, "22"),
        )

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val areContentsTheSame = oldList[oldItemPosition].text == newList[newItemPosition].text
                println("areContentsTheSame:$oldItemPosition&$newItemPosition = $areContentsTheSame")
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
        })

        println("end")

        assertTrue(true)
    }


    class TestData(
        val id: Int,
        val text: String
    )
}