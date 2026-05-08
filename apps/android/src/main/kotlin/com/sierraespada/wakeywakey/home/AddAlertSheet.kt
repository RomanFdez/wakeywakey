package com.sierraespada.wakeywakey.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.R
import java.text.SimpleDateFormat
import java.util.*

private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertSheet(
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateTimeMillis: Long, notes: String) -> Unit,
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Default: today + 1 hour rounded to next half-hour
    val defaultCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, if (get(Calendar.MINUTE) < 30) 30 else 0)
            if (get(Calendar.MINUTE) == 0) add(Calendar.HOUR_OF_DAY, 1)
        }
    }
    var pickedCal by remember { mutableStateOf(defaultCal.clone() as Calendar) }

    val dateFmt = remember { SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NavySurface,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.add_alert_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

            // Title
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                placeholder   = { Text(stringResource(R.string.add_alert_title_hint), color = Color.White.copy(alpha = 0.35f)) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier      = Modifier.fillMaxWidth(),
                colors        = alertFieldColors(),
            )

            // Date + Time row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Date picker
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                pickedCal = (pickedCal.clone() as Calendar).apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                            },
                            pickedCal.get(Calendar.YEAR),
                            pickedCal.get(Calendar.MONTH),
                            pickedCal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Yellow.copy(alpha = 0.4f)),
                ) {
                    Text(dateFmt.format(pickedCal.time), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Time picker
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                pickedCal = (pickedCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, min)
                                    set(Calendar.SECOND, 0)
                                }
                            },
                            pickedCal.get(Calendar.HOUR_OF_DAY),
                            pickedCal.get(Calendar.MINUTE),
                            true,
                        ).show()
                    },
                    modifier = Modifier.width(100.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Yellow.copy(alpha = 0.4f)),
                ) {
                    Text(timeFmt.format(pickedCal.time), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Notes
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                placeholder   = { Text(stringResource(R.string.add_alert_notes_hint), color = Color.White.copy(alpha = 0.35f)) },
                minLines      = 3,
                maxLines      = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier      = Modifier.fillMaxWidth(),
                colors        = alertFieldColors(),
            )

            // Confirm
            Button(
                onClick  = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), pickedCal.timeInMillis, notes.trim())
                    }
                },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
            ) {
                Text(stringResource(R.string.add_alert_confirm), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun alertFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    focusedBorderColor   = Yellow,
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    cursorColor          = Yellow,
)
