class DraggableMaskView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    
    private var initialX = 0f
    private var maskAlpha = 128
    private var onPositionChanged: ((Int) -> Unit)? = null
    
    init {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - initialX
                    translationX += dx
                    
                    // 回傳位置給MainActivity
                    val position = (x + translationX).toInt()
                    onPositionChanged?.invoke(position)
                    
                    initialX = event.x
                }
            }
            true
        }
    }
    
    fun setMaskAlpha(alpha: Int) {
        maskAlpha = alpha
        invalidate()
    }
    
    fun setOnPositionChangedListener(listener: (Int) -> Unit) {
        onPositionChanged = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint().apply {
            color = Color.argb(maskAlpha, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
