package com.raymund.uvcmonitor

import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.raymund.recorder.CameraRecorder
import com.raymund.widget.CameraTextureView
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera

class MainActivity : BaseActivity(), CameraDialogParent {
    private var mUSBMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mRecorder: CameraRecorder? = null

    private var mCameraView: CameraTextureView? = null
    private var mCameraButton: ImageButton? = null
    private var mRecordButton: ImageButton? = null
    private var mPreviewSurface: Surface? = null

    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTED = 1
    private val STATE_RECORDING = 2
    private var mState: Int? = STATE_DISCONNECTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCameraButton = findViewById<View>(R.id.camera_button) as ImageButton
        mCameraButton!!.setOnClickListener(mDeviceOnClickListener)
        mRecordButton = findViewById(R.id.record_button) as ImageButton
        mRecordButton!!.setOnClickListener(mRecordOnClickListener)
        mCameraView = findViewById<View>(R.id.camera_texture_view) as CameraTextureView
        mCameraView!!.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (UVCCamera.DEFAULT_PREVIEW_HEIGHT).toDouble())
        mCameraView!!.surfaceTextureListener = mSurfaceTextureListener
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor!!.register()
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

    private fun destroyViewAndMonitor() {
        mCameraView = null
        mCameraButton = null
        mRecordButton = null
        if (mUSBMonitor != null) {
            mUSBMonitor!!.destroy()
            mUSBMonitor = null
        }
    }

    private fun destroyUVCCamera() {
        if (mUVCCamera != null) {
            mUVCCamera!!.destroy()
            mUVCCamera = null
        }
        if (mPreviewSurface != null) {
            mPreviewSurface!!.release()
            mPreviewSurface = null
        }
        mState = STATE_DISCONNECTED
    }

    private val mDeviceOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mUVCCamera == null) {
            CameraDialog.showDialog(this@MainActivity)
        } else {
            destroyUVCCamera()
        }
    }

    private val mRecordOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mState == STATE_CONNECTED) {
            mRecorder!!.startRecord();
        } else if (mState == STATE_RECORDING) {
            mRecorder!!.endRecord();
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
                Toast.makeText(this@MainActivity, "USB device detected", Toast.LENGTH_SHORT).show()
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
                    mUVCCamera = camera
                    mRecorder = CameraRecorder(
                        mPreviewSurface!!,
                        UVCCamera.DEFAULT_PREVIEW_WIDTH,
                        UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                    30)
                    mState = STATE_CONNECTED
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
                Toast.makeText(this@MainActivity, "USB device detached", Toast.LENGTH_SHORT).show()
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
        object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                // TODO: Add Video Encoder code here
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                if(mPreviewSurface != null) {
                    mPreviewSurface!!.release()
                    mPreviewSurface = null
                }
                return true
            }
        }
}