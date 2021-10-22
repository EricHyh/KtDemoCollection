package com.hyh.paging3demo.list.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hyh.list.FlatListItem
import com.hyh.list.SimpleItemSource
import com.hyh.paging3demo.R

class TestInnerFragment : Fragment() {

    private val testInnerModel = TestInnerModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test_inner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}


interface ITestInnerModel {

    fun getIds(): List<Long>

    fun getVisibleIds(): List<Long>
}

class TestInnerModel : ITestInnerModel {


    override fun getIds(): List<Long> {
        return listOf()
    }

    override fun getVisibleIds(): List<Long> {
        return listOf()
    }
}

/*class UniversalItemSource(val id: Long) : SimpleItemSource<Unit>() {

    override val sourceToken: Any
        get() = id

    override suspend fun getParam() {
    }

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        return PreShowResult.Unused()
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {

    }
}*/

class NormalItemSource(val id: Long) : SimpleItemSource<Unit>() {

    override val sourceToken: Any
        get() = id

    override suspend fun getParam() {
        TODO("Not yet implemented")
    }

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        TODO("Not yet implemented")
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {
        TODO("Not yet implemented")
    }
}


class SettingItemSource() : SimpleItemSource<Unit>() {

    override val sourceToken: Any
        get() = SettingItemSource::class.java

    override suspend fun getParam() {
        TODO("Not yet implemented")
    }

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        TODO("Not yet implemented")
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {
        TODO("Not yet implemented")
    }
}

class WebItemSource() : SimpleItemSource<Unit>() {

    override val sourceToken: Any
        get() = WebItemSource::class.java

    override suspend fun getParam() {
        TODO("Not yet implemented")
    }

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        TODO("Not yet implemented")
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {
        TODO("Not yet implemented")
    }
}
