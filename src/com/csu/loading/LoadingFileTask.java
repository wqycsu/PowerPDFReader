package com.csu.loading;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.csu.powerpdf.ChoosePDFActivity;
import com.csu.utils.FileUtils;
import com.csu.utils.MyHashMap;

public class LoadingFileTask extends AsyncTask<Void, Void, Boolean> {
	private ArrayList<MyHashMap<String,ArrayList<String>>> fileList;
	private ArrayList<String> directories;
	private Activity activity;
	public LoadingFileTask(Activity activity,ArrayList<MyHashMap<String,ArrayList<String>>> fileList,ArrayList<String> directories){
		this.fileList = fileList;
		this.directories = directories;
		this.activity = activity;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		// TODO Auto-generated method stub
		String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		FileUtils.getFileUtils().findFilesInPath(sdcardPath, fileList, directories);
		if(fileList==null)
			return false;
		return true;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		Intent intent = new Intent(activity,LoginActivity.class);
		activity.startActivity(intent);
		activity.finish();
	}
	
}
