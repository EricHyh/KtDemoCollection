package com.hyh.paging3demo.list;

import android.util.Log;

import java.util.ArrayList;

/**
 * TODO: Add Description
 *
 * @author eriche 2023/7/4
 */
class TestJava
{
    private ArrayList<TextInterface> list = new ArrayList<>();

    public void add(TextInterface test)
    {
        Log.d("TestJava", "add: " + test.toString());
        list.add(test);
    }

    public void remove(TextInterface test)
    {
        Log.d("TestJava", "remove: "+ test.toString());
        list.remove(test);
        Log.d("TestJava", "remove: " + list.size());
    }

    interface TextInterface
    {

        void test();

    }
}
