package org.poe.defectfinder.network;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

public class LoginResponse {


    public LoginResponse(@Nullable String jwt) {
        this.jwt = jwt;
    }

    @Nullable
    public String getJwt() {
        return jwt;
    }

    @Nullable
        @SerializedName("jwt")
        public String jwt;




    @Nullable
    @SerializedName("error")
    public String error;

    @Nullable
    @SerializedName("system")
    public System system;

    public class System {

        @Nullable
        @SerializedName("time")
        public double time;

    }

}
