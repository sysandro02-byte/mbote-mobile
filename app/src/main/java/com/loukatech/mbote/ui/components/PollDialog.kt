package com.loukatech.mbote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.ui.theme.MbotePurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (question: String, options: List<String>, isMultipleChoice: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var question by remember { mutableStateOf("") }
    var options by remember {
        mutableStateOf(listOf("Option 1", "Option 2"))
    }
    var isMultipleChoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Poll,
                    contentDescription = null,
                    tint = MbotePurplePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Créer un sondage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Posez votre question...") },
                    placeholder = { Text("Ex: Quelle date pour la rencontre ?") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_question_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Options de réponse",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MbotePurplePrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    itemsIndexed(options) { index, optionText ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { newText ->
                                    options = options.toMutableList().also { it[index] = newText }
                                },
                                label = { Text("Option ${index + 1}") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("poll_option_input_$index")
                            )

                            if (options.size > 2) {
                                IconButton(
                                    onClick = {
                                        options = options.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Supprimer option",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (options.size < 6) {
                    TextButton(
                        onClick = { options = options + "Option ${options.size + 1}" },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter une option", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Choix multiples",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Autoriser plusieurs réponses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isMultipleChoice,
                        onCheckedChange = { isMultipleChoice = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MbotePurplePrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && options.count { it.isNotBlank() } >= 2) {
                        onCreatePoll(question, options, isMultipleChoice)
                    }
                },
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                modifier = Modifier.testTag("submit_poll_button")
            ) {
                Text("Publier le sondage")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.testTag("poll_dialog")
    )
}
