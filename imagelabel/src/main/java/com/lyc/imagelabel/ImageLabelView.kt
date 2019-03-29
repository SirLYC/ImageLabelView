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

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.*

/**
 * @author liuyuchuan
 * @date 2019/3/22
 * @email kevinliu.sir@qq.com
 */
class ImageLabelView(context: Context, attrs: AttributeSet?) : View(context, attrs), GestureDetector.OnGestureListener,
    ScaleGestureDetector.OnScaleGestureListener {

    // the image to be labeled
    private var labelBitmap: Bitmap? = null

    private val gestureDetector: GestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private val edgeSlop = context.resources.displayMetrics.density * 12

    // changed only in move mode
    private var transX = 0f
    private var transY = 0f
    private var scale = 1f

    // only change when the bitmap was set to this view
    // never change after then util next bitmap comes(or null)
    private var baseTransX = 0f
    private var baseTransY = 0f
    private var baseScale = 0f

    // current label to draw (in draw mode) or update (in update mode)
    private var curLabel: Label<*>? = null
    // the labels already created
    // add like a stack
    // the smaller the index, the upper level (or the newer added) the label
    private val createdLabels = mutableListOf<Label<*>>()

    private var bitmapWidth = 0f
    private var bitmapHeight = 0f

    private var downBitmapX = 0f
    private var downBitmapY = 0f

    var previewAfterOperate = false

    companion object {
        // move or scale image
        const val PREVIEW = 0
        // draw labels
        const val DRAW = 1
        // update labels (size, position...)
        const val UPDATE = 2
        // select a label to change its info
        // such as message
        // or just delete it
        const val SELECT = 3

        const val TAG = "ImageLabelView"

        @IntDef(value = [PREVIEW, DRAW, UPDATE])
        @Retention(AnnotationRetention.SOURCE)
        private annotation class Mode

        fun modeToString(@Mode mode: Int) = when (mode) {
            PREVIEW -> "PREVIEW"
            DRAW -> "DRAW"
            UPDATE -> "UPDATE"
            SELECT -> "SELECT"
            else -> "UNKNOWN"
        }
    }

    @Mode
    var mode: Int = PREVIEW
        @MainThread set(value) {
            if (value == field) return
            if (field == UPDATE || field == DRAW || field == SELECT) {
                if (releaseCurrentLabel()) {
                    invalidate()
                }
            }
            field = value
        }

    /**
     * release [curLabel]
     * there are 3 modes for current label:
     * draw mode: user is dragging and drawing a new label.
     * if release success, the label should be added to [createdLabels]
     * and it will also call [LabelDrawListener.onLabelDrawEnd]
     * update mode: move or resize [curLabel], after release [LabelUpdateListener.onLabelUpdateEnd] will be called
     * select mode: select a label. The selected label can be retrieve by [selectingLabel].
     * after release, [LabelSelectListener.onLabelSelectEnd] will be called
     * @return true if this view should be updated otherwise false
     */
    @MainThread
    @CheckResult
    fun releaseCurrentLabel(): Boolean {
        if (curLabel == null) return false

        var shouldRepaint = false

        // every operation should be only one state is true
        // [drawing, updating, selecting]
        curLabel?.run {
            if (isDrawing()) {
                val drawResult = drawEnd()
                if (drawResult) {
                    curLabel?.let {
                        createdLabels.add(0, it)
                    }
                }
                labelDrawListener?.onLabelDrawEnd(drawResult, if (drawResult) this else null)
                shouldRepaint = true
                return@run
            }

            if (isUpdating()) {
                val updateResult = updateEnd()
                labelUpdateListener?.onLabelUpdateEnd(this)
                if (updateResult == Label.RESULT_DELETE) {
                    createdLabels.remove(this)
                    shouldRepaint = true
                } else if (updateResult == Label.RESULT_UPDATED) {
                    shouldRepaint = true
                }
                return@run
            }

            if (isSelecting()) {
                if (this.selectEnd()) {
                    shouldRepaint = true
                }
                labelSelectListener?.onLabelSelectEnd(this)
            }
        }

        curLabel = null
        return shouldRepaint
    }

    /**
     * return [curLabel] if [curLabel] is present and in drawing mode
     */
    fun drawingLabel(): Label<*>? {
        if (curLabel?.isDrawing() == true) {
            return curLabel
        }
        return null
    }

    /**
     * return [curLabel] if [curLabel] is present and in drawing mode
     */
    fun updatingLabel(): Label<*>? {
        if (curLabel?.isUpdating() == true) {
            return curLabel
        }
        return null
    }

    /**
     * return [curLabel] if [curLabel] is present and in selecting mode
     */
    fun selectingLabel(): Label<*>? {
        if (curLabel?.isSelecting() == true) {
            return curLabel
        }

        return null
    }

    /**
     * this can add a custom label to labels to show
     * repeated label will not be added
     */
    @MainThread
    fun addLabel(label: Label<*>) {
        if (createdLabels.contains(label)) {
            return
        }
        createdLabels.add(0, label)
        invalidate()
    }

    @MainThread
    fun removeLabel(label: Label<*>?): Boolean {
        val result = createdLabels.remove(label)
        if (result) {
            invalidate()
        }
        return result
    }

    @MainThread
    fun removeAllLabels() {
        if (createdLabels.isNotEmpty()) {
            createdLabels.clear()
            invalidate()
        }
    }

    /**
     * retrive copy of [createdLabels]
     */
    fun labels() = listOf(createdLabels)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bitmap = labelBitmap
        if (bitmap?.isRecycled != false) {
            return
        }
        if ((w != oldw || h != oldh) && bitmap.width != 0 && bitmap.height != 0 && w != 0 && h != 0) {
            updateBitmap(bitmap)
        }
    }

    /**
     * we make sure that both view and [labelBitmap]'s width and height is non-zero.
     */
    @MainThread
    private fun updateBitmap(bitmap: Bitmap) {
        val vw = width.toFloat()
        val vh = height.toFloat()

        bitmapWidth = bitmap.width.toFloat()
        bitmapHeight = bitmap.height.toFloat()

        val finalW: Float
        val finalH: Float

        // put the bitmap center inside this view
        val w2hv = vw / vh
        val w2hb = bitmapWidth / bitmapHeight
        if (w2hv > w2hb) {
            // should be center-horizontal in this view
            finalH = vh
            finalW = vh * w2hb
            baseScale = finalH / bitmapHeight
            baseTransY = 0f
            baseTransX = vw - finalW / 2
        } else {
            // should be center-vertical in this view
            finalW = vw
            finalH = vw / w2hb
            baseScale = finalW / bitmapWidth
            baseTransX = 0f
            baseTransY = (vh - finalH) / 2
        }

        if (labelBitmap != bitmap) {
            labelBitmap?.recycle()
            labelBitmap = bitmap.copy(Bitmap.Config.RGB_565, false)
        }

        // reset these values
        transX = 0f
        transY = 0f
        scale = 1f

        curLabel = null
        invalidate()
    }

    @MainThread
    fun setBitmap(bitmap: Bitmap?) {
        if (bitmap != labelBitmap) {
            curLabel = null
            createdLabels.clear()
        }

        if (bitmap == null || bitmap.isRecycled || bitmap.width == 0 || bitmap.height == 0) {
            if (labelBitmap == null) return
            labelBitmap = null
            bitmapWidth = 0f
            bitmapHeight = 0f
            invalidate()
        } else {
            updateBitmap(bitmap)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (labelBitmap?.isRecycled != false || labelBitmap?.width == 0 || labelBitmap?.height == 0) {
            return
        }
        canvas.save()
        canvas.translate(baseTransX + transX, baseTransY + transY)
        canvas.scale(scale * baseScale, scale * baseScale)
        // after transition, the coordinates for every label is relative to bitmap with bitmap's raw size.
        canvas.drawBitmap(labelBitmap!!, 0f, 0f, null)
        if (curLabel?.isDrawing() == true) {
            // only when curLabel is in drawing mode can be drawn
            curLabel?.draw(canvas)
        }
        createdLabels.forEach {
            it.draw(canvas)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (labelBitmap?.isRecycled != false || !isEnabled) {
            return super.onTouchEvent(event)
        }

        return when (mode) {
            PREVIEW -> handleTouchEventPreview(event)
            UPDATE -> handleTouchEventUpdate(event)
            SELECT -> handleTouchEventSelect(event)
            DRAW -> handleTouchEventDraw(event)
            else -> super.onTouchEvent(event)
        }
    }

    /**
     * adjust a point to coordinate relative to bitmap with bound
     * if out of bound, make the point at its nearest edge
     */
    private fun adjustedPoint(x: Float, y: Float): PointF {
        val toX = toBitmapX(x).run {
            when {
                this < 0 -> 0f
                this > bitmapWidth -> bitmapWidth
                else -> this
            }
        }

        val toY = toBitmapY(y).run {
            when {
                this < 0 -> 0f
                this > bitmapHeight -> bitmapHeight
                else -> this
            }
        }

        return PointF(toX, toY)
    }

    /**
     * draw a new label
     */
    private fun handleTouchEventDraw(event: MotionEvent): Boolean {
        if (labelCreator == null) {
            Log.w(TAG, "no label creator set, skip draw label!")
            return super.onTouchEvent(event)
        }

        val action = event.actionMasked
        val x = event.x
        val y = event.y

        val point = adjustedPoint(x, y)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                labelDrawListener?.onLabelDrawStart(point, curLabel)
                curLabel = labelCreator?.createLabel()
                curLabel?.drawStart(point)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (curLabel?.isDrawing() == true) {
                    curLabel?.drawMove(point)
                    labelDrawListener?.onLabelDraw(point, curLabel)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (previewAfterOperate) {
                    mode = PREVIEW
                } else if (releaseCurrentLabel()) {
                    invalidate()
                }

                return true
            }
        }

        return super.onTouchEvent(event)
    }


    private fun handleTouchEventSelect(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }


    private fun handleTouchEventUpdate(event: MotionEvent): Boolean {
        val bitmap = labelBitmap

        if (bitmap?.isRecycled != false) {
            return super.onTouchEvent(event)
        }

        val bitmapX = toBitmapX(event.x)
        val bitmapY = toBitmapY(event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val endLast = when (curLabel?.updateEnd()) {
                    Label.RESULT_IGNORE -> false
                    Label.RESULT_DELETE -> {
                        curLabel?.let(createdLabels::remove)
                        true
                    }
                    Label.RESULT_UPDATED -> true
                    else -> false
                }
                curLabel = null
                var updateNew = false

                createdLabels.forEach {
                    if (it.partialHitTest(bitmapX, bitmapY, edgeSlop, true)) {
                        curLabel = it
                        labelUpdateListener?.onLabelUpdateStart(it)
                        updateNew = true
                        return@forEach
                    }
                }

                if (!updateNew) {
                    createdLabels.forEach {
                        if (it.hitTest(bitmapX, bitmapY, true)) {
                            curLabel = it
                            labelUpdateListener?.onLabelUpdateStart(it)
                            updateNew = true
                            return@forEach
                        }
                    }
                }

                if (endLast || updateNew) {
                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (curLabel?.updateMove(bitmapX, bitmapY, bitmap.width.toFloat(), bitmap.height.toFloat()) == true) {
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (previewAfterOperate) {
                    mode = PREVIEW
                } else if (releaseCurrentLabel()) {
                    invalidate()
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun handleTouchEventPreview(event: MotionEvent): Boolean {
        var handle = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            handle = handle or gestureDetector.onTouchEvent(event)
        }
        return handle
    }

    /**
     * Perform scale gesture.
     * Take x-axis for example:
     * for focusX (relative to view), it should still be focusX after transition before adjust bound
     * let scaledDx = focusX - left, left = transX + transY.
     * We can know that in original bitmap, the "distance" (may be negative)
     * between left of the bitmap and the point at focusX is:
     * dx = scaledDx / (scale * baseScale)
     * After that, we can know that in the new scale (in the view), the "distance" is
     * newDx = dx * factor * scale * baseScale
     * As the focusX is constant, we can get the equation as follow:
     * newDx + newTransX + baseTransX = focusX
     * After solve it, we get:
     * newTranX = focusX - (focusX - transX - baseTransX) * factor - baseTransX
     */
    private fun performScale(focusX: Float, focusY: Float, factor: Float) {
        scale *= factor

        transX = focusX - (focusX - transX - baseTransX) * factor - baseTransX
        transY = focusY - (focusY - transY - baseTransY) * factor - baseTransY

        adjustBound()
        invalidate()
    }

    private fun performTrans(dx: Float, dy: Float) {
        transX += dx
        transY += dy

        adjustBound()
        invalidate()
    }

    /**
     * make the bitmap inside or intersects with view
     */
    private fun adjustBound() {
        val bitmap = labelBitmap
        if (bitmap?.isRecycled != false) {
            return
        }
        val viewWidth = width
        val viewHeight = height

        if (viewHeight == 0 || viewHeight == 0) {
            return
        }
        val w = bitmap.width * scale * baseScale
        val h = bitmap.height * scale * baseScale
        val left = baseTransX + transX
        val top = baseTransY + transY
        if (left > viewWidth) {
            transX = viewWidth - baseTransX
        } else if (left + w < 0) {
            transX = -w - baseTransX
        }

        if (top > viewHeight) {
            transY = viewHeight - baseTransY
        } else if (top + h < 0) {
            transY = -h - baseTransY
        }
    }

    // for selecting
    override fun onSingleTapUp(event: MotionEvent): Boolean {
        if (mode != SELECT) return false

        if (labelBitmap?.isRecycled != false) {
            return false
        }

        var newLabel: Label<*>? = null

        val x = toBitmapX(event.x)
        val y = toBitmapY(event.y)

        // partial hit-test first
        // skip current selected label
        createdLabels.forEach {
            if (it.partialHitTest(downBitmapX, downBitmapY, edgeSlop, false) && it.partialHitTest(
                    x, y, edgeSlop, false
                ) && it != curLabel && it.selectStart()
            ) {
                labelSelectListener?.onLabelSelectStart(it)
                newLabel = it
                return@forEach
            }
        }

        // if no new label present, use full hit-test
        // skip current selected label
        if (newLabel == null) {
            createdLabels.forEach {
                if (it.hitTest(downBitmapX, downBitmapY, false) && it.hitTest(
                        x, y, false
                    ) && it != curLabel && it.selectStart()
                ) {
                    labelSelectListener?.onLabelSelectStart(it)
                    newLabel = it
                    return@forEach
                }
            }
        }

        // release last label
        val release = releaseCurrentLabel()
        curLabel = newLabel

        if (curLabel != null || release) {
            invalidate()
        }


        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onDown(e: MotionEvent): Boolean {
        downBitmapX = toBitmapX(e.x)
        downBitmapY = toBitmapY(e.y)
        return true
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (mode == PREVIEW) {
            performTrans(-distanceX, -distanceY)
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        onSingleTapUp(e)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return PREVIEW == mode
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // as the doc says, check if not exits
        if (detector.focusX == Float.NaN || detector.focusX == Float.NEGATIVE_INFINITY || detector.focusX == Float.POSITIVE_INFINITY) {
            return false
        }

        performScale(detector.focusX, detector.focusY, detector.scaleFactor)
        return true
    }

    // convert view positions to coordinates relative to original bitmap
    private fun toBitmapX(x: Float) = (x - transX - baseTransX) / (scale * baseScale)

    private fun toBitmapY(y: Float) = (y - transY - baseTransY) / (scale * baseScale)


    var labelCreator: LabelCreator? = null

    interface LabelCreator {
        /**
         * @return a new label when start to draw
         */
        fun createLabel(): Label<*>
    }

    var labelDrawListener: LabelDrawListener? = null

    interface LabelDrawListener {
        /**
         * called when a new label created
         * this method will be called only once in the procedure of creating new label
         * @param pointF start press position, bitmap-relative
         * @param label new-created label
         */
        fun onLabelDrawStart(pointF: PointF, label: Label<*>?)

        /**
         * called when a new label drawing (finger move)
         * @param pointF current finger position, bitmap-relative
         * @param label new-created label
         */
        fun onLabelDraw(pointF: PointF, label: Label<*>?)

        /**
         * called when a new label drawing (finger move)
         * @param created if the label is a valid label. Returned by [Label.checkValid]
         * @param label new-created label
         */
        fun onLabelDrawEnd(created: Boolean, label: Label<*>?)
    }


    var labelUpdateListener: LabelUpdateListener? = null

    interface LabelUpdateListener {
        /**
         * called when a label is to be updating
         */
        fun onLabelUpdateStart(label: Label<*>)

        /**
         * called when a label is finishing its updating
         */
        fun onLabelUpdateEnd(label: Label<*>)
    }

    var labelSelectListener: LabelSelectListener? = null

    interface LabelSelectListener {
        /**
         * called when a label is to be selected
         */
        fun onLabelSelectStart(label: Label<*>)

        /**
         * called when a label is finishing its selecting
         */
        fun onLabelSelectEnd(label: Label<*>)
    }
}