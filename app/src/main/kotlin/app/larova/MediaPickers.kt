package app.larova

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * The photo picker.
 *
 * `PickVisualMedia` runs outside the app and hands back one item with a temporary read permission,
 * which is why Larova needs no `READ_MEDIA_IMAGES` and never sees the rest of the gallery. On
 * phones without the system picker the same contract falls back to `OpenDocument`, so there is
 * nothing here to branch on.
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

/**
 * The same picker, asking for a video.
 *
 * One contract for both kinds, so a phone without the system picker falls back to `OpenDocument`
 * for videos exactly as it does for pictures, and neither needs a media permission.
 */
@Composable
fun rememberVideoPicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onPicked(it.toString()) } }

    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
            )
        }
    }
}

/**
 * Sound files, through the document picker.
 *
 * `PickVisualMedia` is exactly that — visual — so a recording cannot come from it.
 * `OpenDocument` filtered to audio types shows what the phone has without asking for a permission
 * either, and it reaches a file somebody was sent as easily as one in the music folder.
 *
 * Recording a new one is its own thing and needs the microphone. This is only for what exists.
 */
@Composable
fun rememberSoundPicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onPicked(it.toString()) } }

    return remember(launcher) { { launcher.launch(arrayOf("audio/*")) } }
}

/**
 * The microphone, asked for at the moment it is needed.
 *
 * Returns a function that either runs [onGranted] straight away or puts the system dialog in front
 * of it. Asked when somebody taps record and never on first launch: a permission requested before
 * there is anything to explain it is a permission people refuse, and this one they only need if
 * they want to record their own voice.
 *
 * A refusal is silent here. The dialog said what it was for, the person said no, and repeating the
 * question is what makes an app feel like it is arguing.
 */
@Composable
fun rememberMicrophoneRequest(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onGranted() }

    return remember(launcher, context, onGranted) {
        {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) onGranted() else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
