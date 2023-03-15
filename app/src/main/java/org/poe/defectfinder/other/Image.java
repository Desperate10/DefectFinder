package org.poe.defectfinder.other;

import android.net.Uri;

import java.io.Serializable;

public class Image implements Serializable {

    private Uri imgURI;

    public Uri getURI(){
        return imgURI;
    }

    public void setURI(Uri imgURI){
        this.imgURI = imgURI;
    }
}
