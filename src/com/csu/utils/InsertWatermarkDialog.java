package com.csu.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.csu.powerpdf.R;

public class InsertWatermarkDialog extends Dialog {
	public enum WatermarkSize{SMALL,MIDDLE,LARGE};
	
	private ImageView pageImage;
	private ImageView watermarkImage;
	private TextView sizeSmall;
	private TextView sizeMiddle;
	private TextView sizeLarge;
	
	private int pageWidth;
	private int pageHeight;
	
	private RelativeLayout dialog;
	private RelativeLayout watermarkSize;
	
	private Button cancelButton;
	private Button sureButton;
	
	private WatermarkSize size;
	/**
	 * 
	 * @param context
	 * @param dialogHeight the height of dialog
	 * @param dialogWidth the width of dialog
	 * @param pageHeight the height of the pdf page
	 * @param pageWidth the width of the pdf page
	 */
	public InsertWatermarkDialog(Context context,int dialogHeight,int dialogWidth,int pageHeight,int pageWidth) {
		super(context);
		// TODO Auto-generated constructor stub
		setContentView(R.layout.image_watermark_layout);
		setTitle(R.string.insert_watermark);
		initViews();
		this.pageHeight = pageHeight;
		this.pageWidth = pageWidth;
		
		LayoutParams paramsDialog = dialog.getLayoutParams();
		paramsDialog.height = dialogHeight;
		paramsDialog.width = dialogWidth;
		RelativeLayout.LayoutParams paramsWatermarkSize = (RelativeLayout.LayoutParams)watermarkSize.getLayoutParams();
		paramsWatermarkSize.width = pageWidth;
		
		RelativeLayout.LayoutParams paramsCancelButton = (RelativeLayout.LayoutParams)cancelButton.getLayoutParams();
		paramsCancelButton.width = (dialogWidth-80)/2;
		paramsCancelButton.height = 80;
		paramsCancelButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.image_watermark_layout);
		paramsCancelButton.rightMargin = 20;
		cancelButton.setLayoutParams(paramsCancelButton);
		
		RelativeLayout.LayoutParams paramsSureButton = (RelativeLayout.LayoutParams)sureButton.getLayoutParams();
		paramsSureButton.width = (dialogWidth-80)/2;
		paramsSureButton.height = 80;
		paramsSureButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT, R.id.image_watermark_layout);
		paramsSureButton.leftMargin = 20;
		sureButton.setLayoutParams(paramsSureButton);
		
		RelativeLayout.LayoutParams paramsPageImage = (RelativeLayout.LayoutParams)pageImage.getLayoutParams();
		paramsPageImage.width = pageWidth;
		paramsPageImage.height = pageHeight;
		paramsPageImage.topMargin = 20;
		
		RelativeLayout.LayoutParams paramsWatermarkImage = (RelativeLayout.LayoutParams)watermarkImage.getLayoutParams();
		paramsWatermarkImage.width = pageWidth/2;
		paramsWatermarkImage.height = pageHeight/2;
		paramsWatermarkImage.topMargin = (paramsPageImage.height-paramsWatermarkImage.height)/2+20;
		
		sizeSmall.setOnClickListener(new TextOnClickListener());
		sizeMiddle.setOnClickListener(new TextOnClickListener());
		sizeLarge.setOnClickListener(new TextOnClickListener());
	}

	public void setWatermarkSize(WatermarkSize size){
		this.size = size;
	}
	
	public WatermarkSize getWatermarkSize(){
		return this.size;
	}
	
	private void initViews(){
		dialog = (RelativeLayout)findViewById(R.id.image_watermark_layout);
		watermarkSize = (RelativeLayout)dialog.findViewById(R.id.watermark_size);
		pageImage = (ImageView)findViewById(R.id.page_image);
		watermarkImage = (ImageView)findViewById(R.id.watermark_image);
		sizeSmall = (TextView)findViewById(R.id.size_small);
		sizeMiddle = (TextView)findViewById(R.id.size_middle);
		sizeLarge = (TextView)findViewById(R.id.size_large);
		cancelButton = (Button)findViewById(R.id.cancel_button);
		sureButton = (Button)findViewById(R.id.sure_button);
		watermarkImage.setAlpha(255);
	}
	
	public void setPageImageBgBitmap(Bitmap bitmap){
		pageImage.setBackgroundDrawable(new BitmapDrawable(bitmap));
	}
	
	public void setWatermarkeImageBgBitmap(Bitmap bitmap){
		watermarkImage.setBackgroundDrawable(new BitmapDrawable(bitmap));
	}
	
	public Button getSureButton(){
		return this.sureButton;
	}
	
	public Button getCancelButton(){
		return this.cancelButton;
	}
	
	class TextOnClickListener implements android.view.View.OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v.equals(sizeSmall)){
				Log.d("weiquanyun","watermark  size small");
				int width = watermarkImage.getWidth();
				int height = watermarkImage.getHeight();
				int left = watermarkImage.getLeft();
				int top = watermarkImage.getTop();
				if(watermarkImage.getWidth()!=0&&watermarkImage.getHeight()!=0){
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)watermarkImage.getLayoutParams();
					params.width = pageWidth/4;
					params.height = pageHeight/4;
					params.leftMargin = left+(width-params.width)/2;
					params.topMargin = top+(height-params.height)/2;
					watermarkImage.setLayoutParams(params);
					setWatermarkSize(WatermarkSize.SMALL);
				}
			}else if(v.equals(sizeMiddle)){
				Log.d("weiquanyun","watermark  size middle");
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)watermarkImage.getLayoutParams();
				params.width = pageWidth/2;
				params.height = pageHeight/2;
				params.topMargin = pageHeight/4+20;
				watermarkImage.setLayoutParams(params);
				setWatermarkSize(WatermarkSize.MIDDLE);
			}else if(v.equals(sizeLarge)){
				Log.d("weiquanyun","watermark  size large");
				int width = watermarkImage.getWidth();
				int height = watermarkImage.getHeight();
				int left = watermarkImage.getLeft();
				int top = watermarkImage.getTop();
				if(watermarkImage.getWidth()!=0&&watermarkImage.getHeight()!=0){
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)watermarkImage.getLayoutParams();
					params.width = pageWidth/4*3;
					params.height = pageHeight/4*3;
					params.leftMargin = left-(params.width-width)/2;
					params.topMargin = top-(params.height-height)/2;
					watermarkImage.setLayoutParams(params);
					setWatermarkSize(WatermarkSize.LARGE);
				}
			}
		}
		
	}
}
