/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ErrorCode.java at 2016-2-20 11:26:02, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.server.protocol;

public interface ErrorCode
{
	int COMMON_CODE_OK = 0;
	int COMMON_NO_LOGIN = 1;
	int COMMON_UNKNOWN_ERROR = 2;
	int COMMON_DATA_SEND_FAILED = 3;
	int COMMON_INVALID_PROTOCOL = 4;

	interface ForC
	{
		int BROKEN_CONNECT_TO_SERVER = 201;
		int BAD_CONNECT_TO_SERVER = 202;
		int CLIENT_SDK_NO_INITIALED = 203;
		int LOCAL_NETWORK_NOT_WORKING = 204;
		int TO_SERVER_NET_INFO_NOT_SETUP = 205;
	}
/*/
	interface ForS
	{
		int RESPONSE_FOR_UN_LOGIN = 301;
	}
//*/
}