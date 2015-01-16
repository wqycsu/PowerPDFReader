package com.csu.powerpdf;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import com.csu.utils.MyHashMap;

public class PdfListAdapter extends BaseAdapter{
	private ArrayList<MyHashMap<String,ArrayList<String>>> fileList;
	private ArrayList<String> directories;
	private LayoutInflater inflater;
	private Context context;
	private String pdfFileName;
	private String tempStr;
	private ArrayList<String> dataList;
	private ArrayList<String> chooseList;
	private Filter filter;
	private Button deleteBtn = null;
	private DeletePdfCallback deltePdfCallback;
	
	private ListView listView;
	
	static class DeletePdfCallback{
		public void deletePdf(String path){};
	}
	
	public void setDeletePdfCallback(DeletePdfCallback deltePdfCallback){
		this.deltePdfCallback = deltePdfCallback;
	}
	
	private ListViewItemClickCallback listViewItemClickCallback;
	
	static class ListViewItemClickCallback{
		public void onClick(View view,int position){};
		public void onLongClick(View view,int position){};
	}
	
	public void setListViewItemClickCallback(ListViewItemClickCallback listViewItemClickCallback){
		this.listViewItemClickCallback = listViewItemClickCallback;
	}
	
	public PdfListAdapter(Context context,
			ArrayList<MyHashMap<String, ArrayList<String>>> fileList,
			ArrayList<String> directories) {
		this.context = context;
		this.inflater = LayoutInflater.from(context);
		this.fileList = fileList;
		this.directories = directories;
		
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	public ArrayList<String> setListData(ArrayList<String> directories,ArrayList<MyHashMap<String,ArrayList<String>>> fileList){
		ArrayList<String> dataList = new ArrayList<String>();
		if(directories.size()==0||fileList.size()==0)
		{
			return dataList;
		}
		for(int i=0;i<directories.size()&&i<fileList.size();i++)
		{
			if(fileList.get(i).get(directories.get(i))!=null&&fileList.get(i).get(directories.get(i)).size()>0){
				dataList.add(directories.get(i));
				for(int j=0;j<fileList.get(i).get(directories.get(i)).size();j++)
				{
					tempStr = fileList.get(i).get(directories.get(i)).get(j);
					dataList.add(tempStr);
				}
			}
		}
		return dataList;
	}
	
	public void setSearchDataList(ArrayList<String> list){
		this.dataList = list;
	}
	
	public int getListDataSize(){
		return this.dataList.size();
	}
	
	public void setDataList(ArrayList<String> list)
	{
		this.dataList = list;
	}
	
	public void setChooseList(ArrayList<String> list){
		this.chooseList = list;
	}
	
	public void setListView(ListView listView){
		this.listView = listView;
	}
	
	public ArrayList<String> getDataList(){
		return this.dataList;
	}
	float firstX = 0;
	float firstY = 0;
	float lastX = 0;
	float lastY = 0;
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(dataList.size()==0)
			return null;
		if(!dataList.get(position).contains(".pdf"))
			convertView = inflater.inflate(R.layout.pdflist_item_title, null);
		else{
			convertView = inflater.inflate(R.layout.pdflist_item_files, null);
			convertView.setTag(position);
			final int index = position;
			convertView.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					deleteBtn = (Button)v.findViewById(R.id.pdflist_delete);
					if(deleteBtn!=null&&deleteBtn.getVisibility()==View.VISIBLE){
						deleteBtn.setOnClickListener(new OnClickListener(){
				
							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								deltePdfCallback.deletePdf(dataList.get(index));
							}
							
						});
					}
					switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						firstX = event.getX();
						firstY = event.getY();
						break;
					case MotionEvent.ACTION_MOVE:
						lastX = event.getX();
						lastY = event.getY();
						
						if(Math.abs(lastX-firstX)>Math.abs(lastY-firstY)){
							if(listView!=null){
								listView.requestDisallowInterceptTouchEvent(true);
							}
						}
						if((lastX-firstX)<-20){
							MotionEvent cancelEvent = MotionEvent.obtain(event); 
							cancelEvent.setAction(MotionEvent.ACTION_CANCEL|(event.getActionIndex()<< MotionEvent.ACTION_POINTER_INDEX_SHIFT));
							v.onTouchEvent(cancelEvent);
							if(deleteBtn.getVisibility()==View.INVISIBLE){
								deleteBtn.setVisibility(View.VISIBLE);
								Animation animation = AnimationUtils.loadAnimation(context,R.anim.delete_button_scalein);
								deleteBtn.startAnimation(animation);
							}
							deleteBtn.setVisibility(View.VISIBLE);
							cancelEvent.recycle();
							return true;
						}else if((lastX-firstX)>20){
							MotionEvent cancelEvent = MotionEvent.obtain(event); 
							cancelEvent.setAction(MotionEvent.ACTION_CANCEL|(event.getActionIndex()<< MotionEvent.ACTION_POINTER_INDEX_SHIFT));
							v.onTouchEvent(cancelEvent);
							if(deleteBtn.getVisibility()==View.VISIBLE){
								Animation animation = AnimationUtils.loadAnimation(context,R.anim.delete_button_scaleout);
								deleteBtn.startAnimation(animation);
								deleteBtn.setVisibility(View.INVISIBLE);
							}
							cancelEvent.recycle();
							return true;
						}
						break;
					case MotionEvent.ACTION_UP:
						return false;
						
					}
					return v.onTouchEvent(event);
				}
			});
			convertView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Log.d("weiquanyun","pdflist item onClick");
					listViewItemClickCallback.onClick(v,index);
				}
			});
			convertView.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View v) {
					// TODO Auto-generated method stub
					listViewItemClickCallback.onLongClick(v,index);
					return true;
				}
				
			});
		}
		TextView title = (TextView)convertView.findViewById(R.id.pdflist_text);
		tempStr = dataList.get(position);
		if(!dataList.get(position).contains(".pdf"))
			title.setText(tempStr);
		else{
			int index = tempStr.lastIndexOf('/');
			pdfFileName = tempStr.substring(index+1, tempStr.length());
			title.setText(pdfFileName);
			if(chooseList!=null){
				if(chooseList.contains(tempStr))
					convertView.setBackgroundColor(context.getResources().getColor(R.color.button_pressed));
			}
		}
		return convertView;
	}
	
	/*float firstX = 0;
	float lastX = 0;
	@Override
	public boolean onTouch(View v,MotionEvent event){
		final int position = (Integer)(v.getTag());
		deleteBtn = (Button)v.findViewById(R.id.pdflist_delete);
		v.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				listViewItemClickCallback.onClick(v,position);
			}
		});
		v.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				listViewItemClickCallback.onLongClick(v,position);
				return true;
			}
			
		});
		if(deleteBtn!=null&&deleteBtn.getVisibility()==View.VISIBLE){
			deleteBtn.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Log.d("weiquanyun","deleteBtn onClick==========="+position+dataList.get(position));
					deltePdfCallback.deletePdf(dataList.get(position));
				}
				
			});
		}
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			firstX = event.getX();
			break;
		case MotionEvent.ACTION_MOVE:
			lastX = event.getX();
			if((lastX-firstX)<-20){
				if(deleteBtn.getVisibility()==View.INVISIBLE){
					deleteBtn.setVisibility(View.VISIBLE);
					Animation animation = AnimationUtils.loadAnimation(context,R.anim.delete_button_scalein);
					deleteBtn.startAnimation(animation);
				}
				deleteBtn.setVisibility(View.VISIBLE);
			}
			if((lastX-firstX)>20){
				if(deleteBtn.getVisibility()==View.VISIBLE){
					Animation animation = AnimationUtils.loadAnimation(context,R.anim.delete_button_scaleout);
					deleteBtn.startAnimation(animation);
					deleteBtn.setVisibility(View.INVISIBLE);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
//			lastX = event.getX();
//			if(Math.abs(lastX-firstX)<3){
//				if(!isLongPress){
//					listViewItemClickCallback.onClick(v,position);
//				}else{
//					listViewItemClickCallback.onLongClick(v,position);
//				}
//			}
		}
		return true;
		
	}*/
	
	public Filter getFilter(){
		if(filter==null)
			filter = new ListFileFilter(fileList);
		return filter;
	}
	
	class ListFileFilter extends Filter{
		ArrayList<MyHashMap<String,ArrayList<String>>> originalList;
		
		public ListFileFilter(ArrayList<MyHashMap<String,ArrayList<String>>> list){
			this.originalList = list;
		}
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// TODO Auto-generated method stub
			FilterResults result = new FilterResults();
			if(constraint==null||constraint.length()==0){
				ArrayList<String> dataList = new ArrayList<String>();
				dataList = setListData(directories, originalList);
				result.values = dataList;
				result.count = dataList.size();
			}else{
				ArrayList<String> dataList = new ArrayList<String>();
				for(int i=0;i<directories.size()&&i<originalList.size();i++)
				{
					if(originalList.get(i).get(directories.get(i))!=null&&originalList.get(i).get(directories.get(i)).size()>0){
						dataList.add(directories.get(i));
						int flag = 0;
						for(int j=0;j<originalList.get(i).get(directories.get(i)).size();j++)
						{
							tempStr = originalList.get(i).get(directories.get(i)).get(j);
							int index = tempStr.lastIndexOf('/');
							pdfFileName = tempStr.substring(index+1, tempStr.length());
							if(pdfFileName.toLowerCase().contains(constraint))
							{
								dataList.add(tempStr);
								flag++;
							}
						}
						if(flag==0)
							dataList.remove(directories.get(i));
					}
				}
				result.values = dataList;
				result.count = dataList.size();
			}
			return result;
		}
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			// TODO Auto-generated method stub
			dataList = (ArrayList<String>)results.values;
			notifyDataSetChanged();
		}
	}
	
}
