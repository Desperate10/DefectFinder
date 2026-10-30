package org.poe.defectfinder;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

public class App extends Application {

    public static App instance;

    private ObjectsDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = Room.databaseBuilder(this, ObjectsDatabase.class, "database")
                .addMigrations(ObjectsDatabase.MIGRATION_1_2, ObjectsDatabase.MIGRATION_2_3, ObjectsDatabase.MIGRATION_3_4, ObjectsDatabase.MIGRATION_4_5, ObjectsDatabase.MIGRATION_5_6)
                .allowMainThreadQueries()
                .build();
        // Android 16 ignores windowOptOutEdgeToEdgeEnforcement for apps targeting it: the screens are drawn
        // under the status and navigation bars, so every activity keeps its content clear of them here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            registerActivityLifecycleCallbacks(new SystemBarsInsets());
        }
    }

    public static App getInstance() {
        return instance;
    }

    public ObjectsDatabase getDatabase() {
        return database;
    }

    private static class SystemBarsInsets implements ActivityLifecycleCallbacks {

        @Override
        public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            // what reaches the content is the navigation bar, the keyboard (no longer resizing the window) and
            // the sides in landscape. The action bar moves itself below the status bar, but appcompat places the
            // content only an action bar height from the top: the status bar height is added here
            View content = activity.findViewById(android.R.id.content);
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
                WindowInsetsCompat root = ViewCompat.getRootWindowInsets(v);
                int statusBar = root != null ? root.getInsets(WindowInsetsCompat.Type.statusBars()).top : 0;
                v.setPadding(bars.left, Math.max(bars.top, statusBar), bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
            // the status bar is transparent now (statusBarColor is ignored): paint it like before
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            View statusBar = new View(activity);
            TypedValue color = new TypedValue();
            activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryVariant, color, true);
            statusBar.setBackgroundColor(color.data);
            decor.addView(statusBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
            ViewCompat.setOnApplyWindowInsetsListener(statusBar, (v, insets) -> {
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setLayoutParams(lp);
                return insets;
            });
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }
}
