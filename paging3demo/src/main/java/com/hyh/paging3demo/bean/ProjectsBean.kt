package com.hyh.paging3demo.bean

import com.google.gson.annotations.SerializedName

data class ProjectsBean(val data: DataBean?)

data class DataBean(
    val curPage: Int = 0,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
    @SerializedName("datas")
    val projects: List<ProjectBean>? = null
)