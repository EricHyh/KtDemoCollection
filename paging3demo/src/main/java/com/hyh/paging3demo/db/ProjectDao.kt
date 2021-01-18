package com.hyh.paging3demo.db

import androidx.paging.PagingSource
import androidx.room.*
import com.hyh.paging3demo.bean.ProjectBean

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectBean>)

    @Query("SELECT * FROM projects WHERE chapterId = :chapterId ORDER BY orderNum ASC")
    fun getProjectsByChapterId(chapterId: Int): PagingSource<Int, ProjectBean>

    @Query("DELETE FROM projects WHERE chapterId = :chapterId")
    suspend fun deleteByChapterId(chapterId: Int)

}