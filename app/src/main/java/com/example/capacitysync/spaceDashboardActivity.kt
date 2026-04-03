package com.example.capacitysync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.capacitysync.databinding.ActivitySpaceDashboardBinding
import com.example.capacitysync.databinding.CardWeeklyCapacityBinding
import com.example.capacitysync.databinding.ItemTimeIntervalCardsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    private val savedDateData = mutableMapOf<String, MutableList<TimeSlot>>()
    private var currentStartDate = Calendar.getInstance()
    private var currentSelectedDateKey = ""

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    private val dayLetterFormat = SimpleDateFormat("E", Locale.getDefault())

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

        binding.cardLogsHours.setOnClickListener {
            showLogsBottomSheet()
        }
    }

    private fun showLogsBottomSheet() {
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        val sheetBinding = CardWeeklyCapacityBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        currentStartDate = Calendar.getInstance()

        sheetBinding.ivCalendarIcon.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    currentStartDate.set(year, month, dayOfMonth)
                    populateHorizontalCalendar(sheetBinding)
                },
                currentStartDate.get(Calendar.YEAR),
                currentStartDate.get(Calendar.MONTH),
                currentStartDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        sheetBinding.btnAddInterval.setOnClickListener {
            if (savedDateData[currentSelectedDateKey] == null) {
                savedDateData[currentSelectedDateKey] = mutableListOf()
            }
            savedDateData[currentSelectedDateKey]?.add(TimeSlot())
            renderTimeSlotsForCurrentDay(sheetBinding)
        }

        sheetBinding.btnSaveHours.setOnClickListener {
            dialog.dismiss()
        }

        populateHorizontalCalendar(sheetBinding)
        dialog.show()
    }

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

            // ✅ FIX: Force the text to span the whole bubble and center perfectly
            val tvLetter = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                text = dayLetter
                setTextColor(Color.parseColor("#8E8E93"))
                textSize = 12f
            }

            // ✅ FIX: Force the text to span the whole bubble and center perfectly
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

            dayBubble.setOnClickListener {
                selectDay(sheetBinding, dayBubble, bubbleDateKey, calendarIterator.clone() as Calendar)
            }

            sheetBinding.llDateContainer.addView(dayBubble)

            if (bubbleDateKey == targetKey) {
                clickedViewForToday = dayBubble
            }

            calendarIterator.add(Calendar.DAY_OF_MONTH, 1)
        }

        clickedViewForToday?.performClick()
        sheetBinding.hsvDateSelector.post {
            sheetBinding.hsvDateSelector.scrollTo(0, 0)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun selectDay(
        sheetBinding: CardWeeklyCapacityBinding,
        clickedBubble: LinearLayout,
        dateKey: String,
        actualDate: Calendar
    ) {
        currentSelectedDateKey = dateKey
        sheetBinding.tvSelectedDayLabel.text = displayDateFormat.format(actualDate.time)

        for (i in 0 until sheetBinding.llDateContainer.childCount) {
            val bubble = sheetBinding.llDateContainer.getChildAt(i) as? LinearLayout ?: continue
            bubble.setBackgroundResource(R.drawable.bg_day_unselected)
            // ✅ FIX: Immediately re-apply padding after setting background
            bubble.setPadding(0, dpToPx(8), 0, dpToPx(8))
            (bubble.getChildAt(0) as TextView).setTextColor(Color.parseColor("#8E8E93"))
            (bubble.getChildAt(1) as TextView).setTextColor(Color.parseColor("#000000"))
        }

        clickedBubble.setBackgroundResource(R.drawable.bg_day_selected)
        // ✅ FIX: Immediately re-apply padding after setting background
        clickedBubble.setPadding(0, dpToPx(8), 0, dpToPx(8))
        (clickedBubble.getChildAt(0) as TextView).setTextColor(Color.parseColor("#E5D1FF"))
        (clickedBubble.getChildAt(1) as TextView).setTextColor(Color.parseColor("#FFFFFF"))

        if (!savedDateData.containsKey(dateKey)) {
            savedDateData[dateKey] = mutableListOf()
        }

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

        recalculateTotalHours(sheetBinding)
    }

    private fun recalculateTotalHours(sheetBinding: CardWeeklyCapacityBinding) {
        var totalMinutes = 0

        for (dayList in savedDateData.values) {
            for (slot in dayList) {
                totalMinutes += slot.getDurationMinutes()
            }
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        val formattedString = if (mins == 0) {
            "${hours}h Total"
        } else {
            "${hours}h ${mins}m Total"
        }

        sheetBinding.tvWeeklyTotal.text = formattedString
    }

    private fun formatTimeForUI(hour24: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour24)
        calendar.set(Calendar.MINUTE, minute)
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
    }
}