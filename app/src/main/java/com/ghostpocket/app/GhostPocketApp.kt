package com.ghostpocket.app

import android.app.Application
import com.ghostpocket.app.data.local.GhostPocketDatabase
import com.ghostpocket.app.data.repository.GhostPocketRepository
import com.ghostpocket.app.data.repository.GhostPocketRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GhostPocketApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy {
        GhostPocketDatabase.getDatabase(this)
    }

    val repository: GhostPocketRepository by lazy {
        GhostPocketRepositoryImpl(
            transactionDao = database.transactionDao(),
            friendDao = database.friendDao(),
            friendLedgerDao = database.friendLedgerDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            currentUserIdProvider = {
                try {
                    FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
                } catch (e: Exception) {
                    "local_user"
                }
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Offline-first
        }
    }
}
