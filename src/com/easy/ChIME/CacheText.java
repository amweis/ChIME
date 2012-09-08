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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import android.util.Log;


public class CacheText {
	private TextItem[] mItems;
	private int startPos = 0;
	private int counterStored = 0;
	private boolean changed = false;
	private boolean needSearch = true;
	private Stack<HashMap<TextItem, Integer>> mCache = new Stack<HashMap<TextItem, Integer>>();
	
	public CacheText (int size) {
		mItems = new TextItem[size];
	}
	
	public void putText(String codes, String text) {
		if (counterStored < mItems.length) {
			mItems[counterStored] = new TextItem (codes, text);
			counterStored += 1;
		} else {
			mItems[startPos].setItem(codes, text);
			startPos += 1;
			startPos %= mItems.length;
		}
		changed = true;
	}

	public void readCache(int type) {
		changed = false;
		this.counterStored = 0;
		this.startPos = 0;
		File fo = getCacheFile(type);
		if (fo != null && fo.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(fo));
				while (this.counterStored < mItems.length) {
					String text = br.readLine();
					if (text == null) {
						break;
					}
					String[] cache = text.split(" ");
					if (cache != null && cache.length == 2) {
						mItems[counterStored] = new TextItem(cache[0], cache[1]);
						 ++this.counterStored;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private File getCacheFile(int type) {
		File fo = new File ("/sdcard/easy/ChIME/cache/");
		if (!fo.exists() && !fo.mkdirs()) {
			return null;
		}
		String dict = null;
		switch (type) {
		case R.integer.wubi:
			dict = new String ("wb.cache");
			break;
		case R.integer.cnj:
			dict = new String ("cnj.cache");
			break;
		case R.integer.pnyn:
			dict = new String ("pnyn.cache");
			break;
		default:
			return null;
		}
		return new File (fo, dict);
	}

	public void writeCache (int type) {
		if (!changed) {
			return;
		}
		File fo = getCacheFile (type);
		if (fo != null) {
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter (new FileWriter (fo));
				for (int i = 0; i < this.counterStored; ++i) {
					bw.write(mItems[i].toString());
					bw.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				bw = null;
			}
		}
		changed = false;
	}

	public void matchCodes(char code) {
		if (this.counterStored > 0 && needSearch) {
			if (mCache.isEmpty()) {
				matchAll (code);
			} else {
				matchCached (code);
			}
			Log.v("ChIME", String.format("Cache.matchCodes,  code = %c, counter = %d, needSearch = %s, matchLen = %d",
					code, counterStored, needSearch, mCache.size()));
		}
	}

	private void matchCached(char code) {
		HashMap<TextItem, Integer> cache = null;
		HashMap<TextItem, Integer> top = mCache.peek();
		Set<TextItem> keys = top.keySet();
		Iterator<TextItem> iter = keys.iterator();
		while (iter.hasNext()) {
			TextItem item = iter.next();
			int start = top.get(item) + 1;
			int matchedPos = item.matchCode(code, start);
			if (matchedPos > -1) {
				if (cache == null) {
					cache = new HashMap<TextItem, Integer>();
				}
				cache.put(item, matchedPos);
			}
		}
		if (cache != null) {
			mCache.add(cache);
		} else {
			needSearch = false;
		}
	}

	private void matchAll(char code) {
		HashMap<TextItem, Integer> cache = null;
		for (int i = 0; i < counterStored; ++i) {
			int pos = mItems[i].matchCode(code, 0);
			if (0 == pos) {
				if (cache == null) {
					cache = new HashMap<TextItem, Integer>();
				}
				cache.put(mItems[i], 0);
			}
		}
		if (cache != null) {
			mCache.add(cache);
		} else {
			needSearch = false;
		}
	}

	public int onSelect (int index) {
		this.needSearch = true;
		int result = 0;
		if (!mCache.isEmpty()) {
			int len = mCache.peek().size();
			if (index > 0 && index <= len) {
				result = mCache.size();
			}
			mCache.clear();
		}
		return result;
	}

	public void setCacheSize(int size) {
		if (size != mItems.length) {
			TextItem[] cache = new TextItem[size];
			int right = counterStored - startPos;
			int left = startPos;
			if (right >= size) {
				System.arraycopy(mItems, startPos, cache, 0, size);
				counterStored = size;
			} else {
				System.arraycopy(mItems, startPos, cache, 0, right);
				counterStored = right;
				size -= right;
				if (left >= size) {
					System.arraycopy(mItems, 0, cache, right, size);
					counterStored += size;
				} else {
					System.arraycopy(mItems, 0, cache, right, left);
					counterStored += left;
				}
			}
			mItems = cache;
			startPos = 0;
		}
	}

	public String getCacheText() {
		if (mCache.isEmpty()) {
			return null;
		}
		HashMap<TextItem, Integer> top = mCache.peek();
		Set<TextItem> items = top.keySet();
		Iterator<TextItem> iter = items.iterator();
		StringBuilder sb = new StringBuilder ();
		while (iter.hasNext()) {
			TextItem t = iter.next();
			sb.append(t.getText());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public void onDelete (int codeslen) {
		if (codeslen > mCache.size()) {
			return;
		}
		this.needSearch = true; 
		if (codeslen < mCache.size()) {
			mCache.pop();
		}
	}
}