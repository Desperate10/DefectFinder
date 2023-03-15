package org.poe.defectfinder.network;

public class Response {

    public Response(String error, String message, String image) {
        this.error = error;
        this.message = message;
        this.image = image;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getImage() {
        return image;
    }

    String error;
    String message;
    String image;
}
