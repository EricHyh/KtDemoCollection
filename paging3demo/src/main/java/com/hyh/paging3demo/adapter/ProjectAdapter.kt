package com.hyh.paging3demo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.databinding.ItemProjectInfoBinding


class ProjectAdapter(diffCallback: DiffUtil.ItemCallback<ProjectBean>) :
    PagingDataAdapter<ProjectBean, ProjectAdapter.ProjectItemHolder>(diffCallback) {

    override fun onBindViewHolder(holder: ProjectItemHolder, position: Int) {
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
                    Toast.makeText(itemView.context, "", Toast.LENGTH_SHORT).show()
                }
        }
    }
}