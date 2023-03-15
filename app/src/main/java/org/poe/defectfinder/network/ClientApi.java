package org.poe.defectfinder.network;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClientApi {

    private static final String BASE_URL = "http://task.poe.pl.ua:8880/app/";

    private static ClientApi mInstance;

    private static Retrofit retrofit = null;
    private static Retrofit retrofitCache = null;

    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_PRAGMA = "Pragma";
    int cacheSize = 10 * 1024 * 1024; // 10 MB

    public static synchronized ClientApi getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ClientApi(context);
        }
        return mInstance;
    }

    private ClientApi(Context context) {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofitCache = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public MyApi getApi() {
        return retrofit.create(MyApi.class);
    }

    public MyApi getCachedApi() {
        return retrofitCache.create(MyApi.class);
    }

    Interceptor networkInterceptor() {
        return chain -> {
            CacheControl cacheControl = new CacheControl.Builder()
                    .onlyIfCached()
                    .maxAge(3, TimeUnit.DAYS) //cache validity time
                    .build();

            Response response = chain.proceed(chain.request());

            return response.newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                    .build();
        };
    }

    OkHttpClient okHttpClient(Context context) {
        Cache cache = new Cache(context.getCacheDir(), cacheSize);

        return new OkHttpClient.Builder()
                .cache(cache)
                .addNetworkInterceptor(networkInterceptor())
                .build();
    }

    /*public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            //.create(MyApi.class);
        }
        return retrofit;
    }*/

}
