package com.csu.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	private static ArrayList<String> directories;
	private static ArrayList<MyHashMap<String,ArrayList<String>>> filesList;
	public static final String APP_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/PowerPDFReader";
	private static volatile FileUtils fileUtils;
	static int index = 0;
	private FileUtils(){
		directories = new ArrayList<String>();
		filesList = new ArrayList<MyHashMap<String,ArrayList<String>>>();
	}
	
	public ArrayList<String> getDirectories(){
		return directories;
	}
	
	public ArrayList<MyHashMap<String,ArrayList<String>>> getFilesList(){
		return filesList;
	}
	
	public static FileUtils getFileUtils(){
		if(fileUtils==null)
			synchronized (FileUtils.class) {
				if(fileUtils==null){
					fileUtils = new FileUtils();
				}
			}
		return fileUtils;
	}
	
	/**
	 * 列出给定path下的所有PDF文件
	 * @param path 传入要查找的目录的路径
	 * @param fileList 获取存储pdf的列表，其中每一项是一个map，map的key为目录名，values为该目录下所有的pdf文件名
	 * @param directory 存储有pdf文件的目录的名字
	 */
	public void findFilesInPath(String path,ArrayList<MyHashMap<String,ArrayList<String>>> fileList,ArrayList<String> directory){
		File file = new File(path);
		try {
			listFileNameWithThreads(file,fileList,directory);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Collections.sort(directory);
		Collections.sort(fileList);
	}
	
	/**
	 * 
	 * @param file 要查找的目录
	 * @param fileList
	 * @param directory
	 */
	public void listFileName(File file,ArrayList<MyHashMap<String,ArrayList<String>>> fileList,ArrayList<String> directory){
		if(!file.isDirectory())
			return;
		if(file.getName().startsWith("."))
			return;
		if(file.getName().equals("cache")||file.getName().startsWith("cache")||file.getName().endsWith("cache"))
			return;
		if(file.getName().equals("Android"))
			return;
		if(file.getName().equals("DCIM"))
			return;
		if(file.getName().equals("data"))
			return;
		File []files = file.listFiles();
		if(files==null)
			return;
		MyHashMap<String,ArrayList<String>> map = new MyHashMap<String,ArrayList<String>>();
		ArrayList<String> list = new ArrayList<String>();
		for(int i=0;i<files.length;i++)
		{
			if(files[i].isFile())
			{
				if(files[i].getName().endsWith(".pdf"))
				{
					if(!directory.contains(file.getAbsolutePath()))
					{
						directory.add(file.getAbsolutePath());
					}
					list.add(files[i].getAbsolutePath());
					map.put(file.getAbsolutePath(),list);
					if(contains(fileList,map))
					{
						fileList.remove(index);
						fileList.add(index,map);
					}else{
						fileList.add(map);
					}
				}
			}else if(files[i].isDirectory()){
				listFileName(files[i],fileList,directory);
			}
		}
	}
	
	/**
	 * 
	 * @param fileList
	 * @param map
	 * @return
	 */
	private static boolean contains(ArrayList<MyHashMap<String,ArrayList<String>>> fileList,HashMap<String,ArrayList<String>> map){
		boolean flag = false;
		MyHashMap<String,ArrayList<String>> temp;
		for(int i=0;i<fileList.size();i++)
		{
			temp = fileList.get(i);
			if(temp.keySet().toArray()[0].equals(map.keySet().toArray()[0])){
				flag = true;
				index = i;
			}
		}
		return flag;
	}
	//delete file
	public void deleteFileFromPath(String path){
		File file = new File(path);
		if(file.exists())
			file.delete();
	}
	
	//delete dirs
	public void deleteDir(File dir){
		File files[] = dir.listFiles();
		for(int i=0;i<files.length;i++)
		{
			if(files[i].isFile())
				files[i].delete();
			else if(files[i].isDirectory())
				deleteDir(files[i]);
		}
	}
	
	public boolean renameFileFromPath(String srcPath,String destPath){
		File srcFile = new File(srcPath);
		File destFile = new File(destPath);
		if(destFile.exists()){
			return false;
		}else{
			srcFile.renameTo(destFile);
		}
		return true;
	}
	
	public String copyFile(String path){
		int i = 1;
		int endIndex = path.lastIndexOf('.');
		String copyName = path.substring(0,endIndex-1);
		File file = new File(copyName+i+".pdf");
		while(file.exists()){
			i++;
			file = new File(copyName+i+".pdf");
		}
		try {
			copyFile2DestName(path,copyName+i+".pdf");
			return copyName+i+".pdf";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean copyFile2DestName(String srcName,String destName) throws IOException{
		File srcFile = new File(srcName);
		File destFile = new File(destName);
		if (srcFile.isDirectory() || destFile.isDirectory())  
			return false;// 是目录则返回false  
		FileInputStream fis = new FileInputStream(srcFile);  
		FileOutputStream fos = new FileOutputStream(destFile);  
		int readLen = 0;  
		byte[] buf = new byte[1024];  
		while ((readLen = fis.read(buf)) != -1) {  
			fos.write(buf, 0, readLen);  
		}  
		fos.flush();  
		fos.close();  
		fis.close();  
		return true;  
	}
	
	public static File getAssetFileToFileDir(File dir,Context context, String fileName) {
		try {
			final String filePath = dir.getAbsolutePath() + File.separator
					+ fileName;
			InputStream is = context.getAssets().open(fileName);
			File file = new File(filePath);
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			byte[] temp = new byte[1024];
			int i = 0;
			while ((i = is.read(temp)) > 0) {
				fos.write(temp, 0, i);
			}
			fos.flush();  
			fos.close();
			is.close();
			Log.d("weiquanyun","getAssetFileToFileDir-->"+file);
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	final int nThreads = Runtime.getRuntime().availableProcessors()+1;
	final ExecutorService service = Executors.newFixedThreadPool(nThreads);
	
	class SubDirectory{
		final public List<File> subDirectories;
		public SubDirectory(List<File> subDirectories){
			this.subDirectories = subDirectories;
		}
	}
	
	public SubDirectory listFilesInSub(File file,ArrayList<MyHashMap<String,ArrayList<String>>> fileList,ArrayList<String> directory){
		File []files = file.listFiles();
		final List<File> subDirectories = new ArrayList<File>();
		MyHashMap<String,ArrayList<String>> map = new MyHashMap<String,ArrayList<String>>();
		ArrayList<String> list = new ArrayList<String>();
		for(int i=0;i<files.length;i++)
		{
			if(files[i].isFile())
			{
				if(files[i].getName().endsWith(".pdf"))
				{
					if(!directory.contains(file.getAbsolutePath()))
					{
						directory.add(file.getAbsolutePath());
					}
					list.add(files[i].getAbsolutePath());
					Collections.sort(list);
					map.put(file.getAbsolutePath(),list);
					if(contains(fileList,map))
					{
						fileList.remove(index);
						fileList.add(index,map);
					}else{
						fileList.add(map);
					}
				}
			}else if(files[i].isDirectory()){
				subDirectories.add(files[i]);
			}
		}
		return new SubDirectory(subDirectories);
	}
	
	/**
	 * 
	 * @param file
	 * @param fileList
	 * @param directory
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void listFileNameWithThreads(File file,final ArrayList<MyHashMap<String,ArrayList<String>>> fileList,final ArrayList<String> directory) throws InterruptedException, ExecutionException, TimeoutException{
		if(!file.isDirectory())
			return;
//		if(file.getName().startsWith("."))
//			return;
//		if(file.getName().equals("cache")||file.getName().startsWith("cache")||file.getName().endsWith("cache"))
//			return;
//		if(file.getName().equals("Android"))
//			return;
//		if(file.getName().equals("DCIM"))
//			return;
//		if(file.getName().equals("data"))
//			return;
		File []files = file.listFiles();
		if(files==null)
			return;
		final List<File> directories = new ArrayList<File>();
		directories.add(file);
		while(!directories.isEmpty()){
			final List<Future<SubDirectory>> partialResults = new ArrayList<Future<SubDirectory>>();
			for(final File subDirectory:directories){
				partialResults.add(service.submit(new Callable<SubDirectory>(){
					@Override
					public SubDirectory call() throws Exception {
						return listFilesInSub(subDirectory, fileList, directory);
					}
					
				}));
			}
			directories.clear();
			for(final Future<SubDirectory> partialResultFUture:partialResults){
				final SubDirectory subDirectory = partialResultFUture.get(200, TimeUnit.MILLISECONDS);
				directories.addAll(subDirectory.subDirectories);
			}
		}
	}
	
}
