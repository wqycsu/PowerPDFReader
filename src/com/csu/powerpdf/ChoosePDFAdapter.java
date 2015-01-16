package com.csu.powerpdf;

import java.util.LinkedList;

import android.graphics.Interpolator.Result;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChoosePDFAdapter extends BaseAdapter {
	private LinkedList<ChoosePDFItem> mItems;
	private LayoutInflater mInflater;
	private Filter filter;
	public ChoosePDFAdapter(LayoutInflater inflater) {
		mInflater = inflater;
		mItems = new LinkedList<ChoosePDFItem>();
	}

	public void clear() {
		mItems.clear();
	}

	public void add(ChoosePDFItem item) {
		mItems.add(item);
		notifyDataSetChanged();
	}

	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int i) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	private int iconForType(ChoosePDFItem.Type type) {
		switch (type) {
		case PARENT: return R.drawable.ic_arrow_up;
		case DIR: return R.drawable.ic_dir;
		case DOC: return R.drawable.ic_pdf;
		default: return 0;
		}
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = mInflater.inflate(R.layout.picker_entry, null);
		} else {
			v = convertView;
		}
		ChoosePDFItem item = mItems.get(position);
		((TextView)v.findViewById(R.id.name)).setText(item.name);
		((ImageView)v.findViewById(R.id.icon)).setImageResource(iconForType(item.type));
		return v;
	}
	
	public Filter getFilter(){
		if(this.filter==null)
			return new FileListFilter(mItems);
		else
			return this.filter;
	}
	
	class FileListFilter extends Filter{

		LinkedList<ChoosePDFItem> originalmItems;
		
		public FileListFilter(LinkedList<ChoosePDFItem> mItems){
			this.originalmItems = mItems;
		}
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// TODO Auto-generated method stub
			FilterResults result = new FilterResults();
			if(constraint==null||constraint.length()==0){
				result.values = originalmItems;
				result.count = originalmItems.size();
				return result;
			}
			else{
				LinkedList<ChoosePDFItem> resultItems = new LinkedList<ChoosePDFItem>();
				for(int i=0;i<originalmItems.size();i++){
					Log.d("weiquanyun","mItems.get(i).name="+originalmItems.get(i).name+" constraint="+constraint);
					if(originalmItems.get(i).name.contains(constraint)){
						resultItems.add(originalmItems.get(i));
					}
				}
				result.values = resultItems;
				result.count = resultItems.size();
				return result;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			// TODO Auto-generated method stub
			mItems = (LinkedList<ChoosePDFItem>)(results.values);
			Log.d("weiquanyun","mItems.size"+mItems.size());
			notifyDataSetChanged();
		}
		
	}
	
}
