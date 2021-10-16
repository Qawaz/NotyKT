/*
 * Copyright 2020 Shreyas Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.shreyaspatil.noty.composeapp.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.navigation.NavHostController
import dev.shreyaspatil.noty.composeapp.R
import dev.shreyaspatil.noty.composeapp.component.action.DeleteAction
import dev.shreyaspatil.noty.composeapp.component.action.ShareAction
import dev.shreyaspatil.noty.composeapp.component.action.ShareActionItem
import dev.shreyaspatil.noty.composeapp.component.action.ShareDropdown
import dev.shreyaspatil.noty.composeapp.component.dialog.FailureDialog
import dev.shreyaspatil.noty.composeapp.component.text.NoteField
import dev.shreyaspatil.noty.composeapp.component.text.NoteTitleField
import dev.shreyaspatil.noty.composeapp.utils.CaptureBitmap
import dev.shreyaspatil.noty.composeapp.utils.ShowToast
import dev.shreyaspatil.noty.composeapp.utils.saveImage
import dev.shreyaspatil.noty.composeapp.utils.shareImageUri
import dev.shreyaspatil.noty.core.ui.UIDataState
import dev.shreyaspatil.noty.utils.validator.NoteValidator
import dev.shreyaspatil.noty.view.viewmodel.NoteDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@Composable
fun NoteDetailsScreen(
    navController: NavHostController,
    viewModel: NoteDetailViewModel
) {
    val activity = LocalContext.current as Activity

    val updateState = viewModel.updateNoteState.collectAsState(initial = null)
    val deleteState = viewModel.deleteNoteState.collectAsState(initial = null)

    val note = viewModel.note.collectAsState(initial = null).value

    if (note != null) {
        var titleText by remember { mutableStateOf(note.title) }
        var noteText by remember { mutableStateOf(note.note) }
        var snapShot: () -> Bitmap? = { null }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Noty",
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                "Back",
                                tint = MaterialTheme.colors.onPrimary
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onPrimary,
                    elevation = 0.dp,
                    actions = {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()
                        DeleteAction(onClick = { viewModel.deleteNote() })
                        ShareAction(onClick = {
                            dropdownExpanded = true
                        })
                        ShareDropdown(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            },
                            shareActions = listOf(
                                ShareActionItem(
                                    label = "Text",
                                    onActionClick = {
                                        shareNote(activity, titleText, noteText)
                                    }
                                ),
                                ShareActionItem(
                                    label = "Image",
                                    onActionClick = {
                                        coroutineScope.launch {
                                            val bitmap = snapShot.invoke()
                                            if (bitmap == null) {
                                                Toast.makeText(
                                                    activity,
                                                    "Something Went Wrong!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }
                                            shareNoteImage(bitmap, activity)
                                        }
                                    }
                                ),
                            )
                        )
                    }
                )
            },
            content = {
                snapShot = CaptureBitmap {
                    Column(
                        Modifier
                            .scrollable(
                                rememberScrollState(),
                                orientation = Orientation.Vertical
                            )
                            .padding(16.dp)
                    ) {
                        NoteTitleField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.background),
                            value = titleText,
                            onTextChange = { titleText = it }
                        )

                        NoteField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(top = 8.dp)
                                .background(MaterialTheme.colors.background),
                            value = noteText,
                            onTextChange = { noteText = it }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (NoteValidator.isValidNote(titleText, noteText)) {
                    ExtendedFloatingActionButton(
                        text = { Text("Save", color = Color.White) },
                        icon = {
                            Icon(
                                Icons.Filled.Done,
                                "Save",
                                tint = Color.White
                            )
                        },
                        onClick = { viewModel.updateNote(titleText.trim(), noteText.trim()) },
                        backgroundColor = MaterialTheme.colors.primary
                    )
                } else {
                    ShowToast("Note title or note text are not valid!")
                }
            }
        )

        val registerOnStateChanged: @Composable (UIDataState<Unit>?) -> Unit = { state ->
            when (state) {
                is UIDataState.Success -> navController.navigateUp()
                is UIDataState.Failed -> FailureDialog(state.message)
            }
        }

        registerOnStateChanged(updateState.value)
        registerOnStateChanged(deleteState.value)
    }
}

fun shareNote(activity: Activity, title: String, note: String) {
    val shareMsg = activity.getString(
        R.string.text_message_share,
        title,
        note
    )

    val intent = ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(shareMsg)
        .intent

    activity.startActivity(Intent.createChooser(intent, null))
}

suspend fun shareNoteImage(bitmap: Bitmap, context: Context) {
    val uri = saveImage(bitmap, context)
    if (uri != null) {
        shareImageUri(context, uri)
    } else {
        Toast.makeText(
            context,
            "uri is null",
            Toast.LENGTH_SHORT
        ).show()
    }
}
