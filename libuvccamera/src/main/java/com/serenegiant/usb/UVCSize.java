package com.serenegiant.usb;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class UVCSize implements Parcelable {
    private static final int FORMAT_DESC_TYPE_UNCOMPRESSED = 4;
    private static final int FORMAT_DESC_TYPE_MJPEG = 6;

    public static class Frame implements Parcelable, Comparable {
        private int bDescriptorSubtype;
        private int wWidth;
        private int wHeight;
        private int dwDefaultFrameInterval;
        private int bFrameIntervalType;
        private int dwMinFrameInterval;
        private int dwMaxFrameInterval;
        private int dwFrameIntervalStep;
        private ArrayList<Integer> intervals;

        public Frame(
                int _bDescriptorSubtype,
                int _wWidth,
                int _wHeight,
                int _dwDefaultFrameInterval,
                int _bFrameIntervalType,
                int _dwMinFrameInterval,
                int _dwMaxFrameInterval,
                int _dwFrameIntervalStep) {
            bDescriptorSubtype = _bDescriptorSubtype;
            wWidth = _wWidth;
            wHeight = _wHeight;
            dwDefaultFrameInterval = _dwDefaultFrameInterval;
            bFrameIntervalType = _bFrameIntervalType;
            dwMinFrameInterval = _dwMinFrameInterval;
            dwMaxFrameInterval = _dwMaxFrameInterval;
            dwFrameIntervalStep = _dwFrameIntervalStep;
        }

        public Frame(Parcel parcel) {
            bDescriptorSubtype = parcel.readInt();
            wWidth = parcel.readInt();
            wHeight = parcel.readInt();
            dwDefaultFrameInterval = parcel.readInt();
            bFrameIntervalType = parcel.readInt();
            dwMinFrameInterval = parcel.readInt();
            dwMaxFrameInterval = parcel.readInt();
            dwFrameIntervalStep = parcel.readInt();
            intervals = parcel.readArrayList(Integer.class.getClassLoader());
        }

        public static final Creator<Frame> CREATOR = new Creator<Frame>() {
            @Override
            public Frame createFromParcel(Parcel in) {
                return new Frame(in);
            }

            @Override
            public Frame[] newArray(int size) {
                return new Frame[size];
            }
        };

        public int getDescriptorSubtype() {
            return bDescriptorSubtype;
        }

        public int getWidth() {
            return wWidth;
        }

        public int getHeight() {
            return wHeight;
        }

        public int getDefaultFrameInterval() {
            return dwDefaultFrameInterval;
        }

        public int getFrameIntervalType() {
            return bFrameIntervalType;
        }

        public int getMinFrameInterval() {
            return dwMinFrameInterval;
        }

        public int getMaxFrameInterval() {
            return dwMaxFrameInterval;
        }

        public int getFrameIntervalStep() {
            return dwFrameIntervalStep;
        }

        public int getInterval(int index) {
            if (index < intervals.size()) {
                return intervals.get(index);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public String getResolutionString() {
            return getResolutionString(wWidth, wHeight);
        }

        public static String getResolutionString(int width, int height) {
            return (width + "x" + height);
        }

        public CharSequence[] getIntervalCharSeq() {
            int size = intervals.size();
            CharSequence[] result = new CharSequence[size];

            for (int i = 0; i < size; i++) {
                result[i] = intervals.get(i).toString();
            }

            return result;
        }

        public int findInterval(int framerate) {
            for (int i = 0; i < intervals.size(); i++) {
                if (framerate == intervals.get(i)) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            return "Frame{" +
                    "bDescriptorSubtype=" + bDescriptorSubtype +
                    ", wWidth=" + wWidth +
                    ", wHeight=" + wHeight +
                    ", dwDefaultFrameInterval=" + dwDefaultFrameInterval +
                    ", bFrameIntervalType=" + bFrameIntervalType +
                    ", dwMinFrameInterval=" + dwMinFrameInterval +
                    ", dwMaxFrameInterval=" + dwMaxFrameInterval +
                    ", dwFrameIntervalStep=" + dwFrameIntervalStep +
                    ", intervals=" + intervals +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(bDescriptorSubtype);
            dest.writeInt(wWidth);
            dest.writeInt(wHeight);
            dest.writeInt(dwDefaultFrameInterval);
            dest.writeInt(bFrameIntervalType);
            dest.writeInt(dwMinFrameInterval);
            dest.writeInt(dwMaxFrameInterval);
            dest.writeInt(dwFrameIntervalStep);
            dest.writeArray(intervals.toArray());
        }

        @Override
        public int compareTo(Object o) {
            int result = Integer.compare(wWidth, ((Frame) o).wWidth);

            if (result == 0) {
                return Integer.compare(wHeight, ((Frame) o).wHeight);
            } else {
                return result;
            }
        }
    }

    public static class Format implements Parcelable, Comparable {
        private int bDescriptorSubtype;
        private int bFormatIndex;
        private int bDefaultFrameIndex;
        private ArrayList<Frame> frameDescs;

        public Format(int _bDescriptorSubtype, int _bFormatIndex, int _bDefaultFrameIndex) {
            bDescriptorSubtype = _bDescriptorSubtype;
            bFormatIndex = _bFormatIndex;
            bDefaultFrameIndex = _bDefaultFrameIndex;
        }

        public Format(Parcel parcel) {
            bDescriptorSubtype = parcel.readInt();
            bFormatIndex = parcel.readInt();
            bDefaultFrameIndex = parcel.readInt();
            frameDescs = parcel.readArrayList(Frame.class.getClassLoader());
        }

        public static final Creator<Format> CREATOR = new Creator<Format>() {
            @Override
            public Format createFromParcel(Parcel in) {
                return new Format(in);
            }

            @Override
            public Format[] newArray(int size) {
                return new Format[size];
            }
        };

        public int getDescriptorSubtype() {
            return bDescriptorSubtype;
        }

        public int getFrameFormat() {
            switch (bDescriptorSubtype) {
                case FORMAT_DESC_TYPE_UNCOMPRESSED:
                    return UVCCamera.FRAME_FORMAT_YUYV;
                case FORMAT_DESC_TYPE_MJPEG:
                    return UVCCamera.FRAME_FORMAT_MJPEG;
                default:
                    return UVCCamera.DEFAULT_PREVIEW_MODE;
            }
        }

        public static int getFrameFormat(String frameFormat) {
            if (frameFormat.equals(getTypeString(UVCCamera.FRAME_FORMAT_YUYV))) {
                return UVCCamera.FRAME_FORMAT_YUYV;
            } else if (frameFormat.equals(getTypeString(UVCCamera.FRAME_FORMAT_MJPEG))) {
                return UVCCamera.FRAME_FORMAT_MJPEG;
            } else {
                return UVCCamera.DEFAULT_PREVIEW_MODE;
            }
        }

        public String getTypeString() {
            switch (bDescriptorSubtype) {
                case FORMAT_DESC_TYPE_UNCOMPRESSED:
                    return "YUYV";
                case FORMAT_DESC_TYPE_MJPEG:
                    return "MJPEG";
                default:
                    return "Undefined";
            }
        }

        public static String getTypeString(int frameFormat) {
            switch (frameFormat) {
                case UVCCamera.FRAME_FORMAT_YUYV:
                    return "YUYV";
                case UVCCamera.FRAME_FORMAT_MJPEG:
                    return "MJPEG";
                default:
                    return "Undefined";
            }
        }

        public int getFormatIndex() {
            return bFormatIndex;
        }

        public int getDefaultFrameIndex() {
            return bDefaultFrameIndex;
        }

        public Frame getFrame(int index) {
            if (index < frameDescs.size()) {
                return frameDescs.get(index);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public CharSequence[] getFrameResCharSeq() {
            int size = frameDescs.size();
            CharSequence[] result = new CharSequence[size];

            for (int i = 0; i < size; i++) {
                Frame frame = frameDescs.get(i);
                result[i] = frame.getResolutionString();
            }

            return result;
        }

        public int findFrame(String resolution) {
            for (int i = 0; i < frameDescs.size(); i++) {
                if (resolution.equals(frameDescs.get(i).getResolutionString())) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            return "Format{" +
                    "bDescriptorSubtype=" + bDescriptorSubtype +
                    ", bFormatIndex=" + bFormatIndex +
                    ", bDefaultFrameIndex=" + bDefaultFrameIndex +
                    ", frameDescs=" + frameDescs +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(bDescriptorSubtype);
            dest.writeInt(bFormatIndex);
            dest.writeInt(bDefaultFrameIndex);
            dest.writeArray(frameDescs.toArray());
        }

        @Override
        public int compareTo(Object o) {
            return (Integer.compare(bFormatIndex, ((Format) o).bFormatIndex));
        }

    }

    private ArrayList<Format> mFormats;

    public UVCSize(String jsonString) {
        mFormats = new ArrayList<Format>();

        if (!TextUtils.isEmpty(jsonString)) {
            try {
                final JSONObject json = new JSONObject(jsonString);
                final JSONArray formats = json.getJSONArray("formats");
                mFormats.addAll(parseFormat(formats));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static final Creator<UVCSize> CREATOR = new Creator<UVCSize>() {
        @Override
        public UVCSize createFromParcel(Parcel in) {
            return new UVCSize(in);
        }

        @Override
        public UVCSize[] newArray(int size) {
            return new UVCSize[size];
        }
    };

    private ArrayList<Format> parseFormat(JSONArray formats) {
        ArrayList<Format> result = new ArrayList<Format>();

        try {
            for (int i = 0; i < formats.length(); i++) {
                JSONObject jsonFormat = formats.getJSONObject(i);
                JSONArray jsonFrames = jsonFormat.getJSONArray("frame_descs");
                Format format = new Format(
                        jsonFormat.getInt("bDescriptorSubtype"),
                        jsonFormat.getInt("bFormatIndex"),
                        jsonFormat.getInt("bDefaultFrameIndex")
                );
                format.frameDescs = new ArrayList<Frame>(parseFrame(jsonFrames));
                Collections.sort(format.frameDescs);
                result.add(format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private ArrayList<Frame> parseFrame(JSONArray frames) {
        ArrayList<Frame> result = new ArrayList<Frame>();

        try {
            for (int i = 0; i < frames.length(); i++) {
                JSONObject jsonFrame = frames.getJSONObject(i);
                JSONArray jsonIntervals = jsonFrame.getJSONArray("intervals");
                Boolean isDiscreteInterval = (jsonFrame.getInt("bFrameIntervalType") > 0);
                Frame frame = new Frame(
                        jsonFrame.getInt("bDescriptorSubtype"),
                        jsonFrame.getInt("wWidth"),
                        jsonFrame.getInt("wHeight"),
                        jsonFrame.getInt("dwDefaultFrameInterval"),
                        jsonFrame.getInt("bFrameIntervalType"),
                        (isDiscreteInterval) ? 0 : jsonFrame.getInt("dwMinFrameInterval"),
                        (isDiscreteInterval) ? 0 : jsonFrame.getInt("dwMaxFrameInterval"),
                        (isDiscreteInterval) ? 0 : jsonFrame.getInt("dwFrameIntervalStep")
                );
                frame.intervals = new ArrayList<Integer>();
                if (isDiscreteInterval) {
                    for (int j = 0; j < frame.bFrameIntervalType; j++) {
                        frame.intervals.add(jsonIntervals.getInt(j));
                    }
                    Collections.sort(frame.intervals);
                }
                result.add(frame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public UVCSize(Parcel parcel) {
        mFormats = parcel.readArrayList(Format.class.getClassLoader());
    }

    public Format getFormat(int index) {
        if (index < mFormats.size()) {
            return mFormats.get(index);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public int findFrameFormat(int frameFormat) {
        for (int i = 0; i < mFormats.size(); i++) {
            if (frameFormat == mFormats.get(i).getFrameFormat()) {
                return i;
            }
        }
        return 0;
    }

    public CharSequence[] getFormatCharSeq() {
        int size = mFormats.size();
        CharSequence[] result = new CharSequence[size];

        for (int i = 0; i < size; i++) {
            result[i] = mFormats.get(i).getTypeString();
        }

        return result;
    }

    @Override
    public String toString() {
        return "UVCSize{" +
                "mFormats=" + mFormats +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeArray(mFormats.toArray());
    }
}