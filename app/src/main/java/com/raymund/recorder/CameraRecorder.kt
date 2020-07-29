package com.raymund.recorder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.IOException


class CameraRecorder : CameraEncoder {
    private var mMediaWidth: Int = 640
    private var mMediaHeight: Int = 480
    private var mMediaFramerate: Int = 30
    private var mInputSurface: Surface? = null

    private val mimeType = "video/avc"
    private val bitrate = 1000000
    private val iframeInterval = 10

    constructor(width: Int, height: Int, framerate: Int) : super() {
        mMediaWidth = width
        mMediaHeight = height
        mMediaFramerate = framerate

        var dirMov: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        var dirFinal: File = File(
            dirMov,
            "UvcMonitorTest"
        )
        dirFinal.mkdirs()
        if (dirFinal.canWrite()) {
            val randomFileNumber = (Math.random() * Math.floor(10000000.toDouble())).toInt().toString()
            setOutputFile(File(dirFinal, randomFileNumber + ".mp4").toString())
        }
    }

    public fun updateProperties(width: Int, height: Int, framerate: Int) {
        // TODO: Add code when code for the size of the video changes
    }

    public fun getInputSurface(): Surface? {
        return mInputSurface
    }

    @Throws(IOException::class)
    override fun prepare() {
        mTrackIndex = -1
        mMuxerStarted = false
        mIsCapturing = true
        mIsEOS = false

        val codecInfo = selectCodec(mimeType)
        if (codecInfo == null) {
            return
        }

        mBufferInfo = MediaCodec.BufferInfo()

        val format = MediaFormat.createVideoFormat(
            mimeType,
            mMediaWidth,
            mMediaHeight
        )

        // Set configuration, invalid configurations will crash app
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(
            MediaFormat.KEY_BIT_RATE,
            bitrate
        )
        format.setInteger(
            MediaFormat.KEY_FRAME_RATE,
            mMediaFramerate
        )
        format.setInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL,
            iframeInterval
        )

        // Create a MediaCodec encoder with specific configuration
        mMediaCodec = MediaCodec.createEncoderByType(mimeType)
        mMediaCodec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // Get Surface for input to encoder
        mInputSurface = mMediaCodec!!.createInputSurface() // API >= 18
        mMediaCodec!!.start()

        // Create MediaMuxer. You should never call #start here
        mMuxer = MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        if (mEncodeListener != null) {
            try {
                mEncodeListener!!.onPrepared(this)
            } catch (e: Exception) {
                Log.w(logTag, e)
            }
        }
    }

    override fun release() {
        super.release()

        if (mInputSurface != null) {
            mInputSurface!!.release()
            mInputSurface = null
        }
    }
}