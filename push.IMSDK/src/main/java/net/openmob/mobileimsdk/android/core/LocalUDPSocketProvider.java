/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * LocalUDPSocketProvider.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.support.annotation.Nullable;
import android.util.Log;

import java.net.DatagramSocket;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getLocalUDPPort;

public final class LocalUDPSocketProvider
{
	private static final String TAG = LocalUDPSocketProvider.class.getSimpleName();
	@Nullable
	private static DatagramSocket localUDPSocket = null;

	private LocalUDPSocketProvider() {}

	private static DatagramSocket resetLocalUDPSocket()
	{
		try
		{
			closeLocalUDPSocket();
			if (DEBUG) {
				Log.d(TAG, "【IMCORE】new DatagramSocket()中...");
			}
			localUDPSocket = (getLocalUDPPort() == 0 ?
					new DatagramSocket() : new DatagramSocket(getLocalUDPPort()));
			localUDPSocket.setReuseAddress(true);
			if (DEBUG) {
				Log.d(TAG, "【IMCORE】new DatagramSocket()已成功完成.");
			}
			return localUDPSocket;
		}
		catch (Exception e)
		{
			Log.w(TAG, "【IMCORE】localUDPSocket创建时出错，原因是：" + e.getMessage(), e);
			closeLocalUDPSocket();
			return null;
		}
	}

	private static boolean isLocalUDPSocketReady()
	{
		return (localUDPSocket != null) && (!localUDPSocket.isClosed());
	}

	static DatagramSocket getLocalUDPSocket()
	{
		if (isLocalUDPSocketReady())
		{
			if (DEBUG)
				Log.d(TAG, "【IMCORE】isLocalUDPSocketReady()==true，直接返回本地socket引用哦。");
			return localUDPSocket;
		} else {
			if (DEBUG)
				Log.d(TAG, "【IMCORE】isLocalUDPSocketReady()==false，需要先resetLocalUDPSocket()...");
			return resetLocalUDPSocket();
		}
	}

	public static void closeLocalUDPSocket()
	{
		try
		{
			if (DEBUG)
				Log.d(TAG, "【IMCORE】正在closeLocalUDPSocket()...");
			if (localUDPSocket != null)
			{
				localUDPSocket.close();
				localUDPSocket = null;
			}
			else
			{
				Log.d(TAG, "【IMCORE】Socket处于未初化状态（可能是您还未登陆），无需关闭。");
			}
		}
		catch (Exception e)
		{
			Log.w(TAG, "【IMCORE】closeLocalUDPSocket时出错，原因是：" + e.getMessage(), e);
		}
	}
}