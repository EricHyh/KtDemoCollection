package com.hyh.list.adapter

import androidx.recyclerview.widget.RecyclerView
import com.hyh.AppendActuator
import com.hyh.RearrangeActuator
import com.hyh.RefreshActuator
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.list.ItemSourceLoadState
import com.hyh.list.PagingSourceLoadState
import com.hyh.tabs.R

interface IFlatListManager {

    val listAdapter: IListAdapter<*>

    fun <T : Any> getService(clazz: Class<T>): T?

    fun <T : Any> setService(clazz: Class<T>, service: T)

    fun scrollItem2Top(sourceToken: Any, localPosition: Int)

    fun requestKeepPosition(sourceToken: Any, localPosition: Int)

    fun getRefreshActuator(sourceToken: Any): RefreshActuator {
        return {
            listAdapter.refreshSources(sourceToken, it)
        }
    }

    fun getAppendActuator(sourceToken: Any): AppendActuator {
        return {
            listAdapter.sourceAppend(sourceToken, it)
        }
    }

    fun getRearrangeActuator(sourceToken: Any): RearrangeActuator {
        return {
            listAdapter.sourceRearrange(sourceToken, it)
        }
    }
}


fun RecyclerView.ViewHolder.setFlatListManager(manager: IFlatListManager) {
    this.itemView.setTag(R.id.flat_list_manager_tag_id, manager)
}


fun RecyclerView.ViewHolder.getFlatListManager(): IFlatListManager? {
    return this.itemView.getTag(R.id.flat_list_manager_tag_id) as? IFlatListManager
}

inline fun <reified T : Any> IFlatListManager.getService() = getService(T::class.java)
inline fun <reified T : Any> IFlatListManager.setService(service: T) = setService(T::class.java, service)

inline fun <reified T : Any> RecyclerView.ViewHolder.getService(): T? {
    return getService(T::class.java)
}

fun <T : Any> RecyclerView.ViewHolder.getService(clazz: Class<T>): T? {
    return getFlatListManager()?.getService(clazz)
}

fun RecyclerView.ViewHolder.getListAdapter(): IListAdapter<*>? {
    return getFlatListManager()?.listAdapter
}

fun RecyclerView.ViewHolder.getRefreshActuator(sourceToken: Any): RefreshActuator? {
    return getFlatListManager()?.getRefreshActuator(sourceToken)
}

fun RecyclerView.ViewHolder.getAppendActuator(sourceToken: Any): AppendActuator? {
    return getFlatListManager()?.getAppendActuator(sourceToken)
}

fun RecyclerView.ViewHolder.getRearrangeActuator(sourceToken: Any): RearrangeActuator? {
    return getFlatListManager()?.getRearrangeActuator(sourceToken)
}

fun RecyclerView.ViewHolder.getItemSourceLoadState(sourceToken: Any): SimpleStateFlow<ItemSourceLoadState>? {
    return getFlatListManager()?.listAdapter?.getItemSourceLoadState(sourceToken)
}

fun RecyclerView.ViewHolder.getPagingSourceLoadState(sourceToken: Any): SimpleStateFlow<PagingSourceLoadState>? {
    return getFlatListManager()?.listAdapter?.getPagingSourceLoadState(sourceToken)
}