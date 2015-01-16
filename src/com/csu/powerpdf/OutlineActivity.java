package com.csu.powerpdf;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class OutlineActivity extends Activity implements OnItemClickListener{
	OutlineItem mItems[];
	private ListView outlineList;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outline_activity_layout);
		outlineList = (ListView)findViewById(R.id.outline_list);
		mItems = OutlineActivityData.get().items;
		outlineList.setAdapter(new OutlineAdapter(getLayoutInflater(),mItems));
		// Restore the position within the list from last viewing
		outlineList.setSelection(OutlineActivityData.get().position);
		setResult(-1);
		outlineList.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		OutlineActivityData.get().position = outlineList.getFirstVisiblePosition();
		setResult(mItems[position].page);
		finish();
	}
}
