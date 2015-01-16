package com.csu.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceHandler {
	private SharedPreferences preference;
	private SharedPreferences.Editor _editor;
	private static final String SharedPreferencesName = "PowerPDFReader";
	public static final String FileList = "PDFFileList";
	private Context context;
	public SharedPreferenceHandler(Context context){
		this.context = context;
		preference = context.getSharedPreferences(SharedPreferencesName,Context.MODE_PRIVATE);
		_editor = preference.edit();
	}
	
	public void setFileListPreference(ArrayList<String> list){
		_editor.clear();
		for(int i=0;i<list.size();i++)
			_editor.putString(FileList+i, list.get(i));
		_editor.commit();
	}
	
	public ArrayList<String> getFileListPreference(){
		ArrayList<String> list = new ArrayList<String>();
		String temp;
		int i = 0;
		while((temp = preference.getString(FileList+i++, null))!=null)
			list.add(temp);
		return list;
	}
	
	public void clearPreference(){
		_editor.putStringSet(FileList, null);
		_editor.clear();
		_editor.commit();
	}
}
