package com.hyh.load

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hyh.page.state.PageState

open class CacheDataPageStateController(
    private val pageDataRepository: IPageDataRepository
) {

    private var hasReceivedResultAtLeastOnce = false

    private val _pageStateLiveData: MutableLiveData<PageState>
    val pageStateLiveData: LiveData<PageState>
        get() = _pageStateLiveData

    init {
        val initialPageState: PageState = when {
            pageDataRepository.hasCache() -> {
                if (pageDataRepository.isEmptyData()) {
                    PageState.EMPTY
                } else {
                    PageState.SUCCESS
                }
            }
            else -> {
                PageState.LOADING
            }
        }
        _pageStateLiveData = MutableLiveData(initialPageState)
    }

    fun requestPageData() {
        if (pageDataRepository.hasCache()) {
            if (pageDataRepository.isEmptyData()) {
                PageState.EMPTY
            } else {
                PageState.SUCCESS
            }
            return
        }
        if (!hasReceivedResultAtLeastOnce) {
            _pageStateLiveData.postValue(PageState.LOADING)
        }
        pageDataRepository.request()
    }

    protected fun onRequested() {
        hasReceivedResultAtLeastOnce = true
        val pageState: PageState = when {
            pageDataRepository.hasCache() -> {
                if (pageDataRepository.isEmptyData()) {
                    PageState.EMPTY
                } else {
                    PageState.SUCCESS
                }
            }
            else -> {
                PageState.ERROR
            }
        }
        _pageStateLiveData.postValue(pageState)
    }

    private fun postValue(pageState: PageState) {

    }
}


interface IPageDataRepository {

    fun hasCache(): Boolean

    fun isEmptyData(): Boolean

    fun request()

}

object GlobalLoadState {



}