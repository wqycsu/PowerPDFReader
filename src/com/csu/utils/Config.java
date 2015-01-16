package com.csu.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Environment;
import android.util.TypedValue;

public class Config {
	public static final String SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	
	public static final String SERVER_IP = "192.168.0.118";
	public static final int PORT = 9090;
	
	public static String randomString() {
		String random = "";
		for(int i = 0; i < 4; i++) {
			random = random + (int)(Math.random() * 10);
		}
		return random;
	}
	
	//将字符串转成日期
	public static Date stringToDate(String string, String format) {
		Date strDate = null;
		SimpleDateFormat sd = new SimpleDateFormat(format,Locale.CHINA); 
		try{ 
			strDate = sd.parse(string); 
		}catch(ParseException e){ 
		    e.printStackTrace(); 
		} 
		return strDate;
	}
	
	//将日期转成字符串
	public static String stringFromDate(Date date, String format) {
		String dateStr="";
	    if (date != null) {
		    DateFormat df=new SimpleDateFormat(format,Locale.CHINA);
		    dateStr=df.format(date);
	    }
	    return dateStr;
	}
	
	//将String（1， 2， 3， 4）转换成Color
	public static int colorFromString(String colorString) {	
		String str[] = colorString.split(",");
		return Color.argb((int)(255 * Float.parseFloat(str[3])),(int)(255 * Float.parseFloat(str[0])), (int)(255 * Float.parseFloat(str[1])), (int)(255 * Float.parseFloat(str[2])));	
	}
	
	//将字符串转成Matrix
	
	
	//将Matrix转成字符串
	public static String stringFromMatrix(Matrix matrix) {
		float values[] = new float[9]; 
		try {
			matrix.getValues(values);
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return values[0] + "," + values[1] + "," + values[3] + "," + values[4] + "," + values[2] + "," + values[5];
	}
	
	public static Matrix matrixFromString(String matrixString) {
		String str[] = matrixString.split(",");
		float values[] = {Float.parseFloat(str[0]),Float.parseFloat(str[1]),Float.parseFloat(str[4]),Float.parseFloat(str[2]),Float.parseFloat(str[3]),Float.parseFloat(str[5]), 0, 0, 1};
		Matrix m = new Matrix();
		m.setValues(values);
		return m;
	}
	
	public static RectF rectFromString(String rectString) {
		String str[] = rectString.split(",");		
		RectF m = new RectF(Float.parseFloat(str[0]), Float.parseFloat(str[1]), Float.parseFloat(str[2]) + Float.parseFloat(str[0]), Float.parseFloat(str[3]) + Float.parseFloat(str[1]));
		return m;
	}
	
	public static String stringFromRect(RectF rect) {
		return rect.left + "," + rect.top + "," + (rect.right - rect.left) + "," + (rect.bottom - rect.top);	
	}

	// 根据文件的大小，返回格式化的字符串
	public static String noteSizeString(long size) {
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (size < 1024) {
			fileSizeString = df.format((double) size) + "B";
		} else if (size < 1048576) {
			fileSizeString = df.format((double) size / 1024) + "K";
		} else if (size < 1073741824) {
			fileSizeString = df.format((double) size / 1048576) + "M";
		} else {
			fileSizeString = df.format((double) size / 1073741824) + "G";
		}
		return fileSizeString;
	}
	
	//计算图片缩放比例
	public static int computeSampleSize(BitmapFactory.Options options, 
            int minSideLength, int maxNumOfPixels) { 
        int initialSize = computeInitialSampleSize(options, minSideLength, 
                maxNumOfPixels); 
        int roundedSize; 
        if (initialSize <= 8) { 
            roundedSize = 1; 
            while (roundedSize < initialSize) { 
                roundedSize <<= 1; 
            } 
        } else { 
            roundedSize = (initialSize + 7) / 8 * 8; 
        } 
        return roundedSize; 
    } 
      
    private static int computeInitialSampleSize(BitmapFactory.Options options, 
            int minSideLength, int maxNumOfPixels) { 
        double w = options.outWidth; 
        double h = options.outHeight; 
      
        int lowerBound = (maxNumOfPixels == -1) ? 1 : 
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels)); 
        int upperBound = (minSideLength == -1) ? 128 : 
                (int) Math.min(Math.floor(w / minSideLength), 
                Math.floor(h / minSideLength)); 
      
        if (upperBound < lowerBound) { 
            // return the larger one when there is no overlapping zone. 
            return lowerBound; 
        } 
      
        if ((maxNumOfPixels == -1) && 
                (minSideLength == -1)) { 
            return 1; 
        } else if (minSideLength == -1) { 
            return lowerBound; 
        } else { 
            return upperBound; 
        } 
    }
    
    public static boolean isSuportCamera() {
//    	android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
	    int n = Camera.getNumberOfCameras();
	    if (n > 0) {
	    	return true;
	    }
//	    for (int i = 0;i < n-1;i ++) {
//	    	android.hardware.Camera.getCameraInfo(i, info);
//	    	if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//	    		return true;
//	    	}
//	    }
    	return false;
    }
   
    public static float dp2pxF(Context context,int dp){
    	return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
    
    public static float px3dpF(Context context,int px){
    	float scale = context.getResources().getDisplayMetrics().density;
    	return (float)px/scale;
    }
    
}
