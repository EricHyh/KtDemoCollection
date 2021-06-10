package com.hyh.list.adapter

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.coroutine.SingleRunner
import com.hyh.list.RepoLoadState
import com.hyh.list.SourceLoadState
import com.hyh.list.internal.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.util.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/7
 */
class MultiSourceAdapter(
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "MultiSourceAdapter"
    }

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private val sourceAdapterCallback = SourceAdapterCallback()
    private var wrappers = mutableListOf<SourceAdapterWrapper>()
    private var reusableHolder: WrapperAndLocalPosition = WrapperAndLocalPosition()
    private val binderLookup = IdentityHashMap<RecyclerView.ViewHolder, SourceAdapterWrapper>()
    private var receiver: UiReceiverForRepo? = null
    private val _loadStateFlow: MutableStateFlow<RepoLoadState> = MutableStateFlow(RepoLoadState.Initial)

    fun submitData(lifecycle: Lifecycle, data: RepoData) {

    }

    suspend fun submitData(data: RepoData) {
        collectFromRunner.runInIsolation {





            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is RepoEvent.UsingCache -> {
                            val newWrappers = updateWrappers(event.sources)
                            _loadStateFlow.value = RepoLoadState.UsingCache(newWrappers.size)

                        }
                        is RepoEvent.Loading -> {
                            _loadStateFlow.value = RepoLoadState.Loading
                        }
                        is RepoEvent.Error -> {
                            _loadStateFlow.value = RepoLoadState.Error(event.error, event.usingCache)
                        }
                        is RepoEvent.Success -> {
                            val newWrappers = updateWrappers(event.sources)
                            _loadStateFlow.value = RepoLoadState.Success(newWrappers.size)
                        }
                    }
                }
            }
        }
    }

    private fun updateWrappers(sources: List<SourceData<out Any>>): MutableList<SourceAdapterWrapper> {
        val oldWrappers = wrappers
        val oldSourceTokens = oldWrappers.map { it.sourceToken }
        val newSourceTokens = mutableListOf<Any>()
        val newWrappers = mutableListOf<SourceAdapterWrapper>()
        sources.forEach {
            newSourceTokens.add(it.sourceToken)
            val oldWrapper = oldWrappers.findWrapper(it.sourceToken)
            if (oldWrapper != null) {
                newWrappers.add(oldWrapper)
            } else {
                newWrappers.add(createWrapper(it))
            }
        }
        wrappers = newWrappers
        if (oldWrappers.isEmpty() || newWrappers.isEmpty()) {
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(oldSourceTokens, newSourceTokens))
            diffResult.dispatchUpdatesTo(SourceWrappersUpdateCallback(oldWrappers, newWrappers))
        }
        return newWrappers
    }

    override fun getItemId(position: Int): Long {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val itemId = wrapperAndPos.wrapper?.adapter?.getItemId(wrapperAndPos.localPosition) ?: 0L
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return itemId
    }

    override fun getItemViewType(position: Int): Int {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val viewType = wrapperAndPos.wrapper?.let {
            val itemViewType = it.adapter.getItemViewType(wrapperAndPos.localPosition)
            itemViewType
        } ?: 0
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("Not yet implemented")
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
        wrapperAndPos.wrapper?.let {
            binderLookup[holder] = it
            it.adapter.onBindViewHolder(
                holder,
                wrapperAndPos.localPosition
            )
        }
        releaseWrapperAndLocalPosition(wrapperAndPos)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        wrapperAndPos.wrapper?.let {
            binderLookup[holder] = it
            it.adapter.onBindViewHolder(
                holder,
                wrapperAndPos.localPosition,
                payloads
            )
        }
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

    private fun createWrapper(sourceData: SourceData<out Any>): SourceAdapterWrapper {
        return SourceAdapterWrapper(
            sourceData.sourceToken,
            SourceAdapter(workerDispatcher),
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

    // region inner class


    private inner class SourceWrappersUpdateCallback(
        private val oldWrappers: List<SourceAdapterWrapper>,
        private val newWrappers: List<SourceAdapterWrapper>
    ) : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            val wrapper = newWrappers[position]
            val offset = countItemsBefore(wrapper, newWrappers)
            var totalItemCount = wrapper.cachedItemCount
            for (index in (position + 1) until (position + count)) {
                totalItemCount += newWrappers[index].cachedItemCount
            }
            notifyItemRangeChanged(offset, totalItemCount)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            val wrapper = oldWrappers[fromPosition]
            if (wrapper.cachedItemCount > 0) {
                val oldOffset = countItemsBefore(wrapper, oldWrappers)
                notifyItemRangeRemoved(oldOffset, wrapper.cachedItemCount)

                val newOffset = countItemsBefore(wrapper, newWrappers)
                notifyItemRangeInserted(newOffset, wrapper.cachedItemCount)
            }
        }

        override fun onInserted(position: Int, count: Int) {
            val wrapper = newWrappers[position]
            val offset = countItemsBefore(wrapper, newWrappers)
            var totalItemCount = wrapper.cachedItemCount
            for (index in (position + 1) until (position + count)) {
                totalItemCount += newWrappers[index].cachedItemCount
            }
            notifyItemRangeInserted(offset, totalItemCount)
        }

        override fun onRemoved(position: Int, count: Int) {
            val wrapper = oldWrappers[position]
            if (wrapper.cachedItemCount > 0) {
                val offset = countItemsBefore(wrapper, oldWrappers)
                notifyItemRangeRemoved(offset, wrapper.cachedItemCount)
            }
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

class SourceAdapterWrapper(
    val sourceToken: Any,
    val adapter: SourceAdapter,
    val callback: Callback
) {

    var initialized = false

    private var _cachedItemCount = 0
    val cachedItemCount
        get() = _cachedItemCount

    init {
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onChanged() {
                _cachedItemCount = adapter.itemCount
                callback.onChanged(this@SourceAdapterWrapper)
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
        })
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