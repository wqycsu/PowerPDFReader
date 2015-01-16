package com.csu.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.csu.powerpdf.R;
/**
 * 
 * @author lenovo
 * 存放popupwindow子项的布局
 *
 */
public class PopupWindowLayout extends RelativeLayout {

	private Context context;
	
	public PopupWindowLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PopupWindowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PopupWindowLayout(Context context,int textId,int imgId,int height) {
		super(context);
		this.context = context;
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.popupwindow_layout, null);
		ImageView icon = (ImageView)view.findViewById(R.id.popupwindow_icon);
		icon.setImageResource(imgId);
		icon.setPadding(5, 5, 5, 5);
		TextView text = (TextView)view.findViewById(R.id.popupwindow_text);
		text.setText(context.getString(textId));
		this.addView(view);
		LayoutParams params = (LayoutParams)view.getLayoutParams();
		params.height = height+2;
		view.setLayoutParams(params);
	}

}
