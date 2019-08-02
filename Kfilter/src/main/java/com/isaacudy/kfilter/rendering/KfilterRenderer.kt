/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2018 Isaac Udy
 */

package com.isaacudy.kfilter.rendering

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.Matrix
import android.util.Log

import com.isaacudy.kfilter.Kfilter
import com.isaacudy.kfilter.KfilterView
import com.isaacudy.kfilter.utils.*
import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val FLOAT_SIZE_BYTES = 4
private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

private val triangleVerticesData = floatArrayOf(
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0f, 0f, 0f,
        1.0f, -1.0f, 0f, 1f, 0f,
        -1.0f, 1.0f, 0f, 0f, 1f,
        1.0f, 1.0f, 0f, 1f, 1f
)

private const val VERTEX_SHADER = """
        uniform mat4 mvpMatrix;
        uniform mat4 surfaceMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 textureCoord;

        void main() {
            gl_Position = mvpMatrix * aPosition;
            textureCoord = (surfaceMatrix * aTextureCoord).xy;
        }
"""

/**
 * Renders Kfilter onto a surface using OpenGL ES 2.0.
 */
internal class KfilterRenderer(val kfilter: Kfilter) {

    private val triangleVertices: Buffer =
            ByteBuffer.allocateDirect(triangleVerticesData.size * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(triangleVerticesData)
                    .position(0)

    private var program: Int = 0

    private val mvpMatrix = FloatArray(16)
    private val surfaceMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    private var mvpMatrixHandle: Int = 0
    private var surfaceMatrixHandle: Int = 0
    private var positionHandle: Int = 0
    private var textureHandle: Int = 0

    private var inputWidth: Int = kfilter.inputWidth
    private var inputHeight: Int = kfilter.inputHeight

    private var targetWidth: Int = kfilter.inputWidth
    private var targetHeight: Int = kfilter.inputHeight
    public var prepareMedia: KfilterView.PrepareMedia? = null

    var initialised: Boolean = false
        private set

    val textureId: Int
        get() = kfilter.externalTexture.id

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    fun initialise() {
        if (initialised) {
            release()
        }

        program = createProgram(VERTEX_SHADER, kfilter.getShader())
        if (program == 0) {
            Log.d("initialise", "failed creating program")
//            throw RuntimeException("failed creating program")
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        if(!checkGlError("glGetAttribLocation aPosition")){
            prepareMedia?.error()
            return
        }
        if (positionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }

        textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        if(!checkGlError("glGetAttribLocation aTextureCoord")){
            prepareMedia?.error()
            return
        }
        if (textureHandle == -1) {
            Log.d("initialise", "Could not get attrib location for aTextureCoord")
            prepareMedia?.error()
            return
//            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "mvpMatrix")
        if(!checkGlError("glGetUniformLocation mvpMatrix")){
            prepareMedia?.error()
            return
        }
        if (mvpMatrixHandle == -1) {
            Log.d("initialise", "Could not get attrib location for mvpMatrix")
            prepareMedia?.error()
            return
//            throw RuntimeException("Could not get attrib location for mvpMatrix")
        }

        surfaceMatrixHandle = GLES20.glGetUniformLocation(program, "surfaceMatrix")
        if(!checkGlError("glGetUniformLocation surfaceMatrix")){
            prepareMedia?.error()
            return
        }
        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        if (surfaceMatrixHandle == -1) {
            Log.d("initialise", "Could not get attrib location for surfaceMatrix")
            prepareMedia?.error()
            return
//            throw RuntimeException("Could not get attrib location for surfaceMatrix")
        }

        kfilter.resize(inputWidth, inputHeight, targetWidth, targetHeight)
        kfilter.initialise(program)

        initialised = true
    }

    fun draw(milliseconds: Long, st: SurfaceTexture, scissorAmount: Float = 1f, offset: Boolean = false) {
        if (!initialised) initialise()

        checkGlError("onDrawFrame start")
        /*if(!checkGlError("onDrawFrame start")){
            prepareMedia?.error()
            return
        }*/
        st.getTransformMatrix(surfaceMatrix)


        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        var leftOffset = 0
        if (offset) leftOffset = (targetWidth * (1 - scissorAmount)).toInt()
        GLES20.glScissor(leftOffset, 0, (targetWidth * scissorAmount).toInt(), targetHeight)

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        if (!checkGlError("glUseProgram")){
            prepareMedia?.error()
            return
        }

        kfilter.apply(milliseconds)
        GLES20.glGetError() // It appears "apply" can sometimes cause erroneous errors

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        if(!checkGlError("glVertexAttribPointer maPosition")){
            prepareMedia?.error()
            return
        }

        GLES20.glEnableVertexAttribArray(positionHandle)
        if(!checkGlError("glEnableVertexAttribArray positionHandle")){
            prepareMedia?.error()
            return
        }

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices)
        if(!checkGlError("glVertexAttribPointer textureHandle")){
            prepareMedia?.error()
            return
        }

        GLES20.glEnableVertexAttribArray(textureHandle)
        if(!checkGlError("glEnableVertexAttribArray textureHandle")){
            prepareMedia?.error()
            return
        }

        Matrix.setIdentityM(mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(surfaceMatrixHandle, 1, false, surfaceMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        if(!checkGlError("glDrawArrays")){
            prepareMedia?.error()
            return
        }
        GLES20.glFinish()
    }

    fun release() {
        GLES20.glDeleteProgram(program)
        kfilter.release()
    }

    fun setDimensions(inputWidth: Int, inputHeight: Int, targetWidth: Int, targetHeight: Int) {
        this.inputWidth = inputWidth
        this.inputHeight = inputHeight

        this.targetWidth = targetWidth
        this.targetHeight = targetHeight
    }
}
