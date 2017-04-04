package ow.framework;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ow.implementation.MemorySharedPreference;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.os.Bundle.EMPTY;
import static ow.CurrentUser.GUEST_USER_ID;
import static ow.android.app.Application.APPLICATION_CREATE_ACTION;

/**
 * a util class as Dagger {@link Module} to provide some application scoped singleton dependencies.
 * </br>
 * @see ow.android.app.Application#attachBaseContext(Context) for usecase.
 * </br>
 * for easy to use with Dagger, I keep this class non-final & instantiable.
 * </br>
 *
 * Created by ohmygod on 17/3/31.
 */
@Module
public class ApplicationModule {
    /**
     * init this module.
     * @see ow.android.app.Application#attachBaseContext(Context) for usecase.
     * @param app the {@link Application} in app module that needs attach.
     */
    public static final void attachTo(@NonNull Application app) {
        // current instance. maybe null.
        Application instance = singleton.get();
        if (instance == null) {
            // normal case : first time init.
            singleton = new SoftReference<>(app);
            Log.i(TAG, "attachTo("+app+")");
            //
            setupPreference(app);
            // send broadcast when app is ready in using, to trigger some library to init their code.
            app.sendBroadcast(new Intent(app.getPackageName()+ APPLICATION_CREATE_ACTION));
            //
        } else if (instance.equals(app)) {
            // safety case : duplicated call in Activity if forget
            Log.v(TAG, "already attached to "+app);
            //
        } else {
            // warning case : another application instantiated for extra process,
            // often started by extra push library.
            Log.w(TAG, "attachTo("+app+")",
                    new IllegalStateException("already attached to "+instance));
        }
    }

    @Provides @Singleton
    public static Bundle provideAppMetaData() {
        Application app = singleton.get();
        if (app == null) {
            return EMPTY;
        } else try {
            return app.getPackageManager()
                    .getApplicationInfo(app.getPackageName(), GET_META_DATA)
                    .metaData;
        } catch (NameNotFoundException e) {
            return EMPTY;
        }
    }

    /** @return 整个应用共享的设置 */
    @Provides @Singleton
    public static SharedPreferences provideAppPreference() {
        return applicationPreference;
    }

    /**
     * @return 单个用户的设置。
     * 当{@link #applicationPreference}的{@link #CURRENT_USER}改变时会返回不同的实例。
     */
    @Provides @Named(CURRENT_USER)
    public static SharedPreferences provideUserPreference() {
        return userPreference;
    }

    /** @return 缓存的帐号，用来在用户注销或登录失败之前做自动登录 */
    @Provides @Named(LAST_SIGNED_ACCOUNT)
    public static String provideLastSignedAccount() {
        return provideUserPreference().getString(LAST_SIGNED_ACCOUNT, "");
    }

    /** @return 缓存的帐号，用来在用户注销或登录失败之前做自动登录 */
    @Provides @Named(LAST_VALID_PASSWORD)
    public static String provideLastValidPassword() {
        return provideUserPreference().getString(LAST_VALID_PASSWORD, "");
    }

    /** 当前登录用户的UserID，int型，未登录的时候是{@link ow.CurrentUser#GUEST_USER_ID} */
    public static final String CURRENT_USER = "CurrentUserID";
    /** 登录成功后需要在{@link #userPreference}里面缓存的帐号，用来在用户注销或登录失败之前做自动登录 */
    public static final String LAST_SIGNED_ACCOUNT = "LastSignedAccount";
    /** 登录成功后需要在{@link #userPreference}里面缓存的密码，用来在用户注销或登录失败之前做自动登录 */
    public static final String LAST_VALID_PASSWORD = "LastValidPassword";

    private static void setupPreference(Application app) {
        SharedPreferences preferences = app.getSharedPreferences(app.getPackageName(), MODE_PRIVATE);
        if (applicationPreference instanceof MemorySharedPreference) {
            ((MemorySharedPreference) applicationPreference).attach(preferences);
        } else {
            applicationPreference = preferences;
        }
    }
    private final static OnSharedPreferenceChangeListener switchUserPreference
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switchUserPreference(sharedPreferences, key);
        }
    };
    private static void switchUserPreference(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences != applicationPreference || !CURRENT_USER.equals(key)) return;
        Application app = singleton.get();
        if (app == null) {
            if (!(userPreference instanceof MemorySharedPreference)) {
                userPreference = new MemorySharedPreference();
            }
            return;
        }
        int userID = sharedPreferences.getInt(CURRENT_USER, GUEST_USER_ID);
        if (userID != GUEST_USER_ID) {
            String name = app.getPackageName() + ".u" + userID;
            userPreference = app.getSharedPreferences(name, MODE_PRIVATE);
        } else if ((userPreference instanceof MemorySharedPreference)) {
            // do nothing. return;
        } else {
            userPreference = new MemorySharedPreference();
        }
    }
    @NonNull
    private static SharedPreferences applicationPreference = new MemorySharedPreference();
    @NonNull
    private static SharedPreferences userPreference = new MemorySharedPreference();

    /** cached {@link Application} singleton instance, for provides some convenient instance. */
    @NonNull
    private static Reference<Application> singleton = new SoftReference<>(null);
    /** just log tag */
    private static final String TAG = "ow.android.app";

    static {
        applicationPreference.registerOnSharedPreferenceChangeListener(switchUserPreference);
    }
}
