package com.example.capacitysync

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.capacitysync.databinding.ActivitySpacescreenBinding
import com.example.capacitysync.databinding.ItemSpaceCardBinding
import com.example.capacitysync.databinding.NewspacesmaplecardBinding
import com.example.capacitysync.databinding.InviteMembersCardBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class newspaces : AppCompatActivity() {

    private lateinit var binding: ActivitySpacescreenBinding

    private val sharedPrefs by lazy {
        getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Catch any system animations trying to play when the screen is pulled forward
        overridePendingTransition(0, 0)

        binding = ActivitySpacescreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    // Triggered automatically every time this screen is brought to the front
    override fun onResume() {
        super.onResume()
        loadSavedSpaces()
    }

    // Force Android to skip animations when leaving this screen
    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun setupClickListeners() {
        // 1. Bottom Nav: Go back to Home
        binding.navHomeIcon.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            // Pull existing Home screen to the front, don't build a new one
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            // Instant snap, no slide or blink
            overridePendingTransition(0, 0)
        }

        // 2. Bottom Nav: Spaces Tab (Do nothing, already here)
        binding.navSpaceIcon.setOnClickListener { }

        // 3. Bottom Nav: Plus Button (Create Space)
        binding.createSpaces.setOnClickListener {
            showCreateSpacePopup()
        }

        // 4. Invite Button (Make sure your Invite button ID on this screen matches this)
        // If you don't have an invite button on this screen yet, you can comment this out.
        binding.btnInvite.setOnClickListener {
            showInviteMembersPopup()
        }
    }

    // ==========================================
    // ✅ INVITE MEMBERS BOTTOM SHEET
    // ==========================================
    private fun showInviteMembersPopup() {
        val popupBinding = InviteMembersCardBinding.inflate(layoutInflater)

        popupBinding.etInviteEmail.doAfterTextChanged { text ->
            val input = text?.toString()?.trim() ?: ""

            if (input.isNotEmpty()) {
                // If using sky_blue, change R.color.primary_blue to R.color.sky_blue
                popupBinding.btnSubmitInvite.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            } else {
                popupBinding.btnSubmitInvite.setTextColor(Color.parseColor("#C7C7CC"))
            }
        }

        val dialog = BottomSheetDialog(
            this,
            com.google.android.material.R.style.Theme_Design_BottomSheetDialog
        )

        dialog.setContentView(popupBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            bottomSheet.setBackgroundResource(android.R.color.transparent)

            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }

            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams

            bottomSheet.post {
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    skipCollapsed = true
                    isFitToContents = false
                    expandedOffset = 0
                    state = BottomSheetBehavior.STATE_EXPANDED
                }

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            popupBinding.etInviteEmail.requestFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(popupBinding.etInviteEmail, InputMethodManager.SHOW_IMPLICIT)
                            behavior.removeBottomSheetCallback(this)
                        }
                    }

                    override fun onSlide(sheet: View, slideOffset: Float) {}
                })
            }
        }

        dialog.show()

        popupBinding.btnCloseInvite.setOnClickListener {
            dialog.dismiss()
        }

        popupBinding.btnSubmitInvite.setOnClickListener {
            val email = popupBinding.etInviteEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                Toast.makeText(this, "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // ✅ CREATE SPACE BOTTOM SHEET
    // ==========================================
    private fun showCreateSpacePopup() {
        val popupBinding = NewspacesmaplecardBinding.inflate(layoutInflater)

        popupBinding.etSpaceName.doAfterTextChanged { text ->
            val input = text?.toString()?.trim() ?: ""

            if (input.isNotEmpty()) {
                popupBinding.btnCreate.setTextColor(ContextCompat.getColor(this, R.color.sky_blue))
            } else {
                popupBinding.btnCreate.setTextColor(Color.parseColor("#C7C7CC"))
            }
        }

        val dialog = BottomSheetDialog(
            this,
            com.google.android.material.R.style.Theme_Design_BottomSheetDialog
        )

        dialog.setContentView(popupBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            bottomSheet.setBackgroundResource(android.R.color.transparent)

            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }

            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams

            bottomSheet.post {
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    skipCollapsed = true
                    isFitToContents = false
                    expandedOffset = 0
                    state = BottomSheetBehavior.STATE_EXPANDED
                }

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            popupBinding.etSpaceName.requestFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(popupBinding.etSpaceName, InputMethodManager.SHOW_IMPLICIT)
                            behavior.removeBottomSheetCallback(this)
                        }
                    }

                    override fun onSlide(sheet: View, slideOffset: Float) {}
                })
            }
        }

        dialog.show()

        popupBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        popupBinding.btnCreate.setOnClickListener {
            val newSpaceName = popupBinding.etSpaceName.text.toString().trim()

            if (newSpaceName.isEmpty()) {
                return@setOnClickListener
            }

            if (doesSpaceExist(newSpaceName)) {
                Toast.makeText(this, "Can't create: Space already exists!", Toast.LENGTH_SHORT).show()
            } else {
                saveSpaceToStorage(newSpaceName)
                addSpaceCardToView(newSpaceName)
                dialog.dismiss()
            }
        }
    }

    // ==========================================
    // ✅ UI & LIST LOGIC
    // ==========================================
    private fun loadSavedSpaces() {
        val savedSpaces = sharedPrefs.getStringSet("SAVED_SPACES", mutableSetOf()) ?: mutableSetOf()

        binding.llSpacesList.removeAllViews()

        val titleView = TextView(this).apply {
            text = "All Spaces"
            setTextColor(ContextCompat.getColor(this@newspaces, R.color.text_gray_secondary))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val marginHorizontal = (20 * resources.displayMetrics.density).toInt()
            val marginTop = (8 * resources.displayMetrics.density).toInt()
            val marginBottom = (12 * resources.displayMetrics.density).toInt()

            params.setMargins(marginHorizontal, marginTop, 0, marginBottom)
            layoutParams = params
        }

        binding.llSpacesList.addView(titleView)

        for (spaceName in savedSpaces) {
            addSpaceCardToView(spaceName)
        }
    }

    private fun addSpaceCardToView(spaceName: String) {
        val cardBinding = ItemSpaceCardBinding.inflate(layoutInflater, binding.llSpacesList, false)

        cardBinding.tvSpaceName.text = spaceName

        val initial = if (spaceName.isNotEmpty()) spaceName.take(1).uppercase() else "?"
        cardBinding.tvSpaceInitial.text = initial

        val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
        val randomColor = Color.parseColor(colors.random())
        cardBinding.flInitialContainer.background.setTint(randomColor)

        cardBinding.tvMemberCount.text = "mem: 1"

        binding.llSpacesList.addView(cardBinding.root)
    }

    // ==========================================
    // ✅ STORAGE HELPERS
    // ==========================================
    private fun getSavedSpacesList(): MutableSet<String> {
        return sharedPrefs.getStringSet("SAVED_SPACES", mutableSetOf()) ?: mutableSetOf()
    }

    private fun saveSpaceToStorage(name: String) {
        val currentSpaces = getSavedSpacesList()
        val updatedSpaces = mutableSetOf<String>().apply {
            addAll(currentSpaces)
            add(name)
        }
        sharedPrefs.edit().putStringSet("SAVED_SPACES", updatedSpaces).apply()
    }

    private fun doesSpaceExist(name: String): Boolean {
        val currentSpaces = getSavedSpacesList()
        return currentSpaces.any { it.equals(name, ignoreCase = true) }
    }
}