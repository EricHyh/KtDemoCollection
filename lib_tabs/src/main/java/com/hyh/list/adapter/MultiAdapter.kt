package com.hyh.list.adapter

import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.internal.utils.ListUpdate
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * 管理多个[RecyclerView.Adapter]
 *
 * @author eriche
 * @data 2021/6/7
 */
abstract class MultiSourceAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "MultiSourceAdapter"
    }

    protected val binderLookup = IdentityHashMap<RecyclerView.ViewHolder, AdapterWrapper>()
    private var reusableHolder: WrapperAndLocalPosition = WrapperAndLocalPosition()
    protected val attachedRecyclerViews: MutableList<WeakReference<RecyclerView>> = mutableListOf()

    private val wrappers: List<AdapterWrapper>
        get() = getItemDataAdapterWrappers()

    protected abstract fun getItemDataAdapterWrappers(): List<AdapterWrapper>

    protected abstract fun getViewTypeStorage(): ViewTypeStorage

    protected open fun isWrapperValid(wrapper: AdapterWrapper): Boolean {
        return wrappers.find {
            it.adapter == wrapper.adapter
        } != null
    }

    override fun getItemId(position: Int): Long {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val itemId = wrapperAndPos.wrapper?.adapter?.getItemId(wrapperAndPos.localPosition) ?: 0L
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return itemId
    }

    override fun getItemViewType(position: Int): Int {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        val itemViewType = wrapperAndPos.wrapper?.getItemViewType(wrapperAndPos.localPosition) ?: 0
        releaseWrapperAndLocalPosition(wrapperAndPos)
        return itemViewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val wrapper = getViewTypeStorage().getWrapperForGlobalType(viewType)
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val wrapperAndPos = findWrapperAndLocalPosition(position)
        wrapperAndPos.wrapper?.onBindViewHolder(holder, wrapperAndPos.localPosition, payloads)
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
        if (isAttachedTo(recyclerView)) {
            return
        }
        attachedRecyclerViews.add(WeakReference(recyclerView))
        for (wrapper in wrappers) {
            wrapper.adapter.onAttachedToRecyclerView(recyclerView)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        for (index in attachedRecyclerViews.indices.reversed()) {
            val reference: WeakReference<RecyclerView> = attachedRecyclerViews[index]
            if (reference.get() == null) {
                attachedRecyclerViews.removeAt(index)
            } else if (reference.get() === recyclerView) {
                attachedRecyclerViews.removeAt(index)
                break // here we can break as we don't keep duplicates
            }
        }
        for (wrapper in wrappers) {
            wrapper.adapter.onDetachedFromRecyclerView(recyclerView)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val wrapper: AdapterWrapper = binderLookup.remove(holder)
            ?: throw java.lang.IllegalStateException(
                "Cannot find wrapper for " + holder
                        + ", seems like it is not bound by this adapter: " + this
            )
        wrapper.adapter.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        val wrapper: AdapterWrapper = binderLookup.remove(holder)
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
        val wrapper: AdapterWrapper = binderLookup[viewHolder] ?: return RecyclerView.NO_POSITION
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

    protected fun onAdapterAdded(wrapper: AdapterWrapper) {
        for (reference in attachedRecyclerViews) {
            val recyclerView = reference.get()
            if (recyclerView != null) {
                wrapper.adapter.onAttachedToRecyclerView(recyclerView)
            }
        }
    }

    protected fun onAdapterRemoved(wrapper: AdapterWrapper) {
        for (reference in attachedRecyclerViews) {
            val recyclerView = reference.get()
            if (recyclerView != null) {
                wrapper.adapter.onDetachedFromRecyclerView(recyclerView)
            }
        }
    }

    private fun isAttachedTo(recyclerView: RecyclerView): Boolean {
        for (reference in attachedRecyclerViews) {
            if (reference.get() === recyclerView) {
                return true
            }
        }
        return false
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

    protected fun countItemsBefore(wrapper: AdapterWrapper): Int {
        return countItemsBefore(wrapper, wrappers)
    }

    protected fun countItemsBefore(wrapper: AdapterWrapper, wrappers: Collection<AdapterWrapper>): Int {
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

    protected fun countItemsBefore(wrapper: AdapterWrapper, wrappers: LinkedHashMap<Any, AdapterWrapper>): Int {
        var count = 0
        for (item in wrappers) {
            count += if (item.value !== wrapper) {
                item.value.cachedItemCount
            } else {
                break
            }
        }
        return count
    }

    protected fun countItemsBefore(position: Int, wrappers: List<AdapterWrapper>): Int {
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

    inner class SourceAdapterCallback : AdapterWrapper.Callback {

        override fun onChanged(wrapper: AdapterWrapper) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemRangeChanged(offset, wrapper.cachedItemCount)
            calculateAndUpdateStateRestorationPolicy()
        }

        override fun onItemRangeChanged(wrapper: AdapterWrapper, positionStart: Int, itemCount: Int) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemRangeChanged(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeChanged(wrapper: AdapterWrapper, positionStart: Int, itemCount: Int, payload: Any?) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemRangeChanged(
                positionStart + offset,
                itemCount,
                payload
            );
        }

        override fun onItemRangeInserted(wrapper: AdapterWrapper, positionStart: Int, itemCount: Int) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemRangeInserted(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeRemoved(wrapper: AdapterWrapper, positionStart: Int, itemCount: Int) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemRangeRemoved(
                positionStart + offset,
                itemCount
            )
        }

        override fun onItemRangeMoved(wrapper: AdapterWrapper, fromPosition: Int, toPosition: Int) {
            if (!isWrapperValid(wrapper)) return
            val offset = countItemsBefore(wrapper, wrappers)
            notifyItemMoved(
                fromPosition + offset,
                toPosition + offset
            )
        }

        override fun onStateRestorationPolicyChanged(wrapper: AdapterWrapper?) {
            calculateAndUpdateStateRestorationPolicy()
        }
    }

    private data class WrapperAndLocalPosition(
        var wrapper: AdapterWrapper? = null,
        var localPosition: Int = -1,
        var inUse: Boolean = false
    )

    // endregion
}

open class AdapterWrapper constructor(
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    viewTypeStorage: ViewTypeStorage,
    val callback: Callback
) {

    private var _cachedItemCount = 0
    val cachedItemCount
        get() = _cachedItemCount

    private val viewTypeLookup: ViewTypeStorage.ViewTypeLookup by lazy {
        viewTypeStorage.createViewTypeWrapper(this)
    }

    private val lock = Any()

    @Volatile
    private var adapterObserverRegistered = false

    @Volatile
    private var destroyed = false

    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            val oldItemCount = _cachedItemCount
            val newItemCount = adapter.itemCount
            if (oldItemCount > 0) {
                _cachedItemCount = 0
                callback.onItemRangeRemoved(
                    this@AdapterWrapper,
                    0,
                    oldItemCount
                )
            }
            _cachedItemCount = newItemCount
            callback.onItemRangeInserted(
                this@AdapterWrapper,
                0,
                newItemCount
            )
            if (newItemCount > 0
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@AdapterWrapper)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            callback.onItemRangeChanged(
                this@AdapterWrapper,
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
                this@AdapterWrapper,
                positionStart,
                itemCount,
                payload
            )
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            _cachedItemCount += itemCount
            callback.onItemRangeInserted(
                this@AdapterWrapper,
                positionStart,
                itemCount
            )
            if (_cachedItemCount > 0
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@AdapterWrapper)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            _cachedItemCount -= itemCount
            callback.onItemRangeRemoved(
                this@AdapterWrapper,
                positionStart,
                itemCount
            )
            if (_cachedItemCount < 1
                && adapter.stateRestorationPolicy == RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            ) {
                callback.onStateRestorationPolicyChanged(this@AdapterWrapper)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            callback.onItemRangeMoved(
                this@AdapterWrapper,
                fromPosition,
                toPosition
            )
        }

        override fun onStateRestorationPolicyChanged() {
            callback.onStateRestorationPolicyChanged(this@AdapterWrapper)
        }
    }

    init {
        synchronized(lock) {
            if (!destroyed && !adapterObserverRegistered) {
                adapter.registerAdapterDataObserver(adapterObserver)
                adapterObserverRegistered = true
            }
        }
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
        adapter.onBindViewHolder(viewHolder, localPosition)
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, localPosition: Int, payloads: MutableList<Any>) {
        adapter.onBindViewHolder(viewHolder, localPosition, payloads)
    }

    open fun destroy() {
        synchronized(lock) {
            destroyed = true
            if (adapterObserverRegistered) {
                adapterObserverRegistered = false
                adapter.unregisterAdapterDataObserver(adapterObserver)
            }
        }
        viewTypeLookup.dispose()
    }

    interface Callback {

        fun onChanged(wrapper: AdapterWrapper)

        fun onItemRangeChanged(
            wrapper: AdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeChanged(
            wrapper: AdapterWrapper,
            positionStart: Int,
            itemCount: Int,
            payload: Any?
        )

        fun onItemRangeInserted(
            wrapper: AdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeRemoved(
            wrapper: AdapterWrapper,
            positionStart: Int,
            itemCount: Int
        )

        fun onItemRangeMoved(
            wrapper: AdapterWrapper,
            fromPosition: Int,
            toPosition: Int
        )

        fun onStateRestorationPolicyChanged(wrapper: AdapterWrapper?)
    }
}

interface ViewTypeStorage {

    fun getWrapperForGlobalType(globalViewType: Int): AdapterWrapper

    fun createViewTypeWrapper(
        wrapper: AdapterWrapper
    ): ViewTypeLookup


    interface ViewTypeLookup {
        fun localToGlobal(localType: Int): Int
        fun globalToLocal(globalType: Int): Int
        fun dispose()
    }

    class SharedIdRangeViewTypeStorage : ViewTypeStorage {
        // we keep a list of nested wrappers here even though we only need 1 to create because
        // they might be removed.
        var globalTypeToWrapper = SparseArray<MutableList<AdapterWrapper>>()

        override fun getWrapperForGlobalType(globalViewType: Int): AdapterWrapper {
            val nestedAdapterWrappers: List<AdapterWrapper>? = globalTypeToWrapper[globalViewType]
            require(!(nestedAdapterWrappers == null || nestedAdapterWrappers.isEmpty())) {
                ("Cannot find the wrapper for global view"
                        + " type " + globalViewType)
            }
            // just return the first one since they are shared
            return nestedAdapterWrappers[0]
        }

        override fun createViewTypeWrapper(wrapper: AdapterWrapper): ViewTypeLookup {
            return WrapperViewTypeLookup(wrapper)
        }

        fun removeWrapper(wrapper: AdapterWrapper) {
            for (i in globalTypeToWrapper.size() - 1 downTo 0) {
                val wrappers = globalTypeToWrapper.valueAt(i)
                if (wrappers.remove(wrapper)) {
                    if (wrappers.isEmpty()) {
                        globalTypeToWrapper.removeAt(i)
                    }
                }
            }
        }

        internal inner class WrapperViewTypeLookup(private val wrapper: AdapterWrapper) : ViewTypeLookup {

            override fun localToGlobal(localType: Int): Int {
                // register it first
                var wrappers = globalTypeToWrapper[localType]
                if (wrappers == null) {
                    wrappers = ArrayList()
                    globalTypeToWrapper.put(localType, wrappers)
                }
                if (!wrappers.contains(wrapper)) {
                    wrappers.add(0, wrapper)
                } else {
                    ListUpdate.move(wrappers, wrappers.indexOf(wrapper), 0)
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