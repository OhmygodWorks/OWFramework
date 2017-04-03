/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ProtocolFactory.java at 2016-2-20 11:26:02, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.server.protocol;

import com.alibaba.fastjson.JSON;

import net.openmob.mobileimsdk.server.protocol.c.PKeepAlive;
import net.openmob.mobileimsdk.server.protocol.c.PLoginInfo;
import net.openmob.mobileimsdk.server.protocol.s.PErrorResponse;
import net.openmob.mobileimsdk.server.protocol.s.PKeepAliveResponse;
import net.openmob.mobileimsdk.server.protocol.s.PLoginInfoResponse;

import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_LOGIN;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_LOGOUT;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.C.FROM_CLIENT_TYPE_OF_RECEIVED;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE;
import static net.openmob.mobileimsdk.server.protocol.ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN;

@SuppressWarnings("unused")
public final class ProtocolFactory
{
	private ProtocolFactory() {
		// for util class
	}

	private static String create(Object c)
	{
		return JSON.toJSONString(c);
	}

	private static <T> T parse(byte[] fullProtocolJSONBytes, int len, Class<T> clazz)
	{
		return parse(CharsetHelper.getString(fullProtocolJSONBytes, len), clazz);
	}

	private static <T> T parse(String dataContentOfProtocol, Class<T> clazz)
	{
		return JSON.parseObject(dataContentOfProtocol, clazz);
	}

	public static Protocol parse(byte[] fullProtocolJSONBytes, int len)
	{
		return parse(fullProtocolJSONBytes, len, Protocol.class);
	}

	public static Protocol createPKeepAliveResponse(int to_user_id)
	{
		return new Protocol(FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE,
				create(new PKeepAliveResponse()), 0, to_user_id);
	}

	public static PKeepAliveResponse parsePKeepAliveResponse(String dataContentOfProtocol)
	{
		return parse(dataContentOfProtocol, PKeepAliveResponse.class);
	}

	public static Protocol createPKeepAlive(int from_user_id)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_KEEP$ALIVE,
				create(new PKeepAlive()), from_user_id, 0);
	}

	public static PKeepAlive parsePKeepAlive(String dataContentOfProtocol)
	{
		return parse(dataContentOfProtocol, PKeepAlive.class);
	}

	public static Protocol createPErrorResponse(int errorCode, String errorMsg, int user_id)
	{
		return new Protocol(FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR,
				create(new PErrorResponse(errorCode, errorMsg)), 0, user_id);
	}

	public static PErrorResponse parsePErrorResponse(String dataContentOfProtocol)
	{
		return parse(dataContentOfProtocol, PErrorResponse.class);
	}

	public static Protocol createPLogoutInfo(int user_id, String loginName)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_LOGOUT
//				, create(new PLogoutInfo(user_id, loginName))
				, null
				, user_id, 0);
	}

	public static Protocol createPLoginInfo(String loginName, String loginPsw, String extra)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_LOGIN
				, create(new PLoginInfo(loginName, loginPsw, extra)), -1, 0);
	}

	public static PLoginInfo parsePLoginInfo(String dataContentOfProtocol)
	{
		return parse(dataContentOfProtocol, PLoginInfo.class);
	}

	public static Protocol createPLoginInfoResponse(int code, int user_id)
	{
		return new Protocol(FROM_SERVER_TYPE_OF_RESPONSE$LOGIN,
				create(new PLoginInfoResponse(code, user_id)),
				0, user_id, true, Protocol.genFingerPrint());
	}

	public static PLoginInfoResponse parsePLoginInfoResponse(String dataContentOfProtocol)
	{
		return parse(dataContentOfProtocol, PLoginInfoResponse.class);
	}

	public static Protocol createCommonData(String dataContent, int from_user_id, int to_user_id,
											boolean QoS, String fingerPrint)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_COMMON$DATA,
				dataContent, from_user_id, to_user_id, QoS, fingerPrint);
	}

	public static Protocol createCommonData(String dataContent, int from_user_id, int to_user_id)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_COMMON$DATA,
				dataContent, from_user_id, to_user_id);
	}

	public static Protocol createReceivedBack(int from_user_id, int to_user_id,
											  String receivedMessageFingerPrint)
	{
		return new Protocol(FROM_CLIENT_TYPE_OF_RECEIVED,
				receivedMessageFingerPrint, from_user_id, to_user_id);// 该包当然不需要QoS支持！
	}
}