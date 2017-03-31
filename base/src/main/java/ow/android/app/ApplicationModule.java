package ow.android.app;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import dagger.Module;

import static ow.android.app.Application.INTENT_APPLICATION_CREATE;

/**
 * a util class as Dagger {@link Module} to provide some application scoped singleton dependencies.
 * @see Application#attachBaseContext(Context) for usecase.
 * for easy use with Dagger, I keep this class non-final & instantiable.
 *
 * Created by ohmygod on 17/3/31.
 */
@Module
public class ApplicationModule {
    /**
     * init this module.
     * @see Application#attachBaseContext(Context) for usecase.
     * @param app the {@link android.app.Application} in app module that needs attach.
     */
    public static final void attachTo(@NonNull android.app.Application app) {
        // check duplicated call
        if (singleton.equals(app)) {
            // safety path 1
            Log.d(TAG, "already attached to "+app);
            return;
        }
        final Context base = singleton.getBaseContext();
        if (app.equals(base)) {
            // safety path 2
            Log.d(TAG, "already attached to "+app);
            return;
        } else if (base != null) {
            // duplicate call
            Log.w(TAG, "attachTo("+app+")", new IllegalStateException("already attached to "+base));
            return;
        } else if (app instanceof Application) {
            // normal path 1
            singleton = (Application) app;
        } else {
            // normal path 2
            singleton.attachBaseContext(app);
        }
        Log.i(TAG, "attachTo("+app+")");
        // send broadcast when app is ready in using, to trigger some library to init their code.
        app.sendBroadcast(new Intent(app.getPackageName()+INTENT_APPLICATION_CREATE));
    }

    /** cached {@link Application} singleton instance, for provides some convenient instance. */
    @NonNull
    private static Application singleton = new Application();
    /** just log tag */
    private static final String TAG = "ow.android.app";
}
