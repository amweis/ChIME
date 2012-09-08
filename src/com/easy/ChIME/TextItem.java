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

public class TextItem {

	private String codes;
	private String text;

	public TextItem(String codes, String text) {
		this.setItem(codes, text);
	}

	public boolean matchText(String text) {
		if (this.text != null && text != null) {
			return this.text.equals(text);
		}
		return false;
	}

	public int matchCode (char code, int start) {
		return codes.indexOf(code, start);
	}
	
	public String getText() {
		return text;
	}
	
	public void setItem (String codes, String text) {
		this.codes = codes;
		this.text = text;
	}
	
	public String toString () {
		return String.format("%s %s", codes, text);
	}
}