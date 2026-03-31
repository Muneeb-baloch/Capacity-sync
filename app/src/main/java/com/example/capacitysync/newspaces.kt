package com.example.capacitysync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.capacitysync.databinding.ActivitySpacescreenBinding

class newspaces : AppCompatActivity() {

    // 2. Declare the binding variable using the new binding class
    private lateinit var binding: ActivitySpacescreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Inflate the layout using the new binding class
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

    private fun setupClickListeners() {
        // You can start adding your button clicks here!
        // Example: binding.btnBack.setOnClickListener { finish() }
    }
}