package com.dis.ajcra.distest2.util

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class DraggableViewPager: ViewPager {
    var draggable: Boolean = true

    constructor(context: Context)
        :super(context)
    {
    }

    constructor(context: Context, attrs: AttributeSet)
        :super(context, attrs)
    {
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return (draggable && super.onTouchEvent(ev))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        try {
            return (draggable && super.onInterceptTouchEvent(ev))
        } catch(e: IllegalArgumentException) {
            return false
        }
    }
}