package com.hyh.paging3demo.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.WebActivity
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.databinding.ItemProjectInfoBinding


class ProjectAdapter() :
    PagingDataAdapter<ProjectBean, ProjectAdapter.ProjectItemHolder>(PROJECT_COMPARATOR) {

    init {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                Log.d("ProjectAdapter", "onItemRangeRemoved -> $positionStart - $itemCount")
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                Log.d(
                    "ProjectAdapter",
                    "onItemRangeMoved -> $fromPosition - $toPosition - $itemCount"
                )
            }

            override fun onStateRestorationPolicyChanged() {
                super.onStateRestorationPolicyChanged()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                Log.d("ProjectAdapter", "onItemRangeInserted -> $positionStart - $itemCount")
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                Log.d("ProjectAdapter", "onItemRangeChanged -> $positionStart - $itemCount")
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                Log.d(
                    "ProjectAdapter",
                    "onItemRangeChanged -> $positionStart - $itemCount - $payload"
                )
            }
        })
    }


    override fun onBindViewHolder(holder: ProjectItemHolder, position: Int) {
        Log.d("ProjectAdapter", "onBindViewHolder -> $position")
        getItem(position)?.let {
            holder.bindDataAndEvent(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectItemHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_project_info, parent, false)
        return ProjectItemHolder(view)
    }

    inner class ProjectItemHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val mDataBinding: ItemProjectInfoBinding? = DataBindingUtil.bind(itemView)

        fun bindDataAndEvent(projectBean: ProjectBean) {
            mDataBinding?.project = projectBean
            mDataBinding?.root
                ?.setOnClickListener {
                    WebActivity.start(it.context, projectBean.link)
                }
        }
    }

    companion object {

        val PROJECT_COMPARATOR = object : DiffUtil.ItemCallback<ProjectBean>() {
            /**
             * areItemsTheSame = true时，执行该函数，判断数据内容是否相同.
             */
            override fun areContentsTheSame(oldItem: ProjectBean, newItem: ProjectBean): Boolean =
                oldItem.projectId == newItem.projectId

            /**
             * 判断是否为同一条数据.
             */
            override fun areItemsTheSame(oldItem: ProjectBean, newItem: ProjectBean): Boolean =
                oldItem.projectId == newItem.projectId

            /**
             * areItemsTheSame = true && areContentsTheSame = false 时，执行该函数，
             * 用于获取新旧数据变动的字段.
             */
            override fun getChangePayload(oldItem: ProjectBean, newItem: ProjectBean): Any? = null
        }
    }
}