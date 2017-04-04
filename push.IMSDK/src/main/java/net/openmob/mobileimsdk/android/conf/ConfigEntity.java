/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ConfigEntity.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.conf;

import android.support.annotation.NonNull;

import net.openmob.mobileimsdk.android.core.KeepAliveDaemon;
import net.openmob.mobileimsdk.android.utils.UDPUtils;

/** 服务器设置 */
public final class ConfigEntity
{
	private ConfigEntity() {
		//  for util class
	}

	/** 默认的服务器的IP或域名 */
	@SuppressWarnings("SpellCheckingInspection")
	private static final String DEFAULT_SERVER_IP = "rbcore.openmob.net";
	/** 默认的服务器的UDP端口号 */
	private static final int DEFAULT_SERVER_UDP_PORT = 7901;
	/** 默认的安卓手机本地的UDP端口号。0代表用系统自动分配的 */
	private static final int DEFAULT_LOCAL_UDP_PORT = 0;

	/** 推送系都爱用的appKey，用来区分同一个IP端口下的哪一个app的用户 */
	private static String appKey = "";
	/** 指定连接的服务器的IP或域名 */
	private static String serverIP = DEFAULT_SERVER_IP;
	/** 指定连接的服务器的UDP端口号 */
	private static int serverUDPPort = DEFAULT_SERVER_UDP_PORT;
	/** 安卓手机本地的UDP端口号。0代表用系统自动分配的 */
	private static int localUDPPort = DEFAULT_LOCAL_UDP_PORT;

	@NonNull
	public static String getAppKey() {
		return appKey;
	}

	public static void setAppKey(@NonNull String appKey) {
		ConfigEntity.appKey = appKey;
	}

	@NonNull
	public static String getServerIP() {
		return serverIP;
	}

	public static void setServerIP(@NonNull String serverIP) {
		ConfigEntity.serverIP = serverIP;
	}

	/** @return 当前指定的服务器端UDP端口号 */
	public static int getServerUDPPort() {
		return serverUDPPort;
	}

	/**
	 * 指定服务器端的UDP端口号，有效范围在256～65535之间。
	 * 不在有效范围内的话会被设置成{@link #DEFAULT_SERVER_UDP_PORT}。
	 * @param serverUDPPort 服务器端的UDP端口号。
	 */
	public static void setServerUDPPort(int serverUDPPort) {
		ConfigEntity.serverUDPPort =
				UDPUtils.isValidUDPPort(serverUDPPort)?serverUDPPort:DEFAULT_SERVER_UDP_PORT;
	}

	/** @return 当前指定的本地UDP端口号 */
	public static int getLocalUDPPort() {
		return localUDPPort;
	}

	/**
	 * 指定本地UDP端口号，有效范围在256～65535之间。
	 * 不在有效范围内的话会被设置成{@link #DEFAULT_LOCAL_UDP_PORT}。
	 * @param localUDPPort 本地UDP端口号。
	 */
	public static void setLocalUDPPort(int localUDPPort) {
		ConfigEntity.localUDPPort =
				UDPUtils.isValidUDPPort(localUDPPort)?localUDPPort:DEFAULT_LOCAL_UDP_PORT;
	}

	/**
	 * 设置心跳间隔
	 * @param mode 用来指定心跳间隔。见{@link SenseMode}
	 */
	public static void setSenseMode(SenseMode mode)
	{
		int keepAliveInterval = 0;
		int networkConnectionTimeout = 0;
		switch (mode)
		{
			case MODE_3S:
				// 心跳间隔3秒
				keepAliveInterval = 3000;// 3s
				// 10秒后未收到服务端心跳反馈即认为连接已断开（相当于连续3 个心跳间隔后仍未收到服务端反馈）
				networkConnectionTimeout = 3000 * 3 + 1000;// 10s
				break;
			case MODE_10S:
				// 心跳间隔10秒
				keepAliveInterval = 10000;// 10s
				// 10秒后未收到服务端心跳反馈即认为连接已断开（相当于连续2 个心跳间隔后仍未收到服务端反馈）
				networkConnectionTimeout = 10000 * 2 + 1000;// 21s
				break;
			case MODE_30S:
				// 心跳间隔30秒
				keepAliveInterval = 30000;// 30s
				// 10秒后未收到服务端心跳反馈即认为连接已断开（相当于连续2 个心跳间隔后仍未收到服务端反馈）
				networkConnectionTimeout = 30000 * 2 + 1000;// 61s
				break;
			case MODE_60S:
				// 心跳间隔60秒
				keepAliveInterval = 60000;// 60s
				// 10秒后未收到服务端心跳反馈即认为连接已断开（相当于连续2 个心跳间隔后仍未收到服务端反馈）
				networkConnectionTimeout = 60000 * 2 + 1000;// 121s
				break;
			case MODE_120S:
				// 心跳间隔120秒
				keepAliveInterval = 120000;// 120s
				// 10秒后未收到服务端心跳反馈即认为连接已断开（相当于连续2 个心跳间隔后仍未收到服务端反馈）
				networkConnectionTimeout = 120000 * 2 + 1000;// 241s
				break;
		}
		//noinspection ConstantConditions
		if(keepAliveInterval > 0)
		{
			// 设置Keep alive心跳间隔
			KeepAliveDaemon.KEEP_ALIVE_INTERVAL = keepAliveInterval;
		}
		//noinspection ConstantConditions
		if(networkConnectionTimeout > 0)
		{
			// 设置与服务端掉线的超时时长
			KeepAliveDaemon.NETWORK_CONNECTION_TIME_OUT = networkConnectionTimeout;
		}
	}
	/** 心跳间隔，越短心跳的越快 */
	public enum SenseMode
	{
		/** 心跳间隔3秒 */
		MODE_3S,
		/** 心跳间隔10秒 */
		MODE_10S,
		/** 心跳间隔30秒 */
		MODE_30S,
		/** 心跳间隔60秒 */
		MODE_60S,
		/** 心跳间隔120秒 */
		MODE_120S,
	}
}