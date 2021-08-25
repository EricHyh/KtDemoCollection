package com.hyh.list

/**
 * [ItemSourceRepo]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class RepoLoadState {

    object Initial : RepoLoadState()

    object Loading : RepoLoadState()

    data class UsingCache(val sourceCount: Int) : RepoLoadState()

    data class Success(val sourceCount: Int) : RepoLoadState()

    data class Error(val error: Throwable, val usingCache: Boolean) : RepoLoadState()

}


/**
 * [ItemSource]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class SourceLoadState {

    object Initial : SourceLoadState()

    object Loading : SourceLoadState()

    data class PreShow(val itemCount: Int) : SourceLoadState()

    data class Success(val itemCount: Int) : SourceLoadState()

    data class Error(val error: Throwable, val preShowing: Boolean) : SourceLoadState()

}


class SourceLoadStates(
    val sourceStateMap: Map<Any, SourceLoadState>,
) {

    companion object {
        val Initial = SourceLoadStates(emptyMap())
    }

    override fun hashCode(): Int {
        return sourceStateMap.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SourceLoadStates) return false
        if (this.sourceStateMap.size != other.sourceStateMap.size) return false
        if (this.sourceStateMap.isEmpty() && other.sourceStateMap.isEmpty()) return true
        this.sourceStateMap.forEach {
            if (it.value != other.sourceStateMap[it.key]) {
                return false
            }
        }
        return true
    }
}

data class SourceStateCount(
    val totalCount: Int,
    val initialCount: Int,
    val loadingCount: Int,
    val preShowCount: Int,
    val successCount: Int,
    val errorCount: Int
)