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

    private UVCCamera mCamera = null;
    private CameraTextureView mCameraView;
    private Surface mPreviewSurface = null;

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

    public void open(Context context, USBMonitor.UsbControlBlock ctrlBlock) {
        synchronized (mSync) {
            if (isOpened()) {
                mCamera.destroy();
            }

            // Get the preferences if it exists
            String id = ctrlBlock.getVenderId() + "-" + ctrlBlock.getProductId();

            mCamera = new UVCCamera();
            mCamera.open(ctrlBlock);
            mCamera.initPreviewSize(getPreferences(context, id));

            mCameraView.setAspectRatio(mCamera.getWidth(), mCamera.getHeight());
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

    private UVCCameraPrefs getPreferences(Context context, String id) {
        SharedPreferences prefs = context.getSharedPreferences(id, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(id, "");

        if (!json.isEmpty()) {
            return gson.fromJson(json, UVCCameraPrefs.class);
        } else {
            return null;
        }
    }

    public void applyPreferences(Context context) {
        synchronized (mSync) {
            if (isOpened()) {
                String id = getVenProId();
                UVCCameraPrefs cameraPrefs = getPreferences(context, id);

                if (cameraPrefs == null) {
                    cameraPrefs = mCamera.getCameraPrefs();
                }

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

    public void startPreview() {
        synchronized (mSync) {
            mCamera.stopPreview();

            if (mPreviewSurface == null) {
                mPreviewSurface = new Surface(mCameraView.getSurfaceTexture());
            }

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
