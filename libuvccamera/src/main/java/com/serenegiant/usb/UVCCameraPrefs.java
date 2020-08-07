package com.serenegiant.usb;

public class UVCCameraPrefs {
    private int mVendorId;
    private int mProductId;
    private int mFrameFormat;
    private int mWidth;
    private int mHeight;
    private int mFramerate;

    UVCCameraPrefs(
            int _vendorId,
            int _productId,
            int _frameFormat,
            int _width,
            int _height,
            int _framerate
    ) {
        mVendorId = _vendorId;
        mProductId = _productId;
        mFrameFormat = _frameFormat;
        mWidth = _width;
        mHeight = _height;
        mFramerate = _framerate;
    }

    UVCCameraPrefs(
            int _vendorId,
            int _productId,
            int _frameFormat,
            String _resolution,
            String _framerate
    ) {
        this.mVendorId = _vendorId;
        this.mProductId = _productId;
        this.mFrameFormat = _frameFormat;
        String[] dimens = _resolution.split("x");
        this.mWidth = Integer.parseInt(dimens[0]);
        this.mHeight = Integer.parseInt(dimens[1]);
        this.mFramerate = Integer.parseInt(_framerate);
    }

    public String getResolutionString(){
        return (mWidth + "x" + mHeight);
    }
}
