package com.hyh.paging3demo.anim

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.core.view.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.fragment.CommonBaseFragment

/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
class Mode2Fragment : CommonBaseFragment() {
    companion object {
        private const val TAG = "Mode1Fragment"
    }

    private val anim: Boolean by lazy { arguments?.getBoolean("anim", false) ?: false }

    private var contentView: ViewGroup? = null

    override fun initData() {
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        val context = container?.context ?: inflater.context
        return RecyclerView(context).apply {
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            alpha = 0.0F
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = Mode2ListAdapter()
        }
    }

    override fun initView(contentView: View) {
        this.contentView = contentView as ViewGroup
    }

    fun animIn() {
        contentView?.apply {
            forEach {
                val animationSet = AnimationSet(true)
                animationSet.addAnimation(AlphaAnimation(0.0F, 1.0F))
                animationSet.addAnimation(TranslateAnimation(0F, 0F, 100F, 0F))
                animationSet.duration = 500
                it.startAnimation(animationSet)
            }
            alpha = 1.0F
        }
    }
}


class Mode2ListAdapter : RecyclerView.Adapter<Mode2Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Mode2Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anim_test_mode1_1, parent, false)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {

            }

            override fun onViewDetachedFromWindow(v: View) {
            }
        })
        return Mode2Holder(view)
    }

    override fun getItemCount(): Int {
        return 2
    }

    override fun onBindViewHolder(holder: Mode2Holder, position: Int) {

    }
}

class Mode2Holder(view: View) : RecyclerView.ViewHolder(view) {

}