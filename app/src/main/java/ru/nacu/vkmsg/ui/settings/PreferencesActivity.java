package ru.nacu.vkmsg.ui.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 6/26/12 2:29 PM
 */
public final class PreferencesActivity
        extends android.preference.PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private PreferenceCategory pollSettings;
    private ListPreference connectionType;
    private ListPreference timeout;
    private CheckBoxPreference connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        connect = (CheckBoxPreference) findPreference("always_connect");
        pollSettings = (PreferenceCategory) findPreference("poll_settings");
        connectionType = (ListPreference) findPreference("connection_type");
        timeout = (ListPreference) findPreference("poll_time");

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        final String val = connectionType.getValue();
        connectionType.setSummary(connectionType.getTitle() + ": " +
                ("PUSH".equals(val) ? getString(R.string.push_connection) : getString(R.string.poll_connection)));

        final String val2 = timeout.getValue();
        timeout.setSummary(timeout.getTitle() + ": " + val2);
        pollSettings.setEnabled(!"PUSH".equals(val) && connect.isChecked());

        if (Build.VERSION.SDK_INT <= 7) {
            connectionType.setEnabled(false);
            connectionType.setValue("POLL");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if ("connection_type".equals(s)) {
            final String val = connectionType.getValue();
            connectionType.setSummary(connectionType.getTitle() + ": " +
                    ("PUSH".equals(val) ? getString(R.string.push_connection) : getString(R.string.poll_connection)));

            pollSettings.setEnabled(!"PUSH".equals(val) && connect.isChecked());
        } else if ("poll_time".equals(s)) {
            final String val = timeout.getValue();
            timeout.setSummary(timeout.getTitle() + ": " + val);
        } else if ("always_connect".equals(s)) {
            final String val = connectionType.getValue();
            pollSettings.setEnabled(!"PUSH".equals(val) && connect.isChecked());
        }
    }
}
