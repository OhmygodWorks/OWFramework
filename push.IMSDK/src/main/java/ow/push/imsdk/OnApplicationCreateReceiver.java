package ow.push.imsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.android.event.ChatBaseEvent;
import net.openmob.mobileimsdk.android.event.ChatBaseEvent.SimpleChatBaseEvent;
import net.openmob.mobileimsdk.android.event.ChatTransDataEvent;
import net.openmob.mobileimsdk.android.event.ChatTransDataEvent.SimpleChatTransDataEvent;
import net.openmob.mobileimsdk.android.event.MessageQoSEvent;
import net.openmob.mobileimsdk.android.event.MessageQoSEvent.SimpleMessageQoSEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import ow.CurrentUser;
import ow.CurrentUser.SignEventSubscriber;

import static net.openmob.mobileimsdk.android.ClientCoreSDK.init;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.logout;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setChatBaseEvent;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setChatTransDataEvent;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.setMessageQoSEvent;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.SenseMode.MODE_3S;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getAppKey;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getLocalUDPPort;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getServerIP;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.getServerUDPPort;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setAppKey;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setLocalUDPPort;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setSenseMode;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setServerIP;
import static net.openmob.mobileimsdk.android.conf.ConfigEntity.setServerUDPPort;
import static ow.framework.ApplicationModule.provideAppMetaData;
import static ow.framework.ApplicationModule.provideAppPreference;
import static ow.framework.ApplicationModule.provideLastSignedAccount;
import static ow.framework.ApplicationModule.provideLastValidPassword;

public final class OnApplicationCreateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // ConfigEntity
        setAppKey(get(IMSDK_APP_KEY, getAppKey()));
        setLocalUDPPort(get(IMSDK_LOCAL_PORT, getLocalUDPPort()));
        setServerUDPPort(get(IMSDK_SERVER_PORT, getServerUDPPort()));
        setServerIP(get(IMSDK_SERVER_HOST, getServerIP()));
        setSenseMode(MODE_3S);
        // ClientCoreSDK
        setChatBaseEvent(DEFAULT_BASE_EVENT);
        setChatTransDataEvent(DEFAULT_DATA_EVENT);
        setMessageQoSEvent(DEFAULT_QoS_EVENT);
        init(context);
        // 开始监听用户登录消息
        EventBus eventBus = EventBus.getDefault();
        if(!eventBus.isRegistered(subscriber)) {
            eventBus.register(subscriber);
        }
    }

    /** 写在AndroidManifest的meta-data里面，方便实际项目根据需要重写 */
    public static final String IMSDK_APP_KEY = "IMSDK_APP_KEY";
    /** 写在AndroidManifest的meta-data里面，方便实际项目根据需要重写 */
    public static final String IMSDK_LOCAL_PORT = "IMSDK_LOCAL_PORT";
    /** 写在AndroidManifest的meta-data里面，方便实际项目根据需要重写 */
    public static final String IMSDK_SERVER_PORT = "IMSDK_SERVER_PORT";
    /** 写在AndroidManifest的meta-data里面，方便实际项目根据需要重写 */
    public static final String IMSDK_SERVER_HOST = "IMSDK_SERVER_HOST";

    private static String get(String key, String defaultValue) {
        return provideAppPreference()
                .getString(key, provideAppMetaData().getString(key, defaultValue));
    }
    private static int get(String key, int defaultValue) {
        return provideAppPreference()
                .getInt(key, provideAppMetaData().getInt(key, defaultValue));
    }

    private static final ChatBaseEvent DEFAULT_BASE_EVENT = new SimpleChatBaseEvent() {
    };
    private static final ChatTransDataEvent DEFAULT_DATA_EVENT = new SimpleChatTransDataEvent() {
        @Override
        public void onTransBuffer(String fingerPrint, int fromUserID, String messageContent) {
            EventBus.getDefault().post(new PushMessage(fingerPrint, fromUserID, messageContent));
        }
    };
    private static final MessageQoSEvent DEFAULT_QoS_EVENT = new SimpleMessageQoSEvent() {
    };

    private static final SignEventSubscriber DEFAULT_SUBSCRIBER = new SignEventSubscriber() {
        @Override @Subscribe
        public void onEvent(@NonNull CurrentUser currentUser) {
            if (currentUser.isGuest()) {
                logout();
            } else {
                login();
            }
        }
        private void login() {
            String name = provideLastSignedAccount();
            String pass = provideLastValidPassword();
            ClientCoreSDK.login(name, pass);
        }
    };
    @NonNull
    private static SignEventSubscriber subscriber = DEFAULT_SUBSCRIBER;

    /**
     * 定制项目的登录或注销的动作。
     * @param customSubscriber 传null会重置成内部默认的操作。
     */
    @SuppressWarnings("unused")
    public static void overrideSubscriber(SignEventSubscriber customSubscriber) {
        // 限制不能为null。传null的时候重置成内部默认的操作
        if (customSubscriber == null) customSubscriber = DEFAULT_SUBSCRIBER;
        //
        EventBus eventBus = EventBus.getDefault();
        // 如果不同
        if (customSubscriber != subscriber) {
            // 就首先注销旧的
            if (eventBus.isRegistered(subscriber)) {
                eventBus.unregister(subscriber);
            }
            // 然后注册新的
            subscriber = customSubscriber;
        }
        // 如果相同，就补注册
        if(!eventBus.isRegistered(subscriber)) {
            eventBus.register(subscriber);
        }
    }
}
