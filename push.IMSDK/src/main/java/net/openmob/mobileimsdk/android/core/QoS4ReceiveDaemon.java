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

import android.support.annotation.NonNull;
import android.util.Log;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.server.protocol.Protocol;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import static io.reactivex.Flowable.fromIterable;
import static io.reactivex.Flowable.interval;
import static io.reactivex.schedulers.Schedulers.computation;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class QoS4ReceiveDaemon
{
	private static boolean _executing = false;
	@NonNull
	private static Disposable disposable = Maybe.empty().subscribe();

	private static final String TAG = QoS4ReceiveDaemon.class.getSimpleName();
	private static final int CHECK_INTERVAL = 300000;
	private static final int MESSAGES_VALID_TIME = 600000;
	private static final Map<String, Long> receivedMessages = new ConcurrentHashMap<>();

	private QoS4ReceiveDaemon() {}

	private static void startCheck(boolean immediately)
	{
		disposable = interval(immediately ? 0 : CHECK_INTERVAL,
				CHECK_INTERVAL, MILLISECONDS, computation())
				// 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
				// 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
				.filter(not_executing)
				.flatMap(checkImpl)
				.subscribe();
	}

	private static final Predicate<Long> not_executing = new Predicate<Long>() {
		@Override
		public boolean test(Long now) throws Exception {
			return !_executing;
		}
	};
	private static final Function<Long, Publisher<String>> checkImpl = new Function<Long, Publisher<String>>() {
		@Override
		public Publisher<String> apply(Long ignore) throws Exception {
			return checkImpl();
		}
	};

	private static Flowable<String> checkImpl() {
		return fromIterable(receivedMessages.keySet())
				.doOnSubscribe(startExecuting)
				.filter(allMessagesThatExceedValidTime)
				.doOnNext(receivedMessages_remove)
				.doFinally(finishExecuting);
	}

	private static final Consumer<Subscription> startExecuting = new Consumer<Subscription>() {
		@Override
		public void accept(Subscription ignore) throws Exception {
			startExecuting();
		}
	};
	private static final Predicate<String> allMessagesThatExceedValidTime = new Predicate<String>() {
		@Override
		public boolean test(String key) throws Exception {
			return allMessagesThatExceedValidTime(key);
		}
	};
	private static final Consumer<String> receivedMessages_remove = new Consumer<String>() {
		@Override
		public void accept(String key) throws Exception {
			receivedMessages.remove(key);
		}
	};
	private static final Action finishExecuting = new Action() {
		@Override
		public void run() throws Exception {
			finishExecuting();
		}
	};

	private static void startExecuting() {
		_executing = true;
		if (ClientCoreSDK.DEBUG) {
			Log.d(TAG, "【IMCORE】【QoS接收方】++++++++++ START " +
					"暂存处理线程正在运行中，当前长度" + currentSize() + ".");
		}
	}

	private static boolean allMessagesThatExceedValidTime(String key) {
		long delta = System.currentTimeMillis() - receivedMessages.get(key);
		// 检查每条消息是否已经保留够久
		boolean isExceed = delta >= MESSAGES_VALID_TIME;
		if (isExceed && ClientCoreSDK.DEBUG)
			Log.d(TAG, "【IMCORE】【QoS接收方】指纹为" + key + "的包已生存" + delta
					+ "ms(最大允许" + MESSAGES_VALID_TIME + "ms), 马上将删除之.");
		return isExceed;
	}

	private static void finishExecuting() {
		if (ClientCoreSDK.DEBUG) {
			Log.d(TAG, "【IMCORE】【QoS接收方】++++++++++ END " +
					"暂存处理线程正在运行中，当前长度" + currentSize() + ".");
		}
		_executing = false;
	}

	private static final Consumer<Subscription> stop = new Consumer<Subscription>() {
		@Override
		public void accept(Subscription s) throws Exception {
			stop();
		}
	};
	private static final Consumer<String> updateTimestamp = new Consumer<String>() {
		@Override
		public void accept(String fingerPrintOfProtocol) throws Exception {
			updateTimestamp(fingerPrintOfProtocol);
		}
	};

	static void startup(final boolean immediately)
	{
		Flowable.fromIterable(receivedMessages.keySet())
				.subscribeOn(computation())
				.doOnSubscribe(stop)
				.doOnNext(updateTimestamp)
				.doOnComplete(new Action() {
					@Override
					public void run() throws Exception {
						startCheck(immediately);
					}
				})
				.subscribe();
	}

	public static void stop()
	{
		disposable.dispose();
	}

	public static boolean isRunning()
	{
		return !disposable.isDisposed();
	}

	static void addReceived(Protocol p)
	{
		if ((p != null) && (p.isQoS()))
			addReceived(p.getFp());
	}

	private static void addReceived(String fingerPrintOfProtocol)
	{
		if (fingerPrintOfProtocol == null)
		{
			Log.w(TAG, "【IMCORE】无效的 fingerPrintOfProtocol==null!");
			return;
		}
		if (receivedMessages.containsKey(fingerPrintOfProtocol)) {
			Log.w(TAG, "【IMCORE】【QoS接收方】指纹为" + fingerPrintOfProtocol +
					"的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
		}
		updateTimestamp(fingerPrintOfProtocol);
	}

	private static void updateTimestamp(String fingerPrintOfProtocol)
	{
		if (fingerPrintOfProtocol != null)
			receivedMessages.put(fingerPrintOfProtocol, System.currentTimeMillis());
	}

	static boolean hasReceived(String fingerPrintOfProtocol)
	{
		return receivedMessages.containsKey(fingerPrintOfProtocol);
	}

	private static int currentSize()
	{
		return receivedMessages.size();
	}
}