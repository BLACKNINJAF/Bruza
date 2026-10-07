package com.example.model

enum class SyncState {
    ONLINE_SYNCED,
    OFFLINE_CACHED,
    SYNCING
}

data class SyncInfo(
    val state: SyncState = SyncState.ONLINE_SYNCED,
    val pendingWritesCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
)
