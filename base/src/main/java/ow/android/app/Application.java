package ow.android.app;

import android.content.BroadcastReceiver;
import android.content.Context;

/**
 * the application class used instead of {@link android.app.Application} for perform some init.
 * Other project should extend this class or else override the {@link #attachBaseContext(Context)}
 * method as this implementation.
 * </br>
 * This class implementation is already registered to the AndroidManifest.xml that will be merged
 * into the final AndroidManifest.xml, so it will automatically perform init action if no other
 * Application implementation overrides it.
 * </br>
 *
 * Created by ohmygod on 17/3/31.
 */

public class Application extends android.app.Application {
    /**
     * the intent action that broadcast on application create.
     * library module may register a {@link BroadcastReceiver} for this action to load and init
     * the library code as the follow code fragment.
     * <pre><code>
     *  <application>
         <receiver
             android:name=".YourApplicationCreateReceiver"
             android:enabled="true"
             android:exported="false">
             <intent-filter>
                <!-- Remember to prefix the action with `${applicationId}` -->
                <action android:name="${applicationId}.OnApplicationCreate"/>
             </intent-filter>
         </receiver>
        </application>
     * </code></pre>
     */
    public static final String APPLICATION_CREATE_ACTION = ".OnApplicationCreate";

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
