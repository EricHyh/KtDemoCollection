package com.hyh.paging3demo.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * apkLink :
 * audit : 1
 * author : devallever
 * canEdit : false
 * chapterId : 294
 * chapterName : 完整项目
 * collect : false
 * courseId : 13
 * desc : 介绍 【文本翻译器】是一款免费的简洁实用的翻译软件。文本翻译器应用程序完全免费，可以非常快速翻译您的单词，帮助您与外国人交流。文本翻译器适用于旅行者、学生、商人和其他语言爱好者，使用文本翻译器可以轻松了解其他语言。文本翻译器支持多国语言，全新领先的翻译引擎，让各种变得更加可靠有保证。界面设计简洁、优雅，体积小巧，但是功能很强大哦。赶快下载来试试吧~
 * descMd :
 * envelopePic : https://www.wanandroid.com/blogimgs/9fc6e10c-b3e8-46bb-928b-05ccd2147335.png
 * fresh : false
 * id : 12244
 * link : https://www.wanandroid.com/blog/show/2719
 * niceDate : 2020-03-08 19:01
 * niceShareDate : 2020-03-08 19:01
 * origin :
 * prefix :
 * projectLink : https://github.com/devallever/TranslationTextOpenSource
 * publishTime : 1583665265000
 * selfVisible : 0
 * shareDate : 1583665265000
 * shareUser :
 * superChapterId : 294
 * superChapterName : 开源项目主Tab
 * tags : [{"name":"项目","url":"/project/list/1?cid=294"}]
 * title : TranslationTextOpenSource-文本翻译器开源版
 * type : 0
 * userId : -1
 * visible : 1
 * zan : 0
 */
@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["projectId"], unique = true),
        Index(value = ["chapterId"], unique = false),
    ]
)
data class ProjectBean(
    @PrimaryKey(autoGenerate = true)
    val _id: Int,
    @SerializedName("id")
    val projectId: Int = 0,
    val apkLink: String?,
    val audit: Int = 0,
    val author: String?,
    val canEdit: Boolean = false,
    val chapterId: Int = 0,
    val chapterName: String?,
    val collect: Boolean = false,
    val courseId: Int = 0,
    val desc: String?,
    val descMd: String?,
    val envelopePic: String?,
    val fresh: Boolean = false,
    val link: String?,
    val niceDate: String?,
    val niceShareDate: String?,
    val origin: String?,
    val prefix: String?,
    val projectLink: String?,
    val publishTime: Long = 0L,
    val selfVisible: Int = 0,
    val shareDate: Long = 0L,
    val shareUser: String?,
    val superChapterId: Int = 0,
    val superChapterName: String?,
    val title: String?,
    val userId: Int = 0,
    val zan: Int = 0
) {
    var orderNum = Int.MIN_VALUE;
}