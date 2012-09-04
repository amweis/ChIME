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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.util.Log;
import android.view.KeyEvent;

public class CTextGen {
	protected ArrayList<Integer> mCodes = new ArrayList<Integer>();
	private int currInputType = 0;
	private boolean needSearch = true;
	private int analysedLen = 0;
	private boolean useLastText = false;
	private String lastSelectText = null;
	final private int MAX_MORE_TEXT = 15;
	final private int MAX_CACHE_TEXT = 100;
	final private CacheText mCache = new CacheText (MAX_CACHE_TEXT);
	
	protected native int buildTextDict (byte[] buf, int len); 
	protected native int cleanInput ();
	protected native boolean widthVisit (int code);
	protected native boolean depthVisit (int code);
	protected native String getInputText ();
	protected native String getMoreText (int maxSum);
	protected native void nativeHandleDelete ();
	protected native void releaseDict ();

 	public CTextGen () {
	}
 	public void release () {
 		this.cleanInputCodes();
 		this.releaseDict();
 	}
	public boolean init (Resources resource, int id) {
		if (id == currInputType) {
			return false;
		}
		mCache.writeCache(currInputType);
		mCache.readCache(id);
		byte[] buf = null;
		InputStream is = null;
		try {
			AssetManager am = resource.getAssets();
			String dir = this.getDictList(id, am);
			buf = new byte[102400];
			if (dir != null) {
				String[] path = am.list(dir);
				if (path != null) {
					for (int i = 0; i < path.length; ++i) {
						is = am.open(String.format("%s/%s", dir, path[i]));
		 				if (is == null) {
							continue;
						}
						this.skipFiletype(is);
						initDict(buf, is);
					}
				}
				currInputType = id;
			}
			
		} catch (IOException e) {
			Log.v("ChIME", "CTextGen init catch exception");
			e.printStackTrace();
		}
		buf = null;
		try {
			if (is != null) {
				is.close();
				is = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	private String getDictList (int id, AssetManager am) throws IOException {
		String path = null;
		switch (id) {
		case R.integer.wubi:
			path = new String ("wb");
			break;
		case R.integer.pnyn:
			path = new String ("pin");
			break;
		case R.integer.cnj:
			path = new String ("cj");
			break;
		default:
			return null;
		}
		return path;
	}

	private void skipFiletype (InputStream is) throws IOException {
		int mk1st = is.read();
		int mk2nd = is.read();
		int mk3rd = is.read();
		if (!(mk1st == 0xef && mk2nd == 0xbb && mk3rd == 0xbf)) {
			is.skip(-3);
		}
	}
	private int initDict(byte[] buf, InputStream is)
			throws IOException {
		int offset = 0, readlen = 0;
		while ((readlen = is.read(buf, offset, buf.length - offset)) > 0) {
			readlen += offset;
			offset = buildTextDict (buf, readlen);
			if (offset > 0) {
				System.arraycopy(buf, readlen - offset, buf, 0, offset);
			}
		}
		return offset;
	}
	
	public int handleCodes(Integer code) {
		int result = 0;
		switch (code) {
		case Keyboard.KEYCODE_DELETE:
		case KeyEvent.KEYCODE_DEL:
			result = handleDelete ();
			break;
		default:
			result = handleCharacter (code);
			break;
		}
		return result;
	}
	
	public void setSelectText (String text) {
		this.lastSelectText = text;
	}
	
	public boolean onSelect (int index, String text) {
		int cachedSize = mCache.onSelect(index);
		if (cachedSize > 0) {
			while (cachedSize > 0) {
				mCodes.remove(0);
				cachedSize -= 1;
			}
		} else if (index > 0) {
			cacheText (text);
			while (analysedLen > 0) {
				mCodes.remove(0);
				analysedLen -= 1;
			}
		} else {
			mCodes.clear();
		}
		this.needSearch = true;
		this.useLastText = false;
		this.cleanInput();
		lastSelectText = index > 0 ? text : null;
		analysedLen = 0;
		if (!mCodes.isEmpty()) {
			this.matchCodes();
		}
		return true;
	}

	private void cacheText(String text) {
		StringBuilder sb = new StringBuilder ();
		for (int i = 0; i < analysedLen; ++i) {
			sb.append((char)mCodes.get(i).intValue());
		}
		mCache.putText(sb.toString(), text);
	}
	
	private int handleCharacter(Integer code) {
		synchronized(this) {
			pushCode (code);
			matchCodes ();
		}
		return 0;
	}

	// 
	private void matchCodes() {
		if (needSearch && R.integer.wubi == this.currInputType && mCodes.get(0) == 'z') {
			useLastText = true;
			needSearch = false;
			return;
		}
		int size = this.mCodes.size();
		int pos = this.analysedLen;
		while (needSearch && pos < size) {
			int code = mCodes.get(pos);
			mCache.matchCodes((char)code);
			switch (this.currInputType) {
			case R.integer.wubi:
				needSearch = this.widthVisit(code);
				break;
			default:
				needSearch = this.depthVisit(code);
				break;
			}
			if (needSearch) {
				pos += 1;
			}
		}
		analysedLen = pos;
	}

	
	private void pushCode(Integer code) {
		this.mCodes.add(code);
	}

	private int handleDelete() {
		int size = this.mCodes.size();
		if (size == 0) {
			return Keyboard.KEYCODE_DELETE;
		}
		// recover the analysis 
		if (this.analysedLen == size) {
			this.nativeHandleDelete();
			this.analysedLen -= 1;
			this.needSearch = true;
			if (this.useLastText) {
				this.useLastText = false;
			}
		}
		mCodes.remove(size - 1);
		mCache.onDelete(mCodes.size());
		return 0;
	}
	
	
	/**
	 * generate text what the user pressed.
	 * @return
	 */
	public ArrayList<String> getText(ArrayList<String> result) {
		if (this.useLastText) {
			if (this.lastSelectText != null) {
				result.add(this.lastSelectText);
				this.analysedLen = 1;
			}
		} else {
			String text = mCache.getCacheText ();
			parseInputText(result, text);
			text = this.getInputText();
			parseInputText(result, text);
			text = this.getMoreText(MAX_MORE_TEXT);
			parseInputText(result, text);
		}
		return result;
	}
	
	public String codesToText() {
		if (mCodes.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder ();
		for (int i = 0; i < this.mCodes.size(); ++i) {
			sb.append((char)mCodes.get(i).intValue());
		}
		return sb.toString();
	}
	
	private void parseInputText(ArrayList<String> result, String text) {
		if (text != null) {
			String[] arrtext = text.split("\n");
			for (int i = 0; i < arrtext.length; ++i) {
				if (!result.contains(arrtext[i])) {
					result.add(arrtext[i]);
				}
			}
		}
	}
	
	public void cleanInputCodes () {
		this.cleanInput();
		this.mCodes.clear();
		this.analysedLen = 0;
		this.useLastText = false;
		this.needSearch = true;
	}
	
	/**
	 * needUpdateText check if all character input has been done. if so, then the text should be updated to candidate view
	 * otherwise (for example, we have reached the last child while the input character continued, we should not call getText
	 * again and again and again.
	 * @return
	 */
	public boolean needUpdateText () {
		if (this.needSearch) {
			return true;
		} else if (this.useLastText && this.mCodes.size() == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	public void writeCache () {
		this.mCache.writeCache(this.currInputType);
	}
}