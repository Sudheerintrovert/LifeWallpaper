package com.lifewallpaper.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // Views
    private lateinit var birthInput: EditText
    private lateinit var goalInput: EditText
    private lateinit var goalTextInput: EditText
    private lateinit var textColorBtn: Button
    private lateinit var darkToggle: Switch
    private lateinit var fontSizeSlider: SeekBar
    private lateinit var fontSizeLabel: TextView
    private lateinit var fontRadio: RadioGroup
    private lateinit var breatheToggle: Switch
    private lateinit var dividerRadio: RadioGroup
    private lateinit var preview: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("life_wallpaper", MODE_PRIVATE)

        // Bind views
        birthInput = findViewById(R.id.input_birth)
        goalInput = findViewById(R.id.input_goal)
        goalTextInput = findViewById(R.id.input_goal_text)
        textColorBtn = findViewById(R.id.btn_text_color)
        darkToggle = findViewById(R.id.toggle_dark)
        fontSizeSlider = findViewById(R.id.slider_font_size)
        fontSizeLabel = findViewById(R.id.label_font_size)
        fontRadio = findViewById(R.id.radio_font)
        breatheToggle = findViewById(R.id.toggle_breathe)
        dividerRadio = findViewById(R.id.radio_divider)
        preview = findViewById(R.id.preview)

        // Load current values
        birthInput.setText(prefs.getString("birth_date", ""))
        goalInput.setText(prefs.getString("goal_date", ""))
        goalTextInput.setText(prefs.getString("goal_text", ""))
        darkToggle.isChecked = prefs.getBoolean("dark_theme", true)
        breatheToggle.isChecked = prefs.getBoolean("breathing", true)

        val savedSize = prefs.getInt("font_size", 48)
        fontSizeSlider.progress = savedSize
        fontSizeLabel.text = "${savedSize}px"

        val savedFont = prefs.getInt("font_family", 0)
        when (savedFont) {
            1 -> fontRadio.check(R.id.font_serif)
            2 -> fontRadio.check(R.id.font_mono)
            else -> fontRadio.check(R.id.font_sans)
        }

        val savedDivider = prefs.getInt("divider_style", 0)
        when (savedDivider) {
            1 -> dividerRadio.check(R.id.div_dashed)
            2 -> dividerRadio.check(R.id.div_gradient)
            else -> dividerRadio.check(R.id.div_thin)
        }

        updatePreview()
        applyTheme()

        // Color picker
        textColorBtn.setOnClickListener {
            val color = prefs.getInt("text_color_int", Color.WHITE)
            // Simple color toggle: cycle through preset colors
            val colors = intArrayOf(
                Color.WHITE, Color.parseColor("#aaaaaa"),
                Color.parseColor("#00e5ff"), Color.parseColor("#76ff03"),
                Color.parseColor("#ff9100"), Color.parseColor("#e040fb"),
                Color.parseColor("#ff1744"), Color.parseColor("#ffd600")
            )
            val hexes = arrayOf("#ffffff","#aaaaaa","#00e5ff","#76ff03","#ff9100","#e040fb","#ff1744","#ffd600")
            val names = arrayOf("White","Gray","Cyan","Green","Orange","Pink","Red","Yellow")
            val currentIdx = colors.indexOf(color)
            val nextIdx = (currentIdx + 1) % colors.size
            prefs.edit()
                .putInt("text_color_int", colors[nextIdx])
                .putString("text_color", hexes[nextIdx])
                .apply()
            textColorBtn.text = "Text Color: ${names[nextIdx]}"
            textColorBtn.setBackgroundColor(colors[nextIdx])
            textColorBtn.setTextColor(if (nextIdx == 0) Color.BLACK else Color.WHITE)
            updatePreview()
        }

        // Font size
        fontSizeSlider.setOnProgressChangeListener { progress ->
            val size = if (progress < 24) 24 else progress
            fontSizeLabel.text = "${size}px"
            prefs.edit().putInt("font_size", size).apply()
            updatePreview()
        }

        // Theme toggle
        darkToggle.setOnCheckedChangeListener { _, _ ->
            prefs.edit().putBoolean("dark_theme", darkToggle.isChecked).apply()
            applyTheme()
            updatePreview()
        }

        // Breathe toggle
        breatheToggle.setOnCheckedChangeListener { _, _ ->
            prefs.edit().putBoolean("breathing", breatheToggle.isChecked).apply()
        }

        // Font family
        fontRadio.setOnCheckedChangeListener { _, id ->
            val family = when (id) {
                R.id.font_serif -> 1
                R.id.font_mono -> 2
                else -> 0
            }
            prefs.edit().putInt("font_family", family).apply()
            updatePreview()
        }

        // Divider style
        dividerRadio.setOnCheckedChangeListener { _, id ->
            val style = when (id) {
                R.id.div_dashed -> 1
                R.id.div_gradient -> 2
                else -> 0
            }
            prefs.edit().putInt("divider_style", style).apply()
        }

        // Save button
        findViewById<Button>(R.id.btn_save).setOnClickListener { saveAndFinish() }
    }

    private fun saveAndFinish() {
        prefs.edit()
            .putString("birth_date", birthInput.text.toString())
            .putString("goal_date", goalInput.text.toString())
            .putString("goal_text", goalTextInput.text.toString())
            .apply()
        finish()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePreview() {
        val isDark = darkToggle.isChecked
        val textColor = prefs.getInt("text_color_int", Color.WHITE)
        val fontFamily = when (fontRadio.checkedRadioButtonId) {
            R.id.font_serif -> Typeface.SERIF
            R.id.font_mono -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }

        preview.apply {
            this.setTextColor(textColor)
            this.typeface = fontFamily
            this.text = if (isDark) "Dark Theme Preview" else "Light Theme Preview"
        }
    }

    private fun applyTheme() {
        val isDark = darkToggle.isChecked
        val bg = if (isDark) Color.parseColor("#111111") else Color.parseColor("#f5f5f5")
        val text = if (isDark) Color.parseColor("#e0e0e0") else Color.parseColor("#1a1a1a")

        window.setBackgroundDrawableResource(android.R.color.transparent)
        findViewById<ScrollView>(R.id.settings_root).setBackgroundColor(bg)

        val labels = listOf<TextView>(
            findViewById(R.id.label_birth), findViewById(R.id.label_goal),
            findViewById(R.id.label_goal_text), findViewById(R.id.label_font),
            findViewById(R.id.label_divider), findViewById(R.id.label_effects)
        )
        labels.forEach { it.setTextColor(text) }

        birthInput.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        goalInput.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        goalTextInput.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        fontSizeLabel.setTextColor(text)
        preview.setBackgroundColor(if (isDark) Color.parseColor("#1a1a1a") else Color.parseColor("#e0e0e0"))
    }

    private fun SeekBar.setOnProgressChangeListener(listener: (Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                listener(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun onBackPressed() {
        saveAndFinish()
        super.onBackPressed()
    }
}
