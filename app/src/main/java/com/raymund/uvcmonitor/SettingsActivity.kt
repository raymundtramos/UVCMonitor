package com.raymund.uvcmonitor

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.serenegiant.usb.UVCSize

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val supportedSizeList: UVCSize = intent.getParcelableExtra("SupportedSizeList");
        Log.i("RAYMUNDTEST_SETTINGS", supportedSizeList.toString())
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val categoryPreview = PreferenceCategory(context).apply {
                title = "Preview Settings"
            }
            val previewFormatType = DropDownPreference(context).apply {
                key = "preview_format_type"
                title = "Format Type"
            }
            val previewResolution = DropDownPreference(context).apply {
                key = "preview_resolution"
                title = "Resolution"
            }
            val previewFramerate = DropDownPreference(context).apply {
                key = "preview_framerate"
                title = "Framerate"
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