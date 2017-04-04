/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * AutoReLoginDaemon.java at 2016-2-20 11:25:50, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package net.openmob.mobileimsdk.android.core;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import static android.os.SystemClock.elapsedRealtime;
import static io.reactivex.Observable.interval;
import static io.reactivex.Single.never;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.DEBUG;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.autoReLogin;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getCurrentLoginExtra;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getCurrentLoginName;
import static net.openmob.mobileimsdk.android.ClientCoreSDK.getCurrentLoginPsw;
import static net.openmob.mobileimsdk.android.core.LocalUDPDataSender.sendLogin;

/**
 * 后台自动重登录
 */
public final class AutoReLoginDaemon {
    private static final String TAG = AutoReLoginDaemon.class.getSimpleName();
    private static final int MIN_AUTO_RE$LOGIN_INTERVAL = 3000;

    private static int AUTO_RE$LOGIN_INTERVAL = MIN_AUTO_RE$LOGIN_INTERVAL;
    private static long latestExecuted = elapsedRealtime();
    private static boolean _executing = false;
    @NonNull
    private static Disposable disposable = never().subscribe();
    static {
        disposable.dispose();
    }

    private AutoReLoginDaemon() {}

    @WorkerThread
    private static int bgAutoReLogin() {
        _executing = true;
        latestExecuted = elapsedRealtime();
        if (DEBUG)
            Log.d(TAG, "【IMCORE】自动重新登陆线程执行中, autoReLogin?" + autoReLogin + "...");
        int code = -1;
        //
        if (autoReLogin) {
            code = sendLogin(
                    getCurrentLoginName(),
                    getCurrentLoginPsw(),
                    getCurrentLoginExtra());
        }
        return code;
    }

    private static void fgAutoReLogin(int result) {
        if (result == 0) {
            // *********************** 同样的代码也存在于LocalUDPDataSender.SendLoginDataAsync中的代码
            // 登陆消息成功发出后就启动本地消息侦听线程：
            // 第1）种情况：首次使用程序时，登陆信息发出时才启动本地监听线程是合理的；
            // 第2）种情况：因网络原因（比如服务器关闭或重启）而导致本地监听线程中断的问题：
            //     当首次登陆后，因服务端或其它网络原因导致本地监听出错，将导致中断本地监听线程，
            //     所以在此处在自动登陆重连或用户自已手机尝试再次登陆时重启监听线程就可以恢复本地监听线程的运行。
            LocalUDPDataReceiver.startup();
        }
        //
        latestExecuted = elapsedRealtime();
        _executing = false;
    }

    /** 停止定期的自动登录 */
    public static synchronized void stop() {
        disposable.dispose();
    }

    private static synchronized void start(long initialDelay) {
        stop();
        disposable = interval(initialDelay, AUTO_RE$LOGIN_INTERVAL, MILLISECONDS, io())
                .filter(not_executing)
                .map(bgAutoReLogin)
                .observeOn(mainThread())
                .subscribe(fgAutoReLogin);
    }

    private static final Predicate<Long> not_executing = new Predicate<Long>() {
        @Override
        public boolean test(Long time) throws Exception {
            return !_executing;
        }
    };
    private static final Function<Long, Integer> bgAutoReLogin = new Function<Long, Integer>() {
        @Override
        public Integer apply(Long time) throws Exception {
            return bgAutoReLogin();
        }
    };
    private static final Consumer<Integer> fgAutoReLogin = new Consumer<Integer>() {
        @Override
        public void accept(Integer result) throws Exception {
            fgAutoReLogin(result);
        }
    };

    /**
     * 停止并重新开始定期自动登录
     * @param immediately 是否立即开始自动登录
     */
    static synchronized void start(boolean immediately) {
        start(immediately ? 0 : AUTO_RE$LOGIN_INTERVAL);
    }

    /**
     * 更新定期登录的间隔时间然后按照新的时间间隔重启自动登录
     * @param millisecond 新的间隔时间，有保证不会低于{@link #MIN_AUTO_RE$LOGIN_INTERVAL 3秒}
     */
    public static synchronized void setInterval(int millisecond) {
        AUTO_RE$LOGIN_INTERVAL = max(millisecond, MIN_AUTO_RE$LOGIN_INTERVAL);
        if (!isAutoReLoginRunning()) return;
        // 检查上次运行到现在所经过的时间是否已经超过本次设定的间隔时间
        long interval = elapsedRealtime() - latestExecuted;
        interval -= AUTO_RE$LOGIN_INTERVAL;
        // 如果上次运行到现在已经够了新设置的间隔时间的话就立刻按新的间隔时间启动
        start(max(interval, 0));
    }

    /** @return 目前是否正在运行自动登录 */
    public static boolean isAutoReLoginRunning() {
        return !disposable.isDisposed();
    }
}