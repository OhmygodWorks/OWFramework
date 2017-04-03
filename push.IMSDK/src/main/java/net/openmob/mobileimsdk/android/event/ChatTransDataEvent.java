/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ChatTransDataEvent.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.event;

import android.util.Log;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;

public interface ChatTransDataEvent {
    /**
     * 收到服务器发过来的消息
     *
     * @param fingerPrint    消息的指纹，用法相当于消息ID
     * @param fromUserID     发送消息的用户的ID
     * @param messageContent 消息内容
     */
    void onTransBuffer(String fingerPrint, int fromUserID, String messageContent);

    /**
     * 收到服务器发过来的错误
     *
     * @param errorCode    错误代码
     * @param errorMessage 错误描述
     */
    void onErrorResponse(int errorCode, String errorMessage);

    /**
     * 简化实现
     */
    abstract class SimpleChatTransDataEvent implements ChatTransDataEvent {
        @Override
        public void onTransBuffer(String fingerPrint, int fromUserID, String messageContent) {
            if (DEBUG) Log.i("", "onTransBuffer"
                    + " id:" + fingerPrint + " from:" + fromUserID + " content:" + messageContent);
        }
        @Override
        public void onErrorResponse(int errorCode, String errorMessage) {
            if (DEBUG) Log.w("", "onErrorResponse"
                    + " errorCode:" + errorCode + " errorMessage:" + errorMessage);
        }
    }
}