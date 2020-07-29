package com.raymund.recorder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaMuxer
import android.util.Log
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class CameraEncoder : Runnable {
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private val TIMEOUT_USEC = 10000 // 10 milliseconds
    protected val TAG = "CameraEncoder"

    //********************************************************************************
    /**
     * Flag that indicate this encoder is capturing now.
     */
    @Volatile
    protected var mIsCapturing = false
        protected set

    /**
     * Flag that indicate the frame data will be available soon.
     */
    private var mRequestDrain = 0

    /**
     * Flag to request stop capturing
     */
    @Volatile
    protected var mRequestStop = false

    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected var mIsEOS = false

    /**
     * Flag the indicate the muxer is running
     */
    protected var mMuxerStarted = false

    /**
     * Track Number
     */
    protected var mTrackIndex = 0

    /**
     * MediaCodec instance for encoding
     */
    protected var mMediaCodec // API >= 16(Android4.1.2)
            : MediaCodec? = null
    protected var mEncodeListener
            : EncodeListener? = null
    protected var mBufferInfo // API >= 16(Android4.1.2)
            : MediaCodec.BufferInfo? = null
    protected var mOutputPath
            : String? = null
    protected var mMuxer // API >= 18
            : MediaMuxer? = null

    interface EncodeListener {
        /**
         * callback after finishing initialization of encoder
         * @param encoder
         */
        fun onPrepared(encoder: CameraEncoder?)

        /**
         * callback before releasing encoder
         * @param encoder
         */
        fun onRelease(encoder: CameraEncoder?)
    }

    constructor() {
        mLock.withLock {
            Thread(this, javaClass.simpleName).start()
            try {
                // wait for starting thread
                mCondition.await()
            } catch (e: InterruptedException) {
            }
        }
    }

    fun startRecording() {
        mLock.withLock {
            mIsCapturing = true
            mRequestStop = false
            mCondition.signalAll()
        }
    }

    /**
     * the method to request stop encoding
     */
    fun stopRecording() {
        mRequestStop = true
        mLock.withLock {
            if (!mIsCapturing) {
                return
            }
            mCondition.signalAll()
        }
    }

    fun setOutputFile(filePath: String?) {
        mOutputPath = filePath
        Log.i(TAG, filePath)
    }

    @Throws(IOException::class)
    abstract fun prepare()
    fun setEncodeListener(listener: EncodeListener?) {
        mEncodeListener = listener
    }

    /**
     * notify to frame data will arrive soon or already arrived.
     * (request to process frame data)
     */
    fun frameAvailable(): Boolean {
        mLock.withLock {
            if (!mIsCapturing || mRequestStop) {
                return false
            }
            mRequestDrain++
            mCondition.signalAll()
        }
        return true
    }

    /**
     * encoding loop on private thread
     */
    override fun run() {
        var isRunning: Boolean? = true
        var localRequestStop: Boolean? = false
        var localRequestDrain: Boolean? = false

        mLock.withLock {
            mRequestStop = false
            mRequestDrain = 0
            localRequestStop = mRequestStop
            localRequestDrain = (mRequestDrain > 0)
            mCondition.signal()
        }

        WHILE@ while (isRunning!!) {
            if (localRequestStop!!) {
                drain()
                // Request stop recording
                signalEndOfInputStream()
                // Process output data again for EOS signal
                drain()
                // Release all related objects
                release()
                // Signal to stop
                isRunning = false
                return@WHILE
            }

            if (localRequestDrain!!) {
                drain()
            } else {
                mLock.withLock {
                    try {
                        mCondition.await()
                    } catch (e: InterruptedException) {
                        isRunning = false
                        //return@WHILE
                    }
                }
            }

            mLock.withLock {
                localRequestStop = mRequestStop
                localRequestDrain = mRequestDrain > 0

                if (localRequestDrain!!) {
                    mRequestDrain--
                }
            }
        }

        mLock.withLock {
            mRequestStop = true
            mIsCapturing = false
        }
    }
    //********************************************************************************
    /**
     * Release all releated objects
     */
    protected open fun release() {
        try {
            mEncodeListener!!.onRelease(this)
        } catch (e: Exception) {
            Log.e(TAG, "failed onStopped", e)
        }

        mIsCapturing = false

        if (mMediaCodec != null) {
            try {
                mMediaCodec!!.stop()
                mMediaCodec!!.release()
                mMediaCodec = null
            } catch (e: Exception) {
                Log.e(TAG, "failed releasing MediaCodec", e)
            }
        }

        if (mMuxerStarted) {
            if (mMuxer != null) {
                try {
                    mMuxer!!.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "failed stopping muxer", e)
                }
            }
        }

        mBufferInfo = null
    }

    protected fun signalEndOfInputStream() {
        // signalEndOfInputStream is only available for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
        encode(null, 0, pTSUs)
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     * @param buffer
     * @param length　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected fun encode(
        buffer: ByteArray?,
        length: Int,
        presentationTimeUs: Long
    ) {
        if (!mIsCapturing) return
        var ix = 0
        var sz: Int
        val inputBuffers = mMediaCodec!!.inputBuffers
        while (mIsCapturing && ix < length) {
            val inputBufferIndex =
                mMediaCodec!!.dequeueInputBuffer(TIMEOUT_USEC.toLong())
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                inputBuffer.clear()
                sz = inputBuffer.remaining()
                sz = if (ix + sz < length) sz else length - ix
                if (sz > 0 && buffer != null) {
                    inputBuffer.put(buffer, ix, sz)
                }
                ix += sz

                if (length <= 0) {
                    // send EOS
                    mIsEOS = true
                    mMediaCodec!!.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    break
                } else {
                    mMediaCodec!!.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        sz,
                        presentationTimeUs,
                        0
                    )
                }
            }
            // inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER
            // wait for MediaCodec encoder is ready to encode
            // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
            // will wait for maximum TIMEOUT_USEC(10msec) on each call
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    protected fun drain() {
        if (mMediaCodec == null) return

        var encoderOutputBuffers = mMediaCodec!!.outputBuffers
        var encoderStatus: Int
        var count = 0

        if (mMuxer == null) {
            Log.w(TAG, "muxer is unexpectedly null");
            return
        }

        LOOP@ while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec!!.dequeueOutputBuffer(
                mBufferInfo,
                TIMEOUT_USEC.toLong()
            )
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5) break@LOOP  // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // this should not come when encoding
                encoderOutputBuffers = mMediaCodec!!.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {    // second time request is error
                    throw RuntimeException("format changed twice")
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                val format = mMediaCodec!!.outputFormat // API >= 16
                mTrackIndex = mMuxer!!.addTrack(format)
                mMuxerStarted = true
                mMuxer!!.start()
            } else if (encoderStatus < 0) {
                // unexpected status
            } else {
                val encodedData = encoderOutputBuffers[encoderStatus]
                    ?: // this never should come...may be a MediaCodec internal error
                    throw RuntimeException("encoderOutputBuffer $encoderStatus was null")
                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // You should set output format to muxer here when you target Android 4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API >= 18(>= Android 4.3), just ignore this flag here
                    mBufferInfo!!.size = 0
                }
                if (mBufferInfo!!.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        throw RuntimeException("drain:muxer hasn't started")
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo!!.presentationTimeUs = pTSUs
                    mMuxer!!.writeSampleData(mTrackIndex, encodedData, mBufferInfo)
                    prevOutputPTSUs = mBufferInfo!!.presentationTimeUs
                }
                // return buffer to encoder
                mMediaCodec!!.releaseOutputBuffer(encoderStatus, false)
                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    // when EOS come.
                    mIsCapturing = false
                    mMuxerStarted = mIsCapturing
                    break // out of while
                }
            }
        }
    }

    /**
     * previous presentationTimeUs for writing
     */
    private var prevOutputPTSUs: Long = 0
    // presentationTimeUs should be monotonic
    // otherwise muxer fail to write

    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected val pTSUs: Long
        protected get() {
            var result = System.nanoTime() / 1000L
            // presentationTimeUs should be monotonic
            // otherwise muxer fail to write
            if (result < prevOutputPTSUs) result = prevOutputPTSUs - result + result
            return result
        }

    /**
     * select primary codec for encoding from the available list which MIME is specific type
     * return null if nothing is available
     * @param mimeType
     */
    fun selectCodec(mimeType: String?): MediaCodecInfo? {
        var result: MediaCodecInfo? = null

        // get avcodec list
        val numCodecs = MediaCodecList.getCodecCount()
        LOOP@ for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skip decoder
                continue
            }

            // select encoder that MIME is equal to the specific type
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    result = codecInfo
                    break@LOOP
                }
            }
        }
        return result
    }
    //********************************************************************************
}
