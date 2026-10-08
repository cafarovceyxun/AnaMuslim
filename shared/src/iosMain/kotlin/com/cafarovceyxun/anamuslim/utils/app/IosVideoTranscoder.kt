package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVLinearPCMIsNonInterleaved
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFoundation.AVAssetReader
import platform.AVFoundation.AVAssetReaderStatusCompleted
import platform.AVFoundation.AVAssetReaderStatusReading
import platform.AVFoundation.AVAssetReaderTrackOutput
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVVideoAverageBitRateKey
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoCodecTypeHEVC
import platform.AVFoundation.AVVideoCompressionPropertiesKey
import platform.AVFoundation.AVVideoExpectedSourceFrameRateKey
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoMaxKeyFrameIntervalKey
import platform.AVFoundation.AVVideoProfileLevelH264HighAutoLevel
import platform.AVFoundation.AVVideoProfileLevelKey
import platform.AVFoundation.AVVideoWidthKey
import platform.AVFoundation.duration
import platform.AVFoundation.AVAssetReaderOutput
import platform.AVFoundation.AVAssetReaderVideoCompositionOutput
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVMutableVideoCompositionInstruction
import platform.AVFoundation.AVMutableVideoCompositionLayerInstruction
import platform.AVFoundation.naturalSize
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.tracksWithMediaType
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGRectApplyAffineTransform
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeRangeFromTimeToTime
import platform.CoreMedia.CMTimeRangeMake
import kotlin.math.abs
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreMedia.CMSampleBufferCreateCopyWithNewTiming
import platform.CoreMedia.CMSampleBufferGetPresentationTimeStamp
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.CMSampleBufferRefVar
import platform.CoreMedia.CMSampleTimingInfo
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeInvalid
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSURL
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.darwin.dispatch_group_t
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t

/**
 * Hekayə videosunu Android-dəki `Transformer` ilə **eyni parametrlərlə** kodlayır: HEVC (yoxdursa
 * H.264 High), qısa kənar [MediaPickLimits.TARGET_VIDEO_SHORT_SIDE], ən çox
 * [MediaPickLimits.TARGET_VIDEO_FRAME_RATE] fps, bitrate [MediaPickLimits.targetVideoBitrate] —
 * üstəlik redaktorun seçimləri ([VideoEdit]: aralıq, kadr, səs).
 *
 * `AVAssetExportSession` əvəzinə reader/writer: export session bitrate qəbul etmir, yalnız hazır
 * preset — `960x540` və ya uzun videoda «Medium» (~360p) idi və ekran yazısının mətni oxunmurdu.
 *
 * Kadr `AVVideoComposition` ilə çəkilir: fırlanma, kəsmə və miqyas bir transformdadır, kadr
 * sürəti isə `frameDuration`-dır. Nəticədə piksellər artıq göstərilən istiqamətdədir (fayla
 * `transform` yazılmır) — Android pleyerləri də onu eyni cür görür.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosVideoTranscoder {

    /** [onDone] arxa fon növbəsində, uğur/uğursuzluqla **bir dəfə** çağırılır. */
    fun transcode(
        source: NSURL,
        output: NSURL,
        edit: VideoEdit,
        onProgress: (Float) -> Unit,
        onDone: (Boolean) -> Unit,
    ) {
        val started = runCatching { start(source, output, edit, onProgress, onDone) }
            .onFailure { AppLogger.d(TAG, "Transcode setup failed: ${it.message}") }
            .getOrDefault(false)
        if (!started) onDone(false)
    }

    private fun start(
        source: NSURL,
        output: NSURL,
        edit: VideoEdit,
        onProgress: (Float) -> Unit,
        onDone: (Boolean) -> Unit,
    ): Boolean {
        val asset = AVURLAsset(uRL = source, options = null)
        val videoTrack = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
            ?: return false
        val audioTrack = (asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack)
            ?.takeUnless { edit.muted }

        val startSeconds = edit.startMillis / 1000.0
        val endSeconds = edit.endMillis / 1000.0
        val startTime = CMTimeMakeWithSeconds(startSeconds, preferredTimescale = TIMESCALE)
        val endTime = CMTimeMakeWithSeconds(endSeconds, preferredTimescale = TIMESCALE)

        val reader = AVAssetReader(asset = asset, error = null)
        reader.timeRange = CMTimeRangeFromTimeToTime(startTime, endTime)
        val writer = AVAssetWriter(uRL = output, fileType = AVFileTypeMPEG4, error = null)
        // `moov` faylın əvvəlinə — pleyer faylın axırını gözləmədən başlayır.
        writer.shouldOptimizeForNetworkUse = true

        val composition = videoComposition(asset, videoTrack, edit.crop)
        val (width, height) = composition.renderSize.useContents { width.toInt() to height.toInt() }

        val videoOutput = AVAssetReaderVideoCompositionOutput(
            videoTracks = listOf(videoTrack),
            videoSettings = mapOf<Any?, Any?>(
                cfKey(kCVPixelBufferPixelFormatTypeKey) to
                    kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange.toInt(),
            ),
        ).apply {
            videoComposition = composition
            alwaysCopiesSampleData = false
        }

        val videoInput = AVAssetWriterInput(
            mediaType = AVMediaTypeVideo,
            outputSettings = videoSettings(
                writer,
                width,
                height,
                MediaPickLimits.targetVideoBitrate(edit.durationMillis),
            ),
        ).apply { expectsMediaDataInRealTime = false }

        if (!reader.canAddOutput(videoOutput) || !writer.canAddInput(videoInput)) return false
        reader.addOutput(videoOutput)
        writer.addInput(videoInput)

        // Səs alınmasa video səssiz gedir — bütöv uğursuzluqdan yaxşıdır.
        val audio = audioTrack?.let { track ->
            val audioOutput = AVAssetReaderTrackOutput(track = track, outputSettings = PCM_SETTINGS)
            val audioInput = AVAssetWriterInput(
                mediaType = AVMediaTypeAudio,
                outputSettings = AAC_SETTINGS,
            ).apply { expectsMediaDataInRealTime = false }

            if (reader.canAddOutput(audioOutput) && writer.canAddInput(audioInput)) {
                reader.addOutput(audioOutput)
                writer.addInput(audioInput)
                audioOutput to audioInput
            } else {
                AppLogger.d(TAG, "Audio track skipped")
                null
            }
        }

        if (!reader.startReading()) {
            AppLogger.d(TAG, "Reader failed: ${reader.error?.localizedDescription}")
            return false
        }
        if (!writer.startWriting()) {
            AppLogger.d(TAG, "Writer failed: ${writer.error?.localizedDescription}")
            reader.cancelReading()
            return false
        }
        writer.startSessionAtSourceTime(startTime)

        val group = dispatch_group_create()

        // Kadr atma: əvvəlki saxlanılan kadrdan bir intervaldan az keçibsə atılır. Kompozisiya
        // onsuz da 30 fps verir — bu, `frameDuration`-a güvənməyən sığortadır.
        val minFrameInterval = 1.0 / MediaPickLimits.TARGET_VIDEO_FRAME_RATE - FRAME_TOLERANCE_SECONDS
        var lastKeptSeconds = Double.NEGATIVE_INFINITY
        var lastKept: CMSampleBufferRef? = null
        val spanSeconds = (endSeconds - startSeconds).coerceAtLeast(0.001)
        pump(
            videoInput,
            videoOutput,
            queue("video"),
            group,
            reader,
            keep = { buffer ->
                val seconds = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(buffer))
                val keep = !seconds.isFinite() || seconds - lastKeptSeconds >= minFrameInterval
                if (keep && seconds.isFinite()) {
                    lastKeptSeconds = seconds
                    lastKept?.let(::CFRelease)
                    lastKept = buffer.also { CFRetain(it) }
                    onProgress(((seconds - startSeconds) / spanSeconds).toFloat().coerceIn(0f, 1f))
                }
                keep
            },
            tail = {
                // Ekran yazısı dəyişkən kadr sürətlidir: ekran sakit qalanda kadr gəlmir və trek
                // son kadrda bitir — 12.6 s-lik yazı 5.4 s çıxırdı (səs treki olanda
                // `endSessionAtSourceTime` də uzatmır). Son kadr aralığın sonunda bir də yazılır.
                val last = lastKept
                lastKept = null
                val tailSeconds = endSeconds - 1.0 / MediaPickLimits.TARGET_VIDEO_FRAME_RATE
                val appended = if (last != null && tailSeconds - lastKeptSeconds >= minFrameInterval) {
                    appendRetimed(videoInput, last, tailSeconds)
                } else {
                    true
                }
                last?.let(::CFRelease)
                appended
            },
        )
        audio?.let { (audioOutput, audioInput) ->
            pump(audioInput, audioOutput, queue("audio"), group, reader, keep = { true }, tail = { true })
        }

        dispatch_group_notify(group, queue("finish")) {
            if (reader.status != AVAssetReaderStatusCompleted) {
                AppLogger.d(TAG, "Reading stopped: ${reader.error?.localizedDescription}")
                writer.cancelWriting()
                onDone(false)
            } else {
                writer.endSessionAtSourceTime(endTime)
                writer.finishWritingWithCompletionHandler {
                    val ok = writer.status == AVAssetWriterStatusCompleted
                    if (!ok) AppLogger.d(TAG, "Writing failed: ${writer.error?.localizedDescription}")
                    onDone(ok)
                }
            }
        }
        return true
    }

    /**
     * Bir trek üçün oxu → yaz dövrü. Writer hazır olduqca blok təkrar çağırılır; trek bitəndə
     * [tail] (writer hazır ikən) bir dəfə işləyir, sonra və ya yazı alınmayanda qrupdan **bir
     * dəfə** çıxılır. Xəta halında reader dayandırılır — o biri trekin dövrü də `null` alıb bitir,
     * nəticəni isə `notify` reader statusundan oxuyur.
     */
    private fun pump(
        input: AVAssetWriterInput,
        output: AVAssetReaderOutput,
        queue: dispatch_queue_t,
        group: dispatch_group_t,
        reader: AVAssetReader,
        keep: (CMSampleBufferRef) -> Boolean,
        tail: () -> Boolean,
    ) {
        dispatch_group_enter(group)
        var finished = false
        var draining = false

        fun finish() {
            if (finished) return
            finished = true
            input.markAsFinished()
            dispatch_group_leave(group)
        }

        input.requestMediaDataWhenReadyOnQueue(queue) {
            while (!finished && input.readyForMoreMediaData) {
                if (draining) {
                    if (!tail()) reader.cancelReading()
                    finish()
                    break
                }
                // `copy…` +1 qaytarır — buraxmaq bizim işimizdir.
                val buffer = output.copyNextSampleBuffer()
                if (buffer == null) {
                    // Reader xətası ilə bitibsə quyruq yazılmır.
                    if (reader.status == AVAssetReaderStatusReading || reader.status == AVAssetReaderStatusCompleted) {
                        draining = true
                        continue
                    }
                    finish()
                    break
                }
                val appended = !keep(buffer) || input.appendSampleBuffer(buffer)
                CFRelease(buffer)
                if (!appended) {
                    reader.cancelReading()
                    finish()
                }
            }
        }
    }

    /** Kadrın surəti, yeni vaxt damğası ilə (müddət bir kadr). Piksellər kopyalanmır. */
    private fun appendRetimed(
        input: AVAssetWriterInput,
        buffer: CMSampleBufferRef,
        seconds: Double,
    ): Boolean = memScoped {
        val timing = alloc<CMSampleTimingInfo>()
        CMTimeMake(value = 1, timescale = MediaPickLimits.TARGET_VIDEO_FRAME_RATE).place(timing.duration.ptr)
        CMTimeMakeWithSeconds(seconds, preferredTimescale = TIMESCALE).place(timing.presentationTimeStamp.ptr)
        kCMTimeInvalid.readValue().place(timing.decodeTimeStamp.ptr)

        val copy = alloc<CMSampleBufferRefVar>()
        val status = CMSampleBufferCreateCopyWithNewTiming(null, buffer, 1L, timing.ptr, copy.ptr)
        val retimed = copy.value
        if (status != 0 || retimed == null) {
            AppLogger.d(TAG, "Tail frame copy failed: $status")
            return@memScoped true // quyruq olmasa da video yararlıdır
        }
        val appended = input.appendSampleBuffer(retimed)
        CFRelease(retimed)
        appended
    }

    /**
     * Göstərilən kadr → [crop] → qısa kənarı 720-yə miqyas. Transformlar ardıcıl birləşir
     * (`Concat(a, b)` = əvvəl a, sonra b): `preferredTransform` kadrı fırladır, ardınca fırlanmış
     * çərçivənin mənfi mənşəyi sıfıra çəkilir, kəsilən hissə mənşəyə sürüşür və miqyaslanır.
     */
    private fun videoComposition(
        asset: AVURLAsset,
        track: AVAssetTrack,
        crop: VideoCrop,
    ): AVMutableVideoComposition {
        val preferred = track.preferredTransform
        val (originX, originY, displayWidth, displayHeight) = track.naturalSize.useContents {
            CGRectApplyAffineTransform(CGRectMake(0.0, 0.0, width, height), preferred)
        }.useContents { listOf(origin.x, origin.y, abs(size.width), abs(size.height)) }

        val cropX = crop.left * displayWidth
        val cropY = crop.top * displayHeight
        val cropWidth = ((crop.right - crop.left) * displayWidth).coerceAtLeast(2.0)
        val cropHeight = ((crop.bottom - crop.top) * displayHeight).coerceAtLeast(2.0)

        val (targetWidth, targetHeight) = MediaPickLimits.scaledSize(
            cropWidth.toInt(),
            cropHeight.toInt(),
            MediaPickLimits.TARGET_VIDEO_SHORT_SIDE,
        ) ?: (cropWidth.toInt().even() to cropHeight.toInt().even())

        var transform = CGAffineTransformConcat(
            preferred,
            CGAffineTransformMakeTranslation(-originX, -originY),
        )
        transform = CGAffineTransformConcat(transform, CGAffineTransformMakeTranslation(-cropX, -cropY))
        transform = CGAffineTransformConcat(
            transform,
            CGAffineTransformMakeScale(targetWidth / cropWidth, targetHeight / cropHeight),
        )

        val layer = AVMutableVideoCompositionLayerInstruction
            .videoCompositionLayerInstructionWithAssetTrack(track)
            .apply { setTransform(transform, atTime = CMTimeMake(value = 0, timescale = 1)) }

        val instruction = AVMutableVideoCompositionInstruction().apply {
            // Mutable alt sinfin setter-ləri K/N-də xassə kimi görünmür — açıq çağırılır.
            setTimeRange(CMTimeRangeMake(CMTimeMake(value = 0, timescale = 1), asset.duration))
            setLayerInstructions(listOf(layer))
        }

        return AVMutableVideoComposition().apply {
            setRenderSize(CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble()))
            setFrameDuration(CMTimeMake(value = 1, timescale = MediaPickLimits.TARGET_VIDEO_FRAME_RATE))
            setInstructions(listOf(instruction))
        }
    }

    private fun videoSettings(
        writer: AVAssetWriter,
        width: Int,
        height: Int,
        bitrate: Int,
    ): Map<Any?, Any?> {
        fun settings(codec: String?): Map<Any?, Any?> = mapOf(
            AVVideoCodecKey to codec,
            AVVideoWidthKey to width,
            AVVideoHeightKey to height,
            AVVideoCompressionPropertiesKey to buildMap<Any?, Any?> {
                put(AVVideoAverageBitRateKey, bitrate)
                put(AVVideoExpectedSourceFrameRateKey, MediaPickLimits.TARGET_VIDEO_FRAME_RATE)
                put(AVVideoMaxKeyFrameIntervalKey, MediaPickLimits.TARGET_VIDEO_FRAME_RATE * 2)
                if (codec == AVVideoCodecTypeH264) {
                    put(AVVideoProfileLevelKey, AVVideoProfileLevelH264HighAutoLevel)
                }
            },
        )

        val hevc = settings(AVVideoCodecTypeHEVC)
        return if (writer.canApplyOutputSettings(hevc, forMediaType = AVMediaTypeVideo)) {
            hevc
        } else {
            AppLogger.d(TAG, "HEVC unavailable, falling back to H.264")
            settings(AVVideoCodecTypeH264)
        }
    }

    private fun queue(name: String): dispatch_queue_t =
        dispatch_queue_create("com.cafarovceyxun.anamuslim.transcode.$name", null)

    /** CoreVideo açarı `CFStringRef`-dir; lüğətə `NSString` kimi düşsün deyə körpülənir. */
    private fun cfKey(key: platform.CoreFoundation.CFStringRef?): Any? =
        CFBridgingRelease(CFRetain(key))

    private fun Int.even(): Int = (this / 2).coerceAtLeast(1) * 2

    /** Mono mənbə də stereo AAC-yə çevrilir — reader özü qarışdırır. */
    private val PCM_SETTINGS: Map<Any?, Any?> = mapOf(
        AVFormatIDKey to kAudioFormatLinearPCM.toInt(),
        AVSampleRateKey to AUDIO_SAMPLE_RATE,
        AVNumberOfChannelsKey to AUDIO_CHANNELS,
        AVLinearPCMBitDepthKey to 16,
        AVLinearPCMIsFloatKey to false,
        AVLinearPCMIsBigEndianKey to false,
        AVLinearPCMIsNonInterleaved to false,
    )

    private val AAC_SETTINGS: Map<Any?, Any?> = mapOf(
        AVFormatIDKey to kAudioFormatMPEG4AAC.toInt(),
        AVSampleRateKey to AUDIO_SAMPLE_RATE,
        AVNumberOfChannelsKey to AUDIO_CHANNELS,
        AVEncoderBitRateKey to MediaPickLimits.AUDIO_BITRATE,
    )

    private const val TIMESCALE = 600
    private const val AUDIO_SAMPLE_RATE = 44_100.0
    private const val AUDIO_CHANNELS = 2

    /** Dəyişkən kadr sürətli ekran yazısında 33.3 ms-lik kadrı «tez gəldi» deyə atmamaq üçün. */
    private const val FRAME_TOLERANCE_SECONDS = 0.002

    private const val TAG = "IosVideoTranscoder"
}
