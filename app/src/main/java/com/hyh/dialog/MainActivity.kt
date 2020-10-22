package com.hyh.dialog

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hyh.dialog.core.IContentContainer
import com.hyh.dialog.core.IContentView
import com.hyh.dialog.core.NNWindow

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* AlertDialog.Builder(this)
             .setPositiveButton("",)*/

        /*NNWindow.with<Runnable>(this)
            .content()
            .eventListener(Runnable {})
            .show()*/
    }

    fun click(view: View) {
        Toast.makeText(this, "click", Toast.LENGTH_LONG).show()
    }
}
