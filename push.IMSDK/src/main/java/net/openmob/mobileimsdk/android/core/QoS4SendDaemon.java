/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * QoS4SendDaemon.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.util.Log;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.server.protocol.Protocol;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;

import static io.reactivex.Flowable.fromIterable;
import static io.reactivex.Flowable.interval;
import static io.reactivex.Maybe.empty;
import static io.reactivex.Single.just;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getMessageQoSEvent;
import static net.openmob.mobileimsdk.android.core.LocalUDPDataSender.sendCommonDataAsync;
import static net.openmob.mobileimsdk.server.protocol.ErrorCode.COMMON_CODE_OK;

public final class QoS4SendDaemon
{
	private static final String TAG = QoS4SendDaemon.class.getSimpleName();

	// 并发Hash，因为本类中可能存在不同的线程同时remove或遍历之
	private static final Map<String, Protocol> sentMessages = new ConcurrentHashMap<>();
	// 关发Hash，因为本类中可能存在不同的线程同时remove或遍历之
	private static final Map<String, Long> sendMessagesTimestamp = new ConcurrentHashMap<>();

	private static final int CHECK_INTERVAL = 5000;
	private static final int MESSAGES_JUST$NOW_TIME = 3000;
	private static final int QOS_TRY_COUNT = 3;

	private static Disposable disposable = empty().subscribe();
	private static boolean _executing = false;

	private QoS4SendDaemon() {}

	private static void startExecuting(long ignored) {
		_executing = true;
		if (ClientCoreSDK.DEBUG) {
			Log.d(TAG, "【IMCORE】【QoS】=========== 消息发送质量保证线程运行中"
					 + ", 当前需要处理的列表长度为" + size() + "...");
		}
	}

	private static void stopExecuting(List<Protocol> lostMessages) {
		getMessageQoSEvent().messagesLost(lostMessages);
		_executing = false;
	}

	private static boolean lostMessage(Protocol p) {
		String key = p.getFp();
		if (!p.isQoS()) {
			remove(key);
		}
		else if (p.getRetryCount() < QOS_TRY_COUNT) {
			long delta = System.currentTimeMillis() - sendMessagesTimestamp.get(key);
			//
			if (delta > MESSAGES_JUST$NOW_TIME) {
				sendCommonDataAsync(p).subscribe(code -> {
					if (code == COMMON_CODE_OK) {
						p.increaseRetryCount();
						if (ClientCoreSDK.DEBUG)
							Log.d(TAG, "【IMCORE】【QoS】指纹为" + key +
									"的消息包已成功进行重传，此次之后重传次数已达" +
									p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
					} else {
						Log.w(TAG, "【IMCORE】【QoS】指纹为" + key +
								"的消息包重传失败，它的重传次数之前已累计为" +
								p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
					}
				});
			} else if (ClientCoreSDK.DEBUG) {
				Log.w(TAG, "【IMCORE】【QoS】指纹为" + key + "的包距\"刚刚\"发出才" + delta
						 + "ms(<=" + MESSAGES_JUST$NOW_TIME + "ms将被认定是\"刚刚\"), 本次不需要重传哦.");
			}
		} else {
			remove(key);
			if (ClientCoreSDK.DEBUG) {
				Log.d(TAG, "【IMCORE】【QoS】指纹为" + key + "的消息包重传次数已达"
						 + p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次)上限，将判定为丢包！");
			}
			return true;
		}
		return false;
	}

	private static Publisher<List<Protocol>> lostMessages(long ignored) {
		return fromIterable(sentMessages.keySet())
				.map(sentMessages::get)
				.filter(QoS4SendDaemon::lostMessage)
				.map(Protocol::clone)
				.toList().toFlowable();
	}

	static synchronized void startup(boolean immediately)
	{
		stop();
		disposable = interval(immediately ? 0 : CHECK_INTERVAL,
				CHECK_INTERVAL, TimeUnit.MILLISECONDS, computation())
				.filter(now -> !_executing)
				.doOnNext(QoS4SendDaemon::startExecuting)
				.flatMap(QoS4SendDaemon::lostMessages)
				.observeOn(mainThread())
				.doOnNext(QoS4SendDaemon::stopExecuting)
				.subscribe();
	}

	public static synchronized void stop()
	{
		disposable.dispose();
	}

	public static boolean isRunning()
	{
		return !disposable.isDisposed();
	}

	static boolean exist(String fingerPrint)
	{
		return sentMessages.get(fingerPrint) != null;
	}

	static void put(Protocol p)
	{
		if (p == null)
		{
			Log.w(TAG, "Invalid arg p==null.");
			return;
		}
		if (!p.isQoS())
		{
			Log.w(TAG, "This protocol is not QoS pkg, ignore it!");
			return;
		}
		String fp = p.getFp();
		if (fp == null)
		{
			Log.w(TAG, "Invalid arg p.getFp() == null.");
			return;
		}
		if (sentMessages.get(fp) != null) {
			Log.w(TAG, "【IMCORE】【QoS】指纹为" + fp + "的消息已经放入了发送质量保证队列，"
					 + "该消息为何会重复？（生成的指纹码重复？还是重复put？）");
		}
		// save it
		sentMessages.put(fp, p);
		// 同时保存时间戳
		sendMessagesTimestamp.put(fp, System.currentTimeMillis());
	}

	static void remove(final String fingerPrint)
	{
		just(fingerPrint)
				.subscribeOn(computation())
				.doOnSuccess(sendMessagesTimestamp::remove)
				.map(sentMessages::remove)
				.subscribe(result -> Log.w(TAG, "【IMCORE】【QoS】指纹为" + fingerPrint +
						"的消息已成功从发送质量保证队列中移除" +
						"(可能是收到接收方的应答也可能是达到了重传的次数上限)，重试次数=" +
						(result != null?result.getRetryCount():"none 呵呵.")));
	}

	private static int size()
	{
		return sentMessages.size();
	}
}