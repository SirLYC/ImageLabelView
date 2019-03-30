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

import android.graphics.*
import androidx.annotation.CheckResult

/**
 * @author liuyuchuan
 * @date 2019/3/27
 * @email kevinliu.sir@qq.com
 */
class RectLabel : Label<FloatArray?>() {

    private val drawRect: RectF = RectF()
    private val baseRect: RectF = RectF()
    private val highlightRect = RectF()

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
    }
    private val highlightPaint = Paint().apply {
        isAntiAlias = true
        color = Color.YELLOW
        alpha = 255 / 5
    }

    private var checkMode = -1

    init {
        paint.isAntiAlias = true
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        highlightPaint.color = Color.YELLOW
        highlightPaint.alpha = 255 / 5
    }

    var rectColor
        get() = paint.color
        set(value) {
            paint.color = value
        }

    var hightlightColor
        get() = highlightPaint.color
        set(value) {
            highlightPaint.color = value
        }

    var rectStrokeWidth
        get() = paint.strokeWidth
        set(value) {
            paint.strokeWidth = value
            highlightPaint.strokeWidth = 1.5f * value
        }

    var highlightStrokeWidth
        get() = highlightPaint.strokeWidth
        set(value) {
            highlightPaint.strokeWidth = value
        }

    var highlightAlpha
        get() = highlightPaint.alpha
        set(value) {
            highlightPaint.alpha = value
        }

    override fun onDraw(canvas: Canvas, startPointF: PointF?, endPointF: PointF?) {
        if (startPointF != null && endPointF != null) {
            drawRect.set(baseRect)
            drawRect.sort()
            when (checkMode) {
                // moving the whole label
                0 -> {
                    highlightPaint.style = Paint.Style.STROKE
                    canvas.drawRect(highlightRect, highlightPaint)
                }

                // updating a vertex
                in 1..4 -> {
                    highlightPaint.style = Paint.Style.FILL
                    canvas.drawOval(highlightRect, highlightPaint)
                }

                // updating a horizontal edge
                5, 7 -> {
                    canvas.drawLine(
                        highlightRect.left, highlightRect.top, highlightRect.right, highlightRect.top, highlightPaint
                    )
                }

                // updating a vertical edge
                6, 8 -> {
                    canvas.drawLine(
                        highlightRect.left, highlightRect.top, highlightRect.left, highlightRect.bottom, highlightPaint
                    )
                }

                // select
                9 -> {
                    highlightPaint.style = Paint.Style.FILL
                    canvas.drawRect(highlightRect, highlightPaint)
                }
            }
            canvas.drawRect(drawRect, paint)
        } else if (startPointF != null) {
            canvas.drawPoint(startPointF.x, startPointF.y, paint)
        }
    }

    override fun onDrawStart(pointF: PointF) {
        baseRect.left = pointF.x
        baseRect.top = pointF.y
        baseRect.right = pointF.x
        baseRect.bottom = pointF.y
    }

    override fun onDrawMove(pointF: PointF) {
        baseRect.right = pointF.x
        baseRect.bottom = pointF.y
    }

    override fun getData(startPointF: PointF?, endPointF: PointF?): FloatArray? {
        if (startPointF != null && endPointF != null) {
            drawRect.set(baseRect)
            return floatArrayOf(drawRect.left, drawRect.top, drawRect.right, drawRect.bottom)
        }

        return null
    }

    override fun checkValid(): Boolean {
        baseRect.sort()
        if (baseRect.width() <= 0 || baseRect.height() <= 0) {
            return false
        }
        return true
    }

    override fun partialHitTest(x: Float, y: Float, slop: Float, forUpdate: Boolean): Boolean {
        // the index for vertexes and edges
        //      1  5  2
        //      -------
        //    8 |     | 6
        //      -------
        //      4  7  3
        // 0 for move all
        // 9 for select mode (always use for change message or delete this)

        highlightRect.set(baseRect)
        highlightRect.sort()
        val left = highlightRect.left
        val right = highlightRect.right
        val top = highlightRect.top
        val bottom = highlightRect.bottom

        // left top vertex
        highlightRect.set(left, top, left, top)
        highlightRect.inset(-slop, -slop)
        if (highlightRect.contains(x, y)) {
            if (forUpdate) checkMode = 1
            return true
        }

        // right top vertex
        highlightRect.set(right, top, right, top)
        highlightRect.inset(-slop, -slop)
        if (highlightRect.contains(x, y)) {
            if (forUpdate) checkMode = 2
            return true
        }

        // right bottom vertex
        highlightRect.set(right, bottom, right, bottom)
        highlightRect.inset(-slop, -slop)
        if (highlightRect.contains(x, y)) {
            if (forUpdate) checkMode = 3
            return true
        }

        // left bottom vertex
        highlightRect.set(left, bottom, left, bottom)
        highlightRect.inset(-slop, -slop)
        if (highlightRect.contains(x, y)) {
            if (forUpdate) checkMode = 4
            return true
        }

        // top edge
        highlightRect.set(left, top, right, top)
        highlightRect.inset(0f, -slop)
        if (highlightRect.contains(x, y)) {
            highlightRect.inset(0f, slop)
            if (forUpdate) checkMode = 5
            return true
        }

        // right edge
        highlightRect.set(right, top, right, bottom)
        highlightRect.inset(-slop, 0f)
        if (highlightRect.contains(x, y)) {
            highlightRect.inset(slop, 0f)
            if (forUpdate) checkMode = 6
            return true
        }

        // bottom edge
        highlightRect.set(left, bottom, right, bottom)
        highlightRect.inset(0f, -slop)
        if (highlightRect.contains(x, y)) {
            highlightRect.inset(0f, slop)
            if (forUpdate) checkMode = 7
            return true
        }

        // left edge
        highlightRect.set(left, top, left, bottom)
        highlightRect.inset(-slop, 0f)
        if (highlightRect.contains(x, y)) {
            highlightRect.inset(slop, 0f)
            if (forUpdate) checkMode = 8
            return true
        }

        return false
    }

    override fun hitTest(x: Float, y: Float, forUpdate: Boolean): Boolean {
        highlightRect.set(baseRect)
        highlightRect.sort()

        if (highlightRect.contains(x, y)) {
            if (forUpdate) checkMode = 0
            lastX = x
            lastY = y
            return true
        }

        return false
    }

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    override fun updateMove(curX: Float, curY: Float, maxWidth: Float, maxHeight: Float): Boolean {

        val lx = when (checkMode) {
            1, 4, 8 -> baseRect.left
            2, 6, 3 -> baseRect.right
            0 -> lastX
            else -> curX
        }
        val ly = when (checkMode) {
            1, 2, 5 -> baseRect.top
            3, 4, 7 -> baseRect.bottom
            0 -> lastY
            else -> curY
        }

        val dx = calculateDx(curX, lx, maxWidth)
        val dy = calculateDy(curY, ly, maxHeight)

        if (dx == 0f && dy == 0f) return false

        drawRect.set(baseRect)

        when (checkMode) {
            0 -> {
                drawRect.sort()

                drawRect.offset(dx, dy)
                if (drawRect.left < 0) {
                    drawRect.left = 0f
                } else if (drawRect.right > maxWidth) {
                    drawRect.left = maxWidth - drawRect.width()
                }

                if (drawRect.top < 0) {
                    drawRect.top = 0f
                } else if (drawRect.bottom > maxHeight) {
                    drawRect.top = maxHeight - drawRect.height()
                }

                val realDx = drawRect.left - baseRect.left
                val realDy = drawRect.top - baseRect.top

                if (realDx == 0f && realDy == 0f) {
                    return false
                }

                lastX += realDx
                lastY += realDy
                baseRect.offset(realDx, realDy)
                highlightRect.offset(realDx, realDy)

                return true
            }
            1 -> {
                baseRect.left += dx
                baseRect.top += dy
                highlightRect.offset(dx, dy)
                return true
            }
            2 -> {
                baseRect.right += dx
                baseRect.top += dy
                highlightRect.offset(dx, dy)
                return true
            }
            3 -> {
                baseRect.right += dx
                baseRect.bottom += dy
                highlightRect.offset(dx, dy)
                return true
            }
            4 -> {
                baseRect.left += dx
                baseRect.bottom += dy
                highlightRect.offset(dx, dy)
                return true
            }
            5 -> {
                if (dy == 0f) return false
                baseRect.top += dy
                highlightRect.offset(0f, dy)
                return true
            }
            6 -> {
                if (dx == 0f) return false
                baseRect.right += dx
                highlightRect.offset(dx, 0f)
                return true
            }
            7 -> {
                if (dy == 0f) return false
                baseRect.bottom += dy
                highlightRect.offset(0f, dy)
                return true
            }
            8 -> {
                if (dx == 0f) return false
                baseRect.left += dx
                highlightRect.offset(dx, 0f)
                return true
            }
        }

        return true
    }

    private fun calculateDx(curX: Float, lastX: Float, w: Float): Float {

        val dx = curX - lastX

        if (dx == 0f) return 0f

        if ((lastX <= 0 && curX <= 0) || (lastX >= w && curX >= w)) {
            // just outside both
            return 0f
        } else if (lastX > 0 && curX > 0 && lastX < w && curX < w) {
            // just inside both
            return dx
        } else if (lastX <= 0 && curX >= w) {
            // across from left to right
            return w
        } else if (lastX >= w && curX <= 0) {
            // across from right to left
            return -w
        } else if (lastX <= 0 && curX > 0) {
            // left outside through left edge to inside
            return curX
        } else if (curX <= 0 && lastX > 0) {
            // inside through right edge to right outside
            return -lastX
        } else if (curX < w && lastX >= w) {
            // right outside through right edge to inside
            return curX - w
        } else {
            // inside through right edge to right outside
            return w - lastX
        }
    }

    private fun calculateDy(curY: Float, lastY: Float, h: Float): Float {
        val dy = curY - lastY

        if (dy == 0f) return 0f

        if ((lastY <= 0 && curY <= 0) || (lastY >= h && curY >= h)) {
            // just outside both
            return 0f
        } else if (lastY > 0 && curY > 0 && lastY < h && curY < h) {
            // just inside both
            return dy
        } else if (lastY <= 0 && curY >= h) {
            // across from top to bottom
            return h
        } else if (lastY >= h && curY <= 0) {
            // across from bottom to top
            return -h
        } else if (lastY <= 0 && curY > 0) {
            // bottom outside through top edge to inside
            return curY
        } else if (curY <= 0 && lastY > 0) {
            // inside through top edge to top outside
            return -lastY
        } else if (curY < h && lastY >= h) {
            // bottom outside through bottom edge to inside
            return curY - h
        } else {
            // inside through bottom edge to bottom outside
            return h - lastY
        }
    }

    override fun isSelecting(): Boolean = checkMode == 9

    override fun isUpdating(): Boolean = checkMode in 0..8

    override fun selectStart(): Boolean {
        if (checkMode == 9) return false
        checkMode = 9
        highlightRect.set(baseRect)
        highlightRect.sort()
        return true
    }

    override fun selectEnd(): Boolean {
        if (isSelecting()) {
            checkMode = -1
            return true
        }
        return false
    }

    @CheckResult
    override fun updateEnd(): Int {
        if (isUpdating()) {
            checkMode = -1
            baseRect.sort()
            return if (checkValid()) {
                RESULT_UPDATED
            } else {
                RESULT_DELETE
            }
        }
        return RESULT_IGNORE
    }
}
