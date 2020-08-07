package com.raymund.uvcmonitor

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceChangeListener
import com.serenegiant.usb.UVCSize

class SettingsActivity : AppCompatActivity() {
    private var mSupportedSizeList: UVCSize? = null;

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

    class SettingsFragment() : PreferenceFragmentCompat(), OnPreferenceChangeListener {
        private var mSupportedSizeList: UVCSize? = null;
        private var mPreviewFrameFormat: ListPreference? = null;
        private var mPreviewResolution: ListPreference? = null;
        private var mPreviewFramerate: ListPreference? = null;

        constructor(supportedSizeList: UVCSize) : this() {
            mSupportedSizeList = supportedSizeList;
        }

        private fun createPreviewPref(
            context: Context,
            _key: String,
            _title: String,
            _entries: Array<CharSequence>
        ): ListPreference {
            return ListPreference(context).apply {
                onPreferenceChangeListener = this@SettingsFragment
                key = _key
                title = _title
                entries = _entries
                entryValues = entries
                summary = entries[0]
                setValueIndex(0)
            }
        }

        private fun updatePreviewPref(preference: ListPreference?, _entries: Array<CharSequence>) {
            preference!!.apply {
                entries = _entries
                entryValues = entries
                summary = entries[0]
                setValueIndex(0)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)
            val format = mSupportedSizeList!!.getFormat(0);
            val frame = format.getFrame(0);

            val categoryPreview = PreferenceCategory(context).apply {
                title = "Preview Settings"
            }
            mPreviewFrameFormat = createPreviewPref(
                context,
                "preview_frame_format",
                "Frame Format",
                mSupportedSizeList!!.formatCharSeq
            )
            mPreviewResolution = createPreviewPref(
                context,
                "preview_resolution",
                "Resolution",
                format.frameResCharSeq
            )
            mPreviewFramerate = createPreviewPref(
                context,
                "preview_framerate",
                "Framerate",
                frame.intervalCharSeq
            )

            val categoryRecord = PreferenceCategory(context).apply {
                title = "Record Settings"
            }

            screen.addPreference(categoryPreview)
            screen.addPreference(mPreviewFrameFormat)
            screen.addPreference(mPreviewResolution)
            screen.addPreference(mPreviewFramerate)
            screen.addPreference(categoryRecord)
            preferenceScreen = screen;
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            val key = preference!!.key
            val index = (preference as ListPreference?)!!.findIndexOfValue(newValue as String);

            if (key == "preview_frame_format") {
                val format = mSupportedSizeList!!.getFormat(index);
                val frame = format!!.getFrame(0);

                preference.summary = newValue
                updatePreviewPref(mPreviewResolution, format!!.frameResCharSeq)
                updatePreviewPref(mPreviewFramerate, frame!!.intervalCharSeq)
                return true
            } else if (key == "preview_resolution") {
                val formatValue = mPreviewFrameFormat!!.value
                val formatIndex = mPreviewFrameFormat!!.findIndexOfValue(formatValue)
                val format = mSupportedSizeList!!.getFormat(formatIndex)
                val frame = format.getFrame(index);

                preference.summary = newValue
                updatePreviewPref(mPreviewFramerate, frame!!.intervalCharSeq)
                return true
            } else if (key == "preview_framerate") {
                preference.summary = newValue
                return true
            } else {
                return false
            }
        }
    }
}