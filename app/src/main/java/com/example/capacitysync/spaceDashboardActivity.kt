package com.example.capacitysync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.capacitysync.databinding.ActivitySpaceDashboardBinding
import com.example.capacitysync.databinding.CardWeeklyCapacityBinding
import com.example.capacitysync.databinding.ItemTimeIntervalCardsBinding
import com.example.capacitysync.databinding.ItemWorkspaceRowBinding
import com.example.capacitysync.databinding.WorkspaceSwitcherCardBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Data model for storing multiple separate weekly logs
data class WeeklyLog(
    val id: String,
    val dateData: MutableMap<String, MutableList<TimeSlot>>
)

data class TimeSlot(
    var startHour: Int = 9,
    var startMinute: Int = 0,
    var endHour: Int = 9,
    var endMinute: Int = 0
) {
    fun getDurationMinutes(): Int {
        val startMins = startHour * 60 + startMinute
        var endMins = endHour * 60 + endMinute
        if (endMins < startMins) {
            endMins += 24 * 60
        }
        return endMins - startMins
    }
}

class spaceDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpaceDashboardBinding

    private val sharedPrefs by lazy {
        getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    }

    private val savedDateData = mutableMapOf<String, MutableList<TimeSlot>>()
    private var currentStartDate = Calendar.getInstance()
    private var currentSelectedDateKey = ""
    private var currentEditingLogId: String? = null

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    private val dayLetterFormat = SimpleDateFormat("E", Locale.getDefault())

    private var isLogsListExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySpaceDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔥 Catch the incoming Space Name
        val passedSpaceName = intent.getStringExtra("SPACE_NAME")
        if (!passedSpaceName.isNullOrEmpty()) {
            sharedPrefs.edit().putString("ACTIVE_WORKSPACE", passedSpaceName).apply()
        }

        updateTopBarUI()

        // ✅ FIXED CLICK LISTENERS
        // This opens the new Log Time Bottom Sheet (Make sure the ID matches your XML)
        binding.cardCompleted.setOnClickListener {
            showLogTimeBottomSheet()
        }

        // This opens the existing Weekly Capacity / Logs Hours editor
        binding.cardLogsHours.setOnClickListener {
            showLogsBottomSheet(null)
        }

        binding.layoutLogsList.setOnClickListener {
            toggleLogsList()
        }

        binding.ivDropdownArrow.setOnClickListener { showWorkspaceSwitcherPopup() }
        binding.tvWorkspaceName.setOnClickListener { showWorkspaceSwitcherPopup() }

        refreshDashboardSavedCards()
    }

    // ==========================================
    // ✅ LOG TIME BOTTOM SHEET (NEW)
    // ==========================================

    private fun showLogTimeBottomSheet() {
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)

        // Ensure this matches the name of your flat card XML file
        val logTimeBinding = com.example.capacitysync.databinding.CardLogTimeBinding.inflate(layoutInflater)
        dialog.setContentView(logTimeBinding.root)

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        logTimeBinding.btnSaveTimeLog.setOnClickListener {
            // Logic to save the logged time goes here
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==========================================
    // ✅ DATA ISOLATION (Workspace Specific)
    // ==========================================

    private fun getWorkspaceKey(): String {
        val spaceName = sharedPrefs.getString("ACTIVE_WORKSPACE", "C4S Workspace") ?: "C4S Workspace"
        return "LOGS_DATA_$spaceName"
    }

    private fun loadLogsForCurrentWorkspace(): List<WeeklyLog> {
        val json = sharedPrefs.getString(getWorkspaceKey(), null) ?: return emptyList()
        val type = object : TypeToken<List<WeeklyLog>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun saveLogsForCurrentWorkspace(logs: List<WeeklyLog>) {
        val json = Gson().toJson(logs)
        sharedPrefs.edit().putString(getWorkspaceKey(), json).apply()
    }

    // ==========================================
    // ✅ HEADER & WORKSPACE UI
    // ==========================================

    private fun updateTopBarUI() {
        val savedSpaces = getSavedSpacesList()
        val spaceName = sharedPrefs.getString("ACTIVE_WORKSPACE", savedSpaces.firstOrNull() ?: "C4S Workspace") ?: "C4S Workspace"
        binding.tvWorkspaceName.text = spaceName

        val initial = if (spaceName.isNotBlank()) spaceName.take(1).uppercase() else "W"

        val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
        val colorIndex = Math.abs(spaceName.hashCode()) % colors.size
        val bgColor = Color.parseColor(colors[colorIndex])

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = bgColor; isAntiAlias = true }
        canvas.drawCircle(50f, 50f, 50f, bgPaint)
        val textPaint = Paint().apply { color = Color.WHITE; textSize = 45f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val yPos = (canvas.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, 50f, yPos, textPaint)
        binding.ivWorkspaceLogo.setImageBitmap(bitmap)
    }

    private fun showWorkspaceSwitcherPopup() {
        val popupBinding = WorkspaceSwitcherCardBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(popupBinding.root)

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        popupBinding.btnCloseWorkspaces.setOnClickListener { dialog.dismiss() }

        val savedSpaces = getSavedSpacesList()
        val activeWorkspace = sharedPrefs.getString("ACTIVE_WORKSPACE", savedSpaces.firstOrNull() ?: "")

        popupBinding.llWorkspacesList.removeAllViews()

        for (spaceName in savedSpaces) {
            val rowBinding = ItemWorkspaceRowBinding.inflate(layoutInflater, popupBinding.llWorkspacesList, false)
            rowBinding.tvWorkspaceName.text = spaceName
            rowBinding.tvWorkspaceInitial.text = if (spaceName.isNotEmpty()) spaceName.take(1).uppercase() else "?"

            val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
            val colorIndex = Math.abs(spaceName.hashCode()) % colors.size
            rowBinding.cardWorkspaceAvatar.setCardBackgroundColor(Color.parseColor(colors[colorIndex]))

            if (spaceName == activeWorkspace) {
                rowBinding.ivWorkspaceCheckmark.visibility = View.VISIBLE
                rowBinding.tvWorkspaceName.setTextColor(ContextCompat.getColor(this, R.color.workspace_primary_purple))
            } else {
                rowBinding.ivWorkspaceCheckmark.visibility = View.INVISIBLE
                rowBinding.tvWorkspaceName.setTextColor(ContextCompat.getColor(this, R.color.black))
            }

            rowBinding.root.setOnClickListener {
                sharedPrefs.edit().putString("ACTIVE_WORKSPACE", spaceName).apply()
                updateTopBarUI()
                refreshDashboardSavedCards()
                dialog.dismiss()
            }
            popupBinding.llWorkspacesList.addView(rowBinding.root)
        }
        dialog.show()
    }

    // ==========================================
    // ✅ DROPDOWN & SUMMARY LIST
    // ==========================================

    private fun toggleLogsList() {
        isLogsListExpanded = !isLogsListExpanded
        if (isLogsListExpanded) {
            binding.llSavedLogsContainer.visibility = View.VISIBLE
            binding.ivChevronLogs.animate().rotation(90f).setDuration(200).start()
        } else {
            binding.llSavedLogsContainer.visibility = View.GONE
            binding.ivChevronLogs.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun refreshDashboardSavedCards() {
        binding.llSavedLogsContainer.removeAllViews()
        val logs = loadLogsForCurrentWorkspace()

        binding.tvLogStatus.text = logs.size.toString()

        for (log in logs) {
            var totalMinutes = 0
            for (dayList in log.dateData.values) {
                for (slot in dayList) totalMinutes += slot.getDurationMinutes()
            }
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60

            val dates = log.dateData.keys.sorted()
            val dateRangeText = if (dates.isNotEmpty()) {
                "${formatDateForDisplay(dates.first())} - ${formatDateForDisplay(dates.last())}"
            } else { "New Log Entry" }

            val cardView = layoutInflater.inflate(R.layout.card_weekly_summary, binding.llSavedLogsContainer, false)

            val timeDisplay = if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
            cardView.findViewById<TextView>(R.id.tvRingTotal).text = timeDisplay
            cardView.findViewById<TextView>(R.id.tvDateRange).text = dateRangeText
            cardView.findViewById<TextView>(R.id.tvHealthStatus).text = "$timeDisplay available"

            cardView.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.progressWeekly).progress = 100

            val currentSpace = sharedPrefs.getString("ACTIVE_WORKSPACE", "C4S Workspace")
            cardView.findViewById<TextView>(R.id.tvWorkspaceContext).text = "My Capacity • $currentSpace"

            cardView.findViewById<TextView>(R.id.tvEditCapacity).setOnClickListener {
                showLogsBottomSheet(log.id)
            }

            val days = arrayOf(
                cardView.findViewById<TextView>(R.id.dayMon),
                cardView.findViewById<TextView>(R.id.dayTue),
                cardView.findViewById<TextView>(R.id.dayWed),
                cardView.findViewById<TextView>(R.id.dayThu),
                cardView.findViewById<TextView>(R.id.dayFri),
                cardView.findViewById<TextView>(R.id.daySat),
                cardView.findViewById<TextView>(R.id.daySun)
            )

            val calendar = Calendar.getInstance()
            var todayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2
            if (todayIndex < 0) todayIndex = 6

            for (i in days.indices) {
                val textView = days[i] ?: continue
                if (i == todayIndex) {
                    val background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(this@spaceDashboardActivity, R.color.workspace_primary_purple))
                    }
                    textView.background = background
                    textView.setTextColor(ContextCompat.getColor(this@spaceDashboardActivity, R.color.white))
                } else {
                    textView.background = null
                    textView.setTextColor(ContextCompat.getColor(this@spaceDashboardActivity, R.color.text_gray_inactive))
                }
            }

            binding.llSavedLogsContainer.addView(cardView)
        }
    }

    // ==========================================
    // ✅ LOG BOOK BOTTOM SHEET
    // ==========================================

    private fun showLogsBottomSheet(logId: String?) {
        currentEditingLogId = logId
        savedDateData.clear()

        if (logId != null) {
            val logs = loadLogsForCurrentWorkspace()
            logs.find { it.id == logId }?.let { savedDateData.putAll(it.dateData) }
        }

        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        val sheetBinding = CardWeeklyCapacityBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.btnSaveHours.setOnClickListener {
            val currentLogs = loadLogsForCurrentWorkspace().toMutableList()

            if (currentEditingLogId == null) {
                val newId = System.currentTimeMillis().toString()
                currentLogs.add(WeeklyLog(newId, savedDateData.toMutableMap()))
            } else {
                val index = currentLogs.indexOfFirst { it.id == currentEditingLogId }
                if (index != -1) currentLogs[index] = WeeklyLog(currentEditingLogId!!, savedDateData.toMutableMap())
            }

            saveLogsForCurrentWorkspace(currentLogs)
            refreshDashboardSavedCards()
            if (!isLogsListExpanded) toggleLogsList()
            dialog.dismiss()
        }

        sheetBinding.btnAddInterval.setOnClickListener {
            if (savedDateData[currentSelectedDateKey] == null) savedDateData[currentSelectedDateKey] = mutableListOf()
            savedDateData[currentSelectedDateKey]?.add(TimeSlot())
            renderTimeSlotsForCurrentDay(sheetBinding)
        }

        sheetBinding.ivCalendarIcon.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                currentStartDate.set(y, m, d)
                populateHorizontalCalendar(sheetBinding)
            },
                currentStartDate.get(Calendar.YEAR), currentStartDate.get(Calendar.MONTH), currentStartDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        populateHorizontalCalendar(sheetBinding)
        dialog.show()
    }

    // ==========================================
    // ✅ HELPER UI FUNCTIONS
    // ==========================================

    private fun populateHorizontalCalendar(sheetBinding: CardWeeklyCapacityBinding) {
        sheetBinding.llDateContainer.removeAllViews()
        val calendarIterator = currentStartDate.clone() as Calendar
        var clickedViewForToday: LinearLayout? = null
        val targetKey = dbDateFormat.format(currentStartDate.time)

        for (i in 0 until 30) {
            val bubbleDateKey = dbDateFormat.format(calendarIterator.time)
            val dayLetter = dayLetterFormat.format(calendarIterator.time).take(1)
            val dayNumber = dayNumberFormat.format(calendarIterator.time)

            val dayBubble = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_day_unselected)
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(dpToPx(58), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dpToPx(8)
                }
            }

            val tvLetter = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                text = dayLetter
                setTextColor(Color.parseColor("#8E8E93"))
                textSize = 12f
            }

            val tvNumber = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                text = dayNumber
                setTextColor(Color.parseColor("#000000"))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(4), 0, 0)
            }

            dayBubble.addView(tvLetter)
            dayBubble.addView(tvNumber)
            dayBubble.setOnClickListener { selectDay(sheetBinding, dayBubble, bubbleDateKey, calendarIterator.clone() as Calendar) }

            sheetBinding.llDateContainer.addView(dayBubble)
            if (bubbleDateKey == targetKey) clickedViewForToday = dayBubble
            calendarIterator.add(Calendar.DAY_OF_MONTH, 1)
        }
        clickedViewForToday?.performClick()
        sheetBinding.hsvDateSelector.post { sheetBinding.hsvDateSelector.scrollTo(0, 0) }
    }

    private fun selectDay(sheetBinding: CardWeeklyCapacityBinding, clickedBubble: LinearLayout, dateKey: String, actualDate: Calendar) {
        currentSelectedDateKey = dateKey
        sheetBinding.tvSelectedDayLabel.text = displayDateFormat.format(actualDate.time)
        for (i in 0 until sheetBinding.llDateContainer.childCount) {
            val bubble = sheetBinding.llDateContainer.getChildAt(i) as? LinearLayout ?: continue
            bubble.setBackgroundResource(R.drawable.bg_day_unselected)
            bubble.setPadding(0, dpToPx(8), 0, dpToPx(8))
            (bubble.getChildAt(0) as TextView).setTextColor(Color.parseColor("#8E8E93"))
            (bubble.getChildAt(1) as TextView).setTextColor(Color.parseColor("#000000"))
        }
        clickedBubble.setBackgroundResource(R.drawable.bg_day_selected)
        clickedBubble.setPadding(0, dpToPx(8), 0, dpToPx(8))
        (clickedBubble.getChildAt(0) as TextView).setTextColor(Color.parseColor("#E5D1FF"))
        (clickedBubble.getChildAt(1) as TextView).setTextColor(Color.parseColor("#FFFFFF"))
        if (!savedDateData.containsKey(dateKey)) savedDateData[dateKey] = mutableListOf()
        renderTimeSlotsForCurrentDay(sheetBinding)
    }

    private fun renderTimeSlotsForCurrentDay(sheetBinding: CardWeeklyCapacityBinding) {
        sheetBinding.llTimeSlotsList.removeAllViews()
        val dailySlots = savedDateData[currentSelectedDateKey] ?: return

        for ((index, slot) in dailySlots.withIndex()) {
            val itemBinding = ItemTimeIntervalCardsBinding.inflate(layoutInflater, sheetBinding.llTimeSlotsList, false)
            itemBinding.tvStartTime.text = formatTimeForUI(slot.startHour, slot.startMinute)
            itemBinding.tvEndTime.text = formatTimeForUI(slot.endHour, slot.endMinute)

            itemBinding.tvStartTime.setOnClickListener {
                TimePickerDialog(this, { _, h, m ->
                    slot.startHour = h
                    slot.startMinute = m
                    renderTimeSlotsForCurrentDay(sheetBinding)
                }, slot.startHour, slot.startMinute, false).show()
            }

            itemBinding.tvEndTime.setOnClickListener {
                TimePickerDialog(this, { _, h, m ->
                    slot.endHour = h
                    slot.endMinute = m
                    renderTimeSlotsForCurrentDay(sheetBinding)
                }, slot.endHour, slot.endMinute, false).show()
            }

            itemBinding.btnDeleteInterval.setOnClickListener {
                dailySlots.removeAt(index)
                renderTimeSlotsForCurrentDay(sheetBinding)
            }
            sheetBinding.llTimeSlotsList.addView(itemBinding.root)
        }

        var totalMinutes = 0
        for (dayList in savedDateData.values) {
            for (slot in dayList) totalMinutes += slot.getDurationMinutes()
        }
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        sheetBinding.tvWeeklyTotal.text = if (mins == 0) "${hours}h Total" else "${hours}h ${mins}m Total"
    }

    private fun getSavedSpacesList(): MutableSet<String> {
        return sharedPrefs.getStringSet("SAVED_SPACES", mutableSetOf()) ?: mutableSetOf()
    }

    private fun formatDateForDisplay(dbDate: String) = SimpleDateFormat("MMM d", Locale.getDefault()).format(dbDateFormat.parse(dbDate)!!)
    private fun formatTimeForUI(hour24: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour24); set(Calendar.MINUTE, minute) }
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
    }
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}