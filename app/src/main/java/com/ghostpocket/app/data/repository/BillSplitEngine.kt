package com.ghostpocket.app.data.repository

import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType

/**
 * BillSplitEngine computes and coordinates bill distributions:
 * - Equal split: Dividing paise evenly with remainder distributed to the first shares.
 * - Custom split: Applying user-specified shares for each participant.
 * - Handling ledger entries for payer vs participants.
 * - Recording wallet expense transactions if the user paid upfront.
 */
object BillSplitEngine {

    suspend fun executeSplit(
        title: String,
        totalPaise: Long,
        payerFriendId: String?,
        participantFriendIds: List<String>,
        category: String,
        customShares: Map<String, Long>?,
        currentUserId: String,
        onAddTransaction: suspend (TransactionEntity) -> Unit,
        onAddLedgerEntry: suspend (friendId: String, amountPaise: Long, direction: LedgerDirection, note: String?) -> Unit
    ) {
        if (totalPaise <= 0) return
        val totalMembers = participantFriendIds.size + 1
        if (totalMembers <= 1) return

        val userShare: Long
        val friendShares: Map<String, Long>

        if (customShares != null && customShares.isNotEmpty()) {
            userShare = customShares["user"] ?: 0L
            friendShares = participantFriendIds.associateWith { customShares[it] ?: 0L }
        } else {
            val shares = MoneyUtils.splitEqually(totalPaise, totalMembers)
            userShare = shares.first()
            friendShares = participantFriendIds.mapIndexed { index, fId -> fId to shares[index + 1] }.toMap()
        }

        if (payerFriendId == null) {
            // 1. User paid the full bill upfront. Deduct the full cash amount from wallet.
            val tx = TransactionEntity(
                amountPaise = totalPaise,
                type = TransactionType.EXPENSE,
                category = category,
                note = "$title (Full bill paid upfront; split with ${participantFriendIds.size} friends)",
                userId = currentUserId
            )
            onAddTransaction(tx)

            // 2. Friends each owe user their share
            friendShares.forEach { (fId, fShare) ->
                if (fShare > 0) {
                    onAddLedgerEntry(
                        fId,
                        fShare,
                        LedgerDirection.THEY_OWE_YOU,
                        "$title (Split share)"
                    )
                }
            }
        } else {
            // A friend paid upfront. User owes friend their share, but ₹0 left user's wallet right now.
            if (userShare > 0) {
                onAddLedgerEntry(
                    payerFriendId,
                    userShare,
                    LedgerDirection.YOU_OWE_THEM,
                    "$title (Split share owed to friend)"
                )
            }
            // Note: Cash deduction happens when user actually pays the friend back via recordRepayment.
        }
    }
}
