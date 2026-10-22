package com.ghostpocket.app.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.ghostpocket.app.model.MoneyUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Generates tactile, humorous, comic-style payment reminder cards as bitmaps.
 */
object ReminderImageGenerator {

    /**
     * Generates a clay-style comic reminder card bitmap.
     *
     * Features:
     * - Handcrafted cartoon ghost mascot with pleading expression 🥺🤲
     * - Humorous copy ("Friendship survival protocol")
     * - Tactile rounded claymorphism container
     * - Big bold formatted rupee amount badge
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
        val ghostCenterX = width / 2f
        val ghostCenterY = 360f

        val ghostShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(ghostCenterX - 110f, ghostCenterY + 115f, ghostCenterX + 110f, ghostCenterY + 145f), ghostShadowPaint)

        val ghostPath = Path().apply {
            moveTo(ghostCenterX - 100f, ghostCenterY)
            cubicTo(
                ghostCenterX - 100f, ghostCenterY - 140f,
                ghostCenterX + 100f, ghostCenterY - 140f,
                ghostCenterX + 100f, ghostCenterY
            )
            cubicTo(
                ghostCenterX + 130f, ghostCenterY + 40f,
                ghostCenterX + 120f, ghostCenterY + 80f,
                ghostCenterX + 90f, ghostCenterY + 95f
            )
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
            cubicTo(
                ghostCenterX - 110f, ghostCenterY + 90f,
                ghostCenterX - 130f, ghostCenterY + 40f,
                ghostCenterX - 100f, ghostCenterY
            )
            close()
        }

        val ghostBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(ghostPath, ghostBodyPaint)

        // Tiny top hat
        val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121214")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(ghostCenterX - 25f, ghostCenterY - 150f, ghostCenterX + 35f, ghostCenterY - 120f), 6f, 6f, hatPaint)
        canvas.drawRoundRect(RectF(ghostCenterX - 40f, ghostCenterY - 122f, ghostCenterX + 50f, ghostCenterY - 114f), 4f, 4f, hatPaint)

        // Pleading big eyes 🥺
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#101012")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(ghostCenterX - 48f, ghostCenterY - 30f, ghostCenterX - 16f, ghostCenterY + 18f), eyePaint)
        canvas.drawOval(RectF(ghostCenterX + 16f, ghostCenterY - 30f, ghostCenterX + 48f, ghostCenterY + 18f), eyePaint)

        // Eye reflections
        val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(ghostCenterX - 36f, ghostCenterY - 16f, 9f, pupilPaint)
        canvas.drawCircle(ghostCenterX - 26f, ghostCenterY + 4f, 4f, pupilPaint)
        canvas.drawCircle(ghostCenterX + 28f, ghostCenterY - 16f, 9f, pupilPaint)
        canvas.drawCircle(ghostCenterX + 38f, ghostCenterY + 4f, 4f, pupilPaint)

        // Cute blushing cheeks
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44FF8080")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(ghostCenterX - 60f, ghostCenterY + 18f, 14f, blushPaint)
        canvas.drawCircle(ghostCenterX + 60f, ghostCenterY + 18f, 14f, blushPaint)

        // Pleading wavy mouth
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#101012")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        val mouthPath = Path().apply {
            moveTo(ghostCenterX - 16f, ghostCenterY + 36f)
            quadTo(ghostCenterX, ghostCenterY + 44f, ghostCenterX + 16f, ghostCenterY + 36f)
        }
        canvas.drawPath(mouthPath, mouthPaint)

        // 6. Cute Comic Speech Bubble
        val bubbleRect = RectF(140f, 520f, width - 140f, 660f)
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#272730")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bubbleRect, 32f, 32f, bubblePaint)

        val tailPath = Path().apply {
            moveTo(width / 2f - 20f, 520f)
            lineTo(width / 2f, 490f)
            lineTo(width / 2f + 20f, 520f)
            close()
        }
        canvas.drawPath(tailPath, bubblePaint)

        val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Pssssst... Hey $friendName! 👀", width / 2f, 580f, bubbleTextPaint)

        val bubbleSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B4B4C0")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("My wallet is starving like a hungry ghost... 🥺", width / 2f, 626f, bubbleSubTextPaint)

        // 7. Amount Container
        val amountCardRect = RectF(120f, 700f, width - 120f, 960f)
        val amountCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#23232A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(amountCardRect, 40f, 40f, amountCardPaint)

        val amountBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#383844")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(amountCardRect, 40f, 40f, amountBorderPaint)

        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9EAA")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText("💰 UNPAID GOLD TO RETURN 💰", width / 2f, 760f, amountLabelPaint)

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

        // 9. Meme disclaimer at bottom
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
}
