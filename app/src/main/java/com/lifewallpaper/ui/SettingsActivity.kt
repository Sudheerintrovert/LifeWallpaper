package com.lifewallpaper.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var birthInput: EditText
    private lateinit var textColorBtn: Button
    private lateinit var darkToggle: Switch
    private lateinit var fontSizeSlider: SeekBar
    private lateinit var fontSizeLabel: TextView
    private lateinit var fontRadio: RadioGroup
    private lateinit var breatheToggle: Switch
    private lateinit var dividerRadio: RadioGroup
    private lateinit var preview: TextView
    private lateinit var goalContainer: LinearLayout
    private lateinit var reachedContainer: LinearLayout
    private lateinit var reachedSection: LinearLayout
    private lateinit var reachedCountText: TextView

    data class Goal(val text: String, val goalDate: String, var reached: Boolean)

    private fun parseGoals(json: String?): MutableList<Goal> {
        if (json.isNullOrEmpty()) return mutableListOf()
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<Goal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Goal(
                    text = obj.optString("text", ""),
                    goalDate = obj.optString("goalDate", ""),
                    reached = obj.optBoolean("reached", false)
                ))
            }
            return list
        } catch (e: Exception) { return mutableListOf() }
    }

    private fun saveGoals(goals: List<Goal>) {
        val arr = JSONArray()
        for (g in goals) {
            val obj = JSONObject()
            obj.put("text", g.text)
            obj.put("goalDate", g.goalDate)
            obj.put("reached", g.reached)
            arr.put(obj)
        }
        prefs.edit().putString("goals_json", arr.toString()).apply()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("life_wallpaper", MODE_PRIVATE)

        birthInput = findViewById(R.id.input_birth)
        textColorBtn = findViewById(R.id.btn_text_color)
        darkToggle = findViewById(R.id.toggle_dark)
        fontSizeSlider = findViewById(R.id.slider_font_size)
        fontSizeLabel = findViewById(R.id.label_font_size)
        fontRadio = findViewById(R.id.radio_font)
        breatheToggle = findViewById(R.id.toggle_breathe)
        dividerRadio = findViewById(R.id.radio_divider)
        preview = findViewById(R.id.preview)
        goalContainer = findViewById(R.id.goals_container)
        reachedContainer = findViewById(R.id.reached_container)
        reachedSection = findViewById(R.id.reached_section)
        reachedCountText = findViewById(R.id.reached_count)

        birthInput.setText(prefs.getString("birth_date", ""))
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

        // Migrate old single-goal data
        migrateOldData()

        refreshGoalList()
        updatePreview()
        applyTheme()

        // Color picker
        textColorBtn.setOnClickListener {
            val color = prefs.getInt("text_color_int", Color.WHITE)
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
            refreshGoalList()
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

        // Add New Goal button
        findViewById<Button>(R.id.btn_add_goal).setOnClickListener {
            showAddGoalDialog()
        }

        // Save button
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            prefs.edit().putString("birth_date", birthInput.text.toString()).apply()
            finish()
        }
    }

    private fun migrateOldData() {
        val existing = prefs.getString("goals_json", "")
        if (existing.isNullOrEmpty()) {
            val oldText = prefs.getString("goal_text", "") ?: ""
            val oldDate = prefs.getString("goal_date", "") ?: ""
            if (oldText.isNotEmpty() && oldDate.isNotEmpty()) {
                val goals = mutableListOf(Goal(oldText, oldDate, false))
                saveGoals(goals)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshGoalList() {
        val goals = parseGoals(prefs.getString("goals_json", ""))
        val isDark = darkToggle.isChecked
        val textCol = if (isDark) Color.parseColor("#e0e0e0") else Color.parseColor("#1a1a1a")
        val subCol = if (isDark) Color.parseColor("#888888") else Color.parseColor("#666666")
        val greenCol = Color.parseColor("#4caf50")
        val redCol = Color.parseColor("#f44336")
        val blueCol = Color.parseColor("#2196f3")
        val bgCard = if (isDark) Color.parseColor("#1a1a1a") else Color.parseColor("#eeeeee")
        val bgReached = if (isDark) Color.parseColor("#1a2e1a") else Color.parseColor("#e8f5e9")

        goalContainer.removeAllViews()
        reachedContainer.removeAllViews()

        val activeGoals = goals.filter { !it.reached }
        val reachedGoals = goals.filter { it.reached }

        // Active goals
        if (activeGoals.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No active goals. Tap '+ Add New Goal' below!"
                textSize = 14f
                setTextColor(subCol)
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            goalContainer.addView(emptyText)
        } else {
            for ((index, goal) in activeGoals.withIndex()) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(bgCard)
                    setPadding(24, 20, 24, 20)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                }

                val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                val goalNum = TextView(this).apply {
                    text = "Goal ${index + 1}"
                    textSize = 11f
                    setTextColor(subCol)
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                topRow.addView(goalNum)

                val dateLabel = TextView(this).apply {
                    text = "Target: ${goal.goalDate}"
                    textSize = 11f
                    setTextColor(subCol)
                }
                topRow.addView(dateLabel)
                card.addView(topRow)

                val goalText = TextView(this).apply {
                    text = goal.text
                    textSize = 18f
                    setTextColor(textCol)
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, 8, 0, 16)
                }
                card.addView(goalText)

                val btnRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val reachedBtn = Button(this).apply {
                    text = "Goal Reached"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(greenCol)
                    setPadding(20, 8, 20, 8)
                    setOnClickListener {
                        goal.reached = true
                        val allGoals = parseGoals(prefs.getString("goals_json", ""))
                        val g = allGoals.find { it.text == goal.text && it.goalDate == goal.goalDate }
                        if (g != null) g.reached = true
                        saveGoals(allGoals)
                        refreshGoalList()
                    }
                }
                btnRow.addView(reachedBtn)

                val deleteBtn = Button(this).apply {
                    text = "Delete"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(redCol)
                    setPadding(20, 8, 20, 8)
                    setMargins(16, 0, 0, 0)
                    setOnClickListener {
                        AlertDialog.Builder(this@SettingsActivity)
                            .setTitle("Delete this goal?")
                            .setMessage("\"${goal.text}\" delete cheyyalsina?")
                            .setPositiveButton("Delete") { _, _ ->
                                val allGoals = parseGoals(prefs.getString("goals_json", ""))
                                val filtered = allGoals.filter { !(it.text == goal.text && it.goalDate == goal.goalDate) }
                                saveGoals(filtered)
                                refreshGoalList()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(32, 0, 0, 0)
                    layoutParams = lp
                }
                btnRow.addView(deleteBtn)
                card.addView(btnRow)

                goalContainer.addView(card)
            }
        }

        // Reached goals section
        if (reachedGoals.isEmpty()) {
            reachedSection.visibility = LinearLayout.GONE
        } else {
            reachedSection.visibility = LinearLayout.VISIBLE
            reachedCountText.text = "Reached Goals (${reachedGoals.size})"

            for ((index, goal) in reachedGoals.withIndex()) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(bgReached)
                    setPadding(20, 14, 20, 14)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 8) }
                }

                val check = TextView(this).apply {
                    text = "\u2713"
                    textSize = 20f
                    setTextColor(greenCol)
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, 0, 16, 0)
                }
                card.addView(check)

                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tView = TextView(this).apply {
                    text = goal.text
                    textSize = 15f
                    setTextColor(greenCol)
                    setTypeface(null, Typeface.BOLD)
                    paint.flags = paint.flags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                }
                info.addView(tView)

                val dView = TextView(this).apply {
                    text = goal.goalDate
                    textSize = 11f
                    setTextColor(subCol)
                }
                info.addView(dView)
                card.addView(info)

                val undoBtn = Button(this).apply {
                    text = "Undo"
                    textSize = 11f
                    setTextColor(blueCol)
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(12, 4, 12, 4)
                    setOnClickListener {
                        val allGoals = parseGoals(prefs.getString("goals_json", ""))
                        val g = allGoals.find { it.text == goal.text && it.goalDate == goal.goalDate }
                        if (g != null) g.reached = false
                        saveGoals(allGoals)
                        refreshGoalList()
                    }
                }
                card.addView(undoBtn)

                reachedContainer.addView(card)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAddGoalDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val goalTextHint = "e.g. Build My Dream Career"
        val goalDateHint = "2030-12-31"

        val goalTextLabel = TextView(this).apply {
            text = "What is your goal?"
            textSize = 14f
            setTextColor(if (darkToggle.isChecked) Color.parseColor("#e0e0e0") else Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 8)
        }
        dialogView.addView(goalTextLabel)

        val goalTextInput = EditText(this).apply {
            hint = goalTextHint
            textSize = 16f
            setTextColor(if (darkToggle.isChecked) Color.WHITE else Color.BLACK)
            setHintTextColor(Color.parseColor("#666666"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        dialogView.addView(goalTextInput)

        val dateLabel = TextView(this).apply {
            text = "Target Deadline (yyyy-MM-dd)"
            textSize = 14f
            setTextColor(if (darkToggle.isChecked) Color.parseColor("#e0e0e0") else Color.parseColor("#1a1a1a"))
            setPadding(0, 20, 0, 8)
        }
        dialogView.addView(dateLabel)

        val dateInput = EditText(this).apply {
            hint = goalDateHint
            textSize = 16f
            setTextColor(if (darkToggle.isChecked) Color.WHITE else Color.BLACK)
            setHintTextColor(Color.parseColor("#666666"))
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
        }
        dialogView.addView(dateInput)

        AlertDialog.Builder(this)
            .setTitle("Add New Goal")
            .setView(dialogView)
            .setPositiveButton("Add Goal") { _, _ ->
                val text = goalTextInput.text.toString().trim()
                val date = dateInput.text.toString().trim()
                if (text.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Enter both goal text and date!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Validate date format
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(date)
                } catch (e: Exception) {
                    Toast.makeText(this, "Date format wrong! Use yyyy-MM-dd", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val goals = parseGoals(prefs.getString("goals_json", ""))
                goals.add(Goal(text, date, false))
                saveGoals(goals)
                refreshGoalList()
                Toast.makeText(this, "Goal added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    @Suppress("DEPRECATION")
    private fun applyTheme() {
        val isDark = darkToggle.isChecked
        val bg = if (isDark) Color.parseColor("#111111") else Color.parseColor("#f5f5f5")
        val text = if (isDark) Color.parseColor("#e0e0e0") else Color.parseColor("#1a1a1a")

        window.setBackgroundDrawableResource(android.R.color.transparent)
        findViewById<ScrollView>(R.id.settings_root).setBackgroundColor(bg)

        val labels = listOf<TextView>(
            findViewById(R.id.label_birth),
            findViewById(R.id.label_goals),
            findViewById(R.id.label_font),
            findViewById(R.id.label_divider),
            findViewById(R.id.label_effects)
        )
        labels.forEach { it.setTextColor(text) }

        birthInput.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
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
        prefs.edit().putString("birth_date", birthInput.text.toString()).apply()
        super.onBackPressed()
    }
}