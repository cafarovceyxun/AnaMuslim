package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import kotlin.math.max

/**
 * iOS qarşılığı: `UIImagePickerController`-in qalereya rejimi.
 *
 * `PHPickerViewController` daha müasirdir, amma nəticəni `NSItemProvider.loadObjectOfClass` ilə
 * verir — Kotlin/Native interop-unda bu, tip-silinmiş `NSItemProviderReading` sinif obyektləri
 * tələb edir. Burada seçilən tək bir şəkildir, ona görə delegatı birbaşa `UIImage` qaytaran sadə
 * yol seçilib. Qalereyadan oxumaq iOS 11-dən bəri icazə tələb etmir (seçici ayrı prosesdə işləyir),
 * yəni `Info.plist`-ə açar əlavə edilmir.
 */

/**
 * Seçici delegatı **zəif** saxlayır — yalnız çağırış çərçivəsinin tutduğu delegat istifadəçi seçimi
 * bitirməmiş toplanardı və geri çağırış heç vaxt gəlməzdi. [TextDocumentIO] ilə eyni səbəb, eyni
 * həll: nəticə gələnə qədər burada saxlanılır.
 */
private val liveDelegates = mutableSetOf<NSObject>()

private class ImagePickerDelegate(
    private val onResult: (UIImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    private fun finish(controller: UIImagePickerController, image: UIImage?) {
        liveDelegates.remove(this)
        controller.dismissViewControllerAnimated(true) { onResult(image) }
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        finish(picker, didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        finish(picker, null)
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ImageBitmap?) -> Unit): ImagePicker {
    val currentOnPicked by rememberUpdatedState(onPicked)

    return remember {
        object : ImagePicker {
            override fun pick() {
                val root = UIApplication.sharedApplication.keyWindow?.rootViewController
                    // Redaktor tam ekran `Dialog`-dadır, yəni təqdim olunmuş kontroller var —
                    // seçici kökdən yox, ən üstdəkindən açılmalıdır, yoxsa görünmür.
                    ?.let { generateSequence(it) { vc -> vc.presentedViewController }.last() }
                    ?: run {
                        currentOnPicked(null)
                        return
                    }

                val delegate = ImagePickerDelegate { image ->
                    currentOnPicked(image?.let { toImageBitmap(it) })
                }
                liveDelegates.add(delegate)

                val controller = UIImagePickerController().apply {
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                    this.delegate = delegate
                }
                root.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

/**
 * `UIImage` → [ImageBitmap], Skia vasitəsilə (Compose MP iOS-da Skia ilə çəkir — [toUIImage] eyni
 * körpünün əks istiqamətidir). Şəkil əvvəlcə [PICKED_IMAGE_MAX_DIMENSION]-a qədər kiçildilir.
 */
@OptIn(ExperimentalForeignApi::class)
private fun toImageBitmap(image: UIImage): ImageBitmap? = try {
    val jpeg = UIImageJPEGRepresentation(downscaled(image), 0.9)
    jpeg?.toByteArray()?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
} catch (e: Exception) {
    AppLogger.saveError(e, "ImagePicker.toImageBitmap")
    null
}

@OptIn(ExperimentalForeignApi::class)
private fun downscaled(image: UIImage): UIImage {
    val size = image.size
    val (width, height) = size.useContents { width to height }
    val largest = max(width, height)
    val limit = PICKED_IMAGE_MAX_DIMENSION.toDouble()
    if (largest <= limit) return image

    val factor = limit / largest
    val target = CGSizeMake(width * factor, height * factor)

    // scale = 1.0: nəticə piksel-piksel `target` ölçüsündə olsun, ekran sıxlığına vurulmasın.
    UIGraphicsBeginImageContextWithOptions(target, opaque = true, scale = 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, width * factor, height * factor))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return resized ?: image
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = bytes?.readBytes(length.toInt()) ?: ByteArray(0)
