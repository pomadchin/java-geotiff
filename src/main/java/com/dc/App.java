package com.dc;


import com.dc.core.GeoTIFF;

import java.util.Date;

public class App  {

    public static void main(String[] args) {
        // GeoTIFF tiff = new GeoTIFF();
        GeoTIFF tiff = new GeoTIFF("path");
        long t1 = System.currentTimeMillis();
        System.out.println(new Date());
        // tiff.merge();
        // tiff.gdalInfo();
        // tiff.mixedMerge();
        // tiff.splitMergeModified();
        tiff.getGrayScalePixel(41, 225);

        System.out.println(new Date());
        long t2 = System.currentTimeMillis();
        System.out.println("Time spend: " + (t2 - t1) + " ms");
    }
}

