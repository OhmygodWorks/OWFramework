/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * KeepAliveDaemon.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import net.openmob.mobileimsdk.android.conf.ConfigEntity;
import net.openmob.mobileimsdk.android.conf.ConfigEntity.SenseMode;

import java.util.Observable;
import java.util.Observer;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;

/**
 * 后台线程不断发心跳消息。
 */
public final class KeepAliveDaemon
{
	private static final String TAG = KeepAliveDaemon.class.getSimpleName();
	private static final Observer dummyObserver = new Observer() {
		@Override
		public void update(Observable o, Object arg) {
			Log.d(TAG, "lost network connection");
		}
	};

	/**
	 * 限制只能通过{@link ConfigEntity#setSenseMode(SenseMode)}来更改，不要手动改
	 * @hide
	 */
	public static int NETWORK_CONNECTION_TIME_OUT = 10000;
	/**
	 * 限制只能通过{@link ConfigEntity#setSenseMode(SenseMode)}来更改，不要手动改
	 * @hide
	 */
	public static int KEEP_ALIVE_INTERVAL = 3000;

	private static Handler handler = new Handler();
	private static Runnable runnable =  new Runnable()
	{
		public void run()
		{
			// 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
			// 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
			if (_executing) {
				return;
			}
			new AsyncTask<Object, Integer, Integer>()
			{
				private boolean willStop = false;

				protected Integer doInBackground(Object[] params)
				{
					return bgKeepAlive();
				}

				protected void onPostExecute(Integer code)
				{
					willStop = fgKeepAliveResult(code, willStop);
				}
			}.execute();
		}
	};
	private static boolean keepAliveRunning = false;
	private static long lastGetKeepAliveResponseFromServerTimestamp = 0L;
	@NonNull
	private static Observer networkConnectionLostObserver = dummyObserver;
	private static boolean _executing = false;

	private KeepAliveDaemon() {}

	@WorkerThread
	private static int bgKeepAlive() {
		_executing = true;
		if (DEBUG) Log.d(TAG, "【IMCORE】心跳线程执行中...");
		return LocalUDPDataSender.sendKeepAlive();
	}

	private static boolean fgKeepAliveResult(int code, boolean willStop)
	{
		boolean isInitialedForKeepAlive =
				lastGetKeepAliveResponseFromServerTimestamp == 0L;
		if (code == 0 && isInitialedForKeepAlive) {
			updateGetKeepAliveResponseFromServerTimestamp();
		}
		if (!isInitialedForKeepAlive)
		{
			long now = System.currentTimeMillis();
			// 当当前时间与最近一次服务端的心跳响应包时间间隔>= 10秒就判定当前与服务端的网络连接已断开
			if (now - lastGetKeepAliveResponseFromServerTimestamp
					>= NETWORK_CONNECTION_TIME_OUT)
			{
				stop();
				networkConnectionLostObserver.update(null, null);
				willStop = true;
			}
		}
		_executing = false;
		if (!willStop)
		{
			// 开始下一个心跳循环
			handler.postDelayed(runnable, KEEP_ALIVE_INTERVAL);
		}
		return willStop;
	}

	public static void stop()
	{
		handler.removeCallbacks(runnable);
		keepAliveRunning = false;
		lastGetKeepAliveResponseFromServerTimestamp = 0L;
	}

	static void start(boolean immediately)
	{
		stop();
		handler.postDelayed(runnable, immediately ? 0 : KEEP_ALIVE_INTERVAL);
		keepAliveRunning = true;
	}

	public static boolean isKeepAliveRunning()
	{
		return keepAliveRunning;
	}

	static void updateGetKeepAliveResponseFromServerTimestamp()
	{
		lastGetKeepAliveResponseFromServerTimestamp = System.currentTimeMillis();
	}

	static void setNetworkConnectionLostObserver(
			@SuppressWarnings("NullableProblems") Observer networkConnectionLostObserver
	) {
		//noinspection ConstantConditions
		KeepAliveDaemon.networkConnectionLostObserver =
				(networkConnectionLostObserver==null?dummyObserver:networkConnectionLostObserver);
	}
}