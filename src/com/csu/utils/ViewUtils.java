package com.csu.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

public class ViewUtils {

	public static final ViewUtils utils = new ViewUtils();
	
	private ViewUtils(){
		
	}
	
	public static ViewUtils getInstance(){
		return utils;
	}
	
	public void shakeView(View view){
		Animation animation = new TranslateAnimation(0,10,0,0);
		animation.setInterpolator(new CycleInterpolator(5));
		animation.setDuration(500);
		view.setAnimation(animation);
		view.startAnimation(animation);
	}
	
	public void toast(Context context,String text){
		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
	}
	
	public void toast(Context context,int textRsd){
		Toast.makeText(context, context.getString(textRsd), Toast.LENGTH_SHORT).show();
	}
	
}
