package com.loukatech.mbote.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loukatech.mbote.model.SyncedContact
import com.loukatech.mbote.ui.theme.PurpleDark
import com.loukatech.mbote.ui.theme.PurplePrimary

data class DialpadKey(
    val mainText: String,
    val subText: String = "",
    val isSpecial: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallDialpadView(
    dialedNumber: String,
    onNumberChange: (String) -> Unit,
    onStartCall: (phoneNumber: String, isVideo: Boolean) -> Unit,
    contacts: List<SyncedContact> = emptyList(),
    onSelectContact: (SyncedContact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val dialpadKeys = listOf(
        listOf(DialpadKey("1", "➿"), DialpadKey("2", "ABC"), DialpadKey("3", "DEF")),
        listOf(DialpadKey("4", "GHI"), DialpadKey("5", "JKL"), DialpadKey("6", "MNO")),
        listOf(DialpadKey("7", "PQRS"), DialpadKey("8", "TUV"), DialpadKey("9", "WXYZ")),
        listOf(DialpadKey("*", ""), DialpadKey("0", "+"), DialpadKey("#", ""))
    )

    // Filter matching contacts as user types
    val matchingContacts = remember(dialedNumber, contacts) {
        if (dialedNumber.length >= 2) {
            val cleaned = dialedNumber.replace(" ", "").replace("-", "")
            contacts.filter { contact ->
                val contactCleaned = contact.phoneNumber.replace(" ", "").replace("-", "")
                contactCleaned.contains(cleaned) || contact.name.contains(dialedNumber, ignoreCase = true)
            }.take(5)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1016))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Display Area for dialed number + Backspace
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Suggested contacts when typing
            AnimatedVisibility(
                visible = matchingContacts.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchingContacts) { contact ->
                        Surface(
                            onClick = {
                                onNumberChange(contact.phoneNumber)
                                onSelectContact(contact)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = contact.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Display Number & Clear Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (dialedNumber.isEmpty()) "Composez un numéro" else dialedNumber,
                    fontSize = if (dialedNumber.length > 12) 24.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dialedNumber.isEmpty()) Color.White.copy(alpha = 0.35f) else Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )

                if (dialedNumber.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (dialedNumber.isNotEmpty()) {
                                onNumberChange(dialedNumber.dropLast(1))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("dialpad_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Effacer",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 3x4 Dialpad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dialpadKeys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { key ->
                        DialpadKeyButton(
                            key = key,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onNumberChange(dialedNumber + key.mainText)
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                if (key.mainText == "0") {
                                    onNumberChange(dialedNumber + "+")
                                } else if (key.mainText == "1") {
                                    // Voicemail speed dial
                                    onStartCall("Messagerie Vocale", false)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Call Action Buttons (Voice Call Green pill + Video Call pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green Call Button (Exact shape and style from Image 1)
            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    val numberToCall = if (dialedNumber.isNotBlank()) dialedNumber else "Grace Makiese"
                    onStartCall(numberToCall, false)
                },
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF22C55E), // Material green
                shadowElevation = 4.dp,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 140.dp)
                    .testTag("dialpad_call_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Appeler",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Call",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Quick Video Call Option Button
            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    val numberToCall = if (dialedNumber.isNotBlank()) dialedNumber else "Grace Makiese"
                    onStartCall(numberToCall, true)
                },
                shape = RoundedCornerShape(32.dp),
                color = PurplePrimary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
                    .testTag("dialpad_video_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Appel vidéo HD",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadKeyButton(
    key: DialpadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFF1B1D28),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .size(width = 86.dp, height = 58.dp)
            .clip(RoundedCornerShape(36.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("dialpad_key_${key.mainText}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.mainText,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 24.sp
            )
            if (key.subText.isNotEmpty()) {
                Text(
                    text = key.subText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
