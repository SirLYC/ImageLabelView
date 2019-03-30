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

package com.lyc.sample

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
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
                info_text.text = ("draw end $created")
            }

            override fun onLabelDrawStart(pointF: PointF, label: Label<*>?) {
                info_text.text = ("down $pointF")
                setLabelMsg("")
            }

            override fun onLabelDraw(pointF: PointF, label: Label<*>?) {
                info_text.text = ("drawMove $pointF")
            }
        }

        label.labelSelectListener = object : ImageLabelView.LabelSelectListener {
            override fun onLabelSelectStart(label: Label<*>) {
                info_text.text = ("select start")
                inputLabelMsg("${label.message ?: ""}")
                Toast.makeText(this@MainActivity, "input a label for your label!", Toast.LENGTH_SHORT).show()
            }

            override fun onLabelSelectEnd(label: Label<*>) {
                info_text.text = ("select end")
                setLabelMsg("")
            }
        }

        label.labelUpdateListener = object : ImageLabelView.LabelUpdateListener {
            override fun onLabelUpdateStart(label: Label<*>) {
                info_text.text = ("label update start")
                setLabelMsg("${label.message ?: ""}")
            }

            override fun onLabelUpdateEnd(label: Label<*>) {
                info_text.text = ("label update end")
                setLabelMsg("")
            }
        }

        set_msg.setOnClickListener {
            label?.selectingLabel()?.run {
                message = "${label_info.text}"
                Toast.makeText(this@MainActivity, "new info applied!", Toast.LENGTH_SHORT).show()
            }
            label.mode = ImageLabelView.PREVIEW
        }

        mode.text = ImageLabelView.modeToString(label.mode)

        label.modeChangeListener = object : ImageLabelView.ModeChangeListener {
            override fun onModeChange(old: Int, new: Int) {
                mode.text = ImageLabelView.modeToString(new)
            }
        }

        mode.setOnClickListener {
            label.mode = (label.mode + 1) % 4
            mode.text = ImageLabelView.modeToString(label.mode)
        }

        show_info.setOnClickListener {
            val curLabel = label.curLabel
            if (curLabel == null) {
                info_text.text = ("recent label: ${label.mostRecentLabel()}")
            } else {
                info_text.text = ("current label: ${label.mostRecentLabel()}")
            }
        }

        delete.setOnClickListener {
            val curLabel = label.selectingLabel()
            if (curLabel != null) {
                label.removeLabel(curLabel)
            }
        }
    }

    private fun inputLabelMsg(msg: String) {
        label_info.isFocusable = true
        label_info.isFocusableInTouchMode = true
        label_info.requestFocus()
        label_info.setText(msg)
        set_msg.isEnabled = true
        delete.isEnabled = true
    }

    private fun setLabelMsg(msg: String) {
        label_info.isFocusable = false
        label_info.isFocusableInTouchMode = false
        label_info.clearFocus()
        label_info.setText(msg)
        set_msg.isEnabled = false
        delete.isEnabled = false
        val imm: InputMethodManager? = getSystemService()
        imm?.hideSoftInputFromWindow(label_info.windowToken, 0)
    }
}
