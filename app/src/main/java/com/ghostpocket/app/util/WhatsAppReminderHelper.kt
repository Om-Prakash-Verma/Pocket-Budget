package com.ghostpocket.app.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.ghostpocket.app.model.MoneyUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object WhatsAppReminderHelper {

    /**
     * Generates a cute, funny, clay-style spooky comic reminder card bitmap.
     * Features:
     * - Handcrafted cartoon ghost mascot with funny begging / pleading expression 🥺🤲
     * - Funny humorous copy ("Empty pockets", "Chai debt cannot be ignored")
     * - Tactile rounded claymorphism container
     * - Big bold rupee amount badge
     * - Spooky comic speech bubble
     */
    fun createReminderImage(
        context: Context,
        friendName: String,
        amountPaise: Long,
        customMessage: String?
    ): Uri? {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Dark matte charcoal background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121214")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Playful floating spooky stars / sparks in background
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.FILL
        }
        // Fun background doodles / floaters
        canvas.drawCircle(140f, 220f, 12f, sparkPaint)
        canvas.drawCircle(920f, 260f, 8f, sparkPaint)
        canvas.drawCircle(180f, 1100f, 10f, sparkPaint)
        canvas.drawCircle(880f, 1050f, 14f, sparkPaint)

        // 3. Main Tactile Clay Card Container
        val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0A0C")
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
        val cardRect = RectF(70f, 80f, width - 70f, height - 90f)
        canvas.drawRoundRect(RectF(70f, 95f, width - 70f, height - 75f), 56f, 56f, cardShadowPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C20")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E2E36")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardBorderPaint)

        // 4. Header Badge Pill: "👻 GHOSTPOCKET SQUAD LEDGER"
        val pillRect = RectF(240f, 130f, width - 240f, 205f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2A32")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 38f, 38f, pillPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0D0D8")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText("👻 POCKET DEBT EXORCISM 🪄", width / 2f, 178f, headerTextPaint)

        // 5. DRAW CUTE & FUNNY GHOST MASCOT
        // Ghost Center: X = width/2, Y = 360
        val ghostCenterX = width / 2f
        val ghostCenterY = 360f

        // Cute Ghost Shadow
        val ghostShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(ghostCenterX - 110f, ghostCenterY + 115f, ghostCenterX + 110f, ghostCenterY + 145f), ghostShadowPaint)

        // Ghost Body Path (Puffy, cute cartoon blob)
        val ghostPath = Path().apply {
            moveTo(ghostCenterX - 100f, ghostCenterY)
            // Head Dome
            cubicTo(
                ghostCenterX - 100f, ghostCenterY - 140f,
                ghostCenterX + 100f, ghostCenterY - 140f,
                ghostCenterX + 100f, ghostCenterY
            )
            // Right arm stretched out cutely
            cubicTo(
                ghostCenterX + 130f, ghostCenterY + 40f,
                ghostCenterX + 120f, ghostCenterY + 80f,
                ghostCenterX + 90f, ghostCenterY + 95f
            )
            // Wavy cartoon skirt tentacles
            cubicTo(
                ghostCenterX + 70f, ghostCenterY + 75f,
                ghostCenterX + 45f, ghostCenterY + 130f,
                ghostCenterX + 15f, ghostCenterY + 95f
            )
            cubicTo(
                ghostCenterX - 15f, ghostCenterY + 130f,
                ghostCenterX - 45f, ghostCenterY + 80f,
                ghostCenterX - 70f, ghostCenterY + 100f
            )
            // Left arm
            cubicTo(
                ghostCenterX - 110f, ghostCenterY + 90f,
                ghostCenterX - 130f, ghostCenterY + 40f,
                ghostCenterX - 100f, ghostCenterY
            )
            close()
        }

        // Draw Ghost White Clay Body
        val ghostBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(ghostPath, ghostBodyPaint)

        // Tiny top hat perched slightly tilted 🎩
        val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121214")
            style = Paint.Style.FILL
        }
        val hatBrim = RectF(ghostCenterX + 10f, ghostCenterY - 135f, ghostCenterX + 75f, ghostCenterY - 120f)
        canvas.drawRoundRect(hatBrim, 6f, 6f, hatPaint)
        val hatCrown = RectF(ghostCenterX + 22f, ghostCenterY - 180f, ghostCenterX + 63f, ghostCenterY - 135f)
        canvas.drawRoundRect(hatCrown, 6f, 6f, hatPaint)
        // Red hat ribbon
        val hatRibbon = RectF(ghostCenterX + 22f, ghostCenterY - 142f, ghostCenterX + 63f, ghostCenterY - 135f)
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
            style = Paint.Style.FILL
        }
        canvas.drawRect(hatRibbon, ribbonPaint)

        // Ghost Face (Anime begging eyes + blush + open mouth)
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#151518")
            style = Paint.Style.FILL
        }
        // Big cute eyes
        val leftEyeX = ghostCenterX - 35f
        val rightEyeX = ghostCenterX + 35f
        val eyeY = ghostCenterY - 25f
        canvas.drawCircle(leftEyeX, eyeY, 14f, eyePaint)
        canvas.drawCircle(rightEyeX, eyeY, 14f, eyePaint)

        // Shiny anime eye glints ✨
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(leftEyeX - 4f, eyeY - 4f, 5f, glintPaint)
        canvas.drawCircle(leftEyeX + 3f, eyeY + 4f, 2.5f, glintPaint)
        canvas.drawCircle(rightEyeX - 4f, eyeY - 4f, 5f, glintPaint)
        canvas.drawCircle(rightEyeX + 3f, eyeY + 4f, 2.5f, glintPaint)

        // Cute rosy cheeks 🌸
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44FF8A80")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(leftEyeX - 25f, eyeY + 12f, leftEyeX - 3f, eyeY + 24f), blushPaint)
        canvas.drawOval(RectF(rightEyeX + 3f, eyeY + 12f, rightEyeX + 25f, eyeY + 24f), blushPaint)

        // Cute w-shaped cat smile / open surprised mouth :3
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#151518")
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
        }
        val mouthPath = Path().apply {
            moveTo(ghostCenterX - 16f, eyeY + 18f)
            quadTo(ghostCenterX - 8f, eyeY + 30f, ghostCenterX, eyeY + 20f)
            quadTo(ghostCenterX + 8f, eyeY + 30f, ghostCenterX + 16f, eyeY + 18f)
        }
        canvas.drawPath(mouthPath, mouthPaint)

        // 6. Speech Bubble from the Ghost: "Psst... $friendName!"
        val bubbleRect = RectF(160f, 530f, width - 160f, 660f)
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#26262E")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bubbleRect, 32f, 32f, bubblePaint)

        // Little speech bubble pointer triangle
        val bubblePointer = Path().apply {
            moveTo(ghostCenterX - 15f, 530f)
            lineTo(ghostCenterX, 505f)
            lineTo(ghostCenterX + 15f, 530f)
            close()
        }
        canvas.drawPath(bubblePointer, bubblePaint)

        val salutationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Yo $friendName! 👀", width / 2f, 585f, salutationPaint)

        val funnySubtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B4B4C0")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("My bank account is currently haunting me: 📉👻", width / 2f, 630f, funnySubtextPaint)

        // 7. Funny "Bento Gold Clay Box" with Huge Rupee Amount
        val amountBoxRect = RectF(120f, 700f, width - 120f, 960f)
        val amountBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#151518")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(amountBoxRect, 40f, 40f, amountBoxPaint)

        val amountBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawRoundRect(amountBoxRect, 40f, 40f, amountBorder)

        // Little yellow label on top of box
        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText("💰 UNPAID GOLD TO RETURN 💰", width / 2f, 760f, amountLabelPaint)

        // Big Bold Rupee Amount
        val formattedAmount = MoneyUtils.formatPaiseToInr(amountPaise)
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 105f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(formattedAmount, width / 2f, 870f, amountPaint)

        val speedNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E8E9A")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("(UPI / Cash / Even Chai is acceptable ☕)", width / 2f, 920f, speedNotePaint)

        // 8. Custom Message / Note Section
        val noteText = customMessage?.takeIf { it.isNotBlank() } ?: "Shared student survival expenses"
        val noteQuotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F7")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“$noteText”", width / 2f, 1040f, noteQuotePaint)

        // 9. Playful, humorous meme disclaimer at bottom
        val funnyPillRect = RectF(140f, 1110f, width - 140f, 1260f)
        val funnyPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222228")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(funnyPillRect, 28f, 28f, funnyPillPaint)

        val memePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCC80")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚠️ FRIENDSHIP SURVIVAL PROTOCOL ⚠️", width / 2f, 1165f, memePaint)

        val memeSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9EAA")
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Pay this back now and your ghost friend won't haunt you!", width / 2f, 1215f, memeSubPaint)

        // 10. Footer branding
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A5A66")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Sent with 🤍 via GhostPocket App 👻", width / 2f, 1315f, footerPaint)

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
     * Generates the humorous image and launches WhatsApp (or Android share chooser) with image + caption pre-filled.
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
