package com.csu.loading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import uk.ac.ic.doc.jpair.pairing.BigInt;
import uk.ac.ic.doc.jpair.pairing.Point;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.csu.ibe.IBEConfig;
import com.csu.powerpdf.ChoosePDFActivity;
import com.csu.powerpdf.R;
import com.csu.service.ConnectServerService;
import com.csu.utils.Config;
import com.csu.utils.ViewUtils;

public class LoginActivity extends Activity{

	private EditText userNameET;
	private EditText pwdET;
	private Button loginBtn;
	private Button registBtn;
	private Button offline;
	private TextView networkTips;
	private AlertDialog.Builder builder;
	private LoginTask task;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Intent service = new Intent(LoginActivity.this,ConnectServerService.class);
		startService(service);
		initViews();
	}

	private void initViews(){
		userNameET = (EditText)findViewById(R.id.name);
		pwdET = (EditText)findViewById(R.id.pwd);
		loginBtn = (Button)findViewById(R.id.login);
		registBtn = (Button)findViewById(R.id.register);
		networkTips = (TextView)findViewById(R.id.network_tips);
		offline = (Button)findViewById(R.id.offline);
		builder = new AlertDialog.Builder(LoginActivity.this);
		builder.setTitle("登录失败");
		
		loginBtn.setOnClickListener(new OnClickListener());
		registBtn.setOnClickListener(new OnClickListener());
		networkTips.setOnClickListener(new OnClickListener());
		offline.setOnClickListener(new OnClickListener());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	class OnClickListener implements android.view.View.OnClickListener{

		@Override
		public void onClick(View v) {
			if(v.equals(loginBtn)){
				if(!netConnetCheck()){
					showNetworkTips();
				} else if(!localCheck()){
					builder.setMessage("用户名和密码不能为空。");
					builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
					builder.create().show();
					ViewUtils.getInstance().shakeView(userNameET);
					ViewUtils.getInstance().shakeView(pwdET);
				} else{
					task = new LoginTask();
					task.execute();
					loginBtn.setEnabled(false);
				}
			}else if(v.equals(registBtn)){
				Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
				startActivity(intent);
			}else if(v.equals(offline)){
				Intent intent = new Intent(LoginActivity.this,ChoosePDFActivity.class);
				startActivity(intent);
			}else if(v.equals(networkTips)){
				Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
				startActivity(intent);
				networkTips.setVisibility(View.GONE);
			}
		}
		
	}
	
	private void showNetworkTips(){
		if(networkTips.getVisibility()!=View.VISIBLE){
			TranslateAnimation animation = new TranslateAnimation(0, 0, -networkTips.getHeight(), 0);
			animation.setDuration(300);
			animation.setFillAfter(true);
			
			final Handler handler = new Handler(){
				public void handleMessage(Message msg){
					TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -networkTips.getHeight());
					animation.setDuration(300);
					animation.setFillAfter(true);
					networkTips.startAnimation(animation);
				}
			};
			
			animation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					Timer timer = new Timer();
					TimerTask task = new TimerTask() {
						
						@Override
						public void run() {
							handler.sendEmptyMessage(0);
						}
					};
					timer.schedule(task, 3000);
				}
			});
			networkTips.setVisibility(View.VISIBLE);
			networkTips.startAnimation(animation);
		}else{
			ViewUtils.getInstance().shakeView(networkTips);
		}
	}
	
	private boolean netConnetCheck(){
		ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
	}
	
	private boolean localCheck(){
		return userNameET.getText().toString().trim().length() > 0
				&& pwdET.getText().toString().length() > 0;
	}
	
	class LoginTask extends AsyncTask<Void, Void, Boolean>{

		String userName;
		String password;
		InputStream is;
		OutputStream os;
		DataInputStream dis;
		DataOutputStream dos;
		int res;
		@Override
		protected void onPreExecute() {
			userName = userNameET.getText().toString();
			password = pwdET.getText().toString();
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if(ConnectServerService.socket.isConnected()&&ConnectServerService.socket.isClosed()){
				Intent service = new Intent(LoginActivity.this,ConnectServerService.class);
				startService(service);
				return false;
			}
			try {
				is = ConnectServerService.socket.getInputStream();
				os = ConnectServerService.socket.getOutputStream();
				dis = new DataInputStream(is);
				Log.d("weiquanyun","1.login-dis:"+dis);
				dos = new DataOutputStream(os);
				Log.d("weiquanyun","2.login-dos:"+dos);
				/**
				 * 发送登录请求代码
				 */
				dos.writeInt(2);
				dos.flush();
				Log.d("weiquanyun","3.login-dos:"+dos);
				/**
				 * 发送用户名和密码
				 */
				dos.writeUTF(userName);
				dos.flush();
				Log.d("weiquanyun","4.login-userName:"+userName);
				dos.writeUTF(password);
				dos.flush();
				Log.d("weiquanyun","4.login-password:"+password);
				/**
				 * 得到结果码
				 */
				res = dis.readInt();
				Log.d("weiquanyun","res:"+res);
				if(res==3){
					try{
						IBEConfig.ID = userName;
						StringBuffer sb = new StringBuffer();
						int i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						IBEConfig.PX = sb.toString();
						sb = new StringBuffer();
						i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						IBEConfig.PY = sb.toString();
						sb = new StringBuffer();
						i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						IBEConfig.PpubX = sb.toString();
						sb = new StringBuffer();
						i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						IBEConfig.PpubY = sb.toString();
						sb = new StringBuffer();
						i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						BigInt sQx = new BigInt(sb.toString());
						sb = new StringBuffer();
						i = is.read();
						while(i!='#'){
							sb.append((char)i);
							i = is.read();
						}
						BigInt sQy = new BigInt(sb.toString());
						IBEConfig.SQ = new Point(sQx,sQy);
					}catch(Exception e){
						Log.d("weiquanyun","e:"+e.toString());
						return false;	
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				Log.d("weiquanyun","login-e:"+e.toString());
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			loginBtn.setEnabled(true);
			if(result){
				if(res == -1){
					builder.setTitle("服务器异常");
					builder.setMessage("服务器出现异常，操作失败。请稍后再试。");
					builder.setPositiveButton("重试", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
						}
					});
					builder.setNegativeButton("离线使用", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(LoginActivity.this,ChoosePDFActivity.class);
							startActivity(intent);
						}
					});
					builder.create().show();
				}else if(res == 0){
					builder.setTitle("登录失败");
					builder.setMessage("您的账户名或密码错误。");
					builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
						}
					});
					builder.create().show();
				}else if(res == 1){
					builder.setTitle("登录失败");
					builder.setMessage("您的账户未通过身份认证，请重新申请。");
					builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
						}
					});
					builder.create().show();
				}else if(res == 2){
			        builder.setTitle("登录失败");
					builder.setMessage("您的账户信息还在审核中，请耐心等待审核完成后再使用。");
					builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
						}
					});
					builder.create().show();
				}else if(res == 3){
					ViewUtils.getInstance().toast(LoginActivity.this, "登录成功");
					Intent intent = new Intent(LoginActivity.this,ChoosePDFActivity.class);
					startActivity(intent);
					ConnectServerService.ready.countDown();
					Log.d("weiquanyun","ConnectServerService.ready:"+ConnectServerService.ready.getCount());
					LoginActivity.this.finish();
				}
				
			}else{
				builder.setTitle("服务器异常");
				builder.setMessage("服务器出现异常，操作失败。请稍后再试。");
				builder.setPositiveButton("重试", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				builder.setNegativeButton("离线使用", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(LoginActivity.this,ChoosePDFActivity.class);
						startActivity(intent);
					}
				});
				builder.create().show();
			}
		}
		
	}
	
}