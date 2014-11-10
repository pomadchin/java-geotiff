package com.dc;

import com.dc.core.GeoTIFF;

import java.util.Date;

public class App  {

    public static void main(String[] args) {
        GeoTIFF tiff = new GeoTIFF();
        //tiff.imageIO(false);
        System.out.println(new Date());
        tiff.merge();
        //tiff.ijMerge();
        //tiff.toPng();
        System.out.println(new Date());
    }

}
