package com.example.fingerprintapp.fingerprint;

import java.util.Date;

public class FingerprintData {
    private int id;
    private byte[] image;
    private Date dateCaptured;

    public FingerprintData(int id, byte[] image, Date dateCaptured) {
        this.id = id;
        this.image = image;
        this.dateCaptured = dateCaptured;
    }

    public int getId() {
        return id;
    }

    public byte[] getImage() {
        return image;
    }

    public Date getDateCaptured() {
        return dateCaptured;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setDateCaptured(Date dateCaptured) {
        this.dateCaptured = dateCaptured;
    }
}
