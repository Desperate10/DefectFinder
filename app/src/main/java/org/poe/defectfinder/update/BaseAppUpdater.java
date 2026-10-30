package org.poe.defectfinder.update;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/** Holds the current offer and tells the dialog about changes. State is touched on the main thread only. */
abstract class BaseAppUpdater implements AppUpdater {

    interface Change {
        void apply(UpdateState state);
    }

    protected final Handler main = new Handler(Looper.getMainLooper());
    private UpdateState state;
    private Listener listener;

    @Nullable
    @Override
    public UpdateState getState() {
        return state;
    }

    @Override
    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    protected void publish(@Nullable UpdateState state) {
        this.state = state;
        if (listener != null) listener.onStateChanged(state);
    }

    /** Changes the current offer, if there is one. */
    protected void update(Change change) {
        if (state == null) return;
        UpdateState s = state.copy();
        change.apply(s);
        publish(s);
    }
}
