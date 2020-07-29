package com.raymund.uvcmonitor

import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import com.raymund.recorder.CameraEncoder
import com.raymund.recorder.CameraEncoder.EncodeListener
import com.raymund.recorder.CameraRecorder
import com.raymund.widget.CameraTextureView
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainActivity : BaseActivity(), CameraDialogParent {
    private val mLock = ReentrantLock()

    private var mUSBMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mEncoder: CameraRecorder? = null

    private var mCameraView: CameraTextureView? = null
    private var mCameraButton: ImageButton? = null
    private var mRecordButton: ImageButton? = null
    private var mPreviewSurface: Surface? = null

    private val logTag = "MainActivity"
    private val stateDisconnected = 0
    private val stateConnected = 1
    private val stateRecording = 2
    private val stateRecordPrepare = 3
    private val stateRecordStop = 4
    private var mState: Int = stateDisconnected

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCameraButton = findViewById(R.id.camera_button)
        mCameraButton!!.setOnClickListener(mDeviceOnClickListener)
        mRecordButton = findViewById(R.id.record_button)
        mRecordButton!!.setOnClickListener(mRecordOnClickListener)
        mCameraView = findViewById(R.id.camera_texture_view)
        mCameraView!!.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (UVCCamera.DEFAULT_PREVIEW_HEIGHT).toDouble())
        mCameraView!!.surfaceTextureListener = mSurfaceTextureListener
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        mLock.withLock {
            mUSBMonitor!!.register()
        }
    }

    override fun onStop() {
        mUSBMonitor!!.unregister()
        super.onStop()
    }

    override fun onDestroy() {
        destroyUVCCamera()
        destroyViewAndMonitor()
        super.onDestroy()
    }

    private fun toastUser(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun destroyViewAndMonitor() {
        mLock.withLock {
            mCameraView = null
            mCameraButton = null
            mRecordButton = null
            if (mUSBMonitor != null) {
                mUSBMonitor!!.destroy()
                mUSBMonitor = null
            }
        }
    }

    private fun destroyUVCCamera() {
        mLock.withLock {
            if (mUVCCamera != null) {
                mUVCCamera!!.destroy()
                mUVCCamera = null
            }
            if (mPreviewSurface != null) {
                mPreviewSurface!!.release()
                mPreviewSurface = null
            }
            mState = stateDisconnected
        }
    }

    private val mDeviceOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mUVCCamera == null) {
            CameraDialog.showDialog(this@MainActivity)
        } else {
            destroyUVCCamera()
        }
    }

    private val mRecordOnClickListener: View.OnClickListener = View.OnClickListener {
        if (checkPermissionWriteExternalStorage()) {
            if (mState == stateConnected || mState == stateRecordStop) {
                startCapture()
            } else if (mState == stateRecording) {
                stopCapture()
            }
        }
    }

    private val mOnDeviceConnectListener: OnDeviceConnectListener =
        object : OnDeviceConnectListener {
            private fun initCamera(camera: UVCCamera, mode: Int) {
                camera.setPreviewSize(
                    UVCCamera.DEFAULT_PREVIEW_WIDTH,
                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                    mode
                )
            }

            override fun onAttach(device: UsbDevice) {
                toastUser("USB device detected")
            }

            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock,
                createNew: Boolean
            ) {
                // Clear out the UVC Camera
                destroyUVCCamera()

                // Initialize the UVC Camera
                queueEvent(Runnable {
                    val camera = UVCCamera()
                    camera.open(ctrlBlock)
                    try {
                        // Attempt to use MJPEG mode
                        initCamera(camera, UVCCamera.FRAME_FORMAT_MJPEG)
                    } catch (e: IllegalArgumentException) {
                        try {
                            // Fallback to YUV mode
                            initCamera(camera, UVCCamera.DEFAULT_PREVIEW_MODE)
                        } catch (e1: IllegalArgumentException) {
                            camera.destroy()
                            return@Runnable
                        }
                    }
                    val surfaceTexture: SurfaceTexture = mCameraView!!.surfaceTexture
                    mPreviewSurface = Surface(surfaceTexture)
                    camera.setPreviewDisplay(mPreviewSurface)
                    camera.startPreview()

                    mLock.withLock {
                        mUVCCamera = camera
                        mState = stateConnected
                    }
                }, 0)
            }

            override fun onDisconnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock
            ) {
                queueEvent({
                    destroyUVCCamera()
                }, 0)
            }

            override fun onDettach(device: UsbDevice) {
                toastUser("USB device detached")
            }

            override fun onCancel(device: UsbDevice) {
            }
        }

    // Override of CameraDialogParent.getUSBMonitor
    override fun getUSBMonitor(): USBMonitor {
        return mUSBMonitor!!
    }

    // Override of CameraDialogParent.onDialogResult
    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            runOnUiThread({
                // TODO: Add layout updates here
            }, 0)
        }
    }

    private val mSurfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                if (mEncoder != null && mState == stateRecording) {
                    Log.i(logTag,"Called frameAvailable")
                    mEncoder!!.frameAvailable()
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                if (mPreviewSurface != null) {
                    mPreviewSurface!!.release()
                    mPreviewSurface = null
                }
                return true
            }
        }

    private val mEncodeListener: EncodeListener = object : EncodeListener {
        override fun onPrepared(encoder: CameraEncoder?) {
            mLock.withLock {
                if (mUVCCamera != null) {
                    mUVCCamera!!.startCapture((encoder as CameraRecorder).getInputSurface())
                }
                mState = stateRecording
            }
            Log.i(logTag,"Finished preparing")
        }

        override fun onRelease(encoder: CameraEncoder?) {
            mLock.withLock {
                if (mUVCCamera != null) {
                    mUVCCamera!!.stopCapture()
                }
                mState = stateRecordStop
            }
            Log.i(logTag,"Finished releasing")
        }
    }

    private fun startCapture() {
        if (mEncoder == null && (mState == stateConnected || mState == stateRecordStop)) {
            mState = stateRecordPrepare
            queueEvent({
                mEncoder = CameraRecorder(
                    UVCCamera.DEFAULT_PREVIEW_WIDTH,
                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                    30
                )
                mEncoder!!.setEncodeListener(mEncodeListener)
                try {
                    mEncoder!!.prepare()
                    mEncoder!!.startRecording()
                    toastUser("Started Recording")
                } catch (e: IOException) {
                    mState = stateRecordStop
                    Log.e(logTag, "Failed startCapture", e)
                }
            }, 0)
        }
    }

    private fun stopCapture() {
        queueEvent({
            mLock.withLock {
                if (mUVCCamera != null) {
                    mUVCCamera!!.stopCapture()
                }
                if (mEncoder != null) {
                    mEncoder!!.stopRecording()
                    mEncoder = null
                }
            }
            toastUser("Stopped Recording")
        }, 0)
    }
}