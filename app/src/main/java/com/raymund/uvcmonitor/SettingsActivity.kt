package com.raymund.uvcmonitor

import android.content.Context
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import com.serenegiant.usb.UVCCameraPrefs
import com.serenegiant.usb.UVCSize


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val supportedSizeList = intent.getParcelableExtra<UVCSize>("SupportedSizeList")
        val cameraPreferences = intent.getParcelableExtra<UVCCameraPrefs>("CameraPreferences")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment(supportedSizeList!!, cameraPreferences!!))
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment() : PreferenceFragmentCompat(), OnPreferenceChangeListener {
        private var mSupportedSizeList: UVCSize? = null
        private var mCameraPreferences: UVCCameraPrefs? = null
        private var mPreviewFrameFormat: ListPreference? = null
        private var mPreviewResolution: ListPreference? = null
        private var mPreviewFramerate: ListPreference? = null

        constructor(supportedSizeList: UVCSize, cameraPreferences: UVCCameraPrefs) : this() {
            mSupportedSizeList = supportedSizeList
            mCameraPreferences = cameraPreferences
        }

        private fun initPreviewPref(
            preference: ListPreference?,
            _index: Int,
            _entries: Array<CharSequence>
        ) {
            preference!!.apply {
                onPreferenceChangeListener = this@SettingsFragment
                entries = _entries
                entryValues = entries
                setValueIndex(_index)
            }
        }

        private fun updatePreviewPref(preference: ListPreference?, _entries: Array<CharSequence>) {
            preference!!.apply {
                entries = _entries
                entryValues = entries
                setValueIndex(0)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val formatIndex =
                mSupportedSizeList!!.findFrameFormat(mCameraPreferences!!.frameFormat)
            val format = mSupportedSizeList!!.getFormat(formatIndex)
            val frameIndex = format.findFrame(mCameraPreferences!!.resolutionString)
            val frame = format.getFrame(frameIndex)
            val framerateIndex = frame.findInterval(mCameraPreferences!!.framerate)

            mPreviewFrameFormat = findPreference("preview_frame_format")
            mPreviewResolution = findPreference("preview_resolution")
            mPreviewFramerate = findPreference("preview_framerate")

            initPreviewPref(mPreviewFrameFormat, formatIndex, mSupportedSizeList!!.formatCharSeq)
            initPreviewPref(mPreviewResolution, frameIndex, format.frameResCharSeq)
            initPreviewPref(mPreviewFramerate, framerateIndex, frame.intervalCharSeq)
        }

        private fun updatePrefsFile() {
            val id = mCameraPreferences!!.prefsFile;
            val editor: Editor = context!!.getSharedPreferences(id, Context.MODE_PRIVATE).edit()
            val gson = Gson()
            val json: String = gson.toJson(mCameraPreferences)
            editor.putString(id, json)
            editor.commit()
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            val key = preference!!.key
            val index = (preference as ListPreference?)!!.findIndexOfValue(newValue as String)

            when (key) {
                "preview_frame_format" -> {
                    val format = mSupportedSizeList!!.getFormat(index)
                    val frame = format!!.getFrame(0)
                    updatePreviewPref(mPreviewResolution, format.frameResCharSeq)
                    updatePreviewPref(mPreviewFramerate, frame.intervalCharSeq)
                    mCameraPreferences!!.setFrameFormat(newValue as String)
                    mCameraPreferences!!.setResolution(mPreviewResolution!!.value)
                    mCameraPreferences!!.setFramerate(mPreviewFramerate!!.value)

                }
                "preview_resolution" -> {
                    val formatValue = mPreviewFrameFormat!!.value
                    val formatIndex = mPreviewFrameFormat!!.findIndexOfValue(formatValue)
                    val format = mSupportedSizeList!!.getFormat(formatIndex)
                    val frame = format.getFrame(index)
                    updatePreviewPref(mPreviewFramerate, frame.intervalCharSeq)
                    mCameraPreferences!!.setResolution(newValue as String)
                    mCameraPreferences!!.setFramerate(mPreviewFramerate!!.value)
                }
                "preview_framerate" -> {
                    mCameraPreferences!!.setFramerate(newValue as String)
                }
            }

            updatePrefsFile()

            return true
        }
    }
}