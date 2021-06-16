package com.hyh.list.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.SystemClock
import android.util.SparseArray
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.coroutine.CloseableCoroutineScope
import com.hyh.coroutine.SingleRunner
import com.hyh.list.*
import com.hyh.list.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/7
 */
class MultiSourceAdapter<Param : Any>(
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "MultiSourceAdapter"
    }

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private val sourceAdapterCallback = SourceAdapterCallback()
    private var wrappers = mutableListOf<SourceAdapterWrapper>()
    private var receiver: UiReceiverForRepo<Param>? = null
    private val _loadStateFlow: MutableStateFlow<RepoLoadState> = MutableStateFlow(RepoLoadState.Initial)
    private val invokeEventCh = Channel<List<SuspendInvoke>>(Channel.BUFFERED)

    private var reusableHolder: WrapperAndLocalPosition = WrapperAndLocalPosition()
    private val binderLookup = IdentityHashMap<RecyclerView.ViewHolder, SourceAdapterWrapper>()
    private val viewTypeStorage: ViewTypeStorage = ViewTypeStorage.SharedIdRangeViewTypeStorage()

    suspend fun submitData(data: RepoData<Param>) {
        collectFromRunner.runInIsolation {
            GlobalScope.launch(mainDispatcher) {
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
                            _loadStateFlow.value = RepoLoadState.UsingCache(result.newWrappers.size)
                            invokeEventCh.send(result.invokes)
                        }
                        is RepoEvent.Loading -> {
                            _loadStateFlow.value = RepoLoadState.Loading
                        }
                        is RepoEvent.Error -> {
                            _loadStateFlow.value = RepoLoadState.Error(event.error, event.usingCache)
                        }
                        is RepoEvent.Success -> {
                            val result = updateWrappers(event.sources)
                            _loadStateFlow.value = RepoLoadState.Success(result.newWrappers.size)
                            invokeEventCh.send(result.invokes)
                        }
                    }
                }
            }
        }
    }

    fun refresh(param: Param) {
        receiver?.refresh(param)
    }

    private suspend fun submitData(wrapper: SourceAdapterWrapper, flow: Flow<SourceData<out Any>>) {
        val context: CoroutineContext = SupervisorJob() + mainDispatcher
        val job = CloseableCoroutineScope(context).launch {
            flow.collectLatest {
                wrapper.adapter.submitData(it)
            }
        }
        wrapper.initialized = true
        wrapper.submitDataJob = job
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateWrappers(sources: List<LazySourceData<out Any>>): UpdateWrappersResult {
        val invokes: MutableList<SuspendInvoke> = mutableListOf()
        val oldWrappers = wrappers
        val oldSourceTokens = oldWrappers.map { it.sourceToken }
        val newSourceTokens = mutableListOf<Any>()
        val newWrappers = mutableListOf<SourceAdapterWrapper>()
        sources.forEach {
            newSourceTokens.add(it.sourceToken)
            val oldWrapper = oldWrappers.findWrapper(it.sourceToken)
            if (oldWrapper != null) {
                oldWrapper.paramProvider = it.paramProvider as IParamProvider<Any>
                newWrappers.add(oldWrapper)
                invokes.add {
                    val param = oldWrapper.paramProvider.getParam()
                    oldWrapper.adapter.refresh(param)
                }
            } else {
                val wrapper = createWrapper(it)
                newWrappers.add(wrapper)
                invokes.add {
                    //submitData(wrapper, it.lazyFlow.await())
                }
            }
        }

        val removedWrappers = mutableListOf<SourceAdapterWrapper>()

        wrappers = newWrappers

        if (oldWrappers.isEmpty() || newWrappers.isEmpty()) {
            if (oldWrappers.isNotEmpty()) {
                removedWrappers.addAll(oldWrappers)
            }
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(oldSourceTokens, newSourceTokens))
            val updateOperates: MutableList<UpdateOperate> = mutableListOf()
            diffResult.dispatchUpdatesTo(SourceWrappersUpdateCallback(updateOperates))
            DispatchUpdatesHelper.dispatch(
                updateOperates,
                ArrayList(oldWrappers),
                newWrappers,
                removedWrappers
            ).forEach {
                it.invoke(this)
            }
        }

        val destroyInvokes: List<SuspendInvoke> = removedWrappers.map { wrapper ->
            suspend {
                wrapper.destroy()
            }
        }
        invokes.addAll(0, destroyInvokes)

        return UpdateWrappersResult(
            newWrappers,
            invokes
        )
    }

    override fun getItemId(position: Int): Long {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val itemId = wrapperAndPos.wrapper?.adapter?.getItemId(wrapperAndPos.localPosition) ?: 0L
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return itemId
    }

    override fun getItemViewType(position: Int): Int {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val itemViewType = wrapperAndPos.wrapper?.getItemViewType(position) ?: 0
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return itemViewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val wrapper = viewTypeStorage.getWrapperForGlobalType(viewType)
        return wrapper.onCreateViewHolder(parent, viewType)
    }

    override fun getItemCount(): Int {
        var total = 0
        for (wrapper in wrappers) {
            total += wrapper.cachedItemCount
        }
        return total
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        wrapperAndPos.wrapper?.onBindViewHolder(holder, wrapperAndPos.localPosition)
        binderLookup[holder] = wrapperAndPos.wrapper
        releaseWrapperAndLocalPosition(wrapperAndPos)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        binderLookup[holder]?.adapter?.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        binderLookup[holder]?.adapter?.onViewDetachedFromWindow(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val wrapper: SourceAdapterWrapper = binderLookup.remove(holder)
            ?: throw java.lang.IllegalStateException(
                "Cannot find wrapper for " + holder
                        + ", seems like it is not bound by this adapter: " + this
            )
        wrapper.adapter.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        val wrapper: SourceAdapterWrapper = binderLookup.remove(holder)
            ?: throw IllegalStateException(
                "Cannot find wrapper for " + holder
                        + ", seems like it is not bound by this adapter: " + this
            )
        return wrapper.adapter.onFailedToRecycleView(holder)
    }

    override fun findRelativeAdapterPositionIn(
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        viewHolder: RecyclerView.ViewHolder,
        globalPosition: Int
    ): Int {
        val wrapper: SourceAdapterWrapper = binderLookup[viewHolder] ?: return RecyclerView.NO_POSITION
        val itemsBefore = countItemsBefore(wrapper)
        // local position is globalPosition - itemsBefore
        // local position is globalPosition - itemsBefore
        val localPosition: Int = globalPosition - itemsBefore
        // Early error detection:
        // Early error detection:
        check(!(localPosition < 0 || localPosition >= wrapper.adapter.itemCount)) {
            ("Detected inconsistent adapter updates. The"
                    + " local position of the view holder maps to " + localPosition + " which"
                    + " is out of bounds for the adapter with size "
                    + wrapper.cachedItemCount + "."
                    + "Make sure to immediately call notify methods in your adapter when you "
                    + "change the backing data"
                    + "viewHolder:" + viewHolder
                    + "adapter:" + adapter)
        }
        return wrapper.adapter.findRelativeAdapterPositionIn(adapter, viewHolder, localPosition)
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        throw UnsupportedOperationException(
            "Calling setHasStableIds is not allowed on the ConcatAdapter. "
                    + "Use the Config object passed in the constructor to control this "
                    + "behavior"
        )
    }

    override fun setStateRestorationPolicy(strategy: StateRestorationPolicy) {
        throw java.lang.UnsupportedOperationException(
            "Calling setStateRestorationPolicy is not allowed on the ConcatAdapter."
                    + " This value is inferred from added adapters"
        )
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
            sourceData.paramProvider as IParamProvider<Any>,
            SourceAdapter(workerDispatcher),
            viewTypeStorage,
            sourceAdapterCallback
        ).apply {
            val items = mutableListOf<ItemData>()
            for (index in 0 until 6) {
                items.add(NumItemData(sourceData.sourceToken.toString(), index))
            }
            adapter.setData(items)
        }
    }


    class NumItemData(
        private val type: String,
        private val num: Int
    ) : IItemData<RecyclerView.ViewHolder> {

        override fun getItemViewType(): Int {
            return 0
        }

        override fun getViewHolderFactory(): ViewHolderFactory {
            return {
                SystemClock.sleep(10)
                val textView = TextView(it.context)
                textView.setTextColor(Color.BLACK)
                textView.setBackgroundColor(Color.WHITE)
                textView.gravity = Gravity.CENTER
                textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
                object : RecyclerView.ViewHolder(textView) {}
            }
        }

        override fun areItemsTheSame(other: ItemData): Boolean {
            if(other !is NumItemData)return false
            return this.type == other.type && this.num == other.num
        }

        override fun areContentsTheSame(other: ItemData): Boolean {
            if(other !is NumItemData)return false
            return this.type == other.type && this.num == other.num
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            SystemClock.sleep(10)
            (viewHolder.itemView as TextView).text = "$type:$num"
        }
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
        for (wrapper in wrappers) {
            if (wrapper.cachedItemCount > localPosition) {
                result.wrapper = wrapper
                result.localPosition = localPosition
                break
            }
            localPosition -= wrapper.cachedItemCount
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

    private fun countItemsBefore(wrapper: SourceAdapterWrapper): Int {
        return countItemsBefore(wrapper, wrappers)
    }

    private fun countItemsBefore(wrapper: SourceAdapterWrapper, wrappers: List<SourceAdapterWrapper>): Int {
        var count = 0
        for (item in wrappers) {
            count += if (item !== wrapper) {
                item.cachedItemCount
            } else {
                break
            }
        }
        return count
    }

    private fun countItemsBefore(position: Int, wrappers: List<SourceAdapterWrapper>): Int {
        var count = 0
        for (index in 0 until position) {
            count += wrappers[index].cachedItemCount
        }
        return count
    }

    private fun calculateAndUpdateStateRestorationPolicy() {
        val newPolicy = computeStateRestorationPolicy()
        if (newPolicy != stateRestorationPolicy) {
            internalSetStateRestorationPolicy(newPolicy)
        }
    }

    private fun computeStateRestorationPolicy(): StateRestorationPolicy {
        for (wrapper in wrappers) {
            val strategy = wrapper.adapter.stateRestorationPolicy
            if (strategy == StateRestorationPolicy.PREVENT) {
                // one adapter can block all
                return StateRestorationPolicy.PREVENT
            } else if (strategy == StateRestorationPolicy.PREVENT_WHEN_EMPTY && wrapper.cachedItemCount == 0) {
                // an adapter wants to allow w/ size but we need to make sure there is no prevent
                return StateRestorationPolicy.PREVENT
            }
        }
        return StateRestorationPolicy.ALLOW
    }

    private fun internalSetStateRestorationPolicy(strategy: StateRestorationPolicy) {
        super.setStateRestorationPolicy(strategy)
    }

    // region inner class

    class UpdateWrappersResult(
        val newWrappers: List<SourceAdapterWrapper>,
        val invokes: List<SuspendInvoke>
    )

    inner class SourceAdapterCallback : SourceAdapterWrapper.Callback {

        override fun onChanged(wrapper: SourceAdapterWrapper) {
            val offset = countItemsBefore(wrapper)
            notifyItemRangeChanged(offset, wrapper.cachedItemCount)
            calculateAndUpdateStateRestorationPolicy()
        }

        override fun onItemRangeChanged(wrapper: SourceAdapterWrapper, positionStart: Int, itemCount: Int) {
            val offset = countItemsBefore(wrapper)
            notifyItemRangeChanged(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeChanged(wrapper: SourceAdapterWrapper, positionStart: Int, itemCount: Int, payload: Any?) {
            val offset = countItemsBefore(wrapper)
            notifyItemRangeChanged(
                positionStart + offset,
                itemCount,
                payload
            );
        }

        override fun onItemRangeInserted(wrapper: SourceAdapterWrapper, positionStart: Int, itemCount: Int) {
            val offset = countItemsBefore(wrapper)
            notifyItemRangeInserted(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeRemoved(wrapper: SourceAdapterWrapper, positionStart: Int, itemCount: Int) {
            val offset = countItemsBefore(wrapper)
            notifyItemRangeRemoved(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeMoved(wrapper: SourceAdapterWrapper, fromPosition: Int, toPosition: Int) {
            val offset = countItemsBefore(wrapper)
            notifyItemMoved(
                fromPosition + offset,
                toPosition + offset
            )
        }

        override fun onStateRestorationPolicyChanged(wrapper: SourceAdapterWrapper?) {
            calculateAndUpdateStateRestorationPolicy()
        }
    }

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
        var wrapper: SourceAdapterWrapper? = null,
        var localPosition: Int = -1,
        var inUse: Boolean = false
    )

    // endregion
}

typealias SuspendInvoke = (suspend () -> Unit)

class SourceAdapterWrapper(
    val sourceToken: Any,
    var paramProvider: IParamProvider<Any>,
    val adapter: SourceAdapter,
    viewTypeStorage: ViewTypeStorage,
    val callback: Callback
) {


    var initialized = false
    var submitDataJob: Job? = null
    private var _cachedItemCount = adapter.itemCount
    /*val cachedItemCount
        get() = _cachedItemCount*/
    val cachedItemCount
        get() = adapter.itemCount

    private val viewTypeLookup: ViewTypeStorage.ViewTypeLookup = viewTypeStorage.createViewTypeWrapper(this)
    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            val oldItemCount = _cachedItemCount
            val newItemCount = adapter.itemCount
            if (oldItemCount > 0) {
                _cachedItemCount = 0
                callback.onItemRangeRemoved(
                    this@SourceAdapterWrapper,
                    0,
                    oldItemCount
                )
            }
            _cachedItemCount = newItemCount
            callback.onItemRangeInserted(
                this@SourceAdapterWrapper,
                0,
                newItemCount
            )
            if (newItemCount > 0
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@SourceAdapterWrapper)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            callback.onItemRangeChanged(
                this@SourceAdapterWrapper,
                positionStart,
                itemCount,
                null
            )
        }

        override fun onItemRangeChanged(
            positionStart: Int, itemCount: Int,
            payload: Any?
        ) {
            callback.onItemRangeChanged(
                this@SourceAdapterWrapper,
                positionStart,
                itemCount,
                payload
            )
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            _cachedItemCount += itemCount
            callback.onItemRangeInserted(
                this@SourceAdapterWrapper,
                positionStart,
                itemCount
            )
            if (_cachedItemCount > 0
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@SourceAdapterWrapper)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            _cachedItemCount -= itemCount
            callback.onItemRangeRemoved(
                this@SourceAdapterWrapper,
                positionStart,
                itemCount
            )
            if (_cachedItemCount < 1
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@SourceAdapterWrapper)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            callback.onItemRangeMoved(
                this@SourceAdapterWrapper,
                fromPosition,
                toPosition
            )
        }

        override fun onStateRestorationPolicyChanged() {
            callback.onStateRestorationPolicyChanged(this@SourceAdapterWrapper)
        }
    }

    init {
        adapter.registerAdapterDataObserver(adapterObserver)
    }

    fun getItemViewType(localPosition: Int): Int {
        return viewTypeLookup.localToGlobal(adapter.getItemViewType(localPosition))
    }

    fun onCreateViewHolder(
        parent: ViewGroup,
        globalViewType: Int
    ): RecyclerView.ViewHolder {
        val localType: Int = viewTypeLookup.globalToLocal(globalViewType)
        return adapter.onCreateViewHolder(parent, localType)
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, localPosition: Int) {
        adapter.bindViewHolder(viewHolder, localPosition)
    }

    fun destroy() {
        adapter.unregisterAdapterDataObserver(adapterObserver)
        viewTypeLookup.dispose()
        submitDataJob?.cancel()
    }

    interface Callback {

        fun onChanged(wrapper: SourceAdapterWrapper)

        fun onItemRangeChanged(
            wrapper: SourceAdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeChanged(
            wrapper: SourceAdapterWrapper,
            positionStart: Int,
            itemCount: Int,
            payload: Any?
        )

        fun onItemRangeInserted(
            wrapper: SourceAdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeRemoved(
            wrapper: SourceAdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeMoved(
            wrapper: SourceAdapterWrapper,
            fromPosition: Int,
            toPosition: Int
        )

        fun onStateRestorationPolicyChanged(wrapper: SourceAdapterWrapper?)
    }
}

interface ViewTypeStorage {

    fun getWrapperForGlobalType(globalViewType: Int): SourceAdapterWrapper

    fun createViewTypeWrapper(
        wrapper: SourceAdapterWrapper
    ): ViewTypeLookup


    interface ViewTypeLookup {
        fun localToGlobal(localType: Int): Int
        fun globalToLocal(globalType: Int): Int
        fun dispose()
    }

    class SharedIdRangeViewTypeStorage : ViewTypeStorage {
        // we keep a list of nested wrappers here even though we only need 1 to create because
        // they might be removed.
        var globalTypeToWrapper = SparseArray<MutableList<SourceAdapterWrapper>>()

        override fun getWrapperForGlobalType(globalViewType: Int): SourceAdapterWrapper {
            val nestedAdapterWrappers: List<SourceAdapterWrapper>? = globalTypeToWrapper[globalViewType]
            require(!(nestedAdapterWrappers == null || nestedAdapterWrappers.isEmpty())) {
                ("Cannot find the wrapper for global view"
                        + " type " + globalViewType)
            }
            // just return the first one since they are shared
            return nestedAdapterWrappers[0]
        }

        override fun createViewTypeWrapper(wrapper: SourceAdapterWrapper): ViewTypeLookup {
            return WrapperViewTypeLookup(wrapper)
        }

        fun removeWrapper(wrapper: SourceAdapterWrapper) {
            for (i in globalTypeToWrapper.size() - 1 downTo 0) {
                val wrappers = globalTypeToWrapper.valueAt(i)
                if (wrappers.remove(wrapper)) {
                    if (wrappers.isEmpty()) {
                        globalTypeToWrapper.removeAt(i)
                    }
                }
            }
        }

        internal inner class WrapperViewTypeLookup(private val wrapper: SourceAdapterWrapper) : ViewTypeLookup {
            override fun localToGlobal(localType: Int): Int {
                // register it first
                var wrappers = globalTypeToWrapper[localType]
                if (wrappers == null) {
                    wrappers = ArrayList()
                    globalTypeToWrapper.put(localType, wrappers)
                }
                if (!wrappers.contains(wrapper)) {
                    wrappers.add(wrapper)
                }
                return localType
            }

            override fun globalToLocal(globalType: Int): Int {
                return globalType
            }

            override fun dispose() {
                removeWrapper(wrapper)
            }
        }
    }
}


sealed class UpdateOperate {

    data class OnChanged(val position: Int, val count: Int) : UpdateOperate()
    data class OnMoved(val fromPosition: Int, val toPosition: Int) : UpdateOperate()
    data class OnInserted(val position: Int, val count: Int) : UpdateOperate()
    data class OnRemoved(val position: Int, val count: Int) : UpdateOperate()

}

class SourceWrappersUpdateCallback(
    private val updateOperates: MutableList<UpdateOperate>,
) : ListUpdateCallback {

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        updateOperates.add(UpdateOperate.OnChanged(position, count))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        updateOperates.add(UpdateOperate.OnMoved(fromPosition, toPosition))
    }

    override fun onInserted(position: Int, count: Int) {
        updateOperates.add(UpdateOperate.OnInserted(position, count))
    }

    override fun onRemoved(position: Int, count: Int) {
        updateOperates.add(UpdateOperate.OnRemoved(position, count))
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
}

object DispatchUpdatesHelper {

    fun dispatch(
        updateOperates: List<UpdateOperate>,
        oldWrappers: List<SourceAdapterWrapper>,
        newWrappers: List<SourceAdapterWrapper>,
        removedWrappers: MutableList<SourceAdapterWrapper>
    ): List<RecyclerView.Adapter<*>.() -> Unit> {
        val operateInvokes = mutableListOf<RecyclerView.Adapter<*>.() -> Unit>()

        val wrapperStubs = mutableListOf<WrapperStub>()
        wrapperStubs.addAll(oldWrappers.map { WrapperStub(wrapper = it) })

        updateOperates.forEach { operate ->
            when (operate) {
                is UpdateOperate.OnChanged -> {
                    onChanged(operate, wrapperStubs, newWrappers, operateInvokes)
                }
                is UpdateOperate.OnMoved -> {
                    onMoved(operate, wrapperStubs, newWrappers, operateInvokes)
                }
                is UpdateOperate.OnInserted -> {
                    onInserted(operate, wrapperStubs, newWrappers, operateInvokes)
                }
                is UpdateOperate.OnRemoved -> {
                    onRemoved(operate, wrapperStubs, newWrappers, removedWrappers, operateInvokes)
                }
            }
        }
        return operateInvokes
    }

    private fun onChanged(
        operate: UpdateOperate.OnChanged,
        wrapperStubs: MutableList<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        val wrapperStubsSnapshot = mutableListOf<WrapperStub>()
        wrapperStubsSnapshot.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.position,
                wrapperStubsSnapshot,
                wrapperStubs,
                newWrappers
            )
            val totalItemCount = countTotalItemCount(
                operate.position,
                operate.count,
                wrapperStubsSnapshot,
                wrapperStubs,
                newWrappers
            )
            notifyItemRangeChanged(offset, totalItemCount)
        }
        operateInvokes.add(invoke)
    }


    private fun onMoved(
        operate: UpdateOperate.OnMoved,
        wrapperStubs: MutableList<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>,
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
                val oldOffset = countItemsBefore(operate.fromPosition, beforeMoveWrapperStubs, wrapperStubs, newWrappers)

                notifyItemRangeRemoved(oldOffset, cachedItemCount)


                val newOffset = countItemsBefore(operate.toPosition, afterMoveWrapperStubs, wrapperStubs, newWrappers)
                notifyItemRangeInserted(newOffset, cachedItemCount)
            }
        }
        operateInvokes.add(invoke)
    }


    private fun onInserted(
        operate: UpdateOperate.OnInserted,
        wrapperStubs: MutableList<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        for (index in operate.position until (operate.position + operate.count)) {
            val wrapperStub = WrapperStub()
            wrapperStubs.add(index, wrapperStub)
        }
        val afterInsertedWrapperStubs = mutableListOf<WrapperStub>()
        afterInsertedWrapperStubs.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.position,
                afterInsertedWrapperStubs,
                wrapperStubs,
                newWrappers
            )
            val totalItemCount = countTotalItemCount(
                operate.position,
                operate.count,
                afterInsertedWrapperStubs,
                wrapperStubs,
                newWrappers
            )
            if (totalItemCount > 0) {
                notifyItemRangeInserted(offset, totalItemCount)
            }
        }
        operateInvokes.add(invoke)
    }

    private fun onRemoved(
        operate: UpdateOperate.OnRemoved,
        wrapperStubs: MutableList<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>,
        removedWrappers: MutableList<SourceAdapterWrapper>,
        operateInvokes: MutableList<RecyclerView.Adapter<*>.() -> Unit>
    ) {
        val beforeRemovedSnapshot = mutableListOf<WrapperStub>()
        beforeRemovedSnapshot.addAll(wrapperStubs)
        val removed = mutableListOf<WrapperStub>()
        for (index in operate.position until (operate.position + operate.count)) {
            val wrapperStub = wrapperStubs[index]
            removed.add(wrapperStub)
        }
        wrapperStubs.removeAll(removed)
        removedWrappers.addAll(removed.mapNotNull { it.wrapper })

        val wrapperStubsSnapshot = mutableListOf<WrapperStub>()
        wrapperStubsSnapshot.addAll(wrapperStubs)
        val invoke: RecyclerView.Adapter<*>.() -> Unit = {
            val offset = countItemsBefore(
                operate.position,
                beforeRemovedSnapshot,
                wrapperStubs,
                newWrappers
            )
            val totalItemCount = countTotalItemCount(
                operate.position,
                operate.count,
                beforeRemovedSnapshot,
                wrapperStubs,
                newWrappers
            )
            notifyItemRangeRemoved(offset, totalItemCount)
        }

        operateInvokes.add(invoke)
    }


    private fun countItemsBefore(
        wrapperStub: WrapperStub,
        wrapperStubsSnapshot: List<WrapperStub>,
        wrapperStubs: List<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>
    ): Int {
        var count = 0
        for (item in wrapperStubsSnapshot) {
            count += if (item !== wrapperStub) {
                val wrapper = item.wrapper
                if (wrapper != null) {
                    wrapper.cachedItemCount
                } else {
                    val index = wrapperStubs.indexOf(wrapperStub)
                    if (index in newWrappers.indices) {
                        newWrappers[index].cachedItemCount
                    } else {
                        0
                    }
                }
            } else {
                break
            }
        }
        return count
    }


    private fun countItemsBefore(
        position: Int,
        wrapperStubsSnapshot: List<WrapperStub>,
        wrapperStubs: List<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>
    ): Int {
        return countItemsBefore(
            wrapperStubsSnapshot[position],
            wrapperStubsSnapshot,
            wrapperStubs,
            newWrappers
        )
    }


    private fun countTotalItemCount(
        position: Int,
        wrapperCount: Int,
        wrapperStubsSnapshot: List<WrapperStub>,
        wrapperStubs: List<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>
    ): Int {
        var count = 0
        for (index in position until (position + wrapperCount)) {
            val wrapperStub = wrapperStubsSnapshot[index]
            val wrapper = wrapperStub.wrapper
            if (wrapper != null) {
                count += wrapper.cachedItemCount
            } else {
                val curIndex = wrapperStubs.indexOf(wrapperStub)
                if (curIndex in newWrappers.indices) {
                    count += newWrappers[curIndex].cachedItemCount
                }
            }
        }
        return count
    }

    private fun getWrapper(
        position: Int,
        wrapperStubsSnapshot: List<WrapperStub>,
        wrapperStubs: List<WrapperStub>,
        newWrappers: List<SourceAdapterWrapper>
    ): SourceAdapterWrapper? {
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
        var wrapper: SourceAdapterWrapper? = null
    )
}