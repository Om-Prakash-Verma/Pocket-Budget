package com.ghostpocket.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.ghostpocket.app.model.MoneyUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object WhatsAppReminderHelper {

    /**
     * Generates a sleek monochrome card bitmap with the spooky reminder, amount, and debtor name.
     */
    fun createReminderImage(
        context: Context,
        friendName: String,
        amountPaise: Long,
        customMessage: String?
    ): Uri? {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Deep dark slate / charcoal
        val bgPaint = Paint().apply {
            color = Color.parseColor("#121212")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Inner Card Background (Clay / Bento surface)
        val cardPaint = Paint().apply {
            color = Color.parseColor("#1E1E1E")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(60f, 80f, (width - 60).toFloat(), (height - 80).toFloat())
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // Card Border: Subtle crisp outline
        val borderPaint = Paint().apply {
            color = Color.parseColor("#333333")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        // Text Paint: Header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 42f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("GHOSTPOCKET • COIN REMINDER 👻", width / 2f, 220f, headerPaint)

        // Mascot emoji
        val emojiPaint = Paint().apply {
            textSize = 140f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🪙", width / 2f, 380f, emojiPaint)

        // Friend Salutation
        val salutationPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 64f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Hey $friendName! 👋", width / 2f, 500f, salutationPaint)

        // Friendly spooky subtext
        val subtextPaint = Paint().apply {
            color = Color.parseColor("#B0B0B0")
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Gentle reminder from the crypt to settle:", width / 2f, 570f, subtextPaint)

        // Amount Box
        val amountBoxPaint = Paint().apply {
            color = Color.parseColor("#282828")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val amountBoxRect = RectF(140f, 650f, (width - 140).toFloat(), 880f)
        canvas.drawRoundRect(amountBoxRect, 32f, 32f, amountBoxPaint)

        val amountBorderPaint = Paint().apply {
            color = Color.parseColor("#444444")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(amountBoxRect, 32f, 32f, amountBorderPaint)

        // Amount Text
        val formattedAmount = MoneyUtils.formatPaiseToInr(amountPaise)
        val amountPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 96f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(formattedAmount, width / 2f, 790f, amountPaint)

        // Custom note if provided
        val noteText = customMessage?.takeIf { it.isNotBlank() } ?: "Shared student expenses / bill split"
        val notePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 38f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“$noteText”", width / 2f, 960f, notePaint)

        // Spooky disclaimer footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#757575")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No ghosts were harmed in sending this reminder 👻✨", width / 2f, 1140f, footerPaint)
        canvas.drawText("Tracked with GhostPocket", width / 2f, 1195f, footerPaint)

        // Save to cache directory
        return try {
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "reminder_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Dispatches an Intent directly to WhatsApp with the generated image and text message.
     */
    fun sendWhatsAppReminder(
        context: Context,
        whatsAppNumber: String?,
        friendName: String,
        amountPaise: Long,
        note: String?
    ) {
        val imageUri = createReminderImage(context, friendName, amountPaise, note)
        val amountFormatted = MoneyUtils.formatPaiseToInr(amountPaise)
        val defaultMessage = "Hey $friendName! 👋 Just a quick reminder about the $amountFormatted from GhostPocket 👻🪙"

        val cleanNumber = whatsAppNumber?.replace(Regex("[^0-9+]"), "")?.trim()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, defaultMessage)
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            `package` = "com.whatsapp"
        }

        try {
            if (!cleanNumber.isNullOrBlank()) {
                // If number is specified, try direct chat or fallback to chooser
                val formattedNumber = if (!cleanNumber.startsWith("+")) {
                    // Default to country code or raw digits
                    cleanNumber
                } else {
                    cleanNumber.removePrefix("+")
                }
                shareIntent.putExtra("jid", "$formattedNumber@s.whatsapp.net")
            }

            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // WhatsApp might not be installed, or direct package intent failed; fall back to universal send chooser
            val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_TEXT, defaultMessage)
                if (imageUri != null) {
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(chooserIntent, "Send Payment Reminder via WhatsApp"))
        }
    }
}
