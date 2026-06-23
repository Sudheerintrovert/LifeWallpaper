package com.lifewallpaper.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.*
import kotlin.math.max
import kotlin.math.min

class LifeWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LifeEngine()

    inner class LifeEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private lateinit var prefs: SharedPreferences
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var holder: SurfaceHolder? = null
        private var timeTickReceiver: BroadcastReceiver? = null
        private var fadeAlpha = 0
        private var breathingPhase = 0f

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            prefs = getSharedPreferences("life_wallpaper", MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            handler.removeCallbacksAndMessages(null)
            try { unregisterReceiver(timeTickReceiver) } catch (_: Exception) {}
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                fadeAlpha = 0
                animateFadeIn()
                registerTimeTick()
                draw()
            } else {
                handler.removeCallbacksAndMessages(null)
                try { unregisterReceiver(timeTickReceiver) } catch (_: Exception) {}
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            this.holder = holder
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            this.holder = null
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.holder = holder
            if (isVisible) draw()
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private fun registerTimeTick() {
            try { unregisterReceiver(timeTickReceiver) } catch (_: Exception) {}
            timeTickReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_TIME_TICK || intent?.action == Intent.ACTION_DATE_CHANGED) {
                        draw()
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
            }
            registerReceiver(timeTickReceiver, filter)
        }

        private fun animateFadeIn() {
            if (fadeAlpha < 255) {
                fadeAlpha = min(255, fadeAlpha + 15)
                draw()
                handler.postDelayed({ animateFadeIn() }, 30)
            }
        }

        private fun draw() {
            val h = holder ?: return
            val canvas = h.lockCanvas() ?: return

            try {
                val w = canvas.width
                val ht = canvas.height

                // Read settings
                val isDark = prefs.getBoolean("dark_theme", true)
                val textHex = prefs.getString("text_color", "#ffffff") ?: "#ffffff"
                val birthDateStr = prefs.getString("birth_date", "") ?: ""
                val goalDateStr = prefs.getString("goal_date", "") ?: ""
                val goalText = prefs.getString("goal_text", "") ?: ""
                val fontSize = prefs.getInt("font_size", 48)
                val fontFamily = prefs.getInt("font_family", 0) // 0=sans, 1=serif, 2=mono
                val breathing = prefs.getBoolean("breathing", true)
                val dividerStyle = prefs.getInt("divider_style", 0) // 0=thin, 1=dashed, 2=gradient

                // Background
                canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)

                if (birthDateStr.isEmpty() || goalDateStr.isEmpty()) {
                    // Draw setup message
                    val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor(if (isDark) "#333333" else "#cccccc")
                        textSize = 40f
                        textAlign = Paint.Align.CENTER
                        typeface = getTypeface(fontFamily)
                    }
                    canvas.drawText("Open Settings to Setup", w / 2f, ht / 2f, msgPaint)
                    h.unlockCanvasAndPost(canvas)
                    return
                }

                // Parse dates
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val birthDate = sdf.parse(birthDateStr) ?: Date(0)
                val goalDate = sdf.parse(goalDateStr) ?: Date(0)
                val now = Date()

                val elapsed = calcDiff(birthDate, now)
                val isGoalPast = goalDate.before(now)
                val remaining = if (isGoalPast) Triple(0, 0, 0) else calcDiff(now, goalDate)

                // Calculate sizes
                val baseSize = fontSize.toFloat() * 3 // scale for px
                val labelSize = baseSize * 0.35f
                val goalSize = baseSize * 0.55f
                val dateSize = baseSize * 0.3f
                val achievedSize = baseSize * 0.5f

                val textColor = try { Color.parseColor(textHex) } catch (_: Exception) { Color.parseColor("#ffffff") }
                val dimColor = Color.argb(
                    (textColor.alpha * 0.5f).toInt(),
                    textColor.red, textColor.green, textColor.blue
                )
                val faintColor = Color.argb(
                    (textColor.alpha * 0.2f).toInt(),
                    textColor.red, textColor.green, textColor.blue
                )
                val dividerColor = Color.argb(
                    max(20, (textColor.alpha * 0.15f).toInt()),
                    textColor.red, textColor.green, textColor.blue
                )

                val cx = w / 2f
                var y = ht / 2f

                // ── Layout Calculation (center vertically) ──
                val lineH = baseSize * 1.3f
                val gap = baseSize * 1.5f
                val dividerH = baseSize * 0.6f

                val goalLines = wrapText(goalText, goalSize, w * 0.7f)
                val goalBlockH = goalLines.size * goalSize * 1.4f

                val totalH = (lineH * 3) + gap + dividerH + gap + goalBlockH + gap + dividerH + gap + dateSize + gap + (lineH * 3)
                var startY = (ht - totalH) / 2f + baseSize * 0.4f

                canvas.save()
                canvas.globalAlpha = fadeAlpha / 255f

                // ── Elapsed Time ──
                y = startY
                drawTimeValue(canvas, elapsed.first, "Y", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)
                y += lineH
                drawTimeValue(canvas, elapsed.second, "M", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)
                y += lineH
                drawTimeValue(canvas, elapsed.third, "D", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)

                y += gap

                // ── Divider 1 ──
                y += dividerH / 2
                drawDivider(canvas, cx, y, w * 0.45f, dividerColor, dividerStyle)
                y += dividerH / 2

                y += gap

                // ── Goal Text ──
                val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = goalSize
                    textAlign = Paint.Align.CENTER
                    typeface = getTypeface(fontFamily)
                    letterSpacing = 0.05f
                }
                if (breathing) {
                    breathingPhase = ((System.currentTimeMillis() % 4000) / 4000f) * 2f * Math.PI.toFloat()
                    val alpha = 0.7f + 0.3f * ((1f + kotlin.math.sin(breathingPhase)) / 2f)
                    goalPaint.alpha = (alpha * 255f).toInt()
                }
                for (line in goalLines) {
                    y += goalSize * 1.2f
                    canvas.drawText(line, cx, y, goalPaint)
                }
                y += goalSize * 0.5f

                y += gap

                // ── Divider 2 ──
                y += dividerH / 2
                drawDivider(canvas, cx, y, w * 0.45f, dividerColor, dividerStyle)
                y += dividerH / 2

                y += gap

                // ── Target Date ──
                val dateStr = formatDate(goalDate)
                val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = faintColor
                    textSize = dateSize
                    textAlign = Paint.Align.CENTER
                    typeface = getTypeface(fontFamily)
                    letterSpacing = 0.15f
                }
                y += dateSize
                canvas.drawText(dateStr, cx, y, datePaint)

                y += gap

                // ── Countdown ──
                if (isGoalPast) {
                    val achPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = dimColor
                        textSize = achievedSize
                        textAlign = Paint.Align.CENTER
                        typeface = getTypeface(fontFamily)
                    }
                    y += achievedSize
                    canvas.drawText("Goal Achieved", cx, y, achPaint)
                } else {
                    drawTimeValue(canvas, remaining.first, "Y", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)
                    y += lineH
                    drawTimeValue(canvas, remaining.second, "M", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)
                    y += lineH
                    drawTimeValue(canvas, remaining.third, "D", cx, y, baseSize, labelSize, textColor, dimColor, fontFamily)
                }

                canvas.restore()

                // Breathing redraw
                if (breathing && isVisible) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ draw() }, 100)
                }

            } finally {
                try { h.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }

        private fun drawTimeValue(
            canvas: Canvas, value: Int, label: String,
            cx: Float, y: Float, numSize: Float, labelSize: Float,
            textColor: Int, dimColor: Int, fontFamily: Int
        ) {
            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = numSize
                textAlign = Paint.Align.CENTER
                typeface = getTypeface(fontFamily)
                letterSpacing = -0.02f
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dimColor
                textSize = labelSize
                textAlign = Paint.Align.LEFT
                typeface = getTypeface(fontFamily)
                letterSpacing = 0.08f
            }
            val numText = value.toString()
            val numWidth = numPaint.measureText(numText)
            canvas.drawText(numText, cx - labelSize * 0.8f, y, numPaint)
            labelPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, cx - labelSize * 0.8f + numWidth / 2f + labelSize * 0.3f, y, labelPaint)
        }

        private fun drawDivider(canvas: Canvas, cx: Float, y: Float, width: Float, color: Int, style: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                strokeWidth = 1f
                style = if (style == 1) Paint.Style.STROKE else Paint.Style.STROKE
            }
            when (style) {
                0 -> { // thin solid
                    paint.pathEffect = null
                    canvas.drawLine(cx - width / 2, y, cx + width / 2, y, paint)
                }
                1 -> { // dashed
                    paint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                    canvas.drawLine(cx - width / 2, y, cx + width / 2, y, paint)
                }
                2 -> { // gradient
                    paint.pathEffect = null
                    val gradColor1 = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
                    val grad = LinearGradient(cx - width / 2, 0f, cx + width / 2, 0f, gradColor1, color, Shader.TileMode.CLAMP)
                    paint.shader = grad
                    canvas.drawLine(cx - width / 2, y, cx + width / 2, y, paint)
                    paint.shader = null
                }
            }
        }

        private fun wrapText(text: String, size: Float, maxWidth: Float): List<String> {
            val paint = Paint().apply { textSize = size }
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val test = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(test) > maxWidth && current.isNotEmpty()) {
                    lines.add(current)
                    current = word
                } else {
                    current = test
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            return lines
        }

        private fun calcDiff(from: Date, to: Date): Triple<Int, Int, Int> {
            val calFrom = Calendar.getInstance().apply { time = from }
            val calTo = Calendar.getInstance().apply { time = to }
            var years = calTo.get(Calendar.YEAR) - calFrom.get(Calendar.YEAR)
            var months = calTo.get(Calendar.MONTH) - calFrom.get(Calendar.MONTH)
            var days = calTo.get(Calendar.DAY_OF_MONTH) - calFrom.get(Calendar.DAY_OF_MONTH)
            if (days < 0) {
                months--
                val prevMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, calTo.get(Calendar.YEAR))
                    set(Calendar.MONTH, calTo.get(Calendar.MONTH))
                }
                prevMonth.add(Calendar.DAY_OF_MONTH, -1)
                days += prevMonth.get(Calendar.DAY_OF_MONTH)
            }
            if (months < 0) { years--; months += 12 }
            return Triple(max(0, years), max(0, months), max(0, days))
        }

        private fun formatDate(date: Date): String {
            val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val cal = Calendar.getInstance().apply { time = date }
            return "${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        }

        private fun getTypeface(family: Int): Typeface {
            return when (family) {
                1 -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                2 -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                else -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
        }

        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
            if (isVisible) {
                handler.removeCallbacksAndMessages(null)
                draw()
            }
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, x: Int, y: Int) {}
        override fun onTouchEvent(event: android.view.MotionEvent?) {}
    }
}
