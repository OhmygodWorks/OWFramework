package ow.android.app;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.app.Application;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import dagger.Module;

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
@Module @SuppressWarnings("WeakerAccess")
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

    /** cached {@link Application} singleton instance, for provides some convenient instance. */
    @NonNull
    private static Reference<Application> singleton = new SoftReference<>(null);
    /** just log tag */
    private static final String TAG = "ow.android.app";
}
