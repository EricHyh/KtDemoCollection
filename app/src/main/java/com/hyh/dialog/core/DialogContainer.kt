package com.hyh.dialog.core

import android.content.Context
import androidx.fragment.app.Fragment

class DialogContainer<T> : IContentContainer<T> {


    companion object {

        fun with(context: Context): Builder {
            return Builder()
        }

        fun with(fragment: Fragment): Builder {

            return Builder()
        }
    }

    override fun dismiss() {

    }
}

class Builder {


}