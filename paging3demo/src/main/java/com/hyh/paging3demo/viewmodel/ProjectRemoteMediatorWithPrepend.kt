package com.hyh.paging3demo.viewmodel

import android.content.Context
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.hyh.paging3demo.api.ProjectApi
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectRemoteKey
import com.hyh.paging3demo.db.ProjectDB
import com.hyh.paging3demo.db.ProjectDao
import com.hyh.paging3demo.db.ProjectRemoteKeyDao
import com.hyh.paging3demo.net.RetrofitHelper

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/15
 */
@ExperimentalPagingApi
class ProjectRemoteMediatorWithPrepend(
    context: Context,
    private val db: ProjectDB,
    private val chapterId: Int
) :
    RemoteMediator<Int, ProjectBean>() {

    companion object {
        private const val TAG = "ProjectRemoteMediator"
    }

    private val projectApi: ProjectApi = RetrofitHelper.create(context, ProjectApi::class.java)
    private val projectDao: ProjectDao = db.projects()
    private val remoteKeyDao: ProjectRemoteKeyDao = db.remoteKeys()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProjectBean>
    ): MediatorResult {
        try {//第一步，获取请求的Key
            val pageIndex: Int = when (loadType) {
                LoadType.PREPEND -> {
                    val remoteKey = db.withTransaction {
                        remoteKeyDao.getRemoteKey(chapterId)
                    }
                    if (remoteKey.prevPageIndex == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    remoteKey.prevPageIndex
                }
                LoadType.REFRESH -> 1
                LoadType.APPEND -> {
                    // Query DB for SubredditRemoteKey for the subreddit.
                    // SubredditRemoteKey is a wrapper object we use to keep track of page keys we
                    // receive from the Reddit API to fetch the next or previous page.
                    val remoteKey = db.withTransaction {
                        remoteKeyDao.getRemoteKey(chapterId)
                    }

                    // We must explicitly check if the page key is null when appending, since the
                    // Reddit API informs the end of the list by returning null for page key, but
                    // passing a null key to Reddit API will fetch the initial page.
                    if (remoteKey.nextPageIndex == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }

                    remoteKey.nextPageIndex
                }
            }

            Log.d("ProjectRemoteMediator", "load -> $loadType, chapterId=$chapterId, pageIndex=$pageIndex")

            val projectsBean = projectApi.get(pageIndex, chapterId)

            val list = projectsBean.data?.projects

            Log.d("ProjectRemoteMediator", "load -> ${list?.size}")

            if (list.isNullOrEmpty()) {
                return if (projectsBean.data?.curPage == projectsBean.data?.pageCount) {
                    MediatorResult.Success(endOfPaginationReached = true)
                } else {
                    MediatorResult.Error(NullPointerException())
                }
            } else {
                var order = if (loadType == LoadType.PREPEND) {
                    (state.firstItemOrNull()?.orderNum ?: 0) - list.size
                } else if (loadType == LoadType.APPEND) {
                    (state.lastItemOrNull()?.orderNum ?: 0) + 1
                } else {
                    0
                }
                list.forEach {
                    it.orderNum = order
                    order++
                }
                db.withTransaction {
                    val newRemoteKey: ProjectRemoteKey
                    newRemoteKey = if (loadType == LoadType.REFRESH) {
                        Log.d( "ProjectRemoteMediator", "load:$chapterId REFRESH-> ${list.size}")
                        projectDao.deleteByChapterId(chapterId)
                        remoteKeyDao.delete(chapterId)
                        val prevPageIndex: Int? = if (pageIndex == 1) null else pageIndex - 1
                        val nextPageIndex: Int = pageIndex + 1
                        ProjectRemoteKey(chapterId, prevPageIndex, nextPageIndex)
                    } else if (loadType == LoadType.PREPEND) {
                        val remoteKey = remoteKeyDao.getRemoteKey(chapterId)
                        val prevPageIndex: Int? = if (pageIndex == 1) null else pageIndex - 1
                        val nextPageIndex: Int? = remoteKey.nextPageIndex
                        ProjectRemoteKey(chapterId, prevPageIndex, nextPageIndex)
                    } else {
                        val remoteKey = remoteKeyDao.getRemoteKey(chapterId)
                        ProjectRemoteKey(chapterId, remoteKey.prevPageIndex, pageIndex + 1)
                    }
                    remoteKeyDao.insert(newRemoteKey)
                    projectDao.insertAll(list)
                }
                return MediatorResult.Success(projectsBean.data.curPage == projectsBean.data.pageCount)
            }
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction {
        return super.initialize()
    }
}