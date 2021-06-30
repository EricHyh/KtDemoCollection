package com.hyh.list.adapter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.SuspendInvoke
import com.hyh.coroutine.CloseableCoroutineScope
import com.hyh.coroutine.SingleRunner
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
import com.hyh.list.RepoLoadState
import com.hyh.list.SourceLoadState
import com.hyh.list.internal.*
import com.hyh.page.PageContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.coroutines.CoroutineContext

/**
 * 管理多个[SourceAdapter]
 *
 * @author eriche
 * @data 2021/6/7
 */
class SourceRepoAdapter<Param : Any>(
    private val pageContext: PageContext,
) : MultiSourceAdapter(), IListAdapter<Param> {

    companion object {
        private const val TAG = "MultiSourceAdapter"
    }


    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private val sourceAdapterCallback = SourceAdapterCallback()
    private var wrapperMap = LinkedHashMap<Any, SourceAdapterWrapper>()
    private var receiver: UiReceiverForRepo<Param>? = null
    private val invokeEventCh = Channel<List<SuspendInvoke>>(Channel.BUFFERED)
    private val _loadStateFlow: MutableStateFlow<RepoLoadState> = MutableStateFlow(RepoLoadState.Initial)
    override val repoLoadStateFlow: StateFlow<RepoLoadState>
        get() = _loadStateFlow

    private var reusableHolder: WrapperAndLocalPosition = WrapperAndLocalPosition()
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

    override fun getSourceLoadState(sourceIndex: Int): StateFlow<SourceLoadState>? {
        if (sourceIndex !in 0 until wrapperMap.size) {
            return null
        }
        var index = 0
        wrapperMap.forEach {
            if (index == sourceIndex) {
                return@getSourceLoadState it.value.sourceAdapter.loadStateFlow
            }
            index++
        }
        return null
    }

    override fun getSourceLoadState(sourceToken: Any): StateFlow<SourceLoadState>? {
        val wrapper = wrapperMap[sourceToken] ?: return null
        return wrapper.sourceAdapter.loadStateFlow
    }

    override fun getItemSnapshot(): List<ItemData> {
        val itemSnapshot = mutableListOf<ItemData>()
        wrapperMap.forEach {
            itemSnapshot += it.value.sourceAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun getItemSnapshot(sourceIndexStart: Int, count: Int): List<ItemData> {
        val itemSnapshot = mutableListOf<ItemData>()
        findWrappers(sourceIndexStart, count).forEach {
            itemSnapshot += it.sourceAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun getItemSnapshot(sourceTokenStart: Any, count: Int): List<ItemData> {
        val itemSnapshot = mutableListOf<ItemData>()
        findWrappers(sourceTokenStart, count).forEach {
            itemSnapshot += it.sourceAdapter.items ?: emptyList()
        }
        return itemSnapshot
    }

    override fun refreshSources(important: Boolean) {
        wrapperMap.forEach {
            it.value.sourceAdapter.refresh(important)
        }
    }

    override fun refreshSources(vararg sourceIndexes: Int, important: Boolean) {
        var index = 0
        val sourceIndexList = sourceIndexes.toMutableList()
        wrapperMap.forEach {
            if (sourceIndexList.isEmpty()) return@refreshSources
            if (sourceIndexList.remove(index)) {
                it.value.sourceAdapter.refresh(important)
            }
            index++
        }
    }

    override fun refreshSources(vararg sourceTokens: Any, important: Boolean) {
        sourceTokens.forEach {
            wrapperMap[it]?.sourceAdapter?.refresh(important)
        }
    }

    override fun refreshSources(sourceIndexStart: Int, count: Int, important: Boolean) {
        findWrappers(sourceIndexStart, count).forEach {
            it.sourceAdapter.refresh(important)
        }
    }

    override fun refreshSources(sourceTokenStart: Any, count: Int, important: Boolean) {
        findWrappers(sourceTokenStart, count).forEach {
            it.sourceAdapter.refresh(important)
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
            pageContext
                .lifecycleScope.launch {
                    invokeEventCh
                        .receiveAsFlow()
                        .collect { array ->
                            array.forEach {
                                it()
                            }
                        }
                }
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is RepoEvent.UsingCache -> {
                            val result = updateWrappers(event.sources)
                            _loadStateFlow.value = RepoLoadState.UsingCache(result.newWrapperMap.size)
                            invokeEventCh.send(result.refreshInvokes)
                        }
                        is RepoEvent.Loading -> {
                            _loadStateFlow.value = RepoLoadState.Loading
                        }
                        is RepoEvent.Error -> {
                            _loadStateFlow.value = RepoLoadState.Error(event.error, event.usingCache)
                        }
                        is RepoEvent.Success -> {
                            val result = updateWrappers(event.sources)
                            _loadStateFlow.value = RepoLoadState.Success(result.newWrapperMap.size)
                            invokeEventCh.send(result.refreshInvokes)
                        }
                    }
                    event.onReceived()
                }
            }
        }
    }

    private suspend fun submitData(wrapper: SourceAdapterWrapper, flow: Flow<SourceData>) {
        val context: CoroutineContext = SupervisorJob() + mainDispatcher
        val job = CloseableCoroutineScope(context).launch {
            flow.collectLatest {
                wrapper.sourceAdapter.submitData(it)
            }
        }
        wrapper.submitDataJob = job
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateWrappers(sources: List<LazySourceData<out Any>>): UpdateWrappersResult {
        val reuseInvokes: MutableList<Invoke> = mutableListOf()
        val newInvokes: MutableList<Invoke> = mutableListOf()
        val refreshInvokes: MutableList<SuspendInvoke> = mutableListOf()
        val oldWrapperMap = wrapperMap

        val oldSourceTokens = ArrayList(oldWrapperMap.keys)
        val newSourceTokens = mutableListOf<Any>()
        val newWrapperMap = LinkedHashMap<Any, SourceAdapterWrapper>()
        sources.forEach {
            newSourceTokens.add(it.sourceToken)
            val oldWrapper = oldWrapperMap[it.sourceToken]
            if (oldWrapper != null) {
                newWrapperMap[it.sourceToken] = oldWrapper
                reuseInvokes.add {
                    (it.onReuse as (oldItemSource: ItemSource<Any>) -> Unit).invoke(oldWrapper.itemSource)
                }
                refreshInvokes.add {
                    oldWrapper.sourceAdapter.refresh(false)
                }
            } else {
                val wrapper = createWrapper(it)
                newWrapperMap[it.sourceToken] = wrapper
                newInvokes.add {
                    onAdapterAdded(wrapper)
                }
                refreshInvokes.add {
                    submitData(wrapper, it.lazyFlow.await())
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
            val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(oldSourceTokens, newSourceTokens))
            val listOperateOperates: MutableList<ListOperate> = mutableListOf()
            diffResult.dispatchUpdatesTo(SourceWrappersUpdateCallback(listOperateOperates))
            SimpleDispatchUpdatesHelper.dispatch(
                listOperateOperates,
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
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(globalPosition) ?: return null
        val wrapper: SourceAdapterWrapper = binderLookup[viewHolder] as? SourceAdapterWrapper ?: return null

        val itemsBefore = countItemsBefore(wrapper)
        val localPosition: Int = globalPosition - itemsBefore

        if (localPosition < 0 || localPosition >= wrapper.adapter.itemCount) {
            Log.e(TAG, "findItemLocalInfo error: localPosition=$localPosition, itemCount=${wrapper.adapter.itemCount}",)
            return null
        }

        return ItemLocalInfo(wrapper.sourceToken, localPosition, wrapper.cachedItemCount)
    }


    private fun List<SourceAdapterWrapper>.findWrapper(sourceToken: Any): SourceAdapterWrapper? {
        forEach {
            if (it.sourceToken == sourceToken) {
                return it
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun createWrapper(sourceData: LazySourceData<out Any>): SourceAdapterWrapper {
        return SourceAdapterWrapper(
            sourceData.sourceToken,
            sourceData.itemSource as ItemSource<Any>,
            SourceAdapter(),
            viewTypeStorage,
            sourceAdapterCallback
        )
    }

    private fun findWrapperAndLocalPosition(globalPosition: Int): WrapperAndLocalPosition {
        val result: WrapperAndLocalPosition
        if (reusableHolder.inUse) {
            result = WrapperAndLocalPosition()
        } else {
            reusableHolder.inUse = true
            result = reusableHolder
        }
        var localPosition = globalPosition
        for (wrapper in wrapperMap) {
            if (wrapper.value.cachedItemCount > localPosition) {
                result.wrapper = wrapper.value
                result.localPosition = localPosition
                break
            }
            localPosition -= wrapper.value.cachedItemCount
        }
        requireNotNull(result.wrapper) { "Cannot find wrapper for $globalPosition" }
        return result
    }

    private fun releaseWrapperAndLocalPosition(wrapperAndPos: WrapperAndLocalPosition) {
        wrapperAndPos.inUse = false
        wrapperAndPos.wrapper = null
        wrapperAndPos.localPosition = -1
        reusableHolder = wrapperAndPos
    }

    // region inner class

    class UpdateWrappersResult(
        val newWrapperMap: LinkedHashMap<Any, SourceAdapterWrapper>,
        val refreshInvokes: List<SuspendInvoke>
    )


    private class DiffUtilCallback(
        private val oldSourceTokens: List<Any>,
        private val newSourceTokens: List<Any>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldSourceTokens.size
        }

        override fun getNewListSize(): Int {
            return newSourceTokens.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val (oldSourceToken, newSourceToken) = getSourceToken(oldItemPosition, newItemPosition)
            return oldSourceToken == newSourceToken
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val (oldSourceToken, newSourceToken) = getSourceToken(oldItemPosition, newItemPosition)
            return oldSourceToken == newSourceToken
        }

        private fun getSourceToken(oldItemPosition: Int, newItemPosition: Int): Pair<Any?, Any?> {
            val oldSourceToken = if (oldItemPosition in oldSourceTokens.indices) {
                oldSourceTokens[oldItemPosition]
            } else {
                null
            }
            val newSourceToken = if (newItemPosition in newSourceTokens.indices) {
                newSourceTokens[newItemPosition]
            } else {
                null
            }
            return Pair(oldSourceToken, newSourceToken)
        }
    }

    private data class WrapperAndLocalPosition(
        var wrapper: AdapterWrapper? = null,
        var localPosition: Int = -1,
        var inUse: Boolean = false
    )

    // endregion
}

class SourceWrappersUpdateCallback(
    private val listOperateOperates: MutableList<ListOperate>,
) : ListUpdateCallback {

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        listOperateOperates.add(ListOperate.OnChanged(position, count))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        listOperateOperates.add(ListOperate.OnMoved(fromPosition, toPosition))
    }

    override fun onInserted(position: Int, count: Int) {
        listOperateOperates.add(ListOperate.OnInserted(position, count))
    }

    override fun onRemoved(position: Int, count: Int) {
        listOperateOperates.add(ListOperate.OnRemoved(position, count))
    }
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
    var itemSource: ItemSource<Any>,
    val sourceAdapter: SourceAdapter,
    viewTypeStorage: ViewTypeStorage,
    callback: Callback
) : AdapterWrapper(sourceAdapter, viewTypeStorage, callback) {

    var submitDataJob: Job? = null

    override fun destroy() {
        submitDataJob?.cancel()
        itemSource.delegate.detach()
        sourceAdapter.destroy()
    }
}