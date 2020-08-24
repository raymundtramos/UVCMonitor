package com.raymund.uvcmonitor

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.google.gson.Gson
import com.raymund.helper.CameraHandler
import com.raymund.widget.CameraTextureView
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.*
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainActivity : BaseActivity(), CameraDialogParent {
    private val REQ_CODE_SETTINGS = 1

    private var mUSBMonitor: USBMonitor? = null
    private var mCameraView: CameraTextureView? = null
    private var mCameraButton: ImageButton? = null
    private var mSettingsButton: ImageButton? = null
    private var mCameraHandler: CameraHandler? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        mCameraButton = findViewById(R.id.camera_button)
        mCameraButton!!.setOnClickListener(mDeviceOnClickListener)

        mSettingsButton = findViewById(R.id.settings_button)
        mSettingsButton!!.setOnClickListener(mSettingsOnClickListener)

        mCameraView = findViewById(R.id.camera_texture_view)
        mCameraView!!.surfaceTextureListener = mSurfaceTextureListener
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)

        mCameraHandler = CameraHandler(mCameraView)
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor!!.register()
        if (mCameraHandler!!.isOpened) {
            mCameraHandler!!.startPreview()
        }
    }

    override fun onStop() {
        if (mCameraHandler!!.isPreviewing) {
            mCameraHandler!!.stopPreview()
        }
        super.onStop()
    }

    override fun onDestroy() {
        mCameraHandler!!.destroy();
        destroyViewAndMonitor()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_SETTINGS) {
            mCameraHandler!!.applyPreferences(this)
        }
    }

    private fun toastUser(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun destroyViewAndMonitor() {
        mCameraView = null
        mCameraButton = null
        if (mUSBMonitor != null) {
            mUSBMonitor!!.destroy()
            mUSBMonitor = null
        }
    }

    private val mIFrameCallback: IFrameCallback = object : IFrameCallback {
        override fun onFrame(frame: ByteBuffer?) {
            runOnUiThread(Runnable {
                Log.i("RAYMUNDTEST_PREFS", "hello from on FRAME")
            })
        }
    }

    private val mSurfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }
        }

    private val mDeviceOnClickListener: View.OnClickListener = View.OnClickListener {
        CameraDialog.showDialog(this@MainActivity)
    }

    private val mSettingsOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mCameraHandler!!.isOpened) {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("SupportedSizeList", mCameraHandler!!.supportedSizeList)
            intent.putExtra("CameraPreferences", mCameraHandler!!.cameraPrefs)
            startActivityForResult(intent, REQ_CODE_SETTINGS)
        } else {
            toastUser("No camera is selected. Cannot access settings.")
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
                runOnUiThread(Runnable {
                    mCameraHandler!!.open(ctrlBlock);
                    mCameraHandler!!.applyPreferences(this@MainActivity)
                    mCameraHandler!!.startPreview()
                })
            }

            override fun onDisconnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock
            ) {
                mCameraHandler!!.close();
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

}