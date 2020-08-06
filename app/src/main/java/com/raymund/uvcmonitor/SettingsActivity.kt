package com.raymund.uvcmonitor

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.serenegiant.usb.UVCSize

class SettingsActivity : AppCompatActivity() {
    private var mSupportedSizeList : UVCSize? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        mSupportedSizeList = intent.getParcelableExtra("SupportedSizeList");
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment(mSupportedSizeList!!))
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment() : PreferenceFragmentCompat() {
        private var mSupportedSizeList: UVCSize? = null;

        constructor(supportedSizeList: UVCSize) : this(){
            mSupportedSizeList = supportedSizeList;
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)
            val format = mSupportedSizeList!!.getFormat(0);
            val frame = format.getFrame(0);

            val categoryPreview = PreferenceCategory(context).apply {
                title = "Preview Settings"
            }
            val previewFormatType = DropDownPreference(context).apply {
                key = "preview_format_type"
                title = "Format Type"
                entries = mSupportedSizeList!!.formatCharSeq
                entryValues =  mSupportedSizeList!!.formatCharSeq
                summary = mSupportedSizeList!!.formatCharSeq[0]
            }
            val previewResolution = DropDownPreference(context).apply {
                key = "preview_resolution"
                title = "Resolution"
                entries = format.frameResCharSeq
                entryValues =  format.frameResCharSeq
                summary = format.frameResCharSeq[0]
            }
            val previewFramerate = DropDownPreference(context).apply {
                key = "preview_framerate"
                title = "Framerate"
                entries = frame.intervalCharSeq
                entryValues =  frame.intervalCharSeq
                summary = frame.intervalCharSeq[0]
            }

            val categoryRecord = PreferenceCategory(context).apply {
                title = "Record Settings"
            }

            screen.addPreference(categoryPreview)
            screen.addPreference(previewFormatType)
            screen.addPreference(previewResolution)
            screen.addPreference(previewFramerate)
            screen.addPreference(categoryRecord)
            preferenceScreen = screen;
        }
    }
}