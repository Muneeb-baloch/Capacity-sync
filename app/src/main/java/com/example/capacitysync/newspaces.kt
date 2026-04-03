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
        // Skip entrance animations for a snappy feel
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

    override fun onResume() {
        super.onResume()
        loadSavedSpaces()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun setupClickListeners() {
        // 1. Bottom Nav: Home
        binding.navHomeIcon.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 2. Plus Button: Create Space
        binding.createSpaces.setOnClickListener {
            showCreateSpacePopup()
        }

        // 3. Invite Button
        binding.btnInvite.setOnClickListener {
            showInviteMembersPopup()
        }
    }

    // ==========================================
    // ✅ INVITE MEMBERS BOTTOM SHEET
    // ==========================================
    private fun showInviteMembersPopup() {
        val popupBinding = InviteMembersCardBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(popupBinding.root)

        popupBinding.btnSubmitInvite.setOnClickListener {
            val email = popupBinding.etInviteEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                Toast.makeText(this, "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // ==========================================
    // ✅ CREATE SPACE BOTTOM SHEET
    // ==========================================
    private fun showCreateSpacePopup() {
        val popupBinding = NewspacesmaplecardBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(popupBinding.root)

        popupBinding.btnCreate.setOnClickListener {
            val newSpaceName = popupBinding.etSpaceName.text.toString().trim()
            if (newSpaceName.isNotEmpty()) {
                if (doesSpaceExist(newSpaceName)) {
                    Toast.makeText(this, "Space already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    saveSpaceToStorage(newSpaceName)
                    addSpaceCardToView(newSpaceName)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    // ==========================================
    // ✅ UI & LIST LOGIC (THE NAVIGATION PART)
    // ==========================================
    private fun loadSavedSpaces() {
        val savedSpaces = getSavedSpacesList()
        binding.llSpacesList.removeAllViews()

        // List Header
        val titleView = TextView(this).apply {
            text = "All Spaces"
            setTextColor(ContextCompat.getColor(this@newspaces, R.color.text_gray_secondary))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins((20 * resources.displayMetrics.density).toInt(), 16, 0, 24)
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

        // Branded Avatar Colors
        val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
        cardBinding.flInitialContainer.background.setTint(Color.parseColor(colors.random()))
        cardBinding.tvMemberCount.text = "mem: 1"

        // 🔥 CRITICAL ADDITION: Navigation to the Dashboard
        cardBinding.root.setOnClickListener {
            val intent = Intent(this, spaceDashboardActivity::class.java)
            // Pass the workspace name so the dashboard knows which one it is
            intent.putExtra("SPACE_NAME", spaceName)
            startActivity(intent)

            // Snape transition
            overridePendingTransition(0, 0)
        }

        binding.llSpacesList.addView(cardBinding.root)
    }

    // ==========================================
    // ✅ STORAGE HELPERS
    // ==========================================
    private fun getSavedSpacesList(): MutableSet<String> {
        return sharedPrefs.getStringSet("SAVED_SPACES", mutableSetOf()) ?: mutableSetOf()
    }

    private fun saveSpaceToStorage(name: String) {
        val updatedSpaces = getSavedSpacesList().toMutableSet().apply { add(name) }
        sharedPrefs.edit().putStringSet("SAVED_SPACES", updatedSpaces).apply()
    }

    private fun doesSpaceExist(name: String): Boolean {
        return getSavedSpacesList().any { it.equals(name, ignoreCase = true) }
    }
}