package com.raymund.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Surface;

import com.google.gson.Gson;
import com.raymund.widget.CameraTextureView;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCCameraPrefs;
import com.serenegiant.usb.UVCSize;

public class CameraHandler {
    private final Object mSync = new Object();

    private UVCCamera mCamera;
    private CameraTextureView mCameraView;
    private Surface mPreviewSurface;

    private boolean mIsPreviewing = false;

    public CameraHandler(CameraTextureView view) {
        mCameraView = view;
    }

    public boolean isOpened() {
        synchronized (mSync) {
            return (mCamera != null);
        }
    }

    public boolean isPreviewing() {
        synchronized (mSync) {
            return (mCamera != null) && mIsPreviewing;
        }
    }

    public void open(USBMonitor.UsbControlBlock ctrlBlock) {
        synchronized (mSync) {
            if (isOpened()) {
                mCamera.destroy();
            }

            mCamera = new UVCCamera();
            mCamera.open(ctrlBlock);
        }
    }

    public void close() {
        synchronized (mSync) {
            if (isOpened()) {
                mCamera.close();
                mCamera = null;
            }
        }
    }

    public void destroy() {
        synchronized (mSync) {
            if (isOpened()) {
                mCamera.destroy();
                mCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }

    public void applyPreferences(Context context) {
        synchronized (mSync) {
            if (isOpened()) {
                stopPreview();

                String id = getVenProId();
                SharedPreferences prefs = context.getSharedPreferences(id, Context.MODE_PRIVATE);
                Gson gson = new Gson();
                String json = prefs.getString(id, "");

                if (!json.isEmpty()) {
                    UVCCameraPrefs cameraPrefs = gson.fromJson(json, UVCCameraPrefs.class);
                    mCamera.setPreviewSize(
                            cameraPrefs.getWidth(),
                            cameraPrefs.getHeight(),
                            cameraPrefs.getFrameFormat(),
                            cameraPrefs.getFramerate()
                    );
                    mCameraView.setAspectRatio(cameraPrefs.getWidth(), cameraPrefs.getHeight());
                }
            }
        }
    }

    public void startPreview() {
        synchronized (mSync) {
            if (isPreviewing()) {
                mCamera.stopPreview();
                mPreviewSurface.release();
            }

            mPreviewSurface = new Surface(mCameraView.getSurfaceTexture());

            if (mPreviewSurface != null && isOpened()) {
                mCamera.setPreviewDisplay(mPreviewSurface);
                mCamera.startPreview();
                mIsPreviewing = true;
            }
        }
    }

    public void stopPreview() {
        synchronized (mSync) {
            if (isPreviewing()) {
                mCamera.stopPreview();
                mIsPreviewing = false;
            }

        }
    }

    public String getVenProId() {
        synchronized (mSync) {
            if (isOpened()) {
                return mCamera.getVenProId();
            } else {
                throw new NullPointerException();
            }
        }
    }

    public UVCSize getSupportedSizeList() {
        synchronized (mSync) {
            if (isOpened()) {
                return mCamera.getSupportedSizeList();
            } else {
                throw new NullPointerException();
            }
        }
    }

    public UVCCameraPrefs getCameraPrefs() {
        synchronized (mSync) {
            if (isOpened()) {
                return mCamera.getCameraPrefs();
            } else {
                throw new NullPointerException();
            }
        }
    }
}
