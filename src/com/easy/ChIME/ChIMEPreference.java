/*
 *  Copyright (C)  Easy Technology, Inc.
 *
 *  Authors: Roy Wei (Wei shufeng)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301, USA.
 */
package com.easy.ChIME;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.inputmethod.InputMethodManager;

public class ChIMEPreference extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private final String PREFERENCE_KEY = "list_preference"; 
	private final String IME_CHANGE = "select_ime";
	private String textType = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
		Preference prc = this.findPreference(IME_CHANGE);
		prc.setOnPreferenceClickListener(new OnPreferenceClickListener () {

			@Override
			public boolean onPreferenceClick(Preference arg0) {
				showInputMethods ();
				return true;
			}
			
		});
	}
	
	protected void showInputMethods() {
		InputMethodManager ima = (InputMethodManager)this.getSystemService(INPUT_METHOD_SERVICE);
		ima.showInputMethodPicker();
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		sp.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(PREFERENCE_KEY)) {
			ListPreference lp = (ListPreference)this.findPreference(this.PREFERENCE_KEY);
			String text = lp.getValue();
			if (text != textType) {
				textType = text;
				PreferenceSynch synch = PreferenceSynch.instance();
				synchronized (synch) {
					synch.notify();
				}
			}
		}
	}
}
