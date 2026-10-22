package com.ghostpocket.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ghostpocket.app.model.MoneyUtils

/**
 * WhatsAppReminderHelper manages generating payment reminders and dispatching them
 * directly to WhatsApp contacts or standard system share sheets.
 */
object WhatsAppReminderHelper {

    /**
     * Delegates reminder card image creation to [ReminderImageGenerator].
     */
    fun createReminderImage(
        context: Context,
        friendName: String,
        amountPaise: Long,
        customMessage: String?
    ): Uri? {
        return ReminderImageGenerator.createReminderImage(
            context = context,
            friendName = friendName,
            amountPaise = amountPaise,
            customMessage = customMessage
        )
    }

    /**
     * Shortcut for dispatching reminder to WhatsApp.
     */
    fun sendWhatsAppReminder(
        context: Context,
        whatsAppNumber: String?,
        friendName: String,
        amountPaise: Long,
        note: String?
    ) {
        shareReminderToWhatsApp(
            context = context,
            friendName = friendName,
            amountPaise = amountPaise,
            whatsAppPhone = whatsAppNumber,
            customNote = note
        )
    }

    /**
     * Generates humorous card image and launches WhatsApp with targeted JID or fallback share sheet.
     */
    fun shareReminderToWhatsApp(
        context: Context,
        friendName: String,
        amountPaise: Long,
        whatsAppPhone: String?,
        customNote: String?
    ) {
        val imageUri = createReminderImage(context, friendName, amountPaise, customNote)
        val formattedAmount = MoneyUtils.formatPaiseToInr(amountPaise)
        val caption = "Yo $friendName! 👻 Just a friendly reminder for $formattedAmount ($customNote). Settle whenever you're free! ✨"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, caption)
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val cleanPhone = whatsAppPhone?.replace("[^0-9+]".toRegex(), "")
        if (!cleanPhone.isNullOrBlank()) {
            val formattedPhone = if (cleanPhone.startsWith("+")) {
                cleanPhone.substring(1)
            } else if (cleanPhone.length == 10) {
                "91$cleanPhone"
            } else {
                cleanPhone
            }
            sendIntent.setPackage("com.whatsapp")
            sendIntent.putExtra("jid", "$formattedPhone@s.whatsapp.net")
        }

        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_TEXT, caption)
                if (imageUri != null) {
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Send Payment Reminder 👻"))
        }
    }
}
