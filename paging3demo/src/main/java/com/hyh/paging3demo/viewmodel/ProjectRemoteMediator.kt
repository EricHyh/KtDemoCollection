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
class ProjectRemoteMediator(
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
        //return MediatorResult.Success(false)
        try {//第一步，获取请求的Key
            val pageIndex: Int = when (loadType) {
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
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

            Log.d(
                "ProjectRemoteMediator",
                "load -> $loadType, chapterId=$chapterId, pageIndex=$pageIndex"
            )

            val projectsBean = projectApi.get(pageIndex, chapterId)

            val list = projectsBean.data.projects/*?.reversed()*/

            Log.d("ProjectRemoteMediator", "load -> ${list?.size}")

            if (list.isNullOrEmpty()) {
                return if (projectsBean.data.curPage == projectsBean.data.pageCount) {
                    MediatorResult.Success(endOfPaginationReached = true)
                } else {
                    MediatorResult.Error(NullPointerException())
                }
            } else {
                db.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        projectDao.deleteByChapterId(chapterId)
                        remoteKeyDao.delete(chapterId)
                    }
                    remoteKeyDao.insert(ProjectRemoteKey(chapterId, pageIndex + 1))
                    projectDao.insertAll(list)
                }
                return MediatorResult.Success(projectsBean.data.curPage == projectsBean.data.pageCount)
            }
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }

}