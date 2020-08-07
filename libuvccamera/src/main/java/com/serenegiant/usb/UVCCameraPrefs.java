package com.serenegiant.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class UVCCameraPrefs implements Parcelable {
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
        mVendorId = _vendorId;
        mProductId = _productId;
        mFrameFormat = _frameFormat;
        mFramerate = Integer.parseInt(_framerate);
        setResolution(_resolution);
    }

    protected UVCCameraPrefs(Parcel in) {
        mVendorId = in.readInt();
        mProductId = in.readInt();
        mFrameFormat = in.readInt();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mFramerate = in.readInt();
    }

    public void setVendorId(int vendorId) {
        mVendorId = vendorId;
    }

    public void setProductId(int productId) {
        mProductId = productId;
    }

    public void setFrameFormat(int frameformat) {
        mFrameFormat = frameformat;
    }

    public void setFrameFormat(String frameFormat) {
        mFrameFormat = UVCSize.Format.getFrameFormat(frameFormat);
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public void setResolution(String resolution) {
        String[] dimens = resolution.split("x");
        mWidth = Integer.parseInt(dimens[0]);
        mHeight = Integer.parseInt(dimens[1]);
    }

    public void setFramerate(int framerate) {
        mFramerate = framerate;
    }

    public void setFramerate(String framerate) {
        mFramerate = Integer.parseInt(framerate);
    }

    public int getVendorId() {
        return mVendorId;
    }

    public int getProductId() {
        return mProductId;
    }

    public int getFrameFormat() {
        return mFrameFormat;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFramerate() {
        return mFramerate;
    }

    public String getFrameFormatString() {
        return UVCSize.Format.getTypeString(mFrameFormat);
    }

    public String getResolutionString() {
        return UVCSize.Frame.getResolutionString(mWidth, mHeight);
    }

    public String getFramerateString() {
        return Integer.toString(mFramerate);
    }

    public String getPrefsFile(){
        return (getVendorId() + "-" + getProductId());
    }

    public static final Creator<UVCCameraPrefs> CREATOR = new Creator<UVCCameraPrefs>() {
        @Override
        public UVCCameraPrefs createFromParcel(Parcel in) {
            return new UVCCameraPrefs(in);
        }

        @Override
        public UVCCameraPrefs[] newArray(int size) {
            return new UVCCameraPrefs[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mVendorId);
        dest.writeInt(mProductId);
        dest.writeInt(mFrameFormat);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeInt(mFramerate);
    }
}