package com.csu.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.ac.ic.doc.jpair.pairing.BigInt;
import uk.ac.ic.doc.jpair.pairing.Point;

import com.csu.ibe.ClientEnc;
import com.csu.ibe.IBEConfig;
import com.csu.ibe.MYCtext;
import com.csu.utils.Config;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class ConnectServerService extends Service {

	private ExecutorService execitorService = Executors.newSingleThreadExecutor();
	public static Socket socket;
	private boolean flag = true;
	public static InputStream is;
	public static OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;
	private AlarmManager alrmManager;
	private PendingIntent pendingIntent;
	private HeartBeatBroadcastReceiver receiver;
	private ClientEnc ce;
	private String mResult;
	public static volatile CountDownLatch ready = new CountDownLatch(1);
	class HeartBeatBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("CONNECT_SERVER")){
				Log.d("weiquanyun","onReceive");
				Log.d("weiquanyun","1.onReceive-->socket:"+socket);
				if(!checkIsAlive()){
					execitorService.shutdownNow();
					execitorService = null;
					closeLastSocket();
					Log.d("weiquanyun","2.onReceive-->socket:"+socket.isConnected());
					Runnable runnable = new Runnable(){
						public void run(){
							try {
								socket = new Socket();
								SocketAddress remoteAddr = new InetSocketAddress(Config.SERVER_IP,Config.PORT);
								socket.connect(remoteAddr, 5000);
//								Log.d("weiquanyun","3.onReceive-->socket:"+socket.isConnected());
								is = socket.getInputStream();
								os = socket.getOutputStream();
								dis = new DataInputStream(is);
								dos = new DataOutputStream(os);
							} catch (UnknownHostException e) {
								e.printStackTrace();
								Log.d("weiquanyun","onReceive-->UnknownHostException:"+e.toString());
							} catch (IOException e) {
								e.printStackTrace();
								Log.d("weiquanyun","onReceive-->IOException:"+e.toString());
							}
							
						}
					};
					execitorService = Executors.newSingleThreadExecutor();
					execitorService.execute(runnable);
				}
			}
		}
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		alrmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		ce = new ClientEnc();
		Intent intent = new Intent("CONNECT_SERVER");
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		alrmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 3000, pendingIntent);
		receiver = new HeartBeatBroadcastReceiver();
		IntentFilter filter = new IntentFilter("CONNECT_SERVER");
		registerReceiver(receiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("weiquanyun","onStartCommand");
		execitorService.shutdownNow();
		execitorService = null;
		closeLastSocket();
		Runnable runnable = new Runnable(){
			public void run(){
				try {
					socket = new Socket();
					SocketAddress remoteAddr = new InetSocketAddress(Config.SERVER_IP,Config.PORT);
					socket.connect(remoteAddr, 5000);
					is = socket.getInputStream();
					os = socket.getOutputStream();
					dis = new DataInputStream(is);
					dos = new DataOutputStream(os);
					if(ready!=null){
						try {
							ready.await();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					Log.d("weiquanyun","waitting over");
					while(!socket.isClosed()){
						try {
		    				int chattingtype = dis.readInt();
		    				if(chattingtype==4){
		    					//首先，解密私钥
		    					String receiversIDQtoString = ce.mydec(getC(),IBEConfig.SQ);
		    					String [] stringQ = receiversIDQtoString.split(",");
		    					System.out.println(stringQ[0]);
		    					System.out.println(stringQ[1]);
		    					Point tempprivatekey = new Point(new BigInt(stringQ[0]),new BigInt(stringQ[1]));
		    					mResult = stringQ[0];
		    					mResult = ce.mydec(getC(),tempprivatekey);
		    					Log.d("weiquanyun","Receive cipher:"+mResult);
		    				}
		    			} catch (IOException e) {
		    				// TODO Auto-generated catch block
		    				e.printStackTrace();
		    				socket.close();
		    				break;
		    			} 
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Log.d("weiquanyun","onStartCommand-->UnknownHostException:"+e.toString());
				} catch (IOException e) {
					e.printStackTrace();
					Log.d("weiquanyun","onStartCommand-->IOException:"+e.toString());
				}
				
			}
		};
		execitorService = Executors.newSingleThreadExecutor();
		execitorService.execute(runnable);
		return START_STICKY;
	}
	
	private void closeLastSocket(){
		if(socket!=null&&socket.isConnected())
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	}
	
	private boolean checkIsAlive(){
		if(socket!=null){
			try {
				socket.sendUrgentData(1);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	
	public MYCtext getC(){
		MYCtext  c = null;
		String ux,uy;
		byte[] v ,w;
		StringBuffer sb = new StringBuffer();
		int i;
		try {
			i = is.read();
			while(i!='#'){
				sb.append((char)i);
				i = is.read();
			}
			ux = sb.toString();
			sb = new StringBuffer();
			i = is.read();
			while(i!='#'){
				sb.append((char)i);
				i = is.read();
			}
			uy = sb.toString();
			int length;
			length = dis.readInt();
			byte b;
			v = new byte[length];
			for(int n=0;n<length;n++){
				v[n] = (byte) is.read();
			}
			length = dis.readInt();
			w = new byte[length];
			for(int n=0;n<length;n++){
				w[n] = (byte) is.read();
			}
			BigInt bux = new BigInt(ux);
			BigInt buy = new BigInt(uy);
			Point U = new Point(bux,buy);
			
			c = new MYCtext(U,v,w);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return c;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("weiquanyun","service->onDestroy");
		flag = false;
		if(execitorService!=null){
			execitorService.shutdownNow();
		}
		if(socket!=null&&socket.isConnected()){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		unregisterReceiver(receiver);
		alrmManager.cancel(pendingIntent);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

}
