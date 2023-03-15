package org.poe.defectfinder.other;

import android.app.Activity;

public abstract class BackgroundTask {
    private Activity activity;
    public BackgroundTask(Activity activity) {
        this.activity = activity;
    }

    private void startBackground() {
        new Thread(new Runnable() {
            public void run() {

                long id = doInBackground();
                activity.runOnUiThread(new Runnable() {
                    public void run() {

                        onPostExecute(id);
                    }
                });
            }
        }).start();
    }
    public void execute(){
        startBackground();
    }

    public abstract long doInBackground();
    public abstract void onPostExecute(long id);

}
