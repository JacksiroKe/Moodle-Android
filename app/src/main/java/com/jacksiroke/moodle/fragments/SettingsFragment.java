package com.jacksiroke.moodle.fragments;


import android.os.Bundle;

import androidx.annotation.Nullable;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import app.MyApplication;
import com.jacksiroke.moodle.R;
import helper.UserAccount;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String KEY_SHOW_SETTINGS = "showSettings";
    boolean themeChanged;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            getActivity().setTheme(R.style.AppTheme_NoActionBar_Dark);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);

        findPreference("notifications");

        CheckBoxPreference darkMode = findPreference("DARK_MODE");
        darkMode.setOnPreferenceChangeListener((preference, o) -> {

            this.themeChanged = true;

            MyApplication.getInstance().setDarkModeEnabled((Boolean) o);

            getActivity().recreate();

            return true;
        });

        CheckBoxPreference notifications = findPreference("notifications");

        final UserAccount userAccount = new UserAccount(getActivity());

        notifications.setChecked(userAccount.isNotificationsEnabled());

        notifications.setOnPreferenceChangeListener(((preference, o) -> {
            userAccount.setNotificationsEnabled((Boolean) o);
            return true;
        }));

    }

    @Override
    public void onSaveInstanceState(Bundle outState){

        if (themeChanged) outState.putBoolean(KEY_SHOW_SETTINGS, true);

        super.onSaveInstanceState(outState);
    }

}
