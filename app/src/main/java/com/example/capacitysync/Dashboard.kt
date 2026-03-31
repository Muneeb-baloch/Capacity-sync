package com.example.capacitysync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.capacitysync.databinding.ActivityDashboardBinding
import com.example.capacitysync.databinding.NewspacesmaplecardBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val sharedPrefs by lazy {
        getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        binding.createNewSpace.setOnClickListener { showCreateSpacePopup() }
        binding.createSpaces.setOnClickListener { showCreateSpacePopup() }
        binding.navSpaceIcon.setOnClickListener {
            val intent = Intent(this, newspaces::class.java)
            startActivity(intent)
        }
    }



    // ✅ MASTER BOTTOM SHEET (Smooth Animation, Full Screen, Auto-Keyboard, Dynamic Color)
    private fun showCreateSpacePopup() {
        val popupBinding = NewspacesmaplecardBinding.inflate(layoutInflater)

        // 🔥 NEW: Real-time text listener for the Create button color!
        popupBinding.etSpaceName.doAfterTextChanged { text ->
            val input = text?.toString()?.trim() ?: ""

            if (input.isNotEmpty()) {
                // When there is text, change it to your new sky_blue color
                popupBinding.btnCreate.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.sky_blue))
            } else {
                // When empty, change it back to the dim grey
                popupBinding.btnCreate.setTextColor(android.graphics.Color.parseColor("#C7C7CC"))
            }
        }

        val dialog = BottomSheetDialog(
            this,
            com.google.android.material.R.style.Theme_Design_BottomSheetDialog
        )

        dialog.setContentView(popupBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<android.view.View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            // 1. Transparent background (for rounded corners)
            bottomSheet.setBackgroundResource(android.R.color.transparent)

            // 2. Fix edge-to-edge padding so it doesn't overlap Android navigation buttons
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }

            // 3. FIX THE FLOATING: Force the hidden container to stretch to the bottom!
            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams

            // 4. JITTER FIX: Apply behavior AFTER layout is ready
            bottomSheet.post {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    skipCollapsed = true
                    isFitToContents = false
                    expandedOffset = 0
                    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                }

                // 5. KEYBOARD FIX: Wait for the slide animation to finish, THEN open the keyboard safely!
                behavior.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(sheet: android.view.View, newState: Int) {
                        if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {

                            // Focus the text box
                            popupBinding.etSpaceName.requestFocus()

                            // Pop the keyboard
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(popupBinding.etSpaceName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

                            // Remove this listener so it doesn't fire again if the user swipes
                            behavior.removeBottomSheetCallback(this)
                        }
                    }

                    override fun onSlide(sheet: android.view.View, slideOffset: Float) {}
                })
            }
        }

        dialog.show()

        // Cancel Button
        popupBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Create Button
        popupBinding.btnCreate.setOnClickListener {
            val newSpaceName = popupBinding.etSpaceName.text.toString().trim()

            if (newSpaceName.isEmpty()) {
                // We don't need a Toast here anymore since the button looks disabled visually,
                // but you can leave it as a fallback!
                return@setOnClickListener
            }

            if (doesSpaceExist(newSpaceName)) {
                android.widget.Toast.makeText(this, "Can't create: Space already exists!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                saveSpaceToStorage(newSpaceName)
                addCardToDashboard(newSpaceName)
                dialog.dismiss()
            }
        }
    }

    // --- ADD CARD TO LIST ---
    private fun addCardToDashboard(spaceName: String) {
        /*
        val cardBinding = YourListCardBinding.inflate(layoutInflater, binding.llSpacesList, false)
        cardBinding.tvSpaceName.text = spaceName
        binding.llSpacesList.addView(cardBinding.root, 0)
        */
    }

    // --- STORAGE LOGIC ---
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