package org.poe.defectfinder.network;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class UploadRequestBody extends RequestBody {

    public UploadRequestBody(File file, String contentType, UploadCallback callback) {
        this.file = file;
        this.contentType = contentType;
        this.callback = callback;
    }

    private File file;
    private String contentType;
    private UploadCallback callback;

    public interface UploadCallback {
        void onProgressUpdate(int percentage);
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType+"/*");
    }

    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long length = file.length();
        byte[] b = new byte[8192];
        FileInputStream fileInputStream = new FileInputStream(file);
        long uploaded = 0;
        Handler handler = new Handler(Looper.getMainLooper());
        int n, tot = 0;
        while ((n = fileInputStream.read(b)) > 0) {
            handler.post(new ProgressUpdater(uploaded, length));
            uploaded+=n;
            sink.write(b, 0, n);
        }
    }

    class ProgressUpdater implements Runnable {

        long uploaded;
        long total;

        public ProgressUpdater(long uploaded, long total) {
            this.uploaded = uploaded;
            this.total = total;
        }

        @Override
        public void run() {
            callback.onProgressUpdate((int) (100 * uploaded / total));
        }
    }
}
