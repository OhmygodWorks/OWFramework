package ow.android.app;

import android.content.Context;

/**
 * Created by ohmygod on 17/3/31.
 */

public class Application extends android.app.Application {
    /**  */
    public static final String INTENT_APPLICATION_CREATE = ".OnApplicationCreate";

    /**
     * for {@link android.app.Application} this is called right after instantiate and right before
     * {@link android.app.Application#onCreate()} is called.
     * I count on it for the best place a library module could use for init application singleton,
     * even better than convenient onCreate() method, because its a little early timing,
     * it could actually prepare something that can be used in the convenient onCreate() method.
     * And so I set it final to protect the timing gimmick.
     *
     * @param base the {@link Context} provided by android system.
     */
    @Override
    protected final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ApplicationModule.attachTo(this);
    }
}
