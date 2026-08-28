package com.loukatech.mbote.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loukatech.mbote.ui.theme.MbotePurplePrimary
import com.loukatech.mbote.ui.theme.MbotePurpleSoft

@Composable
fun InviteLinkDialog(
    titleName: String,
    isChannel: Boolean = false,
    existingLink: String? = null,
    onDismiss: () -> Unit,
    onResetLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedToast by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }

    val linkCode = remember(existingLink, titleName) {
        existingLink ?: "https://mbote.app/join/${if (isChannel) "chn" else "grp"}_${titleName.lowercase().replace(" ", "_")}_${(1000..9999).random()}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("invite_link_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MbotePurpleSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isChannel) Icons.Default.Campaign else Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isChannel) "Lien du canal" else "Lien du groupe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B)
                            )
                            Text(
                                text = titleName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Toute personne disposant de MBoté peut utiliser ce lien pour rejoindre ce ${if (isChannel) "canal" else "groupe"}.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Link Display Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F3FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MbotePurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = linkCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MbotePurplePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MbotePurplePrimary,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(linkCode))
                                    copiedToast = true
                                }
                                .testTag("copy_invite_link_button")
                        ) {
                            Text(
                                text = if (copiedToast) "Copié !" else "Copier",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // QR Code preview
                AnimatedVisibility(visible = showQrCode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MbotePurplePrimary),
                            modifier = Modifier
                                .size(160.dp)
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Code QR",
                                        tint = MbotePurplePrimary,
                                        modifier = Modifier.size(100.dp)
                                    )
                                    Text(
                                        text = "Scannez avec MBoté",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MbotePurplePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share via external apps
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Rejoignez ${if (isChannel) "mon canal" else "notre groupe"} \"$titleName\" sur MBoté en cliquant sur ce lien : $linkCode"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager le lien d'invitation"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager le lien d'invitation", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Toggle QR code
                        OutlinedButton(
                            onClick = { showQrCode = !showQrCode },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showQrCode) "Masquer QR" else "Code QR", fontSize = 12.sp)
                        }

                        // Reset link
                        OutlinedButton(
                            onClick = {
                                onResetLink()
                                copiedToast = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réinitialiser", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JoinByInviteLinkDialog(
    onDismiss: () -> Unit,
    onJoin: (inviteUrl: String) -> Unit
) {
    var linkInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rejoindre via un lien",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Collez le lien d'invitation d'un groupe ou canal MBoté pour y accéder instantanément.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    placeholder = { Text("https://mbote.app/join/...", fontSize = 13.sp) },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { linkInput = it }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Coller", tint = MbotePurplePrimary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (linkInput.isNotBlank()) {
                            onJoin(linkInput)
                            onDismiss()
                        }
                    },
                    enabled = linkInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MbotePurplePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rejoindre le groupe", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
