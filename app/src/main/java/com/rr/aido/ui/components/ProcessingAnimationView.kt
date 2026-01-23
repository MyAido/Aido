package com.rr.aido.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import com.rr.aido.data.models.ProcessingAnimationType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ProcessingAnimationView - Custom view for showing different processing animations
 */
class ProcessingAnimationView(context: Context) : View(context) {
    
    private var animationType: ProcessingAnimationType = ProcessingAnimationType.GENTLE_GLOW
    private var animator: ValueAnimator? = null
    private var animationProgress: Float = 0f
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    
    // For Matrix Rain animation
    private val matrixDrops = mutableListOf<MatrixDrop>()
    private val matrixChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*".toCharArray()
    
    // For Typing Dots animation
    private val dotCount = 3
    private val dotRadius = 12f
    private val dotSpacing = 40f
    
    // For Particle Flow animation
    private val particles = mutableListOf<Particle>()
    
    init {
        // Initialize matrix rain drops
        repeat(20) {
            matrixDrops.add(MatrixDrop(
                x = Random.nextFloat() * 1000,
                y = Random.nextFloat() * 2000 - 1000,
                speed = Random.nextFloat() * 5 + 3,
                char = matrixChars.random()
            ))
        }
        
        // Initialize particles
        repeat(30) {
            particles.add(Particle(
                x = Random.nextFloat() * 1000,
                y = Random.nextFloat() * 2000,
                speedX = Random.nextFloat() * 2 - 1,
                speedY = Random.nextFloat() * 2 - 1,
                size = Random.nextFloat() * 8 + 4
            ))
        }
    }
    
    fun setAnimationType(type: ProcessingAnimationType) {
        animationType = type
        invalidate()
    }
    
    fun startAnimation() {
        stopAnimation()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = when (animationType) {
                ProcessingAnimationType.PULSE_WAVE -> 2000
                ProcessingAnimationType.TYPING_DOTS -> 1500
                ProcessingAnimationType.BRAIN_THINKING -> 3000
                ProcessingAnimationType.COLOR_WAVE -> 2500
                ProcessingAnimationType.GENTLE_GLOW -> 3000
                ProcessingAnimationType.BREATHING_CIRCLE -> 4000
                ProcessingAnimationType.SHIMMER -> 2000
                ProcessingAnimationType.PARTICLE_FLOW -> 100
                ProcessingAnimationType.MATRIX_RAIN -> 100
            }
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        when (animationType) {
            ProcessingAnimationType.PULSE_WAVE -> drawPulseWave(canvas, centerX, centerY)
            ProcessingAnimationType.TYPING_DOTS -> drawTypingDots(canvas, centerX, centerY)
            ProcessingAnimationType.BRAIN_THINKING -> drawBrainThinking(canvas, centerX, centerY)
            ProcessingAnimationType.COLOR_WAVE -> drawColorWave(canvas, centerX, centerY)
            ProcessingAnimationType.GENTLE_GLOW -> drawGentleGlow(canvas, centerX, centerY)
            ProcessingAnimationType.BREATHING_CIRCLE -> drawBreathingCircle(canvas, centerX, centerY)
            ProcessingAnimationType.SHIMMER -> drawShimmer(canvas, centerX, centerY)
            ProcessingAnimationType.PARTICLE_FLOW -> drawParticleFlow(canvas)
            ProcessingAnimationType.MATRIX_RAIN -> drawMatrixRain(canvas)
        }
    }
    
    private fun drawPulseWave(canvas: Canvas, centerX: Float, centerY: Float) {
        val maxRadius = width.coerceAtMost(height) / 2f
        
        // Draw 3 expanding waves
        for (i in 0..2) {
            val offset = i * 0.33f
            val progress = (animationProgress + offset) % 1f
            val radius = progress * maxRadius
            val alpha = ((1f - progress) * 255).toInt()
            
            paint.color = Color.argb(alpha, 100, 200, 255)
            canvas.drawCircle(centerX, centerY, radius, paint)
            
            // Inner circle with different color
            if (progress < 0.5f) {
                paint.color = Color.argb((alpha * 1.5f).toInt().coerceAtMost(255), 255, 150, 100)
                canvas.drawCircle(centerX, centerY, radius * 0.5f, paint)
            }
        }
    }
    
    private fun drawTypingDots(canvas: Canvas, centerX: Float, centerY: Float) {
        val startX = centerX - (dotSpacing * (dotCount - 1) / 2f)
        
        for (i in 0 until dotCount) {
            val x = startX + i * dotSpacing
            val delay = i * 0.33f
            val progress = (animationProgress + delay) % 1f
            
            // Bounce effect using sine
            val bounce = sin(progress * Math.PI).toFloat()
            val y = centerY - bounce * 30f
            
            val alpha = (bounce * 255).toInt().coerceIn(100, 255)
            paint.color = Color.argb(alpha, 100, 200, 255)
            canvas.drawCircle(x, y, dotRadius, paint)
        }
    }
    
    private fun drawBrainThinking(canvas: Canvas, centerX: Float, centerY: Float) {
        val rotation = animationProgress * 360f
        
        // Draw brain shape (simplified as circle with arcs)
        paint.color = Color.argb(200, 255, 150, 200)
        canvas.drawCircle(centerX, centerY, 60f, paint)
        
        // Draw rotating arcs to simulate "thinking"
        strokePaint.color = Color.argb(150, 100, 200, 255)
        strokePaint.strokeWidth = 4f
        
        for (i in 0..3) {
            val angle = rotation + i * 90f
            canvas.save()
            canvas.rotate(angle, centerX, centerY)
            strokePaint.alpha = ((sin(animationProgress * Math.PI * 2 + i) + 1) * 127).toInt()
            canvas.drawArc(
                centerX - 80f, centerY - 80f,
                centerX + 80f, centerY + 80f,
                0f, 180f, false, strokePaint
            )
            canvas.restore()
        }
        
        // Draw particles
        for (i in 0..5) {
            val particleAngle = rotation + i * 60f
            val distance = 100f + sin(animationProgress * Math.PI * 4 + i).toFloat() * 20f
            val particleX = centerX + cos(Math.toRadians(particleAngle.toDouble())).toFloat() * distance
            val particleY = centerY + sin(Math.toRadians(particleAngle.toDouble())).toFloat() * distance
            
            paint.color = Color.argb(200, 255, 200, 100)
            canvas.drawCircle(particleX, particleY, 8f, paint)
        }
    }
    
    private fun drawColorWave(canvas: Canvas, centerX: Float, centerY: Float) {
        val path = Path()
        val waveWidth = width.toFloat()
        val waveHeight = 100f
        
        // Draw flowing wave
        path.moveTo(0f, centerY)
        
        val segments = 50
        for (i in 0..segments) {
            val x = (i.toFloat() / segments) * waveWidth
            val offset = animationProgress * Math.PI * 2
            val y = centerY + sin(offset + i * 0.3).toFloat() * waveHeight
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Gradient effect with multiple waves
        for (j in 0..2) {
            val hue = ((animationProgress + j * 0.33f) * 360f) % 360f
            paint.color = Color.HSVToColor(150, floatArrayOf(hue, 0.7f, 0.9f))
            strokePaint.color = paint.color
            strokePaint.strokeWidth = 6f
            
            canvas.save()
            canvas.translate(0f, j * 40f - 40f)
            canvas.drawPath(path, strokePaint)
            canvas.restore()
        }
    }
    
    private fun drawGentleGlow(canvas: Canvas, centerX: Float, centerY: Float) {
        // Peaceful pulsing glow effect (no dark background)
        val baseRadius = 80f
        val pulseAmount = sin(animationProgress * Math.PI * 2).toFloat() * 20f
        val radius = baseRadius + pulseAmount
        
        // Draw multiple layers for glow effect
        for (i in 0..4) {
            val layerRadius = radius + i * 15f
            val alpha = ((5 - i) * 30).coerceIn(20, 150)
            paint.color = Color.argb(alpha, 150, 200, 255)
            canvas.drawCircle(centerX, centerY, layerRadius, paint)
        }
        
        // Center bright spot
        paint.color = Color.argb(200, 200, 230, 255)
        canvas.drawCircle(centerX, centerY, radius * 0.5f, paint)
    }
    
    private fun drawBreathingCircle(canvas: Canvas, centerX: Float, centerY: Float) {
        // Calming breathing animation (transparent background)
        val breathProgress = sin(animationProgress * Math.PI * 2).toFloat()
        val scale = 0.7f + breathProgress * 0.3f
        val radius = 100f * scale
        
        // Outer ring
        strokePaint.color = Color.argb(180, 100, 180, 255)
        strokePaint.strokeWidth = 6f
        canvas.drawCircle(centerX, centerY, radius, strokePaint)
        
        // Inner glow
        paint.color = Color.argb((breathProgress * 100 + 50).toInt(), 150, 200, 255)
        canvas.drawCircle(centerX, centerY, radius * 0.7f, paint)
        
        // Center dot
        paint.color = Color.argb(200, 120, 190, 255)
        canvas.drawCircle(centerX, centerY, 12f, paint)
    }
    
    private fun drawShimmer(canvas: Canvas, centerX: Float, centerY: Float) {
        // Subtle sparkling effect (transparent background)
        paint.textSize = 40f
        
        // Draw sparkles at various positions
        for (i in 0..8) {
            val angle = i * 40f + animationProgress * 360f
            val distance = 120f + sin(animationProgress * Math.PI * 3 + i).toFloat() * 30f
            val x = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * distance
            val y = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * distance
            
            val sparkleProgress = (animationProgress + i * 0.1f) % 1f
            val alpha = (sin(sparkleProgress * Math.PI) * 255).toInt().coerceIn(0, 255)
            
            paint.color = Color.argb(alpha, 255, 220, 100)
            canvas.drawText("✦", x, y, paint)
        }
        
        // Center glow
        paint.color = Color.argb(150, 255, 230, 150)
        canvas.drawCircle(centerX, centerY, 40f, paint)
    }
    
    private fun drawParticleFlow(canvas: Canvas) {
        // Floating particles animation (transparent background)
        particles.forEach { particle ->
            // Update position
            particle.x += particle.speedX
            particle.y += particle.speedY
            
            // Wrap around screen
            if (particle.x < -50) particle.x = width + 50f
            if (particle.x > width + 50) particle.x = -50f
            if (particle.y < -50) particle.y = height + 50f
            if (particle.y > height + 50) particle.y = -50f
            
            // Draw particle with glow
            val alpha = ((sin(animationProgress * Math.PI * 4 + particle.x) + 1) * 127).toInt()
            paint.color = Color.argb(alpha, 150, 200, 255)
            canvas.drawCircle(particle.x, particle.y, particle.size, paint)
            
            // Add glow ring
            strokePaint.color = Color.argb(alpha / 2, 100, 180, 255)
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(particle.x, particle.y, particle.size * 1.5f, strokePaint)
        }
    }

    private fun drawMatrixRain(canvas: Canvas) {
        paint.textSize = 30f
        
        // Update and draw matrix drops
        matrixDrops.forEach { drop ->
            // Update position
            drop.y += drop.speed
            
            // Reset if off screen
            if (drop.y > height + 100) {
                drop.y = -100f
                drop.x = Random.nextFloat() * width
                drop.char = matrixChars.random()
            }
            
            // Draw character
            val alpha = ((height - drop.y) / height * 255).toInt().coerceIn(50, 255)
            paint.color = Color.argb(alpha, 0, 255, 100)
            canvas.drawText(drop.char.toString(), drop.x, drop.y, paint)
            
            // Draw trail
            for (i in 1..5) {
                val trailY = drop.y - i * 30f
                val trailAlpha = (alpha * (1f - i / 5f)).toInt().coerceAtLeast(0)
                paint.color = Color.argb(trailAlpha, 0, 200, 100)
                canvas.drawText(matrixChars.random().toString(), drop.x, trailY, paint)
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
    
    private data class MatrixDrop(
        var x: Float,
        var y: Float,
        var speed: Float,
        var char: Char
    )
    
    private data class Particle(
        var x: Float,
        var y: Float,
        var speedX: Float,
        var speedY: Float,
        var size: Float
    )
}
