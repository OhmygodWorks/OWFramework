/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * LocalUDPDataSender.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.android.utils.UDPUtils;
import net.openmob.mobileimsdk.server.protocol.Protocol;

import java.net.DatagramSocket;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import static android.text.TextUtils.isEmpty;
import static io.reactivex.Single.just;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static java.net.InetAddress.getByName;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getCurrentLoginName;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getCurrentUserId;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.isInitialed;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.isLocalDeviceNetworkOk;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.isLoginHasInit;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setCurrentLoginExtra;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setCurrentLoginName;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setCurrentLoginPsw;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getServerIP;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getServerUDPPort;
import static net.openmob.mobileimsdk.android.core.LocalUDPSocketProvider.getLocalUDPSocket;
import static net.openmob.mobileimsdk.server.protocol.CharsetHelper.getString;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.COMMON_CODE_OK;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.COMMON_DATA_SEND_FAILED;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.COMMON_INVALID_PROTOCOL;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.ForC.BAD_CONNECT_TO_SERVER;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.ForC.TO_SERVER_NET_INFO_NOT_SETUP;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.createCommonData;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.createPKeepAlive;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.createPLoginInfo;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.createPLogoutInfo;
import static net.openmob.mobileimsdk.server.protocol.ProtocolFactory.createReceivedBack;

public final class LocalUDPDataSender
{
	private static final String TAG = LocalUDPDataSender.class.getSimpleName();

	private LocalUDPDataSender() {}

	public static Disposable login(String loginName, String loginPsw, String extra) {
		return just(extra)
				.subscribeOn(io())
				.map(s -> sendLogin(loginName, loginPsw, extra))
				.observeOn(mainThread())
				.subscribe(code -> {
					if (code!=null && code == COMMON_CODE_OK)
					{
						LocalUDPDataReceiver.startup();
					}
					else
					{
						Log.d(TAG, "【IMCORE】数据发送失败, 错误码是：" + code + "！");
					}
				});
	}

	@WorkerThread
	static int sendLogin(String loginName, String loginPsw, String extra)
	{
		byte[] b = createPLoginInfo(loginName, loginPsw, extra).toBytes();
		int code = send(b, b.length);
		// 登陆信息成功发出时就把登陆名存下来
		if(code == COMMON_CODE_OK)
		{
			setCurrentLoginName(loginName);
			setCurrentLoginPsw(loginPsw);
			setCurrentLoginExtra(extra);
		}
		return code;
	}

	public static Maybe<Integer> logout() {
		return just(getCurrentUserId())
				.subscribeOn(io())
				.filter(i -> isLoginHasInit())
				.map(id -> createPLogoutInfo(id, getCurrentLoginName()))
				.map(Protocol::toBytes)
				.map(b -> send(b, b.length))
				.observeOn(mainThread())
				.doOnSuccess(code -> {
					// 登出信息成功发出时
					if(code == COMMON_CODE_OK)
					{
//						// 发出退出登陆的消息同时也关闭心跳线程
//						KeepAliveDaemon.getInstance(context).stop();
//						// 重置登陆标识
//						ClientCoreSDK.setLoginHasInit(false);
					}
				})
				.doAfterSuccess(code -> ClientCoreSDK.release())
				.doOnComplete(ClientCoreSDK::release);
	}

	@WorkerThread
	static int sendKeepAlive()
	{
		byte[] b = createPKeepAlive(getCurrentUserId()).toBytes();
		return send(b, b.length);
	}

	@WorkerThread
	private static int sendCommonData(Protocol p)
	{
		if (p == null) return COMMON_INVALID_PROTOCOL;
		byte[] b = p.toBytes();
		int code = send(b, b.length);
		if(code == COMMON_CODE_OK)
		{
			//【【C2C或C2S模式下的QoS机制1/4步：将包加入到发送QoS队列中】】
			// 如果需要进行QoS质量保证，则把它放入质量保证队列中供处理(已在存在于列
			// 表中就不用再加了，已经存在则意味当前发送的这个是重传包哦)
			if (p.isQoS() && !QoS4SendDaemon.exist(p.getFp()))
				QoS4SendDaemon.put(p);
		}
		return code;
	}

	@WorkerThread
	private static int send(byte[] fullProtocolBytes, int dataLen)
	{
		if(!isInitialed()) return CLIENT_SDK_NO_INITIALED;
		
		if(!isLocalDeviceNetworkOk())
		{
			Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
			return LOCAL_NETWORK_NOT_WORKING;
		}

		DatagramSocket ds = getLocalUDPSocket();
		// 如果Socket没有连接上服务端
		if(ds != null && !ds.isConnected()) try {
			if (isEmpty(getServerIP())) {
				Log.w(TAG, "【IMCORE】send数据没有继续，原因是ConfigEntity.server_ip==null!");
				return TO_SERVER_NET_INFO_NOT_SETUP;
			}
			// 即刻连接上服务端（如果不connect，即使在DataProgram中设置了远程id和地址则服务端MINA也收不到，跟普通的服
			// 务端UDP貌似不太一样，普通UDP时客户端无需先connect可以直接send设置好远程ip和端口的DatagramPackage）
			ds.connect(getByName(getServerIP()), getServerUDPPort());
		} catch (Exception e) {
			Log.w(TAG, "【IMCORE】send时出错，原因是：" + e.getMessage(), e);
			return BAD_CONNECT_TO_SERVER;
		}
		return UDPUtils.send(ds, fullProtocolBytes, dataLen) ? COMMON_CODE_OK : COMMON_DATA_SEND_FAILED;
	}

	static Single<Integer> sendCommonDataAsync(byte[] dataContent, int dataLen, int to_user_id)
	{
		return sendCommonDataAsync(getString(dataContent, dataLen), to_user_id);
	}

	static Single<Integer> sendCommonDataAsync(String dataContentWidthStr, int to_user_id,
											   boolean QoS)
	{
		return sendCommonDataAsync(dataContentWidthStr, to_user_id, QoS, null);
	}

	static Single<Integer> sendCommonDataAsync(String dataContentWidthStr, int to_user_id,
											   boolean QoS, String fingerPrint)
	{
		return sendCommonDataAsync(createCommonData
				(dataContentWidthStr, getCurrentUserId(), to_user_id, QoS, fingerPrint));
	}

	static Single<Integer> sendCommonDataAsync(String dataContentWidthStr, int to_user_id)
	{
		return sendCommonDataAsync(createCommonData
				(dataContentWidthStr, getCurrentUserId(), to_user_id));
	}

	static Single<Integer> sendCommonDataAsync(@NonNull Protocol p) {
		return just(p)
				.subscribeOn(io())
				.map(LocalUDPDataSender::sendCommonData)
				.observeOn(mainThread());
	}

	static void sendReceivedBack(final Protocol pFromServer)
	{
		if (pFromServer.getFp() == null) {
			Log.w(TAG, "【IMCORE】【QoS】收到"+pFromServer.getFrom()
					 + "发过来需要QoS的包，但它的指纹码却为null！无法发应答包！");
		} else {
			just(createReceivedBack
					(pFromServer.getTo(), pFromServer.getFrom(), pFromServer.getFp()))
					.subscribeOn(io())
					.map(LocalUDPDataSender::sendCommonData)
					.subscribe(code -> {
						if (ClientCoreSDK.DEBUG)
							Log.d(TAG, "【IMCORE】【QoS】向"+pFromServer.getFrom() +"发送"+
									pFromServer.getFp()+"包的应答包成功,from="+pFromServer.getTo()+"！");
					});
		}
	}
}