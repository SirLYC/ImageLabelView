# ImageLabelView
[![](https://jitpack.io/v/SirLYC/ImageLabelView.svg)](https://jitpack.io/#SirLYC/ImageLabelView)

[English](https://github.com/SirLYC/ImageLabelView/blob/master/README-zh.md)|[我的博客](https://juejin.im/user/592e23d3ac502e006c9afdd7)

一款用于图片画框标注的工具。

![1](https://github.com/SirLYC/ImageLabelView/blob/master/images/1.gif?raw=true)
![2](https://github.com/SirLYC/ImageLabelView/blob/master/images/2.gif?raw=true)


## 添加到你的项目
**第一步** 在你的根项目的build.gradle中repositories末尾添加：

``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
````
**第二步** 添加依赖

``` gradle
dependencies {
    implementation 'com.github.SirLYC:ImageLabelView:{latest version}'
}
```
## 在代码中使用
> 你可以先查看样例 [sample code](https://github.com/SirLYC/ImageLabelView/tree/master/sample)

**第一步** 添加到你的布局

**第二步** 设置LabelCreator
LabelCreator是用来创建你的Label的。它的createLabel放你发会在绘制新的Label时调用。
首先，你需要实现一个
```
ImageLabelView#LabelCreator. 
```
在项目给出的sample中有一个简单实现（矩形的label）：
```kotlin
label.labelCreator = object : ImageLabelView.LabelCreator {
            override fun createLabel(): RectLabel {
                return RectLabel()
            }
        }
```
**记得一定要把LabelCreator设置给ImageLabel的labelCreator变量！**

**第三步** 把一张图片设置进去（通过bitmap形式）

类似于ImageView的CenterInside表现。Bitmap可以为空，但是会清空之前的所有标签。
```kotlin
val bitmap: Bitmap? = ... // download or read from disk
label.setBitmap(bitmap)
```

**第四步** 在4种模式下完成工作
- PREVIEW<br>
这是默认模式。每当你设置一个新的bitmap时都会改变到这个模式。在这个模式下，你可以移动或放大缩小图片。
- DRAW<br>
在这个模式可以画框。比如矩形框，对角就是你按下和抬起时的位置。手指抬起后，会选中这个框进入SELECT模式。
- UPDATE<br>
在这个模式可以改变框的大小或者位置。可以拖动一个角或者一条边改变大小，或者按到一个标签中央移动它。
- SELECT<br>
在这个模式下可以通过点击或长按选中一个标签。标签选中后可以用如下代码获取：
``` kotlin
label.selectingLabel()
```
这个模式一般用于处理messaege属性的输入或者删除它。

**第五步** 导出数据
拿到Label的引用后直接调用它的 **getData()** 方法和 **message** 属性获取信息。

## 待完成/修复
- [x] 修复一次会选中多个标签的问题 
- [ ] 配置信息改变时会丢失状态
- [ ] 圆形的框
- [ ] 三角形的框
- [ ] 其他多边形...

## License
```
MIT License

Copyright (c) 2019 Liu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
