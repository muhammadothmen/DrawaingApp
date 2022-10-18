package com.othman.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var drawPath : CustomPath? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var canvasBitmap: Bitmap? = null
    private var brushSize : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private var paths = ArrayList<CustomPath>()
    private var undoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    fun onClickUndo(){
        if (paths.isNotEmpty()) {
            undoPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun onClickRedo(){
        if (undoPaths.isNotEmpty()) {
            paths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }

    private fun setUpDrawing(){
        drawPaint = Paint()
        drawPath = CustomPath(color, brushSize)
        drawPaint!!.color = color
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        //brushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!,0f,0f, canvasPaint)
        for (path in paths) {
        if (!path.isEmpty) {
            drawPaint!!.strokeWidth = path.brushThickness
            drawPaint!!.color = path.color
            canvas.drawPath(path, drawPaint!!)
            }
        }
        if (!drawPath!!.isEmpty) {
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color
            canvas.drawPath(drawPath!!, drawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchx = event?.x
        val touchy = event?.y
        when (event?.action){
            MotionEvent.ACTION_DOWN ->{
                drawPath!!.color = color
                drawPath!!.brushThickness = brushSize
                drawPath!!.reset()
                if (touchx != null) {
                    if (touchy != null) {
                        drawPath!!.moveTo(touchx, touchy)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchx != null) {
                    if (touchy != null) {
                        drawPath!!.lineTo(touchx, touchy)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                paths.add(drawPath!!)
                drawPath = CustomPath(color,brushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize (newSize: Float){
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        drawPaint!!.strokeWidth = brushSize
    }

    fun setColor (newColor: Int){
        color = newColor
        drawPaint!!.color = color
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {
    }

}