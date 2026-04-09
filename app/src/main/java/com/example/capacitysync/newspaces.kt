package com.example.capacitysync

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.capacitysync.databinding.ActivitySpacescreenBinding
import com.example.capacitysync.databinding.ItemSpaceCardBinding
import com.example.capacitysync.databinding.ItemWorkspaceRowBinding
import com.example.capacitysync.databinding.NewspacesmaplecardBinding
import com.example.capacitysync.databinding.InviteMembersCardBinding
import com.example.capacitysync.databinding.WorkspaceSwitcherCardBinding
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
        updateTopBarUI() // ✅ Ensures the top bar updates when returning to this screen
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

        // ✅ 4. Top Bar Dropdown Switcher
        val showSwitcher = View.OnClickListener { showWorkspaceSwitcherPopup() }
        binding.ivWorkspaceLogo.setOnClickListener(showSwitcher)
        binding.tvWorkspaceName.setOnClickListener(showSwitcher)
        binding.ivDropdownArrow.setOnClickListener(showSwitcher)
    }

    // ==========================================
    // ✅ HEADER LOGIC (THE FIX FOR DUMMY DATA)
    // ==========================================
    private fun updateTopBarUI() {
        val savedSpaces = getSavedSpacesList()
        val spaceName = sharedPrefs.getString("ACTIVE_WORKSPACE", savedSpaces.firstOrNull() ?: "C4S Workspace") ?: "C4S Workspace"
        binding.tvWorkspaceName.text = spaceName

        val initial = if (spaceName.isNotBlank()) spaceName.take(1).uppercase() else "W"

        // Dynamic Logo Generation matching the active space
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

        popupBinding.btnCreateWorkspace.setOnClickListener {
            dialog.dismiss()
            showCreateSpacePopup()
        }

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
                dialog.dismiss()
            }
            popupBinding.llWorkspacesList.addView(rowBinding.root)
        }
        dialog.show()
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

                    // Optional: Make the newly created space the active one immediately
                    sharedPrefs.edit().putString("ACTIVE_WORKSPACE", newSpaceName).apply()
                    updateTopBarUI()

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

        // ✅ FIXED: Branded Avatar Colors (No longer random, tied to the space name)
        val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
        val colorIndex = Math.abs(spaceName.hashCode()) % colors.size
        cardBinding.flInitialContainer.background.setTint(Color.parseColor(colors[colorIndex]))

        cardBinding.tvMemberCount.text = "mem: 1"

        // Navigation to the Dashboard
        cardBinding.root.setOnClickListener {
            val intent = Intent(this, spaceDashboardActivity::class.java)
            // Pass the workspace name so the dashboard knows which one it is
            intent.putExtra("SPACE_NAME", spaceName)
            startActivity(intent)

            // Snap transition
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