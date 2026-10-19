package com.ghostpocket.app.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
    FAILED
}

enum class LedgerDirection {
    THEY_OWE_YOU, // Friend borrowed money from user (owed to user)
    YOU_OWE_THEM  // User borrowed money from friend (user owes friend)
}
