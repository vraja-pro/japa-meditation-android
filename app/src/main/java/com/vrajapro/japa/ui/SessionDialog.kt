package com.vrajapro.japa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vrajapro.japa.JapaState
import com.vrajapro.japa.R

@Composable
fun SessionDialog(
    state: JapaState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onInteraction: () -> Unit,
) {
    var notes by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss, // back button = "back to practice"
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = JapaColors.Bg2,
            modifier = Modifier
                .fillMaxWidth()
                // Any touch postpones the inactivity auto-save (like the web modal).
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            onInteraction()
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ॐ", fontSize = 40.sp, color = JapaColors.Saffron)
                Text(
                    stringResource(R.string.modal_title),
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JapaColors.Text,
                )
                Text(stringResource(R.string.modal_subtitle), fontSize = 14.sp, color = JapaColors.TextDim)
                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    DialogStat(state.count.toString(), stringResource(R.string.mantras_label))
                    VerticalDivider(color = JapaColors.Surface2, modifier = Modifier.padding(horizontal = 16.dp))
                    DialogStat(state.rounds.toString(), stringResource(R.string.stat_rounds))
                    VerticalDivider(color = JapaColors.Surface2, modifier = Modifier.padding(horizontal = 16.dp))
                    DialogStat(formatTime(state.elapsedSec), stringResource(R.string.duration))
                }
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; onInteraction() },
                    label = { Text(stringResource(R.string.notes_label)) },
                    placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JapaColors.Saffron,
                        unfocusedBorderColor = JapaColors.Border,
                        focusedLabelColor = JapaColors.Saffron,
                        cursorColor = JapaColors.Saffron,
                    ),
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onSave(notes) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = JapaColors.Saffron),
                ) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dismiss), color = JapaColors.SaffronDeep)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.discard), color = JapaColors.TextDim)
                }
            }
        }
    }
}

@Composable
private fun DialogStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = JapaColors.Saffron)
        Text(label, fontSize = 11.sp, color = JapaColors.TextDim)
    }
}
