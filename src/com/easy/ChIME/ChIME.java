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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import java.util.List;

public class ChIME extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener, Runnable {
    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    private ArrayList<String> mText = new ArrayList<String>();
    private ArrayList<String> mSuggestions = null;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    
    private Keyboard mSymbolsKeyboard;
    private Keyboard mSymbolsShiftedKeyboard;
    private Keyboard mQwertyKeyboard;
    
    private Keyboard mCurKeyboard;
    private String mWordSeparators;
    private int inputText = -1;
    private final String PREFERENCE_KEY = "list_preference";
    private final int KEYCODE_QUICK_COM = -10;
    private final int KEYCODE_CHANGE_KEYBOARD = -11;
    
    private Key mChangeKey = null;
    private boolean inEnglish = false;
    
    private CTextGen mTextGen = new CTextGen ();
    enum DictStatus {
    	DS_NONE, DS_IN_INIT, DS_FINISHED
    }
    private DictStatus mdictStatus = DictStatus.DS_NONE;
    
    public static boolean isDebug () {
    	return true;
    }
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
    	//showProperties ();
    	super.onCreate();
        mWordSeparators = getResources().getString(R.string.word_separators);
        this.initText();
    }
    
    private int getTextType (SharedPreferences sp) {
    	String text = sp.getString(PREFERENCE_KEY, this.getString(R.string.default_im));
    	if (text.equals("pinyin")) {
    		return R.integer.pnyn;
    	} else if (text.equals("cangjie")) {
    		return R.integer.cnj;
    	} else {
    		return R.integer.wubi;
    	}
    }
    
    protected void changeTextType(int arg3) {
    	synchronized(this) {
    		if (this.inputText != arg3) {
    			mTextGen.release();
    			mTextGen.init(this.getResources(), arg3);
    			this.inputText = arg3;
    			changeKeyLabel (getTextName (arg3));
    		}
    	}
	}
    
	private String getTextName(int arg3) {
		this.inEnglish = false;
		switch (arg3) {
		case R.integer.wubi:
			return "Îå";
		case R.integer.pnyn:
			return "Æ´";
		case R.integer.cnj:
			return "²Ö";
		default:
			this.inEnglish = true;
			return "En";
		}
	}
	
	private void changeKeyLabel (String text) {
		if (this.mChangeKey != null && text != null) {
			mChangeKey.label = text;
		}
	}
	/**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new Keyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new Keyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new Keyboard(this, R.xml.symbols_shift);
        getChangeInputKey ();
        this.changeKeyLabel(this.getTextName(this.inputText));
    }
    
    private void getChangeInputKey () {
    	if (mQwertyKeyboard != null) {
    		List<Key> keys = mQwertyKeyboard.getKeys();
    		if (keys != null && !keys.isEmpty()) {
    			int count = keys.size();
    			for (int i = 0; i < count; ++i) {
    				Key k = keys.get(i);
    				if (k.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
    					this.mChangeKey = k;
    					return;
    				}
    			}
    		}
    	}
    }
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        //mInputView = (KeyboardView) getLayoutInflater().inflate(
        //        R.layout.input, null);
    	Context c = this.getApplicationContext();
    	Resources r = this.getResources();
    	XmlResourceParser parser = r.getXml(R.layout.input);
    	AttributeSet as = Xml.asAttributeSet(parser);
    	mInputView = new LatinKeyboardView (c, as);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        initInputContext ();
        mCompletionOn = false;
        mCompletions = null;
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                mCurKeyboard = mSymbolsKeyboard;
                break;
            case EditorInfo.TYPE_CLASS_PHONE:
                mCurKeyboard = mSymbolsKeyboard;
                break;
            case EditorInfo.TYPE_CLASS_TEXT:
                mCurKeyboard = mQwertyKeyboard;
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    mCompletionOn = isFullscreenMode();
                }
                updateShiftKeyState(attribute);
                break;
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
    }
	private void initInputContext () {
		this.setSuggestions(null, false, false);
		this.mTextGen.cleanInputCodes();
	}

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        this.initInputContext ();
        setCandidatesViewShown(false);
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
        mTextGen.writeCache();
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if ((newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, true);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                return false;
            case KeyEvent.KEYCODE_DEL:
            	if (this.handleCharacter(keyCode, null) == 0) {
            		return true;
            	}
            	break;
            default:
            	int c = event.getUnicodeChar();
            	if (c > 0) {
            		onKey (c, null);
            		return true;
            	}
        }
        
        return super.onKeyDown(keyCode, event);
    }
	private int processKey(int keyCode) {
		int result = mTextGen.handleCodes(keyCode);
		if (mTextGen.needUpdateText()) {
			getSuggestions ();
		} else {
			updateSuggestions ();
		}
		this.setSuggestions(mSuggestions, false, mSuggestions != null);
		return result;
	}

    private void updateSuggestions() {
		String text = mTextGen.codesToText();
		if (text != null) {
			mSuggestions = new ArrayList<String>();
			mSuggestions.add(text);
			mSuggestions.addAll(mText);
		} else {
			mText.clear();
			mSuggestions = null;
		}
	}
	/**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            commitText(this.getCurrentInputConnection(), 0);
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
            return;
        }
		switch (primaryCode) {
		case Keyboard.KEYCODE_SHIFT:
			handleShift();
			break;
		case Keyboard.KEYCODE_CANCEL:
			handleClose();
			break;
		case Keyboard.KEYCODE_MODE_CHANGE:
			Log.v("ChIME", "Chime, change mode");
			this.changeKeyLabel(this.getTextName(inEnglish ? this.inputText : -1));
			break;
		case KEYCODE_QUICK_COM:
			InputConnection ic = this.getCurrentInputConnection();
			commitText (ic, 0);
			String text = this.mQwertyKeyboard.isShifted() ? ".COM" : ".com";
			ic.commitText(text, text.length());
			break;
		case KEYCODE_CHANGE_KEYBOARD:
			if (mInputView != null) {
				Keyboard current = mInputView.getKeyboard();
				if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
					current = mQwertyKeyboard;
				} else {
					current = mSymbolsKeyboard;
				}
				mInputView.setKeyboard(current);
				if (current == mSymbolsKeyboard) {
					current.setShifted(false);
				}
			}
			break;
		default:
			if (handleCharacter(primaryCode, keyCodes) != 0) {
				if (primaryCode == Keyboard.KEYCODE_DELETE) {
					this.keyDownUp(KeyEvent.KEYCODE_DEL);
				} else {
					this.sendKey(primaryCode);
				}
			}
			break;
		}
    }
	private void commitText(InputConnection ic, int index) {
		if (this.mSuggestions != null && mSuggestions.size() > index && ic != null) {
			String t = mSuggestions.get(index);
		    ic.commitText(t, t.length());
		    this.mTextGen.onSelect(index, t);
		}
		getSuggestions ();
		this.setSuggestions(mSuggestions, false, mSuggestions != null);
	}

    private void getSuggestions() {
		mSuggestions = null;
		mText.clear();
		String keys = mTextGen.codesToText();
		if (keys != null) {
			mSuggestions = new ArrayList<String>();
			mSuggestions.add(keys);
			mTextGen.getText(mText);
			mSuggestions.addAll(mText);
		}
	}
    
	public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        this.commitText(ic, 0);
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }
    
    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private int handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                return Character.toUpperCase(primaryCode);
            } else if (mInputView.getKeyboard() != this.mQwertyKeyboard) {
            	return primaryCode;
            } else if (inEnglish) {
            	return primaryCode;
            }
        }
        return processKey(primaryCode);
    }

    private void handleClose() {
        requestHideSelf(0);
        this.initInputContext();
        mTextGen.writeCache();
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.restoreStatus();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else {
        	this.commitText(this.getCurrentInputConnection(), index);
        }
    }
    
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }
    

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }
    
    @Override
	public void swipeLeft() {
	}
	public void initText() {
		synchronized (this) {
			if (this.mdictStatus == DictStatus.DS_IN_INIT) {
				return;
			}
			this.mdictStatus = DictStatus.DS_IN_INIT;
		}
		Thread t = new Thread (this);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}
	
	static {
		try {
			System.loadLibrary("wordlib");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
			changeTextType(getTextType(sp));
			synchronized(this) {
				mdictStatus = DictStatus.DS_FINISHED;
			}
			try {
				PreferenceSynch synch = PreferenceSynch.instance();
				synchronized (synch) {
					synch.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
