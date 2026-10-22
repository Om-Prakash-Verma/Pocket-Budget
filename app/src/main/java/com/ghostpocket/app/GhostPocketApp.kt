package com.ghostpocket.app

import android.app.Application
import com.ghostpocket.app.data.local.GhostPocketDatabase
import com.ghostpocket.app.data.remote.D1ApiClient
import com.ghostpocket.app.data.repository.GhostPocketRepository
import com.ghostpocket.app.data.repository.GhostPocketRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GhostPocketApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val d1ApiClient by lazy {
        D1ApiClient(this)
    }

    val updateManager by lazy {
        com.ghostpocket.app.update.UpdateManager(this)
    }

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
                val d1User = d1ApiClient.currentUserId
                if (d1User.isNotBlank() && d1User != "guest_user") {
                    d1User
                } else {
                    "local_user"
                }
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
