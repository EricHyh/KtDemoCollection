package com.hyh.paging3demo.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox

/**
 * TODO: Add Description
 *
 * @author eriche 2023/6/9
 */
class TestCheckBox(context: Context, attrs: AttributeSet?) :
    AppCompatCheckBox(context, attrs) {
    companion object {
        private const val TAG = "TestCheckBox"
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
    }
}