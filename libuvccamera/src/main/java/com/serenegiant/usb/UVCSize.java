package com.serenegiant.usb;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class UVCSize {
    public static class Frame {
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
    }

    public static class Format {
        private int bDescriptorSubtype;
        private int bFormatIndex;
        private int bDefaultFrameIndex;
        private ArrayList<Frame> frameDescs;

        public Format(int _bDescriptorSubtype, int _bFormatIndex, int _bDefaultFrameIndex) {
            bDescriptorSubtype = _bDescriptorSubtype;
            bFormatIndex = _bFormatIndex;
            bDefaultFrameIndex = _bDefaultFrameIndex;
        }

        public int getDescriptorSubtype() {
            return bDescriptorSubtype;
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

        @Override
        public String toString() {
            return "Format{" +
                    "bDescriptorSubtype=" + bDescriptorSubtype +
                    ", bFormatIndex=" + bFormatIndex +
                    ", bDefaultFrameIndex=" + bDefaultFrameIndex +
                    ", frameDescs=" + frameDescs +
                    '}';
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
                }
                result.add(frame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Format getFormat(int index) {
        if (index < mFormats.size()) {
            return mFormats.get(index);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public String toString() {
        return "UVCSize{" +
                "mFormats=" + mFormats +
                '}';
    }
}