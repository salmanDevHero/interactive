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
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_MOVE || it.action == MotionEvent.ACTION_DOWN) {
                // Convert touch coordinates to OpenGL coordinates
                val x = (it.x / width) * 2 - 1
                val y = 1 - (it.y / height) * 2
                renderer.addTouchPoint(x, y)
                requestRender() // Request to render the frame after updating touch points
            }
        }
        return true
    }


}