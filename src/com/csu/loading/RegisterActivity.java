package com.csu.loading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.csu.powerpdf.R;
import com.csu.service.ConnectServerService;

public class RegisterActivity extends Activity {
	private EditText realName;
	private EditText nickName;
	private EditText pwd;
	private EditText pwd2;
	private RadioButton male;
	private RadioButton female;
	/**
	 * true:male
	 * false:female
	 */
	private boolean sex;
	private Spinner branch;
	private Spinner department;
	
	private Button submit;
	private Button cancel;
	private AlertDialog.Builder builder;
	private int res;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		initView();
	}

	private void initView(){
		realName = (EditText)findViewById(R.id.real_name);
		nickName = (EditText)findViewById(R.id.input_nick_name);
		pwd = (EditText)findViewById(R.id.input_pwd);
		pwd2 = (EditText)findViewById(R.id.input_pwd2);
		male = (RadioButton)findViewById(R.id.male);
		female = (RadioButton)findViewById(R.id.female);
		branch = (Spinner)findViewById(R.id.branch_sp);
		department = (Spinner)findViewById(R.id.department_sp);
		realName = (EditText)findViewById(R.id.real_name);
		
		submit = (Button)findViewById(R.id.submit);
		cancel = (Button)findViewById(R.id.cancel);
		
		submit.setOnClickListener(new OnClickListener());
		cancel.setOnClickListener(new OnClickListener());
		
		builder = new AlertDialog.Builder(RegisterActivity.this);
		builder.setTitle("注册信息有错误！");
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(res==1||res==2){
					//信息提交成功
					finish();
				}
			}
		});
		builder.setNegativeButton("取消注册", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
	}
	
	private void submit(){
		
		new RegisteTask().execute();
	}
	
	private class RegisteTask extends AsyncTask<Void,Void,Boolean>{
			
		String name = null;
		String nickame = null;
		String password = null;
		String password2 = null;
		String branchName = null;
		String dptName = null;
		boolean sex = true;
		boolean isInfoCorrect = true;
		
		@Override
		protected void onPreExecute() {
			name = realName.getText().toString().trim();
			nickame = nickName.getText().toString().trim();
			password = pwd.getText().toString();
			password2 = pwd2.getText().toString();
			branchName = (String)branch.getSelectedItem();
			dptName = (String)department.getSelectedItem();
			sex = male.isChecked()?true:false;
			if(name!=null&&name.length()<=0){
				builder.setMessage("请填写真实姓名!");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(nickame!=null&&nickame.length()<=0){
				builder.setMessage("请填写您的登录昵称!");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(password!=null&&password.length()<=0){
				builder.setMessage("密码不能为空哦!");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(password2!=null&&password2.length()<=0){
				builder.setMessage("请确认刚才输入的密码!");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(password!=null&&!password.equals(password2)){
				builder.setMessage("两次密码不一样!");
				pwd2.setText("");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(branchName!=null&&branchName==null){
				builder.setMessage("请选择所在子公司!");
				builder.create().show();
				isInfoCorrect = false;
			}
			else if(dptName!=null&&dptName==null){
				builder.setMessage("请选择所在部门!");
				builder.create().show();
				isInfoCorrect = false;
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if(!checkIsAlive()){
				Intent intent = new Intent(RegisterActivity.this,ConnectServerService.class);
				startService(intent);
			}
			
			if(isInfoCorrect){
				InputStream in = null;
				OutputStream out = null;
				DataOutputStream oos = null;
				DataInputStream ois = null;
				try{
					in = ConnectServerService.socket.getInputStream();
					out = ConnectServerService.socket.getOutputStream();
					oos = new DataOutputStream(out);
					ois = new DataInputStream(in);
					//连接上服务器，首先发送注册请求代码：1
					oos.writeInt(1);
					oos.flush();
					//发送注册信息
					oos.writeUTF(nickame);
					oos.flush();
					oos.writeUTF(password);
					oos.flush();
					oos.writeUTF(name);
					oos.flush();
					oos.writeBoolean(sex);
					oos.flush();
					oos.writeUTF(branchName);
					oos.flush();
					oos.writeUTF(dptName);
					oos.flush();
					//接收服务器返回的结果
					res = ois.readInt();
					
				}catch(Exception e){
					Log.d("weiquanyun","regist:"+e.toString());
					Intent intent = new Intent(RegisterActivity.this,ConnectServerService.class);
					startService(intent);
					return false;
				}finally{
					try {
						if(ois!=null)
							ois.close();
						if(oos!=null)
							oos.close();
						if(out!=null)
							out.close();
						if(in!=null)
							in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			}else{
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result){
				if(res == -1){
					builder.setTitle("服务器异常");
					builder.setMessage("服务器出现异常，操作失败。请稍后再试。");
					builder.create().show();
				}else if(res == 0){
					builder.setTitle("修改信息提交失败");
					builder.setMessage("您所申请的用户ID已被占用，请更换申请ID");
					builder.create().show();
				}else if(res == 1){
					builder.setTitle("申请信息提交成功");
					builder.setMessage("您的申请已提交至服务器，请耐心等待中心的审核；审核通过之前您仍可以重新提交此用户ID的信息作为修改。");
					builder.create().show();
				}else if(res == 2){
					builder.setTitle("修改信息提交成功");
					builder.setMessage("您所做的申请修改已提交至服务器，请耐心等待中心的审核。");
					builder.create().show();
				}
			}else{
				if(isInfoCorrect){
					builder.setTitle("服务器出现故障");
					builder.setMessage("服务器出现故障，请稍后再试。");
					builder.create().show();
				}
			}
		}

	}
	
	private boolean checkIsAlive(){
		if(ConnectServerService.socket!=null){
			try {
				ConnectServerService.socket.sendUrgentData(1);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	
	private void cancel(){
		realName.setText("");
		nickName.setText("");
		pwd.setText("");
		pwd2.setText("");
	}
	
	class OnClickListener implements android.view.View.OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v.equals(submit)){
				submit();
			}else if(v.equals(cancel)){
				cancel();
			}
		}
		
	}
	
}
