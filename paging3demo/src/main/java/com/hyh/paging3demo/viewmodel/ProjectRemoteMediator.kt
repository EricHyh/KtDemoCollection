package com.hyh.paging3demo.viewmodel

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hyh.paging3demo.bean.ProjectBean

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/15
 */
@ExperimentalPagingApi
class ProjectRemoteMediator : RemoteMediator<Int, ProjectBean>() {
    companion object {
        private const val TAG = "ProjectRemoteMediator"
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, ProjectBean>): MediatorResult {

    }
}