package com.example.interactive

import MyGLRenderer
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import javax.microedition.khronos.opengles.GL10

class MyGLSurfaceView(context: Context, attrs: AttributeSet? = null) :
    GLSurfaceView(context, attrs) {
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                renderer.addTouchPoint(event.x, event.y)
                requestRender()
            }
        }
        return true
    }
}