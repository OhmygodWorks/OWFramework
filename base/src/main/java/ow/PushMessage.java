package ow;

/**
 * 推送消息公用的接口。
 * 百度，个推，IMSDK之类的具体实现这个接口并相应的增加所需要的内容。
 *
 * Created by ohmygod on 17/4/4.
 */

public interface PushMessage {
    /** @return 收到的原始消息内容 */
    String getRawMessageContent();
}
