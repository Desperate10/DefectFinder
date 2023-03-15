package org.poe.defectfinder.network;

public class UploadResponse {

    private boolean error;
    private String message;
    private String file;

    public UploadResponse(boolean error, String message, String file) {
        this.error = error;
        this.message = message;
        this.file = file;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getFile() {
        return file;
    }


}
