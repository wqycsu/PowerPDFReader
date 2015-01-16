package com.csu.powerpdf;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.csu.service.ConnectServerService;
import com.csu.utils.FileUtils;
import com.csu.utils.MyHashMap;
import com.csu.utils.PopupWindowListView;
import com.csu.utils.SharedPreferenceHandler;

public class ChoosePDFActivity extends Activity {

	private ListView pdf_list;
	private TextView pdf_file_num;
	
	private EditText search_edit;
	private ImageView search_btn;
	private ImageView other_btn;
	private ImageView search_img;
	private TextView search_text;
	private ImageView search_cancel;
	
	private ImageView cancelChoose;
	private TextView numberChoose;
	private ImageView deleteChoose;
	private ImageView otherChoose;
	
	private RelativeLayout search_layout;
	private RelativeLayout toptoolbar;
	private RelativeLayout longClickOperLayout;
	
	private ArrayList<String> directories = FileUtils.getFileUtils().getDirectories();
	private ArrayList<MyHashMap<String,ArrayList<String>>> filesList = FileUtils.getFileUtils().getFilesList();
	private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	private PdfListAdapter pdf_list_adapter;
	
	private ListView fileDirList;
	private ChoosePDFAdapter fileListAdpater;
	static private File  mDirectory = new File(SDCARD_PATH);
	static private Map<String, Integer> mPositions = new HashMap<String, Integer>();
	private File         mParent;
	private File []      mDirs;
	private File []      mFiles;
	private Handler	     mHandler;
	private Runnable     mUpdateFiles;
	private PopupWindow settingPopupWindow;
	private int[] textRsId = {R.string.show_pdfs,R.string.show_directoy};
	private int[] imgRsId = {R.drawable.ic_pdf,R.drawable.ic_dir};
	//longclick
	ArrayList<String> chooseList = new ArrayList<String>();
	private boolean isLongClick = false;
	private boolean isTopBarShow = false;
	private boolean isSearchLayoutShow = false;
	
	//longclick other operation
	private int[] textId = {R.string.rename,R.string.mail_out,R.string.create_copy};
	private int[] imgId = {R.drawable.ic_rename,R.drawable.ic_email,R.drawable.ic_copy};
	private PopupWindow otherPopupWindow;
	//重命名
	EditText newName;
	private String nameStr;
	private long mExitTime = 0;
	private SharedPreferenceHandler handler;
	private ArrayList<String> dataList;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pdflist);
		initViews();
		initFileDirList();
		handler = new SharedPreferenceHandler(ChoosePDFActivity.this);
//		FileUtils.findFilesInPath(SDCARD_PATH, filesList, directories);
		pdf_list_adapter = new PdfListAdapter(ChoosePDFActivity.this, filesList, directories);
		dataList = pdf_list_adapter.setListData(directories, filesList);
		handler.setFileListPreference(dataList);
//		if(handler.getFileListPreference()==null)
//			handler.setFileListPreference(dataList);
//		if(dataList.size()==0)
//			pdf_list_adapter.setDataList(handler.getFileListPreference());
//		else
//			pdf_list_adapter.setDataList(dataList);
//		if(pdf_list.getVisibility()==View.VISIBLE)
//			pdf_file_num.setText(dataList.size()-directories.size()+"");
//		pdf_list.setAdapter(pdf_list_adapter);
	}

	/**
	 * 初始化views
	 */
	public void initViews(){
		pdf_list = (ListView)findViewById(R.id.pdflist);
		fileDirList = (ListView)findViewById(R.id.filelist);
		pdf_file_num = (TextView)findViewById(R.id.file_num);
		search_btn = (ImageView)findViewById(R.id.searchimage);
		other_btn = (ImageView)findViewById(R.id.otherimage);
		other_btn.setOnClickListener(new OnImageClickListener());
		toptoolbar = (RelativeLayout)findViewById(R.id.toptoolbar);
		
		search_layout = (RelativeLayout)findViewById(R.id.search_edit_layout);
		search_img = (ImageView)search_layout.findViewById(R.id.search_image);
		search_edit = (EditText)search_layout.findViewById(R.id.search_edit);
		search_text = (TextView)search_layout.findViewById(R.id.search_text);
		search_cancel = (ImageView)search_layout.findViewById(R.id.clear_search);
		
		longClickOperLayout = (RelativeLayout)findViewById(R.id.longclick_oper_layout);
		cancelChoose = (ImageView)longClickOperLayout.findViewById(R.id.cancelChoose);
		numberChoose = (TextView)longClickOperLayout.findViewById(R.id.numberChoose);
		deleteChoose = (ImageView)longClickOperLayout.findViewById(R.id.deleteChoose);
		otherChoose = (ImageView)longClickOperLayout.findViewById(R.id.otherChoose);
		
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		ArrayList<String> list = handler.getFileListPreference();
		if(list!=null)
		{
			Log.d("weiquanyun","ChoosePDFActivity-->list.size:"+list.size());
			if(list.size()==0)
			{
				FileUtils.getFileUtils().findFilesInPath(SDCARD_PATH, filesList, directories);
				dataList = pdf_list_adapter.setListData(directories, filesList);
			}
		}
		if(handler.getFileListPreference()==null)
			handler.setFileListPreference(dataList);
		if(dataList.size()==0)
			pdf_list_adapter.setDataList(handler.getFileListPreference());
		else
			pdf_list_adapter.setDataList(dataList);
		if(pdf_list.getVisibility()==View.VISIBLE)
			pdf_file_num.setText(dataList.size()-directories.size()+"");
		pdf_list.setAdapter(pdf_list_adapter);
		pdf_list_adapter.setListView(pdf_list);
		//search operation
		search_btn.setOnClickListener(new OnImageClickListener());
		search_cancel.setOnClickListener(new OnImageClickListener());
		search_edit.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				search_img.setVisibility(View.INVISIBLE);
				search_text.setVisibility(View.INVISIBLE);
				String str = s.toString();
				str = str.toLowerCase();
				if(pdf_list.getVisibility()==View.VISIBLE)
					pdf_list_adapter.getFilter().filter(str);
				else if(fileDirList.getVisibility()==View.VISIBLE)
					fileListAdpater.getFilter().filter(str);
			}

		});
		
		//choose operation
		cancelChoose.setOnClickListener(new OnImageClickListener());
		deleteChoose.setOnClickListener(new OnImageClickListener());
		otherChoose.setOnClickListener(new OnImageClickListener());
		
		PdfListAdapter.ListViewItemClickCallback listViewClickCallback = new PdfListAdapter.ListViewItemClickCallback(){
			public void onClick(View view,int position){
				if(isLongClick){
					view.setClickable(false);
					view.setBackgroundColor(Color.parseColor("#FF2572AC"));
					if(!chooseList.contains(pdf_list_adapter.getDataList().get(position))){
						chooseList.add(pdf_list_adapter.getDataList().get(position));
						pdf_list_adapter.setChooseList(chooseList);
					}else{
						chooseList.remove(pdf_list_adapter.getDataList().get(position));
						pdf_list_adapter.setChooseList(chooseList);
						view.setBackgroundColor(0);
					}
					numberChoose.setText(chooseList.size()+"");
				}else{
					view.setBackgroundColor(getResources().getColor(R.color.button_pressed));
					Uri uri = Uri.parse(pdf_list_adapter.getDataList().get(position));
					Intent intent = new Intent(ChoosePDFActivity.this,MuPDFActivity.class);
					if(uri.toString().endsWith(".pdf"))
					{
						intent.setAction(Intent.ACTION_VIEW);
						intent.setData(uri);
						startActivity(intent);
					}
					view.setBackgroundColor(0);
				}
			}
			public void onLongClick(View view,int position){
				if(!isLongClick){
					TranslateAnimation animation = new TranslateAnimation(0, 0, -longClickOperLayout.getHeight(), 0);
					animation.setDuration(300);
					longClickOperLayout.startAnimation(animation);
					longClickOperLayout.setVisibility(View.VISIBLE);
					if(toptoolbar.getVisibility()==View.VISIBLE){
						isTopBarShow = true;
						toptoolbar.setVisibility(View.INVISIBLE);
					}
					if(search_layout.getVisibility()==View.VISIBLE){
						isSearchLayoutShow = true;
						search_layout.setVisibility(View.INVISIBLE);
					}
					isLongClick = true;
					view.setBackgroundColor(getResources().getColor(R.color.button_pressed));
					view.setClickable(false);
					chooseList.add(pdf_list_adapter.getDataList().get(position));
					pdf_list_adapter.setChooseList(chooseList);
					numberChoose.setText(chooseList.size()+"");
				}
			}
		};
		
		pdf_list_adapter.setListViewItemClickCallback(listViewClickCallback);
		
		PdfListAdapter.DeletePdfCallback callback = new PdfListAdapter.DeletePdfCallback(){
			public void deletePdf(String path){
				ArrayList<String> list = new ArrayList<String>();
				list.add(path);
				deletePdfs(list);
			}
		};
		pdf_list_adapter.setDeletePdfCallback(callback);
	}

	public void initFileDirList(){
		String storageState = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(storageState)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.no_media_warning);
			builder.setMessage(R.string.no_media_hint);
			AlertDialog alert = builder.create();
			alert.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.show();
			return;
		}

		if (mDirectory == null)
			mDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

		// Create a list adapter...
		fileListAdpater = new ChoosePDFAdapter(getLayoutInflater());
		fileDirList.setAdapter(fileListAdpater);
		fileDirList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				Log.d("weiquanyun","fileDirList-->onClick "+mDirectory.getAbsolutePath()+"SDCARD_PATH = "+SDCARD_PATH);
				mPositions.put(mDirectory.getAbsolutePath(), fileDirList.getFirstVisiblePosition());
				if(mDirectory.getAbsolutePath().equals(SDCARD_PATH)){
					if(position==0)
						return;
				}
				if (position < (mParent == null ? 0 : 1)) {
					mDirectory = mParent;
					mHandler.post(mUpdateFiles);
					return;
				}
				position -= (mParent == null ? 0 : 1);
				if (position < mDirs.length) {
					mDirectory = mDirs[position];
					mHandler.post(mUpdateFiles);
					return;
				}
				position -= mDirs.length;
				Log.d("weiquanyun","mFiles[position].getAbsolutePath()==="+mFiles[position].getAbsolutePath());
				if(mFiles[position].getAbsolutePath().endsWith(".pdf")){
					Uri uri = Uri.parse(mFiles[position].getAbsolutePath());
					Intent intent = new Intent(ChoosePDFActivity.this,MuPDFActivity.class);
					intent.setAction(Intent.ACTION_VIEW);
					intent.setData(uri);
					startActivity(intent);
				}
			}

		});
		// ...that is updated dynamically when files are scanned
		mHandler = new Handler();
		mUpdateFiles = new Runnable() {
			public void run() {
				Resources res = getResources();
				
				mParent = mDirectory.getParentFile();

				mDirs = mDirectory.listFiles(new FileFilter() {

					@Override
					public boolean accept(File dir) {
						// TODO Auto-generated method stub
						if(dir.isDirectory()&&!dir.getName().startsWith("."))
							return true;
						else
							return false;
					}
				});
				if (mDirs == null)
					mDirs = new File[0];

				mFiles = mDirectory.listFiles(new FileFilter() {

					public boolean accept(File file) {
						if (file.isDirectory())
							return false;
						String fname = file.getName().toLowerCase();
						if (fname.endsWith(".pdf"))
							return true;
						return false;
					}
				});
				if (mFiles == null)
					mFiles = new File[0];

				Arrays.sort(mFiles, new Comparator<File>() {
					public int compare(File arg0, File arg1) {
						return arg0.getName().compareToIgnoreCase(arg1.getName());
					}
				});

				Arrays.sort(mDirs, new Comparator<File>() {
					public int compare(File arg0, File arg1) {
						return arg0.getName().compareToIgnoreCase(arg1.getName());
					}
				});
				fileListAdpater.clear();
				if (mParent != null)
					fileListAdpater.add(new ChoosePDFItem(ChoosePDFItem.Type.PARENT, getString(R.string.parent_directory)));
				for (File f : mDirs){
					fileListAdpater.add(new ChoosePDFItem(ChoosePDFItem.Type.DIR, f.getName()));
				}
				for (File f : mFiles){
					fileListAdpater.add(new ChoosePDFItem(ChoosePDFItem.Type.DOC, f.getName()));
				}
				if(fileDirList.getVisibility()==View.VISIBLE){
					Log.d("weiquanyun","mDirs.length+mFiles.length = "+mDirs.length+mFiles.length);
					pdf_file_num.setText(mDirs.length+mFiles.length+"");
				}
				lastPosition();
			}
		};
		// Start initial file scan...
		mHandler.post(mUpdateFiles);

		// ...and observe the directory and scan files upon changes.
		FileObserver observer = new FileObserver(mDirectory.getPath(), FileObserver.CREATE | FileObserver.DELETE) {
			public void onEvent(int event, String path) {
				mHandler.post(mUpdateFiles);
			}
		};
		observer.startWatching();
	}
	
	private void lastPosition() {
	String p = mDirectory.getAbsolutePath();
	if (mPositions.containsKey(p))
		fileDirList.setSelection(mPositions.get(p));
	}
	
	public ArrayList<String> getChooseList(){
		return this.chooseList;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mPositions.put(mDirectory.getAbsolutePath(), fileDirList.getFirstVisiblePosition());
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		isLongClick = false;
		pdf_list = null;
		pdf_file_num = null;
		
		search_edit = null;
		search_btn = null;
		other_btn = null;
		search_img = null;
		search_text = null;
		search_cancel = null;
		
		cancelChoose = null;
		numberChoose = null;
		deleteChoose = null;
		otherChoose = null;
		
		search_layout = null;
		toptoolbar = null;
		longClickOperLayout = null;
		
		pdf_list_adapter = null;
		
		fileDirList = null;
		fileListAdpater = null;
		mParent = null;
		mDirs = null;
		mFiles = null;
		mHandler = null;
		mUpdateFiles = null;
		settingPopupWindow = null;
		textRsId = null;
		imgRsId = null;
		textId = null;
		imgId = null;
		otherPopupWindow = null;
		//重命名
		newName = null;
		nameStr = null;
		handler = null;
		System.gc();
//		FileUtils.getFileUtils().getDirectories().clear();
//		FileUtils.getFileUtils().getFilesList().clear();
		
	}
	
	public void deletePdfs(ArrayList<String> list){
		pdf_list_adapter.getDataList().removeAll(list);
		for(int i=0;i<list.size();i++){
			String path = list.get(i);
			String key = path.substring(0, path.lastIndexOf('/'));
			int index = directories.indexOf(key);
			filesList.get(index).get(key).remove(path);
			if(filesList.get(index).get(key).size()<=0)
			{
				directories.remove(index);
				filesList.remove(index);
			}
			FileUtils.getFileUtils().deleteFileFromPath(path);
		}
		list.clear();
		ArrayList<String> dataList = pdf_list_adapter.setListData(directories, filesList);
		pdf_list_adapter.setDataList(dataList);
		pdf_list_adapter.notifyDataSetChanged();
		pdf_file_num.setText(dataList.size()-directories.size()+"");
	}
	
	class OnImageClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v.equals(search_btn)){
				search_edit.requestFocus();
				search_img.setVisibility(View.VISIBLE);
				search_text.setVisibility(View.VISIBLE);
				toptoolbar.setVisibility(View.INVISIBLE);
				search_layout.setVisibility(View.VISIBLE);
			}else if(v.equals(other_btn)){
				getSettingPopupWindow();
				settingPopupWindow.showAsDropDown(v,-200,5);
			}else if(v.equals(search_edit)){
				search_img.setVisibility(View.INVISIBLE);
				search_text.setVisibility(View.INVISIBLE);
			}else if(v.equals(search_cancel)){
				if(search_edit.getText().toString().length()!=0)
				{
					search_edit.setText("");
					search_img.setVisibility(View.VISIBLE);
					search_text.setVisibility(View.VISIBLE);
				}else
				{
					search_layout.setVisibility(View.INVISIBLE);
					toptoolbar.setVisibility(View.VISIBLE);
				}
			}else if(v.equals(cancelChoose)){
				longClickOperLayout.setVisibility(View.INVISIBLE);
				if(isTopBarShow){
					toptoolbar.setVisibility(View.VISIBLE);
					isTopBarShow = false;
				}
				if(isSearchLayoutShow){
					search_layout.setVisibility(View.VISIBLE);
					isSearchLayoutShow = false;
				}
				isLongClick = false;
				chooseList.clear();
				pdf_list_adapter.setChooseList(chooseList);
				pdf_list_adapter.notifyDataSetChanged();
				ArrayList<String> dataList = pdf_list_adapter.setListData(directories, filesList);
				pdf_file_num.setText(dataList.size()-directories.size()+"");
			}else if(v.equals(deleteChoose)){
				deletePdfs(chooseList);
				isLongClick = false;
				pdf_list_adapter.setChooseList(chooseList);
				numberChoose.setText("0");
			}else if(v.equals(otherChoose)){
				getOtherPopupWindow();
				otherPopupWindow.showAsDropDown(v,-200,5);
			}
		}
		
	}
	//topbar other operation
	public void getSettingPopupWindow(){
		if(settingPopupWindow==null){
			initSettingPopupWindow();
		}else{
			settingPopupWindow.dismiss();
		}
	}
	
	public void initSettingPopupWindow(){
		PopupWindowListView listView = new PopupWindowListView(ChoosePDFActivity.this, imgRsId, textRsId,70);
		settingPopupWindow = new PopupWindow(listView, 350, 72*2);
		
		settingPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		settingPopupWindow.setOutsideTouchable(true);
		settingPopupWindow.setFocusable(true);
		listView.setBackgroundColor(Color.argb(255, 255, 255, 255));
		listView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				switch(position){
				case 0:
					if(fileDirList.getVisibility()==View.VISIBLE)
						fileDirList.setVisibility(View.INVISIBLE);
					pdf_list_adapter.notifyDataSetChanged();
					pdf_list.setVisibility(View.VISIBLE);
					pdf_file_num.setText(pdf_list_adapter.getListDataSize()-directories.size()+"");
					settingPopupWindow.dismiss();
					break;
				case 1:
					mHandler.post(mUpdateFiles);
					if(pdf_list.getVisibility()==View.VISIBLE)
						pdf_list.setVisibility(View.INVISIBLE);
					fileDirList.setVisibility(View.VISIBLE);
					settingPopupWindow.dismiss();
					break;
				}
			}
			
		});
		
	}
	//longclick other operation
	public void getOtherPopupWindow(){
		if(otherPopupWindow==null){
			initOtherPopupWindow();
		}else{
			otherPopupWindow.dismiss();
		}
	}
	
	public void initOtherPopupWindow(){
		PopupWindowListView listView = new PopupWindowListView(ChoosePDFActivity.this, imgId, textId,63);
		otherPopupWindow = new PopupWindow(listView, 300, 65*3);
		// 使其聚集
		otherPopupWindow.setFocusable(true);
		// 设置允许在外点击消失
		otherPopupWindow.setOutsideTouchable(true);
		// 这个是为了点"返回Back"也能使其消失，并且并不会影响你的背景
		otherPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		listView.setBackgroundColor(Color.argb(255, 255, 255, 255));
		listView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				switch(position){
				case 0:
					if(chooseList.size()>1)
					{
						view.setClickable(false);
						view.setAlpha(125);
					}
					else
					{
						otherPopupWindow.dismiss();
						isLongClick = false;
						renamePdf();
					}
					break;
				case 1:
					mailOutPdfs(chooseList);
					chooseList.clear();
					numberChoose.setText("0");
					pdf_list_adapter.notifyDataSetChanged();
					otherPopupWindow.dismiss();
					isLongClick = false;
					break;
				case 2:
					createCopy(chooseList);
					chooseList.clear();
					numberChoose.setText("0");
					otherPopupWindow.dismiss();
					isLongClick = false;
					pdf_list_adapter.setChooseList(chooseList);
					pdf_list_adapter.notifyDataSetChanged();
					break;
				}
			}
			
		});
	}
	
	public void renamePdf(){
		nameStr = chooseList.get(0);
		Log.d("weiquanyun","----renamePdf----"+nameStr);
		newName = new EditText(ChoosePDFActivity.this); 
		AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePDFActivity.this);
		builder.setTitle(R.string.rename);
		builder.setView(newName);
		final int startIndex = nameStr.lastIndexOf('/');
		int endIndex = nameStr.lastIndexOf('.');
		final String oldNameStr = nameStr.substring(startIndex+1, endIndex);
		Log.d("weiquanyun","oldNameStr-->"+oldNameStr);
		newName.setText(oldNameStr);
		Selection.selectAll(newName.getText());
		builder.setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
				newName = null;
			}
		});
		builder.setPositiveButton(getApplication().getResources().getString(R.string.sure), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Log.d("weiquanyun","----onClick----"+nameStr);
//				int pdfIndex = pdf_list_adapter.getDataList().indexOf(str);
				
				String newNameStr = nameStr.substring(0,startIndex+1)+newName.getText().toString()+".pdf";
//				pdf_list_adapter.getDataList().remove(pdfIndex);
//				pdf_list_adapter.getDataList().add(pdfIndex, newNameStr);
				String path = nameStr;
				String key = path.substring(0, path.lastIndexOf('/'));
				int index = directories.indexOf(key);
				ArrayList<String> list = filesList.get(index).get(key);
				int indexOfRenamePdf = list.indexOf(path);
				filesList.get(index).get(key).remove(path);
				filesList.get(index).get(key).add(indexOfRenamePdf, newNameStr);
				boolean flag = FileUtils.getFileUtils().renameFileFromPath(nameStr, newNameStr);
				if(flag)
				{
					dialog.dismiss();
					showToast(R.string.successfully_rename);
					ArrayList<String> dataList = pdf_list_adapter.setListData(directories, filesList);
					pdf_list_adapter.setDataList(dataList);
					pdf_list_adapter.notifyDataSetChanged();
				}else
				{
					newName.setText(oldNameStr);
					Selection.selectAll(newName.getText());
				}
				chooseList.clear();
				numberChoose.setText("0");
				pdf_list_adapter.notifyDataSetChanged();
			}
		});
		builder.create().show();
//		pdf_list_adapter.notifyDataSetChanged();
		isLongClick = false;
	}
	
	public void mailOutPdfs(ArrayList<String> list){
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.putExtra(Intent.EXTRA_TEXT, "来自PowerPdfReader的email");
		ArrayList<Uri> uriList = new ArrayList<Uri>();
		for(int i=0;i<list.size();i++){
			uriList.add(Uri.parse("file://"+list.get(i)));
		}
		intent.putExtra(Intent.EXTRA_STREAM, uriList);
		intent.setType("application/octet-stream");
		Intent.createChooser(intent, ChoosePDFActivity.this.getString(R.string.choose_email_client));
		startActivity(intent);
		isLongClick = false;
	}
	
	public void createCopy(ArrayList<String> list){
		for(int i=0;i<list.size();i++){
			String addPdfName = FileUtils.getFileUtils().copyFile(list.get(i));
			String path = list.get(i);
			String key = path.substring(0, path.lastIndexOf('/'));
			int index = directories.indexOf(key);
			ArrayList<String> pdfList = filesList.get(index).get(key);
			if(addPdfName!=null){
				pdfList.add(addPdfName);
			}
		}
		ArrayList<String> dataList = pdf_list_adapter.setListData(directories, filesList);
		pdf_list_adapter.setDataList(dataList);
		pdf_list_adapter.notifyDataSetChanged();
		isLongClick = false;
	}
	 
	@Override
	public void onBackPressed() {
		if(search_layout.getVisibility()==View.VISIBLE){
			search_layout.setVisibility(View.INVISIBLE);
		}else if(longClickOperLayout.getVisibility()==View.VISIBLE){
			if(chooseList.size()>0)
			{
				chooseList.clear();
				numberChoose.setText("0");
				pdf_list_adapter.setChooseList(chooseList);
				pdf_list_adapter.notifyDataSetChanged();
			}else{
				longClickOperLayout.setVisibility(View.INVISIBLE);
				toptoolbar.setVisibility(View.VISIBLE);
			}
			isLongClick = false;
		}else if(!mDirectory.getAbsolutePath().equals(SDCARD_PATH)){
			if(search_layout.getVisibility()==View.VISIBLE){
				search_layout.setVisibility(View.INVISIBLE);
				toptoolbar.setVisibility(View.VISIBLE);
			}else{
				mParent = mDirectory.getParentFile();
				mDirectory = mParent;
				mHandler.post(mUpdateFiles);
			}
		}else{
			//连续按两次返回键退出
			if ((System.currentTimeMillis() - mExitTime) > 2000) 
			{
				 showToast(R.string.back_onemore_press);
				 mExitTime = System.currentTimeMillis();
			}
			else{
				Intent stopServiceIntent = new Intent(ChoosePDFActivity.this,ConnectServerService.class);
				stopService(stopServiceIntent);
				super.onBackPressed();
			}
		}
	}
	private void showToast(int resId){
		Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
	}
}
