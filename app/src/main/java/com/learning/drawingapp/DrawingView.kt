package com.learning.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context : Context, attrs : AttributeSet) : View(context,attrs) {

    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap : Bitmap? = null
    private var mDrawpaint : Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushThickness : Float = 0.toFloat()
    private var color : Int = Color.BLACK
    private var canvas : Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val undoPaths = ArrayList<CustomPath>()


    init {
        setUpDrawing()
    }

    fun undoPaths(){
        if(mPaths.size > 0){
            undoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }

    private fun setUpDrawing() {
        mDrawpaint = Paint()
        mDrawPath = CustomPath(color,mBrushThickness)

        mDrawpaint!!.color = color
        mDrawpaint!!.style = Paint.Style.STROKE
        mDrawpaint!!.strokeJoin = Paint.Join.ROUND
        mDrawpaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        mBrushThickness = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mCanvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas= Canvas(mCanvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f, mCanvasPaint)
        for(path in mPaths){
            mDrawpaint!!.strokeWidth = path.brushThickness
            mDrawpaint!!.color = path.color
            canvas.drawPath(path,mDrawpaint!!)
        }

        if(!mDrawPath!!.isEmpty){
            mDrawpaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawpaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawpaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var touchX = event?.x
        var touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushThickness

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color,mBrushThickness)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize(newBrushSize : Float){
        mBrushThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newBrushSize, resources.displayMetrics)
        mDrawpaint!!.strokeWidth = mBrushThickness
    }

    fun setColor(mColor : String){
        color = Color.parseColor(mColor)
        mDrawpaint!!.color = color
    }

    internal inner class CustomPath(var color:Int, var brushThickness : Float): Path(){

    }

}