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

    private val mLock = ReentrantLock()

    private var mUSBMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null

    private var mCameraView: CameraTextureView? = null
    private var mCameraButton: ImageButton? = null
    private var mSettingsButton: ImageButton? = null
    private var mPreviewSurface: Surface? = null
    private var mCameraPrefs: UVCCameraPrefs? = null;

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

        mSettingsButton = findViewById(R.id.settings_button)
        mSettingsButton!!.setOnClickListener(mSettingsOnClickListener)

        mCameraView = findViewById(R.id.camera_texture_view)
        mCameraView!!.setAspectRatio(mMediaWidth / mMediaHeight.toDouble())

        mCameraView!!.surfaceTextureListener = mSurfaceTextureListener
        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        mLock.withLock {
            mUSBMonitor!!.register()
            if(mUVCCamera != null) {
                startPreview()
            }
        }
    }

    override fun onStop() {
        mUVCCamera!!.stopPreview()
        super.onStop()
    }

    override fun onDestroy() {
        destroyUVCCamera()
        destroyViewAndMonitor()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_SETTINGS) {
            loadCameraPrefs(mUVCCamera!!.venProId)
        }
    }

    private fun toastUser(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun loadCameraPrefs(id: String) {
        val sharedPrefs = getSharedPreferences(id, Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String = sharedPrefs.getString(id, "")

        if (json.isNotEmpty()) {
            mCameraPrefs = gson.fromJson(json, UVCCameraPrefs::class.java)
            mUVCCamera!!.setPreviewSize(
                mCameraPrefs!!.width,
                mCameraPrefs!!.height,
                mCameraPrefs!!.frameFormat,
                mCameraPrefs!!.framerate
            )
            mCameraView!!.setAspectRatio(mCameraPrefs!!.width, mCameraPrefs!!.height)
        } else {
            toastUser("No camera preferences found")
        }
    }

    private fun destroyViewAndMonitor() {
        mLock.withLock {
            mCameraView = null
            mCameraButton = null
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
        }
    }

    private fun startPreview() {
        if (mPreviewSurface != null) {
            mUVCCamera!!.stopPreview()
            mPreviewSurface!!.release()
            mPreviewSurface = null
        }

        mPreviewSurface = Surface(mCameraView!!.surfaceTexture)

        if (mPreviewSurface != null) {
            Log.i("RAYMUNDTEST_PREF", "HERE I AM in START PREVIEW")

            mUVCCamera!!.setPreviewDisplay(mPreviewSurface)
        }
        mUVCCamera!!.startPreview()
    }

    private val mIFrameCallback: IFrameCallback = object: IFrameCallback{
        override fun onFrame(frame: ByteBuffer?) {
            runOnUiThread(Runnable {
                Log.i("RAYMUNDTEST_PREFS", "hello from on FRAME")
            })
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
                Log.i("RAYMUNDTEST_PREF", "Here i am onSurfaceTextureSizeChanged")
                startPreview()
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return true
            }
        }

    private val mDeviceOnClickListener: View.OnClickListener = View.OnClickListener {
        CameraDialog.showDialog(this@MainActivity)
    }

    private val mSettingsOnClickListener: View.OnClickListener = View.OnClickListener {
        if (mUVCCamera != null) {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("SupportedSizeList", mUVCCamera!!.supportedSizeList)
            intent.putExtra("CameraPreferences", mUVCCamera!!.cameraPrefs)
            startActivityForResult(intent, REQ_CODE_SETTINGS)
        } else {
            toastUser("No camera is selected. Cannot access settings.")
        }
    }

    private val mOnDeviceConnectListener: OnDeviceConnectListener =
        object : OnDeviceConnectListener {
            private fun initCamera(camera: UVCCamera, mode: Int) {
                camera.setPreviewSize(
                    mMediaWidth,
                    mMediaHeight,
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

                    mLock.withLock {
                        mUVCCamera = camera
                        startPreview()
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
    }

}