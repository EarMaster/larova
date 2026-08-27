package app.larova

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The photo picker.
 *
 * `PickVisualMedia` runs outside the app and hands back one picture with a temporary read
 * permission, which is why Larova needs no `READ_MEDIA_IMAGES` and never sees the rest of the
 * gallery. On phones without the system picker the same contract falls back to `OpenDocument`, so
 * there is nothing here to branch on.
 *
 * Images only. Video and recordings are M2, and offering a picker that accepts a film the app
 * cannot yet play would be a promise it does not keep.
 */
@Composable
fun rememberPicturePicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onPicked(it.toString()) } }

    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
