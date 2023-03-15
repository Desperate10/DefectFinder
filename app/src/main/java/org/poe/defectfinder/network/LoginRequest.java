package org.poe.defectfinder.network;

import com.google.gson.annotations.SerializedName;

import okhttp3.RequestBody;

public class LoginRequest {

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @SerializedName("login")
    String login;

    @SerializedName("password")
    String password;
}
