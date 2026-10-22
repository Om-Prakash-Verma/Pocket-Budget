package com.ghostpocket.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.ui.components.ClayButton

/**
 * ============================================================================
 * AddFriendDialog
 * ============================================================================
 * Dialog allowing the user to summon a new squad member either via phone contacts
 * or by entering their name and optional WhatsApp number manually.
 *
 * @param context Local context for launching toasts and intents.
 * @param contactPickerLauncher Activity result launcher for ContactsContract.
 * @param initialName Name prefilled if chosen from contacts.
 * @param initialWhatsApp WhatsApp phone number prefilled if chosen from contacts.
 * @param onDismiss Dismiss handler.
 * @param onConfirm Invoked with confirmed friend name and phone/WhatsApp number.
 */
@Composable
fun AddFriendDialog(
    context: Context,
    contactPickerLauncher: ActivityResultLauncher<Intent>,
    initialName: String = "",
    initialWhatsApp: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, whatsApp: String) -> Unit
) {
    var friendNameInput by remember(initialName) { mutableStateOf(initialName) }
    var friendWhatsAppInput by remember(initialWhatsApp) { mutableStateOf(initialWhatsApp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Summon a Friend 👥👻",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Add someone to your squad. Pick directly from your phone contacts or enter details manually!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Quick-Pick from Contacts Button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            contactPickerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No contacts app available on device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contacts",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pick from Contacts 📱",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  or enter manually  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = friendNameInput,
                    onValueChange = { friendNameInput = it },
                    label = { Text("Friend's Name") },
                    placeholder = { Text("e.g. Aarav, Rohan, Priya") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = friendWhatsAppInput,
                    onValueChange = { friendWhatsAppInput = it },
                    label = { Text("WhatsApp Number 💬") },
                    placeholder = { Text("+91 9876543210") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ClayButton(
                text = "Summon 🪄",
                enabled = friendNameInput.isNotBlank(),
                onClick = {
                    if (friendNameInput.isNotBlank()) {
                        onConfirm(friendNameInput.trim(), friendWhatsAppInput.trim())
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
