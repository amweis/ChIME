/*
 *  Copyright (C)  Easy Technoligy, Inc.
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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class ChIMEPreference extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private final String PREFERENCE_KEY = "list_preference"; 
	private String textType = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
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
