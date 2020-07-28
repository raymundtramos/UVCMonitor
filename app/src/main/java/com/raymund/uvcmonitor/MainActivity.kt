package com.raymund.uvcmonitor

import android.content.DialogInterface
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.raymund.widget.CameraTextureView
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
    private var mUVCCameraView: CameraTextureView? = null

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
        mUVCCameraView = findViewById<View>(R.id.camera_texture_view) as CameraTextureView
        mUVCCameraView!!.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (UVCCamera.DEFAULT_PREVIEW_HEIGHT).toDouble())
        mUVCCameraView!!.surfaceTextureListener = mSurfaceTextureListener
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        synchronized(mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor!!.register()
            }
        }
    }

    override fun onStop() {
        synchronized(mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor!!.unregister()
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        synchronized(mSync) {
            isPreview = false
            isActive = false
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

    private val mOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mUVCCamera == null) {
            // XXX calling CameraDialog.showDialog is necessary at only first time(only when app has no permission).
            CameraDialog.showDialog(this@MainActivity)
        } else {
            synchronized(mSync) {
                mUVCCamera!!.destroy()
                mUVCCamera = null
                mPreviewSurface!!.release()
                mPreviewSurface = null
                isPreview = false
                isActive = false
            }
        }
    }
    private val mOnDeviceConnectListener: OnDeviceConnectListener =
        object : OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                Toast.makeText(this@MainActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show()
            }

            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock,
                createNew: Boolean
            ) {
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
                        val surfaceTexture: SurfaceTexture = mUVCCameraView!!.surfaceTexture
                        if (surfaceTexture != null) {
                            mPreviewSurface = Surface(surfaceTexture)
                            camera.setPreviewDisplay(mPreviewSurface)
                            camera.startPreview()
                            isActive = true
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
                // XXX you should check whether the incoming device equal to camera device that currently using
                queueEvent({
                    synchronized(mSync) {
                        if (mUVCCamera != null) {
                            mUVCCamera!!.close()
                            if (mPreviewSurface != null) {
                                mPreviewSurface!!.release()
                                mPreviewSurface = null
                            }
                            isPreview = false
                            isActive = isPreview
                        }
                    }
                }, 0)
            }

            override fun onDettach(device: UsbDevice) {
                Toast.makeText(this@MainActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel(device: UsbDevice) {}
        }

    override fun getUSBMonitor(): USBMonitor {
        return mUSBMonitor!!
    }

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