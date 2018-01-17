package com.dis.ajcra.distest2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager

object AnimationUtils {

    @JvmOverloads
    fun Crossfade(inView: View?, outView: View?, millis: Int = 200) {
        if (inView == null || outView == null) {
            return
        }
        inView.clearAnimation()
        inView.animate().cancel()
        outView.clearAnimation()
        outView.animate().cancel()
        inView.alpha = 0f
        inView.visibility = View.VISIBLE
        inView.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null)
        outView.animate()
                .alpha(0f)
                .setDuration(millis.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        outView.visibility = View.GONE
                    }
                })
    }

    fun HideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        view!!.clearFocus()
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
