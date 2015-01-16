package com.csu.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.csu.powerpdf.R;

public class MediaPlayerDialog extends Dialog {
	private String audioTitle;
	private ImageView play_pause;
	private RelativeLayout mediaPlayLayout;
	private SeekBar seekBar;
	public MediaPlayerDialog(Context context,int width,int height) {
		super(context);
		// TODO Auto-generated constructor stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.media_player_layout);
		mediaPlayLayout = (RelativeLayout)findViewById(R.id.mediaplayer);
		play_pause = (ImageView)mediaPlayLayout.findViewById(R.id.play_pause);
		seekBar = (SeekBar)mediaPlayLayout.findViewById(R.id.progressBar);
		
		LayoutParams paramsDialog = mediaPlayLayout.getLayoutParams();
		paramsDialog.width = width;
		paramsDialog.height = height;
	}
	
	public void setAudioTitle(String title)
	{
		audioTitle = title;
	}

	public ImageView getPlayPause(){
		return this.play_pause;
	}
	
	public SeekBar getSeekBar(){
		return this.seekBar;
	}
}
