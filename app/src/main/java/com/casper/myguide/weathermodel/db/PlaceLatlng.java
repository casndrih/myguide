package com.casper.myguide.weathermodel.db;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class PlaceLatlng {
    @Id
    private long id;
    private String lat;
    private String lng;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

}
