package com.csu.loading;

import com.csu.powerpdf.R;
import com.csu.utils.FileUtils;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

public class LoadingActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_layout);
		LoadingFileTask task = new LoadingFileTask(LoadingActivity.this, FileUtils.getFileUtils().getFilesList(), FileUtils.getFileUtils().getDirectories());
		task.execute();
	}
	
}
