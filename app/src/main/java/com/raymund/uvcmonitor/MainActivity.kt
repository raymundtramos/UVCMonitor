package com.raymund.uvcmonitor

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.UVCCameraHandlerMultiSurface
import com.serenegiant.widget.CameraViewInterface
import com.serenegiant.widget.UVCCameraTextureView

class MainActivity : BaseActivity(), CameraDialogParent {
    private var mUSBMonitor: USBMonitor? = null
    private var mCameraView: UVCCameraTextureView? = null
    private var mCameraButton: ImageButton? = null
    private var mRecordButton: ImageButton? = null
    private var mCameraHandler: UVCCameraHandlerMultiSurface? = null
    private var mPreviewSurfaceId: Int? = null

    // TODO: Need to find a way to dynamically change this
    private val mMediaWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH
    private val mMediaHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        mCameraButton = findViewById(R.id.camera_button)
        mCameraButton!!.setOnClickListener(mDeviceOnClickListener)

        mRecordButton = findViewById(R.id.record_button)
        mRecordButton!!.setOnClickListener(mRecordOnClickListener)

        mCameraView = findViewById(R.id.camera_texture_view)
        mCameraView!!.setAspectRatio(mMediaWidth, mMediaHeight)
        mCameraView!!.setCallback(mCameraViewCallback)

        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)

        mCameraHandler =
            UVCCameraHandlerMultiSurface.createHandler(
                this,
                mCameraView,
                0,
                mMediaWidth,
                mMediaHeight,
                UVCCamera.FRAME_FORMAT_YUYV
            )
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor!!.register()
        if (mCameraView != null) {
            mCameraView!!.onResume()
        }
    }

    override fun onStop() {
        mCameraHandler!!.close();
        if (mCameraView != null) {
            mCameraView!!.onPause();
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (mCameraHandler != null) {
            mCameraHandler!!.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor!!.destroy();
            mUSBMonitor = null;
        }
        mCameraView = null;
        mCameraButton = null;
        mRecordButton = null;
        super.onDestroy()
    }

    private fun toastUser(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun startPreview() {
        if (mPreviewSurfaceId != null) {
            mCameraHandler!!.removeSurface(mPreviewSurfaceId!!)
        }

        val surface = Surface(mCameraView!!.surfaceTexture)

        if (surface != null) {
            mPreviewSurfaceId = surface.hashCode()
            mCameraHandler!!.addSurface(mPreviewSurfaceId!!, surface, true)
        }
        mCameraHandler!!.startPreview()
    }

    private val mCameraViewCallback: CameraViewInterface.Callback =
        object : CameraViewInterface.Callback {
            override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
            }

            override fun onSurfaceChanged(
                view: CameraViewInterface?,
                surface: Surface?,
                width: Int,
                height: Int
            ) {
                if (mCameraHandler!!.isPreviewing) {
                    mCameraHandler!!.stopPreview()
                    mCameraHandler!!.resize(1280, 720)
                    startPreview()
                }
            }

            override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
            }
        }

    private val mDeviceOnClickListener: View.OnClickListener = View.OnClickListener {
        CameraDialog.showDialog(this@MainActivity)
    }

    private val mRecordOnClickListener: View.OnClickListener = View.OnClickListener {
        if (checkPermissionWriteExternalStorage()) {
            if (!mCameraHandler!!.isRecording) {
                mCameraHandler!!.startRecording()
                toastUser("Started Recording")
            } else {
                mCameraHandler!!.stopRecording()
                toastUser("Stopped Recording")
            }
        }
    }

    private val mOnDeviceConnectListener: OnDeviceConnectListener =
        object : OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                toastUser("USB device detected")
            }

            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock,
                createNew: Boolean
            ) {
                if (mCameraHandler!!.isOpened) {
                    mCameraHandler!!.close()
                }
                mCameraHandler!!.open(ctrlBlock)
                startPreview()
            }

            override fun onDisconnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock
            ) {
                if (mCameraHandler != null) {
                    mCameraHandler!!.close()
                }
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
    }

    private fun startCapture() {
    }

    private fun stopCapture() {
    }
}