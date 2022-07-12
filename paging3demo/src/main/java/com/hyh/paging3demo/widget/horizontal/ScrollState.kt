package com.hyh.paging3demo.widget.horizontal

/**
 * 滑动状态
 *
 * @author eriche 2021/12/28
 */
enum class ScrollState {

    INITIAL,    //初始状态

    IDLE,       //空闲状态

    SCROLL,     //滑动状态

    DRAG,       //拖拽状态

    REBOUND,    //回弹状态

    SETTLING    //惯性状态

}