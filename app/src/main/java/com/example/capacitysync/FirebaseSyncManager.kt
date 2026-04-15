package com.example.capacitysync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FirebaseSyncManager(private val context: Context) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("CapacitySyncPrefs", Context.MODE_PRIVATE)
    private val db: FirebaseFirestore
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    init {
        // Enable offline persistence for Firestore
        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }
    
    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val SPACES_COLLECTION = "workspaces"
        private const val LOGS_COLLECTION = "logged_hours"
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
    }
    
    /**
     * Sync workspace names: Local-first, then to Firebase
     */
    fun syncWorkspaces() {
        scope.launch {
            try {
                // Small delay to ensure auth is fully initialized
                delay(500)
                
                // Check if user is authenticated
                if (auth.currentUser == null) {
                    Log.d(TAG, "User not authenticated, skipping Firebase sync")
                    return@launch
                }
                
                val userId = auth.currentUser!!.uid
                Log.d(TAG, "Starting workspace sync for user: $userId")
                
                // Get local workspaces
                val localSpaces = getLocalWorkspaces()
                Log.d(TAG, "Local workspaces: $localSpaces")
                
                // Upload to Firebase
                uploadWorkspacesToFirebase(userId, localSpaces)
                
                // Download from Firebase and merge
                downloadAndMergeWorkspaces(userId)
                
                // Update last sync time
                updateLastSyncTime()
                
                Log.d(TAG, "Workspace sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing workspaces: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sync logged hours for a specific workspace
     */
    fun syncLoggedHours(workspaceName: String) {
        scope.launch {
            try {
                delay(500)
                
                if (auth.currentUser == null) {
                    Log.d(TAG, "User not authenticated, skipping logs sync")
                    return@launch
                }
                
                val userId = auth.currentUser!!.uid
                Log.d(TAG, "Starting logs sync for workspace: $workspaceName")
                
                // Get local logs for this workspace
                val localLogs = getLocalLogsForWorkspace(workspaceName)
                Log.d(TAG, "Local logs: $localLogs")
                
                // Upload to Firebase
                uploadLogsToFirebase(userId, workspaceName, localLogs)
                
                // Download from Firebase and merge
                downloadAndMergeLogs(userId, workspaceName)
                
                Log.d(TAG, "Logs sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing logs: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get local workspaces from SharedPreferences
     */
    private fun getLocalWorkspaces(): Set<String> {
        return sharedPrefs.getStringSet("SAVED_SPACES", mutableSetOf()) ?: mutableSetOf()
    }
    
    /**
     * Get local logs for a specific workspace
     */
    private fun getLocalLogsForWorkspace(workspaceName: String): String {
        val key = "LOGS_DATA_$workspaceName"
        return sharedPrefs.getString(key, "[]") ?: "[]"
    }
    
    /**
     * Upload local workspaces to Firebase
     */
    private fun uploadWorkspacesToFirebase(userId: String, spaces: Set<String>) {
        try {
            Log.d(TAG, "Attempting to upload workspaces for user: $userId")
            val userDocRef = db.collection(SPACES_COLLECTION).document(userId)
            
            val data = mapOf(
                "spaces" to spaces.toList(),
                "lastUpdated" to System.currentTimeMillis(),
                "deviceId" to android.os.Build.DEVICE
            )
            
            userDocRef.set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Workspaces uploaded to Firebase successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to upload workspaces: ${e.message}")
                    Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                    e.printStackTrace()
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading workspaces: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Download workspaces from Firebase and merge with local
     */
    private fun downloadAndMergeWorkspaces(userId: String) {
        try {
            val userDocRef = db.collection(SPACES_COLLECTION).document(userId)
            
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val firebaseSpaces = (document.get("spaces") as? List<String>)?.toSet() ?: emptySet()
                        
                        Log.d(TAG, "Firebase workspaces: $firebaseSpaces")
                        
                        // Merge: Last write wins
                        val localSpaces = getLocalWorkspaces()
                        val mergedSpaces = localSpaces.union(firebaseSpaces)
                        
                        // Save merged spaces locally
                        sharedPrefs.edit().putStringSet("SAVED_SPACES", mergedSpaces).apply()
                        Log.d(TAG, "Merged workspaces: $mergedSpaces")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to download workspaces: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading workspaces: ${e.message}")
        }
    }
    
    /**
     * Update last sync timestamp
     */
    private fun updateLastSyncTime() {
        sharedPrefs.edit().putLong(LAST_SYNC_KEY, System.currentTimeMillis()).apply()
    }
    
    /**
     * Get last sync time
     */
    fun getLastSyncTime(): Long {
        return sharedPrefs.getLong(LAST_SYNC_KEY, 0L)
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        scope.cancel()
    }
    
    /**
     * Upload logged hours to Firebase
     */
    private fun uploadLogsToFirebase(userId: String, workspaceName: String, logsJson: String) {
        try {
            Log.d(TAG, "Attempting to upload logs for workspace: $workspaceName")
            val docRef = db.collection(LOGS_COLLECTION)
                .document(userId)
                .collection("workspaces")
                .document(workspaceName)
            
            val data = mapOf(
                "logs" to logsJson,
                "lastUpdated" to System.currentTimeMillis(),
                "deviceId" to android.os.Build.DEVICE
            )
            
            docRef.set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Logs uploaded to Firebase successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to upload logs: ${e.message}")
                    e.printStackTrace()
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading logs: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Download logs from Firebase and merge with local
     */
    private fun downloadAndMergeLogs(userId: String, workspaceName: String) {
        try {
            val docRef = db.collection(LOGS_COLLECTION)
                .document(userId)
                .collection("workspaces")
                .document(workspaceName)
            
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firebaseLogs = document.getString("logs") ?: "[]"
                        Log.d(TAG, "Firebase logs downloaded for $workspaceName")
                        
                        // For now, last write wins - Firebase data overwrites local if newer
                        val firebaseTimestamp = document.getLong("lastUpdated") ?: 0L
                        val localTimestamp = sharedPrefs.getLong("${workspaceName}_last_updated", 0L)
                        
                        if (firebaseTimestamp > localTimestamp) {
                            val key = "LOGS_DATA_$workspaceName"
                            sharedPrefs.edit()
                                .putString(key, firebaseLogs)
                                .putLong("${workspaceName}_last_updated", firebaseTimestamp)
                                .apply()
                            Log.d(TAG, "Merged logs from Firebase (newer)")
                        } else {
                            Log.d(TAG, "Local logs are newer, keeping local")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to download logs: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading logs: ${e.message}")
        }
    }
}
