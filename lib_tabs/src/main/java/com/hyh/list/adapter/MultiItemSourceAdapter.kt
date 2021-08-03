package com.hyh.list.adapter

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.coroutine.CloseableCoroutineScope
import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.coroutine.SingleRunner
import com.hyh.list.FlatListItem
import com.hyh.list.RepoLoadState
import com.hyh.list.SourceLoadState
import com.hyh.list.internal.*
import com.hyh.page.PageContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * 管理多个[FlatListItemAdapter]
 *
 * @author eriche
 * @data 2021/6/7
 */
class MultiItemSourceAdapter<Param : Any>(
    private val pageContext: PageContext
) : MultiSourceAdapter(), IListAdapter<Param> {

    companion object {
        private const val TAG = "MultiItemSourceAdapter"
    }


    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private val sourceAdapterCallback = SourceAdapterCallback()
    private var wrapperMap = LinkedHashMap<Any, SourceAdapterWrapper>()
    private var receiver: UiReceiverForRepo<Param>? = null

    //private val _loadStateFlow: MutableStateFlow<RepoLoadState> = MutableStateFlow(RepoLoadState.Initial)
    private val _loadStateFlow: SimpleMutableStateFlow<RepoLoadState> = SimpleMutableStateFlow(RepoLoadState.Initial)
    override val repoLoadStateFlow: SimpleStateFlow<RepoLoadState>
        get() = _loadStateFlow.asStateFlow()

    private val viewTypeStorage: ViewTypeStorage = ViewTypeStorage.SharedIdRangeViewTypeStorage()

    override fun getViewTypeStorage(): ViewTypeStorage {
        return viewTypeStorage
    }

    override fun getItemDataAdapterWrappers(): List<AdapterWrapper> {
        return wrapperMap.values.toList()
    }

    init {
        pageContext.invokeOnDestroy {
            wrapperMap.forEach {
                it.value.destroy()
            }
            receiver?.destroy()
        }
    }

    override fun submitData(flow: Flow<RepoData<Param>>) {
        pageContext
            .lifecycleScope
            .launch {
                flow.collectLatest {
                    submitData(it)
                }
            }
    }

    override fun refreshRepo(param: Param) {
        receiver?.refresh(param)
    }

    override fun getSourceLoadState(sourceIndex: Int): SimpleStateFlow<SourceLoadState>? {
        if (sourceIndex !in 0 until wrapperMap.size) {
            return null
        }
        var index = 0
        wrapperMap.forEach {
            if (index == sourceIndex) {
                return@getSourceLoadState it.value.flatListItemAdapter.loadStateFlow
            }
            index++
        }
        return null
    }

    override fun getSourceLoadState(sourceToken: Any): SimpleStateFlow<SourceLoadState>? {
        val wrapper = wrapperMap[sourceToken] ?: return null
        return wrapper.flatListItemAdapter.loadStateFlow
    }

    override fun getItemSnapshot(): List<FlatListItem> {
        val itemSnapshot = mutableListOf<FlatListItem>()
        wrapperMap.forEach {
            itemSnapshot += it.value.flatListItemAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun getItemSnapshot(sourceIndexStart: Int, count: Int): List<FlatListItem> {
        val itemSnapshot = mutableListOf<FlatListItem>()
        findWrappers(sourceIndexStart, count).forEach {
            itemSnapshot += it.flatListItemAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun getItemSnapshot(sourceTokenStart: Any, count: Int): List<FlatListItem> {
        val itemSnapshot = mutableListOf<FlatListItem>()
        findWrappers(sourceTokenStart, count).forEach {
            itemSnapshot += it.flatListItemAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun refreshSources(important: Boolean) {
        wrapperMap.forEach {
            it.value.flatListItemAdapter.refresh(important)
        }
    }

    override fun refreshSources(vararg sourceIndexes: Int, important: Boolean) {
        var index = 0
        val sourceIndexList = sourceIndexes.toMutableList()
        wrapperMap.forEach {
            if (sourceIndexList.isEmpty()) return@refreshSources
            if (sourceIndexList.remove(index)) {
                it.value.flatListItemAdapter.refresh(important)
            }
            index++
        }
    }

    override fun refreshSources(vararg sourceTokens: Any, important: Boolean) {
        sourceTokens.forEach {
            wrapperMap[it]?.flatListItemAdapter?.refresh(important)
        }
    }

    override fun refreshSources(sourceIndexStart: Int, count: Int, important: Boolean) {
        findWrappers(sourceIndexStart, count).forEach {
            it.flatListItemAdapter.refresh(important)
        }
    }

    override fun refreshSources(sourceTokenStart: Any, count: Int, important: Boolean) {
        findWrappers(sourceTokenStart, count).forEach {
            it.flatListItemAdapter.refresh(important)
        }
    }

    private fun findWrappers(sourceIndexStart: Int, count: Int): List<SourceAdapterWrapper> {
        val wrappers = mutableListOf<SourceAdapterWrapper>()
        var index = 0
        val sourceIndexEnd = sourceIndexStart + count - 1
        kotlin.run {
            wrapperMap.forEach {
                if (index < sourceIndexStart) {
                    index++
                    return@forEach
                }
                if (index > sourceIndexEnd) {
                    return@run
                }
                wrappers += it.value
                index++
            }
        }
        return wrappers
    }

    private fun findWrappers(sourceTokenStart: Any, count: Int): List<SourceAdapterWrapper> {
        val wrappers = mutableListOf<SourceAdapterWrapper>()
        var sourceTokenMatched = false
        var addCount = 0
        kotlin.run {
            wrapperMap.forEach {
                if (!sourceTokenMatched) {
                    if (it.key == sourceTokenStart) {
                        sourceTokenMatched = true
                    } else {
                        return@forEach
                    }
                }
                wrappers += it.value
                addCount++
                if (addCount >= count) {
                    return@run
                }
            }
        }
        return wrappers
    }

    private suspend fun submitData(data: RepoData<Param>) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is RepoEvent.UsingCache -> {
                            val processedResult = event.processor.invoke()
                            processedResult.onResultUsed()
                            val result = updateWrappers(
                                processedResult.resultSources,
                                processedResult.listOperates
                            )
                            result.refreshInvokes.forEach {
                                it.invoke()
                            }
                            _loadStateFlow.value = RepoLoadState.UsingCache(result.newWrapperMap.size)
                        }
                        is RepoEvent.Loading -> {
                            _loadStateFlow.value = RepoLoadState.Loading
                        }
                        is RepoEvent.Error -> {
                            _loadStateFlow.value = RepoLoadState.Error(event.error, event.usingCache)
                        }
                        is RepoEvent.Success -> {
                            val processedResult = event.processor.invoke()
                            processedResult.onResultUsed()
                            val result = updateWrappers(
                                processedResult.resultSources,
                                processedResult.listOperates
                            )
                            result.refreshInvokes.forEach {
                                it.invoke()
                            }
                            _loadStateFlow.value = RepoLoadState.Success(result.newWrapperMap.size)
                        }
                    }
                    event.onReceived()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateWrappers(
        sources: List<LazySourceData>,
        listOperates: List<ListOperate>
    ): UpdateWrappersResult {
        val reuseInvokes: MutableList<Invoke> = mutableListOf()
        val newInvokes: MutableList<Invoke> = mutableListOf()
        val refreshInvokes: MutableList<(suspend () -> Unit)> = mutableListOf()
        val oldWrapperMap = wrapperMap

        val newSourceTokens = mutableListOf<Any>()
        val newWrapperMap = LinkedHashMap<Any, SourceAdapterWrapper>()

        sources.forEachIndexed { index, data ->
            newSourceTokens.add(data.sourceToken)
            val oldWrapper = oldWrapperMap[data.sourceToken]
            if (oldWrapper != null) {
                newWrapperMap[data.sourceToken] = oldWrapper
                /*reuseInvokes.add {
                    oldWrapper.reuse(index, data.itemSource)
                }*/
                refreshInvokes.add {
                    oldWrapper.refresh()
                }
            } else {
                val wrapper = createWrapper(data)
                newWrapperMap[data.sourceToken] = wrapper
                newInvokes.add {
                    onAdapterAdded(wrapper)
                }
                refreshInvokes.add {
                    wrapper.submitData(data.lazyFlow.value)
                }
            }
        }

        val removedWrappers = mutableListOf<AdapterWrapper>()

        wrapperMap = newWrapperMap
        reuseInvokes.forEach {
            it()
        }
        newInvokes.forEach {
            it()
        }

        if (oldWrapperMap.isEmpty() || newWrapperMap.isEmpty()) {
            if (oldWrapperMap.isNotEmpty()) {
                removedWrappers.addAll(oldWrapperMap.values)
            }
            notifyDataSetChanged()
        } else {
            SimpleDispatchUpdatesHelper.dispatch(
                listOperates,
                ArrayList(oldWrapperMap.values),
                ArrayList(newWrapperMap.values),
                removedWrappers
            ).forEach {
                it.invoke(this)
            }
        }

        removedWrappers.forEach {
            onAdapterRemoved(it)
            it.destroy()
        }

        return UpdateWrappersResult(
            newWrapperMap,
            refreshInvokes
        )
    }

    override fun findItemLocalInfo(view: View, recyclerView: RecyclerView): ItemLocalInfo? {
        val globalPosition = recyclerView.getChildAdapterPosition(view)
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(globalPosition)
        val wrapper: SourceAdapterWrapper? = binderLookup[viewHolder] as? SourceAdapterWrapper
        if (wrapper != null) {
            val itemsBefore = countItemsBefore(wrapper)
            val localPosition: Int = globalPosition - itemsBefore
            if (localPosition < 0 || localPosition >= wrapper.adapter.itemCount) {
                return findItemLocalInfo(globalPosition)
            }
            val item = wrapper.flatListItemAdapter.findItem(localPosition)
            if (item != null) {
                return ItemLocalInfo(wrapper.sourceToken, localPosition, wrapper.cachedItemCount, item)
            }
        }
        return findItemLocalInfo(globalPosition)
    }


    private fun findItemLocalInfo(globalPosition: Int): ItemLocalInfo? {
        var resultWrapper: SourceAdapterWrapper? = null
        var localPosition: Int = globalPosition
        for (wrapperEntry in wrapperMap) {
            if (wrapperEntry.value.cachedItemCount > localPosition) {
                resultWrapper = wrapperEntry.value
                break
            }
            localPosition -= wrapperEntry.value.cachedItemCount
        }
        if (resultWrapper == null) return null
        if (localPosition < 0 || localPosition >= resultWrapper.cachedItemCount) {
            Log.e(
                TAG,
                "findItemLocalInfo error: localPosition=$localPosition, itemCount=${resultWrapper.adapter.itemCount}"
            )
            return null
        }
        val item = resultWrapper.flatListItemAdapter.findItem(localPosition) ?: return null
        return ItemLocalInfo(resultWrapper.sourceToken, localPosition, resultWrapper.cachedItemCount, item)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createWrapper(sourceData: LazySourceData): SourceAdapterWrapper {
        return SourceAdapterWrapper(
            sourceData.sourceToken,
            FlatListItemAdapter(pageContext),
            viewTypeStorage,
            sourceAdapterCallback
        )
    }

    // region inner class

    class UpdateWrappersResult(
        val newWrapperMap: LinkedHashMap<Any, SourceAdapterWrapper>,
        val refreshInvokes: List<(suspend () -> Unit)>
    )

    // endregion
}

object SimpleDispatchUpdatesHelper {

    fun dispatch(
        listOperateOperates: List<ListOperate>,
        oldWrappers: List<AdapterWrapper>,
        newWrappers: List<AdapterWrapper>,
        removedWrappers: MutableList<AdapterWrapper>
    ): List<RecyclerView.Adapter<*>.() -> Unit> {
        val operateInvokes = mutableListOf<RecyclerView.Adapter<*>.() -> Unit>()

        val wrapperStubs = mutableListOf<WrapperStub>()
        wrapperStubs.addAll(oldWrappers.map { WrapperStub(wrapper = it) })

        listOperateOperates.forEach { operate ->
            when (operate) {
                is ListOperate.OnChanged -> {
                    onChanged(operate, wrapperStubs, operateInvokes)
                }
                is ListOperate.OnMoved -> {
                    onMoved(operate, wrapperStubs, newWrappers, operateInvokes)
                }
                is ListOperate.OnInserted -> {
                    onInserted(operate, wrapperStubs, operateInvokes)
                }
                is ListOperate.OnRemoved -> {
                    onRemoved(operate, wrapperStubs, removedWrappers, operateInvokes)
                }
                else -> {
                }
            }
        }
        return operateInvokes
    }

    private fun onChanged(
        operate: ListOperate.OnChanged,
        wrapperStubs: MutableList<WrapperStub>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        val wrapperStubsSnapshot = mutableListOf<WrapperStub>()
        wrapperStubsSnapshot.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.positionStart,
                wrapperStubsSnapshot
            )
            val totalItemCount = countTotalItemCount(
                operate.positionStart,
                operate.count,
                wrapperStubsSnapshot
            )
            notifyItemRangeChanged(offset, totalItemCount)
        }
        operateInvokes.add(invoke)
    }


    private fun onMoved(
        operate: ListOperate.OnMoved,
        wrapperStubs: MutableList<WrapperStub>,
        newWrappers: List<AdapterWrapper>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        val beforeMoveWrapperStubs = mutableListOf<WrapperStub>()
        beforeMoveWrapperStubs.addAll(wrapperStubs)
        move(wrapperStubs, operate.fromPosition, operate.toPosition)
        val afterMoveWrapperStubs = mutableListOf<WrapperStub>()
        afterMoveWrapperStubs.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val cachedItemCount = getWrapper(
                operate.fromPosition,
                beforeMoveWrapperStubs,
                wrapperStubs,
                newWrappers
            )?.cachedItemCount ?: 0
            if (cachedItemCount > 0) {
                val oldOffset = countItemsBefore(operate.fromPosition, beforeMoveWrapperStubs)
                notifyItemRangeRemoved(oldOffset, cachedItemCount)
                val newOffset = countItemsBefore(operate.toPosition, afterMoveWrapperStubs)
                notifyItemRangeInserted(newOffset, cachedItemCount)
            }
        }
        operateInvokes.add(invoke)
    }


    private fun onInserted(
        operate: ListOperate.OnInserted,
        wrapperStubs: MutableList<WrapperStub>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        for (index in operate.positionStart until (operate.positionStart + operate.count)) {
            val wrapperStub = WrapperStub()
            wrapperStubs.add(index, wrapperStub)
        }
        val afterInsertedWrapperStubs = mutableListOf<WrapperStub>()
        afterInsertedWrapperStubs.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.positionStart,
                afterInsertedWrapperStubs
            )
            val totalItemCount = countTotalItemCount(
                operate.positionStart,
                operate.count,
                afterInsertedWrapperStubs
            )
            if (totalItemCount > 0) {
                notifyItemRangeInserted(offset, totalItemCount)
            }
        }
        operateInvokes.add(invoke)
    }

    private fun onRemoved(
        operate: ListOperate.OnRemoved,
        wrapperStubs: MutableList<WrapperStub>,
        removedWrappers: MutableList<AdapterWrapper>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        val beforeRemovedSnapshot = mutableListOf<WrapperStub>()
        beforeRemovedSnapshot.addAll(wrapperStubs)
        val removed = mutableListOf<WrapperStub>()
        for (index in operate.positionStart until (operate.positionStart + operate.count)) {
            val wrapperStub = wrapperStubs[index]
            removed.add(wrapperStub)
        }
        wrapperStubs.removeAll(removed)
        removedWrappers.addAll(removed.mapNotNull { it.wrapper })

        val wrapperStubsSnapshot = mutableListOf<WrapperStub>()
        wrapperStubsSnapshot.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.positionStart,
                beforeRemovedSnapshot
            )
            val totalItemCount = countTotalItemCount(
                operate.positionStart,
                operate.count,
                beforeRemovedSnapshot
            )
            notifyItemRangeRemoved(offset, totalItemCount)
        }

        operateInvokes.add(invoke)
    }


    private fun countItemsBefore(
        position: Int,
        wrapperStubsSnapshot: List<WrapperStub>
    ): Int {
        return countItemsBefore(
            wrapperStubsSnapshot[position],
            wrapperStubsSnapshot
        )
    }

    private fun countItemsBefore(
        wrapperStub: WrapperStub,
        wrapperStubsSnapshot: List<WrapperStub>
    ): Int {
        var count = 0
        for (item in wrapperStubsSnapshot) {
            count += if (item !== wrapperStub) {
                val wrapper = item.wrapper
                wrapper?.cachedItemCount ?: 0
            } else {
                break
            }
        }
        return count
    }

    private fun countTotalItemCount(
        position: Int,
        wrapperCount: Int,
        wrapperStubsSnapshot: List<WrapperStub>
    ): Int {
        var count = 0
        for (index in position until (position + wrapperCount)) {
            val wrapperStub = wrapperStubsSnapshot[index]
            val wrapper = wrapperStub.wrapper
            if (wrapper != null) {
                count += wrapper.cachedItemCount
            }
        }
        return count
    }

    private fun getWrapper(
        position: Int,
        wrapperStubsSnapshot: List<WrapperStub>,
        wrapperStubs: List<WrapperStub>,
        newWrappers: List<AdapterWrapper>
    ): AdapterWrapper? {
        val wrapperStub = wrapperStubsSnapshot[position]
        val wrapper = wrapperStub.wrapper
        if (wrapper != null) return wrapper
        val index = wrapperStubs.indexOf(wrapperStub)
        if (index in newWrappers.indices) {
            return newWrappers[index]
        }
        return null
    }

    private fun <T> move(list: MutableList<T>, sourceIndex: Int, targetIndex: Int): Boolean {
        if (list.isNullOrEmpty()) return false
        val size = list.size
        if (size <= sourceIndex || size <= targetIndex) return false
        if (sourceIndex == targetIndex) {
            return true
        }
        list.add(targetIndex, list.removeAt(sourceIndex))
        return true
    }

    class WrapperStub(
        var wrapper: AdapterWrapper? = null
    )
}


class SourceAdapterWrapper(
    val sourceToken: Any,
    val flatListItemAdapter: FlatListItemAdapter,
    viewTypeStorage: ViewTypeStorage,
    callback: Callback
) : AdapterWrapper(flatListItemAdapter, viewTypeStorage, callback) {

    companion object {
        private const val TAG = "SourceAdapterWrapper"
    }

    private val coroutineScope = CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun submitData(flow: Flow<SourceData>) {
        coroutineScope.launch {
            flow.collectLatest {
                flatListItemAdapter.submitData(it)
            }
        }
    }

    fun refresh() {
        flatListItemAdapter.refresh(false)
    }

    override fun destroy() {
        super.destroy()
        coroutineScope.cancel()
        flatListItemAdapter.destroy()
    }
}