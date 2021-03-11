package com.hyh.toast;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;
import android.view.WindowManager;

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/3/11
 */
class MyContext extends ContextWrapper {

    public MyContext(Context base) {
        super(base);
    }

    @Override
    public Object getSystemService(String name) {
        if (TextUtils.equals(name, Context.WINDOW_SERVICE)) {
            return new WindowManagerImpl(((WindowManager) super.getSystemService(name)));
        }
        return super.getSystemService(name);
    }
}
