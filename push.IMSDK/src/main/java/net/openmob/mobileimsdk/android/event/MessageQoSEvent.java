/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * MessageQoSEvent.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.event;

import android.util.Log;

import net.openmob.mobileimsdk.server.protocol.Protocol;

import java.util.Collection;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;

public interface MessageQoSEvent {
    /**
     * 有消息没发送成功的时候的回调
     *
     * @param lostMessages 没发送成功的消息
     */
    void messagesLost(Collection<Protocol> lostMessages);

    /**
     * 确认指定消息已送达服务器的时候的回调
     *
     * @param messageFingerPrint 已送达的消息的指纹，相当于消息的ID
     */
    void messagesBeReceived(String messageFingerPrint);

    /**
     * 简化实现
     */
    abstract class SimpleMessageQoSEvent implements MessageQoSEvent {
        @Override
        public void messagesLost(Collection<Protocol> lostMessages) {
            if (DEBUG) Log.w("", "messagesLost" + lostMessages);
        }
        @Override
        public void messagesBeReceived(String messageFingerPrint) {
            if (DEBUG) Log.d("", "messagesBeReceived" + " id:" + messageFingerPrint);
        }
    }
}