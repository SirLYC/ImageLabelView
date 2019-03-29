/*
 * MIT License
 *
 * Copyright (c) 2019 Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.lyc.imagelabel

import android.graphics.Canvas
import android.graphics.PointF
import androidx.annotation.IntDef

/**
 * @author liuyuchuan
 * @date 2019/3/22
 * @email kevinliu.sir@qq.com
 * A label which shows in [ImageLabelView]
 */
abstract class Label<DATA> {

    // initial down position of the finger
    private var startPoint: PointF? = null
    // last finger position of the finger
    private var endPoint: PointF? = null
    private var inDrawing = false
    // the message about this label
    // such as what you circled is in the label
    var message: Any? = null

    /** interface for [android.view.View.onDraw]*/
    fun draw(canvas: Canvas) {
        onDraw(canvas, startPoint, endPoint)
    }

    abstract fun isSelecting(): Boolean
    abstract fun isUpdating(): Boolean

    companion object {
        /** some changes happened to this label, should consider refreshing**/
        const val RESULT_UPDATED = 0
        /** when nothing happens, no need to refresh **/
        const val RESULT_IGNORE = 1
        /** when [checkValid] return false **/
        const val RESULT_DELETE = 2

        @IntDef(value = [RESULT_UPDATED, RESULT_IGNORE, RESULT_DELETE])
        @Retention(value = AnnotationRetention.SOURCE)
        annotation class UpdateResult
    }

    protected abstract fun onDraw(canvas: Canvas, startPointF: PointF?, endPointF: PointF?)
    abstract fun getData(startPointF: PointF?, endPointF: PointF?): DATA

    /**
     * check if a point is inside or hit this label
     * if true, this is always a start of moving part of the label (such as a corner or an edge of a rectangle)
     * @param forUpdate if this hitTest is for updating mode check
     */
    open fun partialHitTest(x: Float, y: Float, slop: Float, forUpdate: Boolean): Boolean = false

    /**
     * check if a point is inside or hit this label
     * if true, this is always a start of moving whole label
     * @param forUpdate if this hitTest is for updating mode check
     */
    open fun hitTest(x: Float, y: Float, forUpdate: Boolean): Boolean = false

    /**
     * for example: a rectangle is invalid (return true) if width or height is 0
     * @return if current params for this view is valid
     */
    protected open fun checkValid(): Boolean = true

    /*------------------------------------------------drawing procedure-----------------------------------------------*/
    fun isDrawing() = inDrawing

    fun drawStart(pointF: PointF) {
        if (inDrawing) return
        startPoint = pointF
        inDrawing = true
        onDrawStart(pointF)
    }

    fun drawMove(pointF: PointF) {
        if (!inDrawing) return
        endPoint = pointF
        onDrawMove(pointF)
    }

    fun drawEnd(): Boolean {
        if (!inDrawing) return false
        val result = checkValid()
        if (!result) {
            startPoint = null
            endPoint = null
        }
        inDrawing = false
        return result
    }

    protected open fun onDrawStart(pointF: PointF) {

    }

    protected open fun onDrawMove(pointF: PointF) {

    }

    /*-----------------------------------------------selecting procedure----------------------------------------------*/
    /**
     * @return true if selected success or false if this label is not selectable or other reason
     */
    open fun selectStart(): Boolean = false

    /**
     * end select mode for this label
     * @return true if ui needs to refresh
     */
    open fun selectEnd(): Boolean = false

    /*-----------------------------------------------selecting procedure----------------------------------------------*/
    /**
     * always use for move the label
     * return true to update ui
     */
    open fun updateMove(curX: Float, curY: Float, maxWidth: Float, maxHeight: Float) = false

    /**
     * @see [UpdateResult]
     */
    @UpdateResult
    open fun updateEnd(): Int = RESULT_IGNORE
}
