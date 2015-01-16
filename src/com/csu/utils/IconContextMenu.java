package com.csu.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class IconContextMenu implements DialogInterface.OnCancelListener ,DialogInterface.OnDismissListener{
	private static final int LIST_PREFERED_HEIGHT = 65;
	private int dialogId;
	private Activity parentActivity;
	private IconContextMenuAdapter menuAdapter;
	private IconContextMenuOnClickListener listener;
	public IconContextMenu(int dialogId, Activity parentActivity) {
		super();
		this.dialogId = dialogId;
		this.parentActivity = parentActivity;
		menuAdapter = new IconContextMenuAdapter(parentActivity);
	}
	
	public void setOnClick(IconContextMenuOnClickListener listener){
		this.listener = listener;
	}
	
	/**
	 * Create menu
	 * @return
	 */
	public Dialog createMenu(String menuItitle) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        builder.setTitle(menuItitle);
        builder.setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialoginterface, int i) {
				IconContextMenuItem item = (IconContextMenuItem) menuAdapter.getItem(i);
				if (listener != null) {
					listener.onClick(item.actionTag);
				}
			}
		});

        builder.setInverseBackgroundForced(true);
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(this);
        dialog.setOnDismissListener(this);
        return dialog;
	}	
	/**
	 * Add menu item
	 * @param menuItem
	 */
	public void addItem(Resources res, CharSequence title,
			int imageResourceId, int actionTag) {
		menuAdapter.addItem(new IconContextMenuItem(res, title, imageResourceId, actionTag));
	}
	
	public void addItem(Resources res, int textResourceId,
			int imageResourceId, int actionTag) {
		menuAdapter.addItem(new IconContextMenuItem(res, textResourceId, imageResourceId, actionTag));
	}
	@Override
	public void onDismiss(DialogInterface dialog) {
		
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		
	}
    
	public interface IconContextMenuOnClickListener{
		public abstract void onClick(int menuId);
	}
	
	protected class IconContextMenuItem{
		public final CharSequence text;
		public final Drawable image;
		public final int actionTag;
		/**
		 * public construct
		 * @param title
		 * @param icon
		 * @param actionId
		 */
		public IconContextMenuItem(Resources res,CharSequence title,int imageResourceId,
				int actionTag) {
			super();
			text = title;
			if(imageResourceId != -1){
				image = res.getDrawable(imageResourceId);
			}else{
				image = null;
			}
			this.actionTag = actionTag;
		}
		/**
		 * public construct
		 * @param res
		 * @param text
		 * @param imageResourceId
		 * @param actionTag
		 */
		public IconContextMenuItem(Resources res,int textResourceId,int imageResourceId,
				int actionTag) {
			super();
			this.text = res.getString(textResourceId);
			if(imageResourceId != -1){
				image = res.getDrawable(imageResourceId);
			}else{
				image = null;
			}
			this.actionTag = actionTag;
		}
	}
	/**
	 * Adapter like List Adapter
	 * @author wqy
	 */
	protected class IconContextMenuAdapter extends BaseAdapter{
		ArrayList<IconContextMenuItem> mItem = new ArrayList<IconContextMenuItem>();
		Context context;
 		public IconContextMenuAdapter(Context context) {
			super();
			this.context = context;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mItem.size();
		}
		
		@Override
		public Object getItem(int position) {
			return mItem.get(position);
		}
		public void addItem(IconContextMenuItem item){
			mItem.add(item);
		}
		@Override
		public long getItemId(int position) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);
			return item.actionTag;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);
			
			Resources res = parentActivity.getResources();
			
			if (convertView == null) {
	        	TextView temp = new TextView(context);
	        	AbsListView.LayoutParams param = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 
	        																  AbsListView.LayoutParams.WRAP_CONTENT);
	        	temp.setLayoutParams(param);
	        	temp.setPadding((int)toPixel(res, 15), 0, (int)toPixel(res, 15), 0);
	        	temp.setGravity(android.view.Gravity.CENTER_VERTICAL);
	        	
	        	Theme th = context.getTheme();
				TypedValue tv = new TypedValue();
				
				if (th.resolveAttribute(android.R.attr.textAppearanceLargeInverse, tv, true)) {
					temp.setTextAppearance(context, tv.resourceId);
				}
	        	
	        	temp.setMinHeight(LIST_PREFERED_HEIGHT);
	        	temp.setCompoundDrawablePadding((int)toPixel(res,14));
	        	convertView = temp;
			}
			
			TextView textView = (TextView) convertView;
			textView.setTag(item);
			textView.setText(item.text);
			textView.setTextColor(Color.BLACK);
			textView.setCompoundDrawablesWithIntrinsicBounds(item.image, null, null, null);
			Log.i("roy", "textview's width is " + textView.getWidth()+" and height is " + textView.getHeight());
	        return textView;
		}
		
		private float toPixel(Resources res, int dip) {
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
			return px;
		}
	}
	
}
