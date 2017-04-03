/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * LocalUDPDataReceiver.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import net.openmob.mobileimsdk.server.protocol.Protocol;
import net.openmob.mobileimsdk.server.protocol.s.PErrorResponse;
import net.openmob.mobileimsdk.server.protocol.s.PLoginInfoResponse;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getChatBaseEvent;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getChatTransDataEvent;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getMessageQoSEvent;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setConnectedToServer;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setCurrentUserId;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setLoginHasInit;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getLocalUDPPort;
import static net.openmob.mobileimsdk.android.core.KeepAliveDaemon.setNetworkConnectionLostObserver;
import static net.openmob.mobileimsdk.android.core.KeepAliveDaemon.updateGetKeepAliveResponseFromServerTimestamp;
import static net.openmob.mobileimsdk.android.core.LocalUDPDataSender.sendReceivedBack;
import static net.openmob.mobileimsdk.android.core.LocalUDPSocketProvider.getLocalUDPSocket;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.COMMON_CODE_OK;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.parse;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.parsePErrorResponse;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.parsePLoginInfoResponse;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_RECEIVED;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN;

/**
 * 通过本地线程不断接收UDP包来处理。
 */
public final class LocalUDPDataReceiver
{
	/** log TAG */
	private static final String TAG = LocalUDPDataReceiver.class.getSimpleName();
	/** UDP包的数据尺寸。TODO：搞到UDP包尺寸的官方常量 */
	private static final int DATAGRAM_PACKET_BUFFER_SIZE = 1024;
	/** 为了优化内存分配而设置的缓存池 */
	private static final Queue<DatagramPacket> cache = new ArrayDeque<>();
	/** 用来处理收到了的UDP包 */
	private static Handler messageHandler = null;
	/** 用来处理收到了的UDP包的线程。因为不想占用前台的时间，所以开后台线程来处理。 */
	private static HandlerThread handlerThread = null;
	/** 用来收UDP包的线程。因为要做无限循环，和后台处理的线程冲突，所以不得不开别的线程 */
	private static Thread listeningThread = null;

	private LocalUDPDataReceiver() {}

	public static synchronized void stop()
	{
		if (listeningThread != null)
		{
			listeningThread.interrupt();
			listeningThread = null;
		}
		if (handlerThread != null)
		{
			handlerThread.quit();
			handlerThread = null;
			messageHandler = null;
		}
	}

	static synchronized void startup()
	{
		stop();
		try
		{
			handlerThread = new HandlerThread(TAG);
			handlerThread.start();
			listeningThread = new Thread(LocalUDPDataReceiver::p2pListening);
			listeningThread.start();
			messageHandler = new Handler(handlerThread.getLooper(), LocalUDPDataReceiver::handleMessage);
		}
		catch (Exception e)
		{
			Log.e(TAG, "【IMCORE】本地UDPSocket监听开启时发生异常," + e.getMessage(), e);
		}
	}

	@WorkerThread
	private static void p2pListening() {
		try
		{
			if (DEBUG) {
				Log.d(TAG, "【IMCORE】本地UDP端口侦听中，端口=" + getLocalUDPPort() + "...");
			}
			//开始侦听
			p2pListeningImpl();
		}
		catch (InterruptedException|InterruptedIOException e)
		{
			Log.i(TAG, "【IMCORE】本地UDP监听停止了:" + "socket被关闭了", e);
		}
		catch (Exception e)
		{
			Log.e(TAG, "【IMCORE】本地UDP监听停止了:" + e.getMessage(), e);
		}
	}

	@WorkerThread
	private static void p2pListeningImpl() throws Exception
	{
		//noinspection InfiniteLoopStatement
		while (true)
		{
			DatagramSocket localUDPSocket = getLocalUDPSocket();
			if (localUDPSocket.isClosed()) continue;
			// 接收数据报的包
			DatagramPacket packet = providePacket();
			localUDPSocket.receive(packet);
			Message.obtain(messageHandler, 0, packet).sendToTarget();
		}
	}

	/**
	 * 实现成缓冲池以减轻回收数据时候整理内存的负担。
	 * @return 缓冲的，或者是新建的UDP包
 	 */
	@WorkerThread
	private static DatagramPacket providePacket() {
		DatagramPacket packet = cache.peek();
		if (packet == null) {
			byte[] data = new byte[DATAGRAM_PACKET_BUFFER_SIZE];
			packet = new DatagramPacket(data, data.length);
		}
		return packet;
	}

	/**
	 * 回收用完的UDP包。目前只是单纯的放回缓存池。
	 * TODO：定义指标评估并控制缓存池的大小，及时放弃那些不经常用到的多余的包。
	 * @param packet 用完需要回收的UDP包
	 */
	@WorkerThread
	private static void recycle(@NonNull DatagramPacket packet) {
		byte[] data = packet.getData();
		if (data.length < DATAGRAM_PACKET_BUFFER_SIZE) {
			data = new byte[DATAGRAM_PACKET_BUFFER_SIZE];
		} else {
			Arrays.fill(data, (byte)0);
		}
		packet.setData(data);
		cache.offer(packet);
	}

	@WorkerThread
	private static boolean hasReceived(Protocol pFromServer) {
		final boolean hasReceived = QoS4ReceiveDaemon.hasReceived(pFromServer.getFp());
		if (hasReceived && DEBUG) {
			Log.d(TAG, "【IMCORE】【QoS机制】" + pFromServer.getFp()
					 + "已经存在于发送列表中，这是重复包，通知应用层收到该包.");
		}
		QoS4ReceiveDaemon.addReceived(pFromServer);
		sendReceivedBack(pFromServer);
		return hasReceived;
	}

	@WorkerThread
	private static void onLoginResponse(PLoginInfoResponse loginInfoRes) {
		if (loginInfoRes.getCode() == COMMON_CODE_OK)
		{
			setLoginHasInit(true);
			setCurrentUserId(loginInfoRes.getUser_id());
			AutoReLoginDaemon.stop();
			setNetworkConnectionLostObserver((observable, data) -> {
				QoS4SendDaemon.stop();
				QoS4ReceiveDaemon.stop();
				setConnectedToServer(false);
				setCurrentUserId(-1);
				getChatBaseEvent().onLinkCloseMessage(-1);
				AutoReLoginDaemon.start(true);
			});
			KeepAliveDaemon.start(false);
			QoS4SendDaemon.startup(true);
			QoS4ReceiveDaemon.startup(true);
			setConnectedToServer(true);
		}
		else
		{
			setConnectedToServer(false);
			setCurrentUserId(-1);
		}
		// 通知用户登录结果
		getChatBaseEvent().onLoginMessage(loginInfoRes.getUser_id(), loginInfoRes.getCode());
	}

	@WorkerThread
	private static void onErrorResponse(PErrorResponse errorRes) {
		if (errorRes.getErrorCode() == 301)
		{
			setLoginHasInit(false);
			Log.e(TAG, "【IMCORE】收到服务端的“尚未登陆”的错误消息，心跳线程将停止，请应用层重新登陆.");
			KeepAliveDaemon.stop();
			AutoReLoginDaemon.start(false);
		}
		// 通知用户收到了服务器传回来的出错信息
		getChatTransDataEvent().onErrorResponse(errorRes.getErrorCode(), errorRes.getErrorMsg());
	}

	@WorkerThread
	private static boolean handleMessage(Message msg) {
		DatagramPacket packet = (DatagramPacket)msg.obj;
		if (packet == null) return false;
		try
		{
			Protocol pFromServer = parse(packet.getData(), packet.getLength());
			// 检查如果是重复收到的QoS包就不需要继续处理了
			if (pFromServer.isQoS() && hasReceived(pFromServer)) return true;
			//
			switch (pFromServer.getType())
			{
				case FROM_CLIENT_TYPE_OF_COMMON$DATA:
				{
					getChatTransDataEvent().onTransBuffer
							(pFromServer.getFp(), pFromServer.getFrom(), pFromServer.getDataContent());
					break;
				}
				case FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE:
				{
					if (DEBUG) {
						Log.d(TAG, "【IMCORE】收到服务端回过来的Keep Alive心跳响应包.");
					}
					updateGetKeepAliveResponseFromServerTimestamp();
					break;
				}
				case FROM_CLIENT_TYPE_OF_RECEIVED:
				{
					String theFingerPrint = pFromServer.getDataContent();
					if (DEBUG) {
						Log.d(TAG, "【IMCORE】【QoS】收到" + pFromServer.getFrom()
								 + "发过来的指纹为" + theFingerPrint + "的应答包.");
					}
					// 通知用户
					getMessageQoSEvent().messagesBeReceived(theFingerPrint);
					// 消息已发送成功
					QoS4SendDaemon.remove(theFingerPrint);
					break;
				}
				case FROM_SERVER_TYPE_OF_RESPONSE$LOGIN:
				{
					onLoginResponse(parsePLoginInfoResponse(pFromServer.getDataContent()));
					break;
				}
				case FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR:
				{
					onErrorResponse(parsePErrorResponse(pFromServer.getDataContent()));
					break;
				}
				default:
					Log.w(TAG, "【IMCORE】收到的服务端消息类型：" + pFromServer.getType()
							 + "，但目前该类型客户端不支持解析和处理！");
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "【IMCORE】处理消息的过程中发生了错误.", e);
		}
		finally {
			recycle(packet);
		}
		return true;
	}
}