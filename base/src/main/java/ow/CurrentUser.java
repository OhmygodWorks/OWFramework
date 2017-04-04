package ow;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Singleton;

/**
 * 投放到{@link EventBus}里面标明用户登录或注销的接口。
 *
 * Created by ohmygod on 17/4/4.
 */

public interface CurrentUser {
    /**
     * 判断当前用户是否未登录
     * @return false：当前用户是正常登录用户。true：当前用户未登录，是GUEST。
     */
    boolean isGuest();

    /**
     * @return 当前登录用户的UserID
     */
    int getUserID();

    /**
     * 未登录用户（GUEST）的UserID
     */
    int GUEST_USER_ID = 0;

    /**
     * 未登录用户（GUEST）的singleton
     */
    @Singleton
    CurrentUser GUEST = new CurrentUser() {
        @Override
        public boolean isGuest() {
            return true;
        }
        @Override
        public int getUserID() {
            return GUEST_USER_ID;
        }
    };

    /**
     * 针对{@link EventBus}使用的监听用户登录和注销事件的接口。
     */
    interface SignEventSubscriber {
        /**
         * 具体实现接收到用户登录和注销事件时候的处理。
         *
         * @param currentUser 通过{@link CurrentUser#isGuest()}来判断是登录还是注销。
         */
        @Subscribe
        void onEvent(@NonNull final CurrentUser currentUser);
    }
}
