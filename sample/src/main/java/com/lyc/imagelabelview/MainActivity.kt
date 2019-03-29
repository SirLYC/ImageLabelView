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

package com.lyc.imagelabelview

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.lyc.imagelabel.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        download.setOnClickListener {
            Glide.with(this).asBitmap().listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean
                ): Boolean {
                    Log.e("LoadImg", "Fail")
                    label.setBitmap(null)
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            }).load(url.text.toString()).into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    label.setBitmap(resource)
                }
            })
        }

        label.labelCreator = object : ImageLabelView.LabelCreator {
            override fun createLabel(): RectLabel {
                return RectLabel()
            }
        }

        label.labelDrawListener = object : ImageLabelView.LabelDrawListener {
            override fun onLabelDrawEnd(created: Boolean, label: Label<*>?) {
                pos.text = ("draw end $created")
            }

            override fun onLabelDrawStart(pointF: PointF, label: Label<*>?) {
                pos.text = ("down $pointF")
            }

            override fun onLabelDraw(pointF: PointF, label: Label<*>?) {
                pos.text = ("drawMove $pointF")
            }
        }

        label.labelSelectListener = object : ImageLabelView.LabelSelectListener {
            override fun onLabelSelectStart(label: Label<*>) {
                pos.text = ("select start")
            }

            override fun onLabelSelectEnd(label: Label<*>) {
                pos.text = ("select end")
            }
        }

        label.labelUpdateListener = object : ImageLabelView.LabelUpdateListener {
            override fun onLabelUpdateStart(label: Label<*>) {
                pos.text = ("label update start")
            }

            override fun onLabelUpdateEnd(label: Label<*>) {
                pos.text = ("label update end")
            }
        }

        mode.text = ImageLabelView.modeToString(label.mode)
        mode.setOnClickListener {
            label.mode = (label.mode + 1) % 4
            mode.text = ImageLabelView.modeToString(label.mode)
        }
    }
}
