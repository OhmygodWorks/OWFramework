/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * QoS4ReciveDaemon.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.server.protocal.Protocol;

import java.util.concurrent.ConcurrentHashMap;

public class QoS4ReceiveDaemon
{
	private static final String TAG = QoS4ReceiveDaemon.class.getSimpleName();
	public static final int CHECK_INTERVAL = 300000;
	public static final int MESSAGES_VALID_TIME = 600000;
	private ConcurrentHashMap<String, Long> receivedMessages = new ConcurrentHashMap();

	private Handler handler = null;
	private Runnable runnable = null;

	private boolean running = false;

	private boolean _executing = false;

	private Context context = null;

	private static QoS4ReceiveDaemon instance = null;

	public static QoS4ReceiveDaemon getInstance(Context context)
	{
		if (instance == null) {
			instance = new QoS4ReceiveDaemon(context);
		}

		return instance;
	}

	public QoS4ReceiveDaemon(Context context)
	{
		this.context = context;

		init();
	}

	private void init()
	{
		this.handler = new Handler();
		this.runnable = new Runnable()
		{
			public void run()
			{
				// 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
				// 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
				if (!QoS4ReceiveDaemon.this._executing)
				{
					QoS4ReceiveDaemon.this._executing = true;

					if (ClientCoreSDK.DEBUG) {
						Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ START 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemon.this.receivedMessages.size() + ".");
					}

					for (String key : QoS4ReceiveDaemon.this.receivedMessages.keySet())
					{
						long delta = System.currentTimeMillis() - QoS4ReceiveDaemon.this.receivedMessages.get(key);

						if (delta < MESSAGES_VALID_TIME)
							continue;
						if (ClientCoreSDK.DEBUG)
							Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】指纹为" + key + "的包已生存" + delta +
									"ms(最大允许" + MESSAGES_VALID_TIME + "ms), 马上将删除之.");
						QoS4ReceiveDaemon.this.receivedMessages.remove(key);
					}

				}

				if (ClientCoreSDK.DEBUG) {
					Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ END 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemon.this.receivedMessages.size() + ".");
				}

				QoS4ReceiveDaemon.this._executing = false;

				QoS4ReceiveDaemon.this.handler.postDelayed(QoS4ReceiveDaemon.this.runnable, CHECK_INTERVAL);
			}
		};
	}

	public void startup(boolean immediately)
	{
		stop();

		if ((this.receivedMessages != null) && (this.receivedMessages.size() > 0))
		{
			for (String key : this.receivedMessages.keySet())
			{
				putImpl(key);
			}

		}

		this.handler.postDelayed(this.runnable, immediately ? 0 : CHECK_INTERVAL);

		this.running = true;
	}

	public void stop()
	{
		this.handler.removeCallbacks(this.runnable);

		this.running = false;
	}

	public boolean isRunning()
	{
		return this.running;
	}

	public void addReceived(Protocol p)
	{
		if ((p != null) && (p.isQoS()))
			addReceived(p.getFp());
	}

	public void addReceived(String fingerPrintOfProtocol)
	{
		if (fingerPrintOfProtocol == null)
		{
			Log.w(TAG, "【IMCORE】无效的 fingerPrintOfProtocol==null!");
			return;
		}

		if (this.receivedMessages.containsKey(fingerPrintOfProtocol)) {
			Log.w(TAG, "【IMCORE】【QoS接收方】指纹为" + fingerPrintOfProtocol +
					"的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
		}

		putImpl(fingerPrintOfProtocol);
	}

	private void putImpl(String fingerPrintOfProtocol)
	{
		if (fingerPrintOfProtocol != null)
			this.receivedMessages.put(fingerPrintOfProtocol, System.currentTimeMillis());
	}

	public boolean hasReceived(String fingerPrintOfProtocol)
	{
		return this.receivedMessages.containsKey(fingerPrintOfProtocol);
	}

	public int size()
	{
		return this.receivedMessages.size();
	}
}