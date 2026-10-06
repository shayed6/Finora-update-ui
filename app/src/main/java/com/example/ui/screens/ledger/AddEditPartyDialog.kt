package com.example.ui.screens.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.LedgerPartyEntity
import com.example.ui.theme.PrimaryBlue

@Composable
fun AddEditPartyDialog(
    party: LedgerPartyEntity? = null,
    existingParties: List<LedgerPartyEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String?) -> Unit,
    onOpenExisting: (partyId: Long) -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf(party?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(party?.phone ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    var duplicatePhoneParty by remember { mutableStateOf<LedgerPartyEntity?>(null) }
    var duplicateNameParty by remember { mutableStateOf<LedgerPartyEntity?>(null) }

    val isEditing = party != null
    val currentPartyId = party?.id ?: 0L
    val title = if (isEditing) LedgerStrings.DIALOG_EDIT_PARTY_TITLE else LedgerStrings.DIALOG_ADD_PARTY_TITLE

    // Dialog 1: Duplicate Phone Dialog
    if (duplicatePhoneParty != null) {
        val existing = duplicatePhoneParty!!
        AlertDialog(
            onDismissRequest = { duplicatePhoneParty = null },
            modifier = Modifier.testTag("dialog_duplicate_phone"),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "নম্বর মিলে গেছে",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "এই নম্বরে '${existing.name}' আগে থেকেই আছে",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToOpen = existing.id
                        duplicatePhoneParty = null
                        onDismiss()
                        onOpenExisting(idToOpen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_open_existing_party")
                ) {
                    Text(LedgerStrings.BTN_OPEN_EXISTING_PARTY, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { duplicatePhoneParty = null },
                    modifier = Modifier.testTag("btn_cancel_duplicate_phone")
                ) {
                    Text(LedgerStrings.BTN_CANCEL)
                }
            }
        )
    }

    // Dialog 2: Duplicate Name Dialog (when phone does not match)
    if (duplicateNameParty != null) {
        val existing = duplicateNameParty!!
        val phoneDisplay = if (!existing.phone.isNullOrBlank()) existing.phone else "নেই"
        AlertDialog(
            onDismissRequest = { duplicateNameParty = null },
            modifier = Modifier.testTag("dialog_duplicate_name"),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "একই নামের ব্যক্তি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "'${existing.name}' নামে আরেকজন আছে (ফোন: $phoneDisplay)। একই ব্যক্তি?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToOpen = existing.id
                        duplicateNameParty = null
                        onDismiss()
                        onOpenExisting(idToOpen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_same_person")
                ) {
                    Text(LedgerStrings.BTN_SAME_PERSON, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        duplicateNameParty = null
                        onSave(name.trim(), phone.trim().ifBlank { null })
                    },
                    modifier = Modifier.testTag("btn_different_person")
                ) {
                    Text(LedgerStrings.BTN_DIFFERENT_PERSON)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_edit_party"),
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Field (Required)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text(LedgerStrings.FIELD_NAME_LABEL) },
                    placeholder = { Text(LedgerStrings.FIELD_NAME_HINT) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    isError = nameError != null,
                    supportingText = {
                        nameError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_party_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Phone Field (Optional)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(LedgerStrings.FIELD_PHONE_LABEL) },
                    placeholder = { Text(LedgerStrings.FIELD_PHONE_HINT) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_party_phone"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        nameError = LedgerStrings.ERROR_NAME_REQUIRED
                        return@Button
                    }

                    val cleanPhone = phone.trim().ifBlank { null }
                    val normPhone = LedgerNormalizer.normalizePhone(cleanPhone)
                    val normName = LedgerNormalizer.normalizeName(trimmedName)

                    // 1. Phone match check
                    if (normPhone != null) {
                        val phoneMatch = existingParties.firstOrNull {
                            it.id != currentPartyId && LedgerNormalizer.normalizePhone(it.phone) == normPhone
                        }
                        if (phoneMatch != null) {
                            duplicatePhoneParty = phoneMatch
                            return@Button
                        }
                    }

                    // 2. Exact normalized name match check
                    val nameMatch = existingParties.firstOrNull {
                        it.id != currentPartyId && LedgerNormalizer.normalizeName(it.name) == normName
                    }
                    if (nameMatch != null) {
                        duplicateNameParty = nameMatch
                        return@Button
                    }

                    // 3. No duplicate detected
                    onSave(trimmedName, cleanPhone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_party")
            ) {
                Text(LedgerStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_party")
            ) {
                Text(LedgerStrings.BTN_CANCEL)
            }
        }
    )
}
