package com.csu.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class PopupWindowListView extends ListView {
	
	private Context context;
	
	public PopupWindowListView(Context context) {
		super(context);
	}

	public PopupWindowListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public PopupWindowListView(Context context,int []imgId,int []textId,int height){
		super(context);
		this.context = context;
		setAdapter(new PopupWindowAdapter(imgId, textId, height));
	}
	
	class PopupWindowAdapter extends BaseAdapter{
		
		private RelativeLayout [] views;
		
		public PopupWindowAdapter(int[] itemImageId,int[] itemTextsId,int height){
			views = new RelativeLayout[itemTextsId.length];
			for(int i=0;i<views.length;i++){
				views[i] = makeView(itemTextsId[i],itemImageId[i],height);
			}
		}
		
		public RelativeLayout makeView(int textId,int imageId,int height){
			RelativeLayout itemView = new PopupWindowLayout(getContext(), textId, imageId, height);
			LayoutParams params = new LayoutParams((int)(350), (int)(height));
			itemView.setLayoutParams(params);
			return itemView;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return views.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return views[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if(convertView==null)
				return views[position];
			else
				return (RelativeLayout)convertView;
		}
		
	}
}
