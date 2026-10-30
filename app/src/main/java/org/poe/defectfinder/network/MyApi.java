package org.poe.defectfinder.network;

import org.poe.defectfinder.other.DefectResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface MyApi {

    @Headers({ "Content-Type: application/json;charset=UTF-8"})
    @POST("authenticate")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @Headers({ "Content-Type: application/json;charset=UTF-8"})
    @POST("tokencheck")
    Call<Response> checkTokenValidity(@Body String token);

    // no Authorization: a phone whose login expired must still be able to update
    @GET("release")
    Call<AppRelease> release();

    @Multipart
    @POST("defects")
    Call<UploadResponse> uploadDefects(@Part List<MultipartBody.Part> files,
                                       @Part("files") RequestBody desc,
                                       @Header("Authorization") String authorization);

    @GET("getdefects")
    Call<List<DefectResponse>> getDefects(@Query("pageNo") String pageNo, @Query("filial") String filial, @Query("tplnr") String tplnr,
                                          @Header("Authorization") String authorization);

    @GET("getlinedefects")
    Call<List<DefectResponse>> getLineDefects(@Query("pageNo") String pageNo, @Query("filial") String filial, @Query("tplnr") String tplnr, @Query("opr") String opr,
                                          @Header("Authorization") String authorization);

    @GET("gettpdefects")
    Call<List<DefectResponse>> getTpDefects(@Query("pageNo") String pageNo, @Query("filial") String filial, @Query("tplnr") String tplnr,
                                          @Header("Authorization") String authorization);

    @GET("gettplnrarray")
    Call<List<DefectResponse>> getPltxt(@Query("pageNo") String pageNo, @Query("filial") String filial, @Query("pltxt") String pltxt, @Query("tplnr") String tplnr,
                                              @Header("Authorization") String authorization);

    @GET("getdefectspltxt")
    Call<List<DefectResponse>> getByPltxt(@Query("pageNo") String pageNo, @Query("filial") String filial, @Query("pltxt") String pltxt,
                                            @Header("Authorization") String authorization);

    @GET("getLines")
    Call<List<ObjectsResponse>> getLineObjects(@Query("filial") String filial, @Query("pltxt") String pltxt,
                                            @Header("Authorization") String authorization, @Header("Cache-Control") String cacheControl);

    @GET("get35Lines")
    Call<List<ObjectsResponse>> getHighLineObjects(@Query("filial") String filial,
                                               @Header("Authorization") String authorization, @Header("Cache-Control") String cacheControl);

    @GET("getTps")
    Call<List<ObjectsResponse>> getTpObjects(@Query("filial") String filial,
                                               @Header("Authorization") String authorization, @Header("Cache-Control") String cacheControl);

    @GET("getPss")
    Call<List<ObjectsResponse>> getPsObjects(@Query("filial") String filial,
                                             @Header("Authorization") String authorization, @Header("Cache-Control") String cacheControl);

    @GET("getKls")
    Call<List<ObjectsResponse>> getKlObjects(@Query("filial") String filial,
                                               @Header("Authorization") String authorization, @Header("Cache-Control") String cacheControl);

    /*static MyApi invoke() {

            return new Retrofit.Builder()
                    .baseUrl("http://task.poe.pl.ua:8880/app/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MyApi.class);


    }*/
}
