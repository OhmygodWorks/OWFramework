/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ClientCoreSDK.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import net.openmob.mobileimsdk.android.core.AutoReLoginDaemon;
import net.openmob.mobileimsdk.android.core.KeepAliveDaemon;
import net.openmob.mobileimsdk.android.core.LocalUDPDataReceiver;
import net.openmob.mobileimsdk.android.core.LocalUDPDataSender;
import net.openmob.mobileimsdk.android.core.QoS4ReceiveDaemon;
import net.openmob.mobileimsdk.android.core.QoS4SendDaemon;
import net.openmob.mobileimsdk.android.event.ChatBaseEvent;
import net.openmob.mobileimsdk.android.event.ChatBaseEvent.SimpleChatBaseEvent;
import net.openmob.mobileimsdk.android.event.ChatTransDataEvent;
import net.openmob.mobileimsdk.android.event.ChatTransDataEvent.SimpleChatTransDataEvent;
import net.openmob.mobileimsdk.android.event.MessageQoSEvent;
import net.openmob.mobileimsdk.android.event.MessageQoSEvent.SimpleMessageQoSEvent;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import io.reactivex.disposables.Disposable;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.SenseMode.MODE_10S;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getAppKey;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setSenseMode;
import static net.openmob.mobileimsdk.android.core.LocalUDPSocketProvider.closeLocalUDPSocket;

public final class ClientCoreSDK
{
	public static boolean DEBUG = true;

	public static boolean autoReLogin = true;

	private static final String TAG = ClientCoreSDK.class.getSimpleName();

	private static final ChatBaseEvent dummyBaseEvent = new SimpleChatBaseEvent(){};

	private static final ChatTransDataEvent dummyDataEvent = new SimpleChatTransDataEvent(){};

	private static final MessageQoSEvent dummyQoSEvent = new SimpleMessageQoSEvent() {};

	private static boolean _init = false;

	private static boolean _localDeviceNetworkOk = true;

	private static boolean _connectedToServer = true;

	private static boolean _loginHasInit = false;

	private static int _currentUserId = -1;
	@NonNull
	private static String _currentLoginName = "";
	@NonNull
	private static String _currentLoginPsw = "";
	@NonNull
	private static String _currentLoginExtra = "";
	@NonNull
	private static ChatBaseEvent _chatBaseEvent = dummyBaseEvent;
	@NonNull
	private static ChatTransDataEvent _chatTransDataEvent = dummyDataEvent;
	@NonNull
	private static MessageQoSEvent _messageQoSEvent = dummyQoSEvent;
	@NonNull
	private static Reference<Context> _context = new SoftReference<>(null);
	@NonNull
	private static final BroadcastReceiver networkConnectStatusReceiver = new BroadcastReceiver()
	{
		private ConnectivityManager getConnectivityManager(Context context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				return context.getSystemService(ConnectivityManager.class);
			} else {
				return (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
			}
		}
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		private boolean noConnectivity(ConnectivityManager manager, Network... networks) {
			boolean noConnectivity = true;
			for(Network network : networks) {
				NetworkInfo info = manager.getNetworkInfo(network);
				if (info == null) continue;
				if (DEBUG) {
					Log.d(TAG, info.getTypeName()+"("+info.getSubtypeName()+"):"+info.getDetailedState());
					if (info.isConnected()) noConnectivity = false;
				} else if (info.isConnected()) {
					Log.i(TAG, info.getTypeName()+"("+info.getSubtypeName()+"):"+"connected");
					return false;
				}
			}
			return noConnectivity;
		}
		private boolean noConnectivity(ConnectivityManager manager) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return noConnectivity(manager, manager.getAllNetworks());
			} else {
				//noinspection deprecation
				NetworkInfo mobNetInfo = manager.getNetworkInfo(TYPE_MOBILE);
				//noinspection deprecation
				NetworkInfo wifiNetInfo = manager.getNetworkInfo(TYPE_WIFI);
				//
				return  !(mobNetInfo != null && mobNetInfo.isConnected()) &&
						!(wifiNetInfo != null && wifiNetInfo.isConnected());
			}
		}
		private boolean noConnectivity(Context context) {
			return noConnectivity(getConnectivityManager(context));
		}
		@Override
		public void onReceive(Context context, Intent intent)
		{
			boolean noConnection = noConnectivity(context);
			if (noConnection != _localDeviceNetworkOk) {
				// 网络状态没改
				return;
			} else if (noConnection) {
				Log.e(TAG, "【IMCORE】【本地网络通知】检测本地网络连接断开了!");
			} else if (DEBUG) {
				Log.i(TAG, "【IMCORE】【本地网络通知】检测本地网络已连接上了!");
			}
			_localDeviceNetworkOk = !noConnection;
			closeLocalUDPSocket();
		}
	};

	private ClientCoreSDK() {
	}

	public static void init(@NonNull Context context)
	{
		if (!_init) {
			// 将全局Application作为context上下文句柄：
			//   由于Android程序的特殊性，整个APP的生命周中除了Application外，其它包括Activity在内
			//   都可能是短命且不可靠的（随时可能会因虚拟机资源不足而被回收），所以MobileIMSDK作为跟
			//   整个APP的生命周期保持一致的全局资源，它的上下文用Application是最为恰当的。
			if ((context instanceof Application)) {
				_context = new SoftReference<>(context);
			} else {
				_context = new SoftReference<>(context = context.getApplicationContext());
			}
			// Register for broadcasts when network status changed
			context.registerReceiver(networkConnectStatusReceiver,
					new IntentFilter(CONNECTIVITY_ACTION));
			setSenseMode(MODE_10S);
			_init = true;
		}
	}

	public static Disposable login(String loginName, String loginPsw) {
		return LocalUDPDataSender.login(loginName, loginPsw, getAppKey());
	}

	public static Disposable logout() {
		return LocalUDPDataSender.logout().subscribe();
	}

	public static void release()
	{
		// 尝试停掉掉线重连线程（如果线程正在运行的话）
		AutoReLoginDaemon.stop(); // 2014-11-08 add by Jack Jiang
		// 尝试停掉QoS质量保证（发送）心跳线程
		QoS4SendDaemon.stop();
		// 尝试停掉Keep Alive心跳线程
		KeepAliveDaemon.stop();
		// 尝试停掉消息接收者
		LocalUDPDataReceiver.stop();
		// 尝试停掉QoS质量保证（接收防重复机制）心跳线程
		QoS4ReceiveDaemon.stop();
		// 尝试关闭本地Socket
		closeLocalUDPSocket();
		try {
			_context.get().unregisterReceiver(networkConnectStatusReceiver);
		} catch (Exception e) {
			Log.w(TAG, e.getMessage(), e);
		} finally {
			_context = new SoftReference<>(null);
		}

		_init = false;

		setLoginHasInit(false);
		setConnectedToServer(false);
	}

	public static int getCurrentUserId()
	{
		return _currentUserId;
	}

	public static void setCurrentUserId(int currentUserId)
	{
		_currentUserId = currentUserId;
	}

	public static String getCurrentLoginName()
	{
		return _currentLoginName;
	}

	public static void setCurrentLoginName(String currentLoginName)
	{
		_currentLoginName = (currentLoginName==null?"":currentLoginName);
	}

	public static String getCurrentLoginPsw()
	{
		return _currentLoginPsw;
	}

	public static void setCurrentLoginPsw(String currentLoginPsw)
	{
		_currentLoginPsw = (currentLoginPsw==null?"":currentLoginPsw);
	}
	
	public static String getCurrentLoginExtra()
	{
		return _currentLoginExtra;
	}

	public static void setCurrentLoginExtra(String currentLoginExtra)
	{
		_currentLoginExtra = (currentLoginExtra==null?"":currentLoginExtra);
	}

	public static boolean isLoginHasInit()
	{
		return _loginHasInit;
	}

	public static void setLoginHasInit(boolean loginHasInit)
	{
		_loginHasInit = loginHasInit;
	}

	public static boolean isConnectedToServer()
	{
		return _connectedToServer;
	}

	public static void setConnectedToServer(boolean connectedToServer)
	{
		_connectedToServer = connectedToServer;
	}

	public static boolean isInitialed()
	{
		return _init;
	}

	public static boolean isLocalDeviceNetworkOk()
	{
		return _localDeviceNetworkOk;
	}

	public static void setChatBaseEvent(ChatBaseEvent chatBaseEvent)
	{
		_chatBaseEvent = (chatBaseEvent==null?dummyBaseEvent:chatBaseEvent);
	}

	@NonNull
	public static ChatBaseEvent getChatBaseEvent()
	{
		return _chatBaseEvent;
	}

	public static void setChatTransDataEvent(ChatTransDataEvent chatTransDataEvent)
	{
		_chatTransDataEvent = (chatTransDataEvent==null?dummyDataEvent:chatTransDataEvent);
	}

	@NonNull
	public static ChatTransDataEvent getChatTransDataEvent()
	{
		return _chatTransDataEvent;
	}

	public static void setMessageQoSEvent(MessageQoSEvent messageQoSEvent)
	{
		_messageQoSEvent = (messageQoSEvent==null?dummyQoSEvent:messageQoSEvent);
	}

	@NonNull
	public static MessageQoSEvent getMessageQoSEvent()
	{
		return _messageQoSEvent;
	}
}