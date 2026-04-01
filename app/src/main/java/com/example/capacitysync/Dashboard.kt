package com.example.capacitysync

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.capacitysync.databinding.ActivityDashboardBinding
import com.example.capacitysync.databinding.NewspacesmaplecardBinding
import com.example.capacitysync.databinding.InviteMembersCardBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val sharedPrefs by lazy {
        getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Catch any system animations trying to play when the screen is pulled forward
        overridePendingTransition(0, 0)

        binding = ActivityDashboardBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        // Handle system bars (status/nav)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadSavedSpaces()

        // --- BUTTON CLICK LISTENERS ---

        // 1. Create Space Buttons
        binding.createNewSpace.setOnClickListener { showCreateSpacePopup() }
        binding.createSpaces.setOnClickListener { showCreateSpacePopup() }

        // 2. Invite Button (Make sure you have a button with this ID in your Dashboard XML!)
        // If your invite button has a different ID, change "btnInvite" to match it.
        binding.btnInvite.setOnClickListener {
            showInviteMembersPopup()
        }

        // 3. Bottom Nav: Go to Spaces
        binding.navSpaceIcon.setOnClickListener {
            val intent = Intent(this, newspaces::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    // Force Android to skip animations when leaving this screen
    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }


    private fun showInviteMembersPopup() {
        val popupBinding = InviteMembersCardBinding.inflate(layoutInflater)

        // Change "Invite" text color when user types an email
        popupBinding.etInviteEmail.doAfterTextChanged { text ->
            val input = text?.toString()?.trim() ?: ""

            if (input.isNotEmpty()) {
                // If using sky_blue, change R.color.primary_blue to R.color.sky_blue
                popupBinding.btnSubmitInvite.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue))
            } else {
                popupBinding.btnSubmitInvite.setTextColor(android.graphics.Color.parseColor("#C7C7CC")) // Disabled Grey
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

            // 1. Transparent background
            bottomSheet.setBackgroundResource(android.R.color.transparent)

            // 2. Handle System Navigation Bars (Copied from Create Space)
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }

            // 3. Force Full Screen / MATCH_PARENT (Copied from Create Space)
            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams

            // 4. Set Behavior to expand fully and skip the half-collapsed state
            bottomSheet.post {
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    skipCollapsed = true
                    isFitToContents = false
                    expandedOffset = 0
                    state = BottomSheetBehavior.STATE_EXPANDED
                }

                // 5. Auto-focus keyboard smoothly once it reaches the top
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

        // Close Button (The X icon)
        popupBinding.btnCloseInvite.setOnClickListener {
            dialog.dismiss()
        }

        // Submit Invite Button
        popupBinding.btnSubmitInvite.setOnClickListener {
            val email = popupBinding.etInviteEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                // For now, just show a success toast and dismiss!
                Toast.makeText(this, "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // ✅ CREATE SPACE BOTTOM SHEET LOGIC
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
                addCardToDashboard(newSpaceName)
                dialog.dismiss()
            }
        }
    }

    // ==========================================
    // ✅ STORAGE & DATA LOGIC
    // ==========================================
    private fun addCardToDashboard(spaceName: String) {
        // Your logic for adding a mini card to the dashboard later
    }

    private fun loadSavedSpaces() {
        val savedSpaces = getSavedSpacesList()
        for (space in savedSpaces) {
            addCardToDashboard(space)
        }
    }

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