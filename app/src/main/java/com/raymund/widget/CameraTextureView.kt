package com.raymund.widget

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import java.lang.IllegalArgumentException

class CameraTextureView: TextureView {
    var mRequestedAspect:Double = -1.0

    constructor(context:Context, attrs: AttributeSet?, defStyle:Int) : super(context, attrs, defStyle) {
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0){
    }

    constructor(context:Context): this(context, null, 0){
    }

    public fun setAspectRatio(aspectRatio:Double){
        if(aspectRatio < 0){
            throw IllegalArgumentException()
        }
        if(mRequestedAspect != aspectRatio){
            mRequestedAspect = aspectRatio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var newWidthSpec:Int = widthMeasureSpec
        var newHeightSpec:Int = heightMeasureSpec

        if(mRequestedAspect > 0) {
            var paddingHorizontal = paddingLeft + paddingRight;
            var paddingVertical = paddingTop + paddingBottom;

            var initialWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingHorizontal
            var initialHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingVertical

            var viewAspectRatio:Double = initialWidth.toDouble() / initialHeight
            var aspectDifference:Double = (mRequestedAspect / viewAspectRatio) - 1

            if(Math.abs(aspectDifference) > 0.01){
                if(aspectDifference > 0) {
                    // Width Priority Decision
                    initialHeight = (initialWidth / mRequestedAspect).toInt()
                } else {
                    initialWidth = (initialHeight * mRequestedAspect).toInt()
                }
                initialWidth += paddingHorizontal
                initialHeight += paddingVertical
                newWidthSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                newHeightSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }

        super.onMeasure(newWidthSpec, newHeightSpec)
    }
}