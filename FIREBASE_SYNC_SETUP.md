# Firebase Sync Implementation - Local-First Architecture

## Overview
This implementation provides a **local-first, cloud-sync** architecture for CapacitySync:
- Data is saved locally first (SharedPreferences)
- When internet is available, data syncs to Firebase
- If offline, data stays local and syncs when connection returns
- **Last write wins** conflict resolution

## What's Been Implemented

### 1. Firebase Configuration ✅
- **Project ID**: `capaciatysync`
- **google-services.json**: Already configured in `app/google-services.json`
- **Firebase Dependencies**: Added to `app/build.gradle.kts`

### 2. FirebaseSyncManager.kt ✅
New file: `app/src/main/java/com/example/capacitysync/FirebaseSyncManager.kt`

**Features:**
- Manages local-to-Firebase sync
- Handles workspace names sync
- Implements "last write wins" conflict resolution
- Automatic merge of local and Firebase data
- Tracks last sync timestamp

**Key Methods:**
```kotlin
syncWorkspaces()                    // Main sync function
getLocalWorkspaces()               // Get workspaces from SharedPreferences
uploadWorkspacesToFirebase()        // Upload to Firebase
downloadAndMergeWorkspaces()        // Download and merge with local
```

### 3. Dashboard.kt Updates ✅
**Added:**
- Firebase Authentication initialization
- Anonymous sign-in (can upgrade to email/phone later)
- Automatic workspace sync on app launch
- Cleanup on app destroy

**Flow:**
1. App launches → Dashboard onCreate
2. Firebase auth initializes (anonymous)
3. FirebaseSyncManager syncs workspaces
4. Local workspaces + Firebase workspaces = merged list

## How It Works

### Sync Flow
```
User Creates Space
    ↓
Saved to SharedPreferences (Local)
    ↓
App detects internet
    ↓
Upload to Firebase
    ↓
Download Firebase data
    ↓
Merge (Last write wins)
    ↓
Update local storage
```

### Offline Scenario
```
No Internet
    ↓
User creates space
    ↓
Saved to SharedPreferences only
    ↓
Internet returns
    ↓
Automatic sync triggers
    ↓
Data uploaded to Firebase
```

## Firebase Firestore Structure

**Collection**: `workspaces`
**Document**: `{userId}`

```json
{
  "spaces": ["Space 1", "Space 2", "Space 3"],
  "lastUpdated": 1704067200000,
  "deviceId": "device_name"
}
```

## Current Status

✅ **Implemented:**
- Firebase setup and authentication
- Local-first storage (SharedPreferences)
- Workspace sync to Firebase
- Conflict resolution (last write wins)
- Automatic sync on app launch

⏳ **Next Steps (Optional):**
- Add sync status UI indicator
- Implement periodic background sync
- Add sync for logged hours data
- Add user profile sync
- Implement real-time listeners for multi-device sync

## Testing

To test the implementation:

1. **First Launch (No Internet)**
   - Create a workspace
   - Data saves locally
   - Check SharedPreferences

2. **With Internet**
   - Connect to internet
   - App automatically syncs
   - Check Firebase Firestore console

3. **Multi-Device Sync**
   - Create workspace on Device A
   - Open app on Device B
   - Workspace appears on Device B (after sync)

## Firebase Rules (Recommended)

Add these Firestore security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /workspaces/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

## Notes

- Currently using **anonymous authentication** (suitable for MVP)
- Can upgrade to email/phone authentication later
- Sync happens automatically on app launch
- No manual sync button needed (can add later if desired)
- All workspace data is stored locally first for offline support
