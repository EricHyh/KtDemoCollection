package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.hyh.paging3demo.api.ProjectApi
import com.hyh.paging3demo.bean.DataBean
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectRemoteKey
import com.hyh.paging3demo.bean.ProjectsBean
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

    override suspend fun load(loadType: LoadType, state: PagingState<Int, ProjectBean>): MediatorResult {
        try {
            //1、从数据库中获取请求的Key，并确定是否需要请求数据
            val loadKeyAndShouldLoad: Pair<Int?, Boolean> = getLoadKeyAndShouldLoad(loadType)
            if (!loadKeyAndShouldLoad.second) return MediatorResult.Success(endOfPaginationReached = true)
            val pageIndex: Int = loadKeyAndShouldLoad.first!!
            //2、请求数据
            val projectsBean = projectApi.get(pageIndex, chapterId)
            //3、处理异常数据
            val exception = checkData(projectsBean)
            if (exception != null) {
                return MediatorResult.Error(exception)
            }
            db.withTransaction {
                //4、如果是刷新则清除数据库中的数据
                if (loadType == LoadType.REFRESH) {
                    projectDao.deleteByChapterId(chapterId)
                    remoteKeyDao.delete(chapterId)
                }
                //5、获取下一页请求参数并存储
                remoteKeyDao.insert(ProjectRemoteKey(chapterId, null, pageIndex + 1))
                //6、存储列表数据
                if (!projectsBean.data?.projects.isNullOrEmpty()) {
                    projectDao.insertAll(projectsBean.data?.projects!!)
                }
            }
            return MediatorResult.Success(projectsBean.data!!.curPage >= projectsBean.data.pageCount)

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }

    private suspend fun getLoadKeyAndShouldLoad(loadType: LoadType): Pair<Int?, Boolean> {
        when (loadType) {
            LoadType.PREPEND -> return Pair(null, false)
            LoadType.REFRESH -> return Pair(1, true)
            LoadType.APPEND -> {
                val remoteKey = db.withTransaction {
                    remoteKeyDao.getRemoteKey(chapterId)
                }
                if (remoteKey.nextPageIndex == null) {
                    return Pair(null, false)
                }
                return Pair(remoteKey.nextPageIndex, true)
            }
        }
    }

    private fun checkData(projectsBean: ProjectsBean): Exception? {
        if (projectsBean.data == null) {
            return NullPointerException()
        }
        return null
    }
}