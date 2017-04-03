/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ChatBaseEvent.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.event;

import android.util.Log;

import net.openmob.mobileimsdk.server.protocol.ErrorCode;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;

public interface ChatBaseEvent {
    /**
     * 通知登录结果的回调
     *
     * @param userID    服务端返回的用户ID
     * @param errorCode 服务端返回的错误码。@see{@link ErrorCode}
     */
    void onLoginMessage(int userID, int errorCode);

    /**
     * 通知网络连接断开的回调
     *
     * @param code 错误码
     */
    void onLinkCloseMessage(int code);

    /**
     * 简化实现
     */
    abstract class SimpleChatBaseEvent implements ChatBaseEvent {
        @Override
        public void onLoginMessage(int userID, int errorCode) {
            if (DEBUG) Log.i("", "onLoginMessage" + " userID:" + userID + " code:" + errorCode);
        }
        @Override
        public void onLinkCloseMessage(int code) {
            if (DEBUG) Log.i("", "onLinkCloseMessage" + " code:" + code);
        }
    }
}