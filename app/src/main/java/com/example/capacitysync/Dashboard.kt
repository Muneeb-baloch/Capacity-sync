package com.example.capacitysync

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.example.capacitysync.databinding.WorkspaceSwitcherCardBinding
import com.example.capacitysync.databinding.ItemWorkspaceRowBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseSyncManager: FirebaseSyncManager

    private val sharedPrefs by lazy {
        getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        removeActivityTransition() // Fixes the yellow deprecation warning

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Sync Manager
        firebaseSyncManager = FirebaseSyncManager(this)
        initializeFirebaseAuth()

        loadSavedSpaces()
        updateTopBarUI()
        setupClickListeners()
    }

    override fun onPause() {
        super.onPause()
        removeActivityTransition() // Fixes the yellow deprecation warning
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseSyncManager.cleanup()
    }

    // ==========================================
    // ✅ FIREBASE AUTHENTICATION & SYNC
    // ==========================================
    
    private fun initializeFirebaseAuth() {
        val auth = FirebaseAuth.getInstance()
        
        Log.d("Firebase", "Initializing Firebase Auth...")
        Log.d("Firebase", "Current user: ${auth.currentUser?.uid ?: "null"}")
        
        // Try anonymous authentication, but don't block the app if it fails
        if (auth.currentUser == null) {
            Log.d("Firebase", "No user found, attempting anonymous sign-in...")
            
            // Set a timeout - if auth doesn't work in 5 seconds, continue without it
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var authCompleted = false
            
            handler.postDelayed({
                if (!authCompleted) {
                    Log.w("Firebase", "Auth timeout - continuing in offline mode")
                    Toast.makeText(this, "Working in offline mode", Toast.LENGTH_SHORT).show()
                }
            }, 5000)
            
            auth.signInAnonymously()
                .addOnSuccessListener {
                    authCompleted = true
                    val userId = auth.currentUser?.uid
                    Log.d("Firebase", "✅ Anonymous sign-in successful! User ID: $userId")
                    Toast.makeText(this, "Connected to Firebase ✓", Toast.LENGTH_SHORT).show()
                    // Sync workspaces after authentication
                    firebaseSyncManager.syncWorkspaces()
                }
                .addOnFailureListener { e ->
                    authCompleted = true
                    Log.e("Firebase", "❌ Anonymous sign-in failed")
                    Log.e("Firebase", "Error message: ${e.message}")
                    
                    // App continues to work with local storage only
                    Toast.makeText(this, "Offline mode - Data saved locally", Toast.LENGTH_SHORT).show()
                }
        } else {
            val userId = auth.currentUser?.uid
            Log.d("Firebase", "✅ User already authenticated. User ID: $userId")
            Toast.makeText(this, "Firebase connected ✓", Toast.LENGTH_SHORT).show()
            // User already authenticated, sync workspaces
            firebaseSyncManager.syncWorkspaces()
        }
    }

    // Helper to fix the deprecated overridePendingTransition warning
    private fun removeActivityTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun setupClickListeners() {
        binding.createNewSpace.setOnClickListener { showCreateSpacePopup() }
        binding.createSpaces.setOnClickListener { showCreateSpacePopup() }
        binding.btnInvite.setOnClickListener { showInviteMembersPopup() }

        binding.navSpaceIcon.setOnClickListener {
            val intent = Intent(this, newspaces::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            removeActivityTransition()
        }

        // Attach switcher to the top bar elements (Logo, Title, and Arrow)
        val showSwitcher = View.OnClickListener { showWorkspaceSwitcherPopup() }
        binding.ivWorkspaceLogo.setOnClickListener(showSwitcher)
        binding.tvWorkspaceName.setOnClickListener(showSwitcher)
        binding.ivDropdownArrow.setOnClickListener(showSwitcher)
    }

    // ==========================================
    // ✅ TOP BAR DYNAMIC UPDATE LOGIC
    // ==========================================
    private fun updateTopBarUI() {
        val savedSpaces = getSavedSpacesList()
        val activeWorkspace = sharedPrefs.getString("ACTIVE_WORKSPACE", savedSpaces.firstOrNull() ?: "My Workspace")

        // 1. Update the Title Text
        binding.tvWorkspaceName.text = activeWorkspace

        // 2. Extract the Initial Letter
        val initial = if (activeWorkspace!!.isNotEmpty()) activeWorkspace.take(1).uppercase() else "?"

        // 3. Pick the background color
        val colors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#00BCD4")
        val colorIndex = Math.abs(activeWorkspace.hashCode()) % colors.size
        val bgColor = Color.parseColor(colors[colorIndex])

        // 4. Generate the dynamic image and set it to the ImageView
        val dynamicLogo = createInitialBitmap(initial, bgColor)
        binding.ivWorkspaceLogo.setImageBitmap(dynamicLogo)
        binding.ivWorkspaceLogo.clearColorFilter() // Ensure the image shows up clean
    }

    // ==========================================
    // ✅ DYNAMIC IMAGE GENERATOR
    // ==========================================
    private fun createInitialBitmap(initial: String, bgColor: Int): Bitmap {
        val size = 120 // Canvas resolution
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw the colored rounded background
        val bgPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 24f, 24f, bgPaint)

        // 2. Draw the white text letter in the center
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, xPos, yPos, textPaint)

        return bitmap
    }

    // ==========================================
    // ✅ WORKSPACE SWITCHER LOGIC
    // ==========================================
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
    // ✅ INVITE MEMBERS BOTTOM SHEET LOGIC
    // ==========================================
    private fun showInviteMembersPopup() {
        val popupBinding = InviteMembersCardBinding.inflate(layoutInflater)

        popupBinding.etInviteEmail.doAfterTextChanged { text ->
            val input = text?.toString()?.trim() ?: ""
            if (input.isNotEmpty()) {
                popupBinding.btnSubmitInvite.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            } else {
                popupBinding.btnSubmitInvite.setTextColor(Color.parseColor("#C7C7CC"))
            }
        }

        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(popupBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
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

        popupBinding.btnCloseInvite.setOnClickListener { dialog.dismiss() }

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

        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(popupBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
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

        popupBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        popupBinding.btnCreate.setOnClickListener {
            val newSpaceName = popupBinding.etSpaceName.text.toString().trim()
            if (newSpaceName.isEmpty()) return@setOnClickListener

            if (doesSpaceExist(newSpaceName)) {
                Toast.makeText(this, "Can't create: Space already exists!", Toast.LENGTH_SHORT).show()
            } else {
                saveSpaceToStorage(newSpaceName)
                sharedPrefs.edit().putString("ACTIVE_WORKSPACE", newSpaceName).apply()
                updateTopBarUI()
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
        
        // ✅ Sync to Firebase immediately after saving locally
        firebaseSyncManager.syncWorkspaces()
    }

    private fun doesSpaceExist(name: String): Boolean {
        val currentSpaces = getSavedSpacesList()
        return currentSpaces.any { it.equals(name, ignoreCase = true) }
    }
}