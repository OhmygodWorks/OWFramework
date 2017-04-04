package ow.implementation;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static java.util.Collections.unmodifiableMap;

/**
 * 对{@link SharedPreferences}的一个实现，主要用来确保@NonNull。
 *
 * Created by ohmygod on 17/4/4.
 */

public final class MemorySharedPreference implements SharedPreferences {
    /**
     * Retrieve all values from the preferences.
     * <p>
     * <p>Note that you <em>must not</em> modify the collection returned
     * by this method, or alter any of its contents.  The consistency of your
     * stored data is not guaranteed if you do.
     *
     * @return Returns a map containing a list of pairs key/value representing
     * the preferences.
     */
    @Override
    public Map<String, ?> getAll() {
        return unmodifiableMap(map);
    }

    /**
     * Retrieve a set of String values from the preferences.
     * <p>
     * <p>Note that you <em>must not</em> modify the set instance returned
     * by this call.  The consistency of the stored data is not guaranteed
     * if you do, nor is your ability to modify the instance at all.
     *
     * @param key       The name of the preference to retrieve.
     * @param defValues Values to return if this preference does not exist.
     * @return Returns the preference values if they exist, or defValues.
     * @throws ClassCastException if there is a preference with this name
     * that is not a Set.
     */
    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (impl != null) {
            return impl.getStringSet(key, defValues);
        } else {
            Object value = map.get(key);
            //noinspection unchecked
            return value==null? defValues: (Set<String>) value;
        }
    }

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * a String.
     */
    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (impl != null) {
            return impl.getString(key, defValue);
        } else {
            Object value = map.get(key);
            return value==null? defValue: (String) value;
        }
    }

    /**
     * Retrieve an int value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * an int.
     */
    @Override
    public int getInt(String key, int defValue) {
        if (impl != null) {
            return impl.getInt(key, defValue);
        } else {
            Object value = map.get(key);
            return value==null? defValue: (Integer) value;
        }
    }

    /**
     * Retrieve a long value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * a long.
     */
    @Override
    public long getLong(String key, long defValue) {
        if (impl != null) {
            return impl.getLong(key, defValue);
        } else {
            Object value = map.get(key);
            return value==null? defValue: (Long) value;
        }
    }

    /**
     * Retrieve a float value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * a float.
     */
    @Override
    public float getFloat(String key, float defValue) {
        if (impl != null) {
            return impl.getFloat(key, defValue);
        } else {
            Object value = map.get(key);
            return value==null? defValue: (Float) value;
        }
    }

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * a boolean.
     */
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (impl != null) {
            return impl.getBoolean(key, defValue);
        } else {
            Object value = map.get(key);
            return value==null? defValue: (Boolean)value;
        }
    }

    /**
     * Checks whether the preferences contains a preference.
     *
     * @param key The name of the preference to check.
     * @return Returns true if the preference exists in the preferences,
     * otherwise false.
     */
    @Override
    public boolean contains(String key) {
        return impl!=null? impl.contains(key): map.containsKey(key);
    }

    /**
     * Create a new Editor for these preferences, through which you can make
     * modifications to the data in the preferences and atomically commit those
     * changes back to the SharedPreferences object.
     * <p>
     * <p>Note that you <em>must</em> call {@link Editor#commit} to have any
     * changes you perform in the Editor actually show up in the
     * SharedPreferences.
     *
     * @return Returns a new instance of the {@link Editor} interface, allowing
     * you to modify the values in this SharedPreferences object.
     */
    @Override
    public Editor edit() {
        if (impl != null) {
            return impl.edit();
        } else {
            return new EditorImpl();
        }
    }

    /**
     * Registers a callback to be invoked when a change happens to a preference.
     * <p>
     * <p class="caution"><strong>Caution:</strong> The preference manager does
     * not currently store a strong reference to the listener. You must store a
     * strong reference to the listener, or it will be susceptible to garbage
     * collection. We recommend you keep a reference to the listener in the
     * instance data of an object that will exist as long as you need the
     * listener.</p>
     *
     * @param listener The callback that will run.
     * @see #unregisterOnSharedPreferenceChangeListener
     */
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (impl != null) {
            impl.registerOnSharedPreferenceChangeListener(listener);
        } else {
            listeners.put(listener, null);
        }
    }

    /**
     * Unregisters a previous callback.
     *
     * @param listener The callback that should be unregistered.
     * @see #registerOnSharedPreferenceChangeListener
     */
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (impl != null) {
            impl.unregisterOnSharedPreferenceChangeListener(listener);
        } else {
            listeners.remove(listener);
        }
    }

    /**
     * 后期指定另外的{@link SharedPreferences}来做这个的内容，同时把这边的修改同步到指定的preference里面。
     * @param impl 必需不能是null，并且只能指定一次。
     */
    public void attach(@NonNull SharedPreferences impl) {
        if (this.impl != null) return;
        if (this == impl) return;
        this.impl = impl;
        // TODO：SharedPreference的同步目前只做到了增加和修改，还做不到删除和清空
        Editor editor = impl.edit();
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Set) {
                //noinspection unchecked
                editor.putStringSet(entry.getKey(), (Set<String>) value);
            }
            if (value instanceof String) {
                editor.putString(entry.getKey(), (String) value);
            }
            if (value instanceof Integer) {
                editor.putInt(entry.getKey(), (Integer) value);
            }
            if (value instanceof Long) {
                editor.putLong(entry.getKey(), (Long) value);
            }
            if (value instanceof Float) {
                editor.putFloat(entry.getKey(), (Float) value);
            }
            if (value instanceof Boolean) {
                editor.putBoolean(entry.getKey(), (Boolean) value);
            }
        }
        editor.apply();
        map.clear();
        try {
            for(OnSharedPreferenceChangeListener listener : listeners.keySet()) {
                impl.registerOnSharedPreferenceChangeListener(listener);
            }
        } finally {
            listeners.clear();
        }
    }

    @Nullable
    private SharedPreferences impl = null;
    private final Map<OnSharedPreferenceChangeListener, Void> listeners = new WeakHashMap<>();
    private final Map<String, Object> map = new JSONObject();
    private final class EditorImpl implements SharedPreferences.Editor {
        @Nullable @SuppressLint("CommitPrefEdits")
        private final SharedPreferences.Editor editor = impl==null?null:impl.edit();
        private final Map<String, Object> put = new JSONObject();
        private final Set<String> remove = new HashSet<>();
        private boolean clear = false;

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            if (editor != null) {
                return editor.putStringSet(key, values);
            }
            put.put(key, values);
            return this;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            if (editor != null) {
                return editor.putString(key, value);
            }
            put.put(key, value);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            if (editor != null) {
                return editor.putInt(key, value);
            }
            put.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            if (editor != null) {
                return editor.putLong(key, value);
            }
            put.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            if (editor != null) {
                return editor.putFloat(key, value);
            }
            put.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            if (editor != null) {
                return editor.putBoolean(key, value);
            }
            put.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            if (editor != null) {
                return editor.remove(key);
            }
            remove.add(key);
            return this;
        }

        @Override
        public Editor clear() {
            if (editor != null) {
                return editor.clear();
            }
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            if (editor != null) {
                return editor.commit();
            }
            apply();
            return true;
        }

        @Override
        public void apply() {
            if (editor != null) {
                editor.apply();
                return;
            }
            Collection<String> keys = new HashSet<>();
            if (clear) {
                keys.addAll(map.keySet());
                map.clear();
            } else {
                keys.addAll(remove);
                map.keySet().removeAll(remove);
            }
            map.putAll(put);
            keys.addAll(put.keySet());
            // 去主线程通知回调
            Flowable.fromIterable(keys)
                    .subscribeOn(mainThread())
                    .subscribe(onSharedPreferenceChanged);
        }
    }

    private void onSharedPreferenceChanged(String key) throws ConcurrentModificationException {
        for(OnSharedPreferenceChangeListener listener : listeners.keySet()) {
            listener.onSharedPreferenceChanged(this, key);
        }
    }
    private final Consumer<String> onSharedPreferenceChanged = new Consumer<String>() {
        @Override
        public void accept(String key) throws Exception {
            try {
                onSharedPreferenceChanged(key);
            } catch (ConcurrentModificationException e) {
                Log.w(getClass().getSimpleName(), "onSharedPreferenceChanged("+key+")", e);
            }
        }
    };
}
