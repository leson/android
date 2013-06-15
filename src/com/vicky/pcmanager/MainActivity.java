package com.vicky.pcmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	/** Called when the activity is first created. */
	public static final String DEFULT_PRES = "192.168.1.102";
	public static final String PREFS_NAME = "PreferencesFile";
	public static final int CONNENTED = 0;
	public static final int UPDATALOG = 1;
	
	private EditText command;
	private EditText ipEdit;
	private TextView log;
	private Button send;
	private Button clean;
	private String ip;
	private String logMsg;
	private Socket socket;
	private BufferedWriter writer;
	private InetSocketAddress isa = null;
	Context mContext;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		findviews();
		setonclick();
		init();
	}

	public void findviews() {
		command = (EditText) this.findViewById(R.id.command);
		log = (TextView) this.findViewById(R.id.log);
		send = (Button) this.findViewById(R.id.send);
		ipEdit = (EditText) this.findViewById(R.id.ipEdit);
		clean = (Button) this.findViewById(R.id.clean);
	}

	private void init() {
		log.setMovementMethod(ScrollingMovementMethod.getInstance());
		logMsg = log.getText().toString();
		socket = new Socket();

		ip = onLoad();
		if (ip != null) {
			ipEdit.setText(ip);
		}
	}

	private void setonclick() {
		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ip = ipEdit.getText().toString();
				tcpClient tcp = new tcpClient(command.getText().toString());
				tcp.start();
			}
		});

		clean.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				logMsg = "";
				log.setText(logMsg);
			}
		});
	}

	public void connecttoserver() throws UnknownHostException, IOException {
		socket = RequestSocket(ip, 20000);
		if (socket.isConnected()) {
			Message msg = new Message();
			msg.what = CONNENTED;
			mHandler.sendMessage(msg);
		}
	}

	private Socket RequestSocket(String host, int port)
			throws UnknownHostException, IOException {

		Socket ConSocket = new Socket();
		isa = new InetSocketAddress(host, port);
		ConSocket.connect(isa);
		return ConSocket;
	}

	private void SendMsg(Socket socket, String msg) throws IOException {
		writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));

		Log.i("msg", msg.replace("\n", " ") + "\n");
		writer.write(msg.replace("\n", " ") + "\n");
		writer.flush();
	}

	private String ReceiveMsg(Socket socket) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		String line;
		String txt = "";
		while ((line = reader.readLine()) != null) {
			txt += line + "\n";
		}
		reader.close();
		return txt;

	}

	class tcpClient extends Thread {
		String commandString;

		public tcpClient() {
			commandString = "cmd /c dir";
		}

		public tcpClient(String command) {
			commandString = command;
		}

		public void run() {
			String recv;
			try {
				connecttoserver();
				SendMsg(socket, commandString);
				recv = ReceiveMsg(socket);
				if (recv != null) {
					logMsg += recv;
					// close BufferedWriter and socket
					writer.close();
					socket.close();
					// updata to log
					Message msg = new Message();
					msg.what = UPDATALOG;
					mHandler.sendMessage(msg);

				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String onLoad() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String mPreferences = settings.getString("preferences", DEFULT_PRES);
		return mPreferences;
	}

	private void onSave(String save) {

		if (TextUtils.isEmpty(save)) {
			setPreferences(DEFULT_PRES);
		} else {
			setPreferences(save);
		}

	}

	private void setPreferences(String mPreferences) {

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("preferences", mPreferences);
		editor.commit();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("PC Manager");
			builder.setMessage("exit ?");
			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							onSave(ipEdit.getText().toString());
							finish();
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

						}
					});
			builder.show();
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 1, "shutdown");
		menu.add(0, 2, 2, "reboot");
		menu.add(0, 3, 3, "exit");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			tcpClient tcp = new tcpClient("sudo poweroff");
			tcp.start();
			return true;
		case 2:
			tcp = new tcpClient("sudo reboot");
			tcp.start();
			return true;
		case 3:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case CONNENTED:
				logMsg += "\n" + "Server Connented\n\n";
				log.setText(logMsg);
				break;

			case UPDATALOG:
				log.setText(logMsg);
				log.setScrollContainer(true);
				break;
			}
		}
	};
}