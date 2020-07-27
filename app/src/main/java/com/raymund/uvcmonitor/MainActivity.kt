package com.raymund.uvcmonitor

import android.R
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera


class MainActivity : BaseActivity(), CameraDialogParent {
    private val mSync = Any()

    // for accessing USB and USB camera
    private var mUSBMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mUVCCameraView: SurfaceView? = null

    // for open&start / stop&close camera preview
    private var mCameraButton: ImageButton? = null
    private var mPreviewSurface: Surface? = null
    private var isActive = false
    private var isPreview = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCameraButton = findViewById<View>(R.id.camera_button) as ImageButton
        mCameraButton!!.setOnClickListener(mOnClickListener)
        mUVCCameraView = findViewById<View>(R.id.camera_surface_view) as SurfaceView
        mUVCCameraView!!.holder.addCallback(mSurfaceViewCallback)
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        if (DEBUG) Log.v(TAG, "onStart:")
        synchronized(mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor!!.register()
            }
        }
    }

    override fun onStop() {
        if (DEBUG) Log.v(TAG, "onStop:")
        synchronized(mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor!!.unregister()
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:")
        synchronized(mSync) {
            isPreview = false
            isActive = isPreview
            if (mUVCCamera != null) {
                mUVCCamera!!.destroy()
                mUVCCamera = null
            }
            if (mUSBMonitor != null) {
                mUSBMonitor!!.destroy()
                mUSBMonitor = null
            }
        }
        mUVCCameraView = null
        mCameraButton = null
        super.onDestroy()
    }

    private val mOnClickListener: OnClickListener = object : OnClickListener() {
        fun onClick(view: View?) {
            if (mUVCCamera == null) {
                // XXX calling CameraDialog.showDialog is necessary at only first time(only when app has no permission).
                CameraDialog.showDialog(this@MainActivity)
            } else {
                synchronized(mSync) {
                    mUVCCamera!!.destroy()
                    mUVCCamera = null
                    isPreview = false
                    isActive = isPreview
                }
            }
        }
    }
    private val mOnDeviceConnectListener: OnDeviceConnectListener =
        object : OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                if (DEBUG) Log.v(TAG, "onAttach:")
                Toast.makeText(this@MainActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show()
            }

            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock,
                createNew: Boolean
            ) {
                if (DEBUG) Log.v(TAG, "onConnect:")
                synchronized(mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera!!.destroy()
                    }
                    isPreview = false
                    isActive = isPreview
                }
                queueEvent(Runnable {
                    synchronized(mSync) {
                        val camera = UVCCamera()
                        camera.open(ctrlBlock)
                        if (DEBUG) Log.i(
                            TAG,
                            "supportedSize:" + camera.supportedSize
                        )
                        try {
                            camera.setPreviewSize(
                                UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                UVCCamera.FRAME_FORMAT_MJPEG
                            )
                        } catch (e: IllegalArgumentException) {
                            try {
                                // fallback to YUV mode
                                camera.setPreviewSize(
                                    UVCCamera.DEFAULT_PREVIEW_WIDTH,
                                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                                    UVCCamera.DEFAULT_PREVIEW_MODE
                                )
                            } catch (e1: IllegalArgumentException) {
                                camera.destroy()
                                return@Runnable
                            }
                        }
                        mPreviewSurface = mUVCCameraView!!.holder.surface
                        if (mPreviewSurface != null) {
                            isActive = true
                            camera.setPreviewDisplay(mPreviewSurface)
                            camera.startPreview()
                            isPreview = true
                        }
                        synchronized(mSync) { mUVCCamera = camera }
                    }
                }, 0)
            }

            override fun onDisconnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock
            ) {
                if (DEBUG) Log.v(TAG, "onDisconnect:")
                // XXX you should check whether the comming device equal to camera device that currently using
                queueEvent({
                    synchronized(mSync) {
                        if (mUVCCamera != null) {
                            mUVCCamera!!.close()
                            if (mPreviewSurface != null) {
                                mPreviewSurface.release()
                                mPreviewSurface = null
                            }
                            isPreview = false
                            isActive = isPreview
                        }
                    }
                }, 0)
            }

            override fun onDettach(device: UsbDevice) {
                if (DEBUG) Log.v(TAG, "onDettach:")
                Toast.makeText(this@MainActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel(device: UsbDevice) {}
        }

    /**
     * to access from CameraDialog
     * @return
     */
    override fun getUSBMonitor(): USBMonitor {
        return mUSBMonitor!!
    }

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            runOnUiThread({
                // FIXME
            }, 0)
        }
    }

    private val mSurfaceViewCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            if (DEBUG) Log.v(TAG, "surfaceCreated:")
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            if (width == 0 || height == 0) return
            if (DEBUG) Log.v(TAG, "surfaceChanged:")
            mPreviewSurface = holder.surface
            synchronized(mSync) {
                if (isActive && !isPreview && mUVCCamera != null) {
                    mUVCCamera.setPreviewDisplay(mPreviewSurface)
                    mUVCCamera!!.startPreview()
                    isPreview = true
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (DEBUG) Log.v(TAG, "surfaceDestroyed:")
            synchronized(mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera!!.stopPreview()
                }
                isPreview = false
            }
            mPreviewSurface = null
        }
    }

    companion object {
        private const val DEBUG = true // TODO set false when production
        private const val TAG = "MainActivity"
    }
}

}