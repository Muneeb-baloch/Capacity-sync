package com.example.capacitysync

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.capacitysync.databinding.ActivityDashboardBinding // Import generated binding

class Dashboard : AppCompatActivity() {

    // 1. Declare the binding variable
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Initialize the binding
        binding = ActivityDashboardBinding.inflate(layoutInflater)

        enableEdgeToEdge()

        // 3. Set the content view to the root of the binding
        setContentView(binding.root)

        // 4. Use binding.root for the WindowInsetsListener
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ACCESS YOUR VIEWS HERE ---
        // No more findViewById!
        binding.tvWorkspaceName.text = "Muneeb's Workspace"

        binding.layoutRecents.setOnClickListener {
            // Your logic for clicking the Recents row
        }
    }
}