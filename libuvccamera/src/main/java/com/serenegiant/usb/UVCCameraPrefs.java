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
        this.mVendorId = _vendorId;
        this.mProductId = _productId;
        this.mFrameFormat = _frameFormat;
        String[] dimens = _resolution.split("x");
        this.mWidth = Integer.parseInt(dimens[0]);
        this.mHeight = Integer.parseInt(dimens[1]);
        this.mFramerate = Integer.parseInt(_framerate);
    }

    protected UVCCameraPrefs(Parcel in) {
        mVendorId = in.readInt();
        mProductId = in.readInt();
        mFrameFormat = in.readInt();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mFramerate = in.readInt();
    }

    public String getResolutionString(){
        return (mWidth + "x" + mHeight);
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