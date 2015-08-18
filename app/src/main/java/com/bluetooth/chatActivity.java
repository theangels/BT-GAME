package com.bluetooth;

import com.bluetooth.Bluetooth.ServerOrCilent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class chatActivity extends Activity implements OnItemClickListener ,OnClickListener{
	/** Called when the activity is first created. */

	//private ListView mListView;
	//private ArrayList<deviceListItem>list;
//	private Button sendButton;
	private Button disconnectButton;
//	private EditText editMsgView;
	//deviceListAdapter mAdapter;
	Context mContext;

	/* 一些常量，代表服务器的名称 */
	public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
	public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
	public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
	public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

	private BluetoothServerSocket mserverSocket = null;
	private ServerThread startServerThread = null;
	private clientThread clientConnectThread = null;
	private BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private readThread mreadThread = null;;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	private Button cl;
	private TableView table;

	private int count = 0;
	private long firClick = 0;
	private long secClick = 0;

	private final Timer timer = new Timer();
	private TimerTask task;

	private String []send;

	//不间断发送信息13秒一次
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			// 要做的事情
			for(int i = 0; i < 32; i++){
				sendMessageHandle(send[i]);
				delay(140);
				System.out.printf("%s\n", send[i]);

			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.bluetooth.R.layout.chat);
		mContext = this;
		init();

		cl = (Button)findViewById(R.id.cl);
		table = (TableView)findViewById(R.id.table);
		task = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message message = new Message();
				message.what = 1;
				handler.sendMessage(message);
			}
		};
		timer.schedule(task, 2000, 13000);//推迟发送 发送间断
		send = new String[32+5];
		msgInit();

		play();
	}

	void play(){

		//清楚命令
		cl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 1; i <= 16; i++) {
					for (int j = 1; j <= 16; j++) {
						msgInit();
						table.grade[i][j].setBackgroundColor(Color.rgb(255, 255, 255));
					}
				}
				sendMessageHandle("c");
			}
		});

		//划屏事件
		table.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int h = get_h(event.getY()) + 1;
				int l = get_l(event.getX()) + 1;
				switch (event.getAction()) {
					//清除
					case MotionEvent.ACTION_UP:
						count++;
						if (count == 1) {
							firClick = System.currentTimeMillis();

						} else if (count == 2) {
							secClick = System.currentTimeMillis();
							if (secClick - firClick < 1000) {
								if(l>=1&&l<=8){
									char tmp[]=send[h-1].toCharArray();
									tmp[3+l]='1';
									String change = "";
									for(int i = 0; i < tmp.length; i++){
										change += tmp[i];
									}
									send[h-1] = change;
									//Toast.makeText(mContext, send[h-1], Toast.LENGTH_SHORT).show();
								}
								else{
									char tmp[]=send[h+15].toCharArray();
									tmp[l-5]='1';
									String change = "";
									for(int i = 0; i < tmp.length; i++){
										change += tmp[i];
									}
									send[h+15] = change;
									//Toast.makeText(mContext, send[h+15], Toast.LENGTH_SHORT).show();
								}
								table.grade[h][l].setBackgroundColor(Color.rgb(255, 255, 255));
								//System.out.printf("坐标为 %d  %d\n",h,l);
								//delay(100);
							}
							count = 0;
							firClick = 0;
							secClick = 0;
						}
						break;
					//绘画
					case MotionEvent.ACTION_MOVE:
						table.grade[h][l].setBackgroundColor(Color.rgb(0, 0, 0));
						if(l>=1&&l<=8){
							char tmp[]=send[h-1].toCharArray();
							tmp[3+l]='0';
							String change = "";
							for(int i = 0; i < tmp.length; i++){
								change += tmp[i];
							}
							send[h-1] = change;
							//Toast.makeText(mContext, send[h-1], Toast.LENGTH_SHORT).show();
						}
						else{
							char tmp[]=send[h+15].toCharArray();
							tmp[l-5]='0';
							String change = "";
							for(int i = 0; i < tmp.length; i++){
								change += tmp[i];
							}
							send[h+15] = change;
							//Toast.makeText(mContext, send[h+15], Toast.LENGTH_SHORT).show();
						}
						//System.out.printf("坐标为 %d  %d\n",h,l);
						//delay(100);
						break;
				}
				return false;
			}
		});
	}

	private int get_h(float h) {
		int steph = table.getHeight() / 16;
		return (int) (h / steph);
	}

	private int get_l(float l) {
		int stepl = table.getWidth() / 16;
		return (int) (l / stepl);
	}

	private void delay(int ms){
		try {
			Thread.currentThread();
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String translate(int x){
		switch (x){
			case 0:
				return "00";
			case 1:
				return "01";
			case 2:
				return "02";
			case 3:
				return "03";
			case 4:
				return "04";
			case 5:
				return "05";
			case 6:
				return "06";
			case 7:
				return "07";
			case 8:
				return "08";
			case 9:
				return "09";
			case 10:
				return "10";
			case 11:
				return "11";
			case 12:
				return "12";
			case 13:
				return "13";
			case 14:
				return "14";
			case 15:
				return "15";
			case 16:
				return "16";
			case 17:
				return "17";
			case 18:
				return "18";
			case 19:
				return "19";
			case 20:
				return "20";
			case 21:
				return "21";
			case 22:
				return "22";
			case 23:
				return "23";
			case 24:
				return "24";
			case 25:
				return "25";
			case 26:
				return "26";
			case 27:
				return "27";
			case 28:
				return "28";
			case 29:
				return "29";
			case 30:
				return "30";
			case 31:
				return "31";
		}
		return "00";
	}

	private void msgInit(){
		for(int i = 0; i < 32; i++){
			send[i] = "@";//包头
			send[i] += translate(i);//行号
			send[i] += "#";//中标识符
			send[i] += "11111111";//数据
			send[i] += "$";//包尾
		}
	}

	private void init() {
		//list = new ArrayList<deviceListItem>();
		//mAdapter = new deviceListAdapter(this, list);
		//mListView = (ListView) findViewById(com.bluetooth.R.id.list);
		//mListView.setAdapter(mAdapter);
		//mListView.setOnItemClickListener(this);
		//mListView.setFastScrollEnabled(true);
//		editMsgView= (EditText)findViewById(com.bluetooth.R.id.MessageText);
//		editMsgView.clearFocus();
//
//		sendButton= (Button)findViewById(com.bluetooth.R.id.btn_msg_send);
//		sendButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				// TODO Auto-generated method stub
//				String msgText =editMsgView.getText().toString();
//
//
//
//				if (msgText.length()>0) {
//					sendMessageHandle(msgText);
//					editMsgView.setText("");
//					editMsgView.clearFocus();
//					//close InputMethodManager
//					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//					imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
//				}else
//					Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT).show();
//			}
//		});

		disconnectButton= (Button)findViewById(com.bluetooth.R.id.btn_disconnect);
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT)
				{
					shutdownClient();
				}
				else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE)
				{
					shutdownServer();
				}
				Bluetooth.isOpen = false;
				Bluetooth.serviceOrCilent=ServerOrCilent.NONE;
				Toast.makeText(mContext, "已断开连接！", Toast.LENGTH_SHORT).show();
			}
		});
	}
	/*
	private Handler LinkDetectedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
			if(msg.what==1)
			{
				list.add(new deviceListItem((String)msg.obj, true));
			}
			else
			{
				list.add(new deviceListItem((String)msg.obj, false));
			}
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
		}

	};
	*/

	@Override
	public synchronized void onPause() {
		super.onPause();
	}
	@Override
	public synchronized void onResume() {
		super.onResume();
		if(Bluetooth.isOpen)
		{
			Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
			return;
		}
		if(Bluetooth.serviceOrCilent==ServerOrCilent.CILENT)
		{
			String address = Bluetooth.BlueToothAddress;
			if(!address.equals("null"))
			{
				device = mBluetoothAdapter.getRemoteDevice(address);
				clientConnectThread = new clientThread();
				clientConnectThread.start();
				Bluetooth.isOpen = true;
			}
			else
			{
				Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
			}
		}
		else if(Bluetooth.serviceOrCilent==ServerOrCilent.SERVICE)
		{
			startServerThread = new ServerThread();
			startServerThread.start();
			Bluetooth.isOpen = true;
		}
	}
	//开启客户端
	private class clientThread extends Thread {
		public void run() {
			try {
				//创建一个Socket连接：只需要服务器在注册时的UUID号
				// socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
				socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				//连接
				Message msg2 = new Message();
				msg2.obj = "请稍候，正在连接服务器:"+Bluetooth.BlueToothAddress;
				msg2.what = 0;
				//LinkDetectedHandler.sendMessage(msg2);

				socket.connect();

				Message msg = new Message();
				msg.obj = "已经连接上服务端！可以发送信息。";
				msg.what = 0;

				//启动接受数据
				mreadThread = new readThread();
				mreadThread.start();
			}
			catch (IOException e)
			{
				Log.e("connect", "", e);
				Message msg = new Message();
				msg.obj = "连接服务端异常！断开连接重新试一试。";
				msg.what = 0;
				//LinkDetectedHandler.sendMessage(msg);
			}
		}
	};

	//开启服务器
	private class ServerThread extends Thread {
		public void run() {

			try {
				/* 创建一个蓝牙服务器 
				 * 参数分别：服务器名称、UUID	 */
				mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
						UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

				Log.d("server", "wait cilent connect...");

				Message msg = new Message();
				msg.obj = "请稍候，正在等待客户端的连接...";
				msg.what = 0;
				//LinkDetectedHandler.sendMessage(msg);
				
				/* 接受客户端的连接请求 */
				socket = mserverSocket.accept();
				Log.d("server", "accept success !");

				Message msg2 = new Message();
				String info = "客户端已经连接上！可以发送信息。";
				msg2.obj = info;
				msg.what = 0;
				//LinkDetectedHandler.sendMessage(msg2);
				//启动接受数据
				mreadThread = new readThread();
				mreadThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	/* 停止服务器 */
	private void shutdownServer() {
		new Thread() {
			public void run() {
				if(startServerThread != null)
				{
					startServerThread.interrupt();
					startServerThread = null;
				}
				if(mreadThread != null)
				{
					mreadThread.interrupt();
					mreadThread = null;
				}
				try {
					if(socket != null)
					{
						socket.close();
						socket = null;
					}
					if (mserverSocket != null)
					{
						mserverSocket.close();/* 关闭服务器 */
						mserverSocket = null;
					}
				} catch (IOException e) {
					Log.e("server", "mserverSocket.close()", e);
				}
			};
		}.start();
	}
	/* 停止客户端连接 */
	private void shutdownClient() {
		new Thread() {
			public void run() {
				if(clientConnectThread!=null)
				{
					clientConnectThread.interrupt();
					clientConnectThread= null;
				}
				if(mreadThread != null)
				{
					mreadThread.interrupt();
					mreadThread = null;
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
			};
		}.start();
	}
	//发送数据
	private void sendMessageHandle(String msg)
	{
		if (socket == null)
		{
			Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			OutputStream os = socket.getOutputStream();
			os.write(msg.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//list.add(new deviceListItem(msg, false));
		//mAdapter.notifyDataSetChanged();
		//mListView.setSelection(list.size() - 1);
	}
	//读取数据
	private class readThread extends Thread {
		public void run() {

			byte[] buffer = new byte[1024];
			int bytes;
			InputStream mmInStream = null;

			try {
				mmInStream = socket.getInputStream();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (true) {
				try {
					// Read from the InputStream
					if( (bytes = mmInStream.read(buffer)) > 0 )
					{
						byte[] buf_data = new byte[bytes];
						for(int i=0; i<bytes; i++)
						{
							buf_data[i] = buffer[i];
						}
						String s = new String(buf_data);
						Message msg = new Message();
						msg.obj = s;
						msg.what = 1;
						//LinkDetectedHandler.sendMessage(msg);
					}
				} catch (IOException e) {
					try {
						mmInStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					break;
				}
			}
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT)
		{
			shutdownClient();
		}
		else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE)
		{
			shutdownServer();
		}
		Bluetooth.isOpen = false;
		Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
	}
	public class SiriListItem {
		String message;
		boolean isSiri;

		public SiriListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}
	public class deviceListItem {
		String message;
		boolean isSiri;

		public deviceListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
}