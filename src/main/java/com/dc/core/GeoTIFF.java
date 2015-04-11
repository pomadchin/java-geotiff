package com.dc.core;


import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;
import org.geotools.gce.geotiff.GeoTiffIIOMetadataAdapter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

public class GeoTIFF {
    private String path;
    private ArrayList<String> fileNames;
    private String targetDir;
    private String grayScalePath;
    private ArrayList<double[]> tiePoints;
    private ArrayList<double[]> pixelScales;

    public GeoTIFF() {
        path = "data/geotiff/";
        tiePoints = new ArrayList<double[]>();
        pixelScales = new ArrayList<double[]>();

        fileNames = new ArrayList<String>();
        fileNames.add("file0.tiff");
        fileNames.add("file1.tiff");
        fileNames.add("file2.tiff");

        targetDir = path + "result.tiff";

        gdalInfo();
    }

    public GeoTIFF(String grayScalePath) {
        this.grayScalePath = grayScalePath;
    }

    private void saveTiffPng() throws IOException {
        final BufferedImage tif = ImageIO.read(new File(targetDir));
        ImageIO.write(tif, "png", new File(path + "result.png"));
    }

    public double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    public int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public int max(int a, int b) {
        return Math.max(a, b);
    }

    public void gdalInfo() {
        println("Getting geo info:");
        for (String fileName : fileNames) {
            println(fileName);
            gdalInfo(fileName);
        }
    }

    public void gdalInfo(String p) {
        try {
            String filename = path + p;
            FileImageInputStream f = new FileImageInputStream(
                    new RandomAccessFile(filename, "r"));

            // Look through ImageIO readers
            Iterator iter = ImageIO.getImageReaders(f);
            IIOMetadata imdata = null;
            GeoTiffIIOMetadataAdapter geoData;
            while (iter.hasNext() && imdata == null) {
                ImageReader reader = (ImageReader) iter.next();
                reader.setInput(f, true);
                String readerName = reader.getFormatName().toLowerCase();
                if (readerName.equalsIgnoreCase("tif")) {
                    // Get Image metadata
                    imdata = reader.getImageMetadata(0);
                    geoData = new GeoTiffIIOMetadataAdapter(imdata);
                    if (geoData != null &&
                            geoData.getGeoKeyDirectoryVersion() == 1) {
                        tiePoints.add(geoData.getModelTiePoints());
                        pixelScales.add(geoData.getModelPixelScales());
                    }
                }
            }
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void merge() {
        try {
            int rgb0, rgb1, rgb2, xoffset1, xoffset2, xscale1, xscale2, yoffset1, yoffset2, yscale1, yscale2, xrollback;
            double minHeight, minWidth, startX, startY;
            double[] scale1, scale2, tiePoints0, tiePoints1, tiePoints2;
            BufferedImage image0, image1, image2, image;

            println("Merge started...");

            image0 = ImageIO.read(new File(path + fileNames.get(0)));
            image1 = ImageIO.read(new File(path + fileNames.get(1)));
            image2 = ImageIO.read(new File(path + fileNames.get(2)));

            image = new BufferedImage(image0.getWidth(), image0.getHeight(), BufferedImage.TYPE_INT_RGB);

            minHeight = min(image0.getHeight(), image1.getHeight(), image2.getHeight());
            minWidth = min(image0.getWidth(), image1.getWidth(), image2.getWidth());

            tiePoints0 = tiePoints.get(0);
            tiePoints1 = tiePoints.get(1);
            tiePoints2 = tiePoints.get(2);

            startX = tiePoints0[3];
            startY = tiePoints0[4];

            scale1 = pixelScales.get(1);
            scale2 = pixelScales.get(2);

            xscale1 = (int) scale1[0]; // 15
            xscale2 = (int) scale2[0]; // 15
            yscale1 = (int) scale1[1]; // 15
            yscale2 = (int) scale2[1]; // 15

            xoffset1 = (int) ((startX - tiePoints1[3]) / xscale1); // 20
            xoffset2 = (int) ((startX - tiePoints2[3]) / xscale2); // 40
            yoffset1 = (int) ((startY - tiePoints1[4]) / yscale1); // 0
            yoffset2 = (int) ((startY - tiePoints2[4]) / yscale2); // 0

            xrollback = max(xoffset1, xoffset2);

            for(int y = 0; y < minHeight; y++) {
                for(int x = 0; x < minWidth; x++) {
                    rgb0 = 0; rgb1 = 0; rgb2 = 0;

                    if((x < image0.getWidth()) && (y < image0.getHeight()))
                        rgb0 = image0.getRGB(x, y);

                    if(((x + xoffset1) < image1.getWidth()) && ((y + yoffset1) < image1.getHeight()))
                        rgb1 = image1.getRGB(x + xoffset1, y + yoffset1);

                    if(((x + xoffset2) < image2.getWidth()) && ((y + yoffset2) < image2.getHeight()))
                        rgb2 = image2.getRGB(x + xoffset2, y + yoffset2);

                    image.setRGB(x, y, rgb0 + rgb1 + rgb2);
                }
            }

            File outputFile = new File(targetDir);
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
            ImageIO.write(image, "tiff", outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mixedMerge() {
        try {
            splitMerge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage splitImage(
            ImageInputStream inputStream,
            int xstart) throws IOException {
        int height, width;
        int xoffset = 3000;
        BufferedImage resampledImage = null;

        Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);

        if(!readers.hasNext()) {
            throw new IOException("No reader available for supplied image stream.");
        }

        ImageReader reader = readers.next();

        ImageReadParam imageReaderParams = reader.getDefaultReadParam();
        reader.setInput(inputStream);

        height = reader.getHeight(0);
        width = reader.getWidth(0);

        if((width - xstart) < 3000)
            xoffset = width - xstart;

        imageReaderParams.setSourceRegion(new Rectangle(xstart, 0, xoffset, height));

        resampledImage = reader.read(0, imageReaderParams);

        return resampledImage;
    }

    public void splitMerge() throws IOException {
        int rgb0, rgb1, rgb2, xoffset1, xoffset2, xscale1, xscale2,
            yoffset1, yoffset2, yscale1, yscale2, minHeight, minWidth, xstart0, xstart1, xstart2, xstart, xrollback;
        double startX, startY;
        double[] scale1, scale2, tiePoints0, tiePoints1, tiePoints2;
        BufferedImage image0, image1, image2, image;
        Boolean loop = true;
        Pair<BufferedImage, Pair<Integer, Boolean>> pair0, pair1, pair2;
        Pair<Integer, Boolean> infoPair0, infoPair1, infoPair2;

        println("Split merge started...");

        ImageInputStream is0 = ImageIO.createImageInputStream(new File(path + fileNames.get(0)));
        ImageInputStream is1 = ImageIO.createImageInputStream(new File(path + fileNames.get(1)));
        ImageInputStream is2 = ImageIO.createImageInputStream(new File(path + fileNames.get(2)));

        Iterator<ImageReader> readers0 = ImageIO.getImageReaders(is0);
        Iterator<ImageReader> readers1 = ImageIO.getImageReaders(is1);
        Iterator<ImageReader> readers2 = ImageIO.getImageReaders(is2);

        if(!readers0.hasNext() && !readers1.hasNext() && !readers2.hasNext()) {
            throw new IOException("No reader available for supplied image stream.");
        }

        ImageReader reader0 = readers0.next();
        ImageReader reader1 = readers1.next();
        ImageReader reader2 = readers2.next();

        reader0.setInput(is0);
        reader1.setInput(is1);
        reader2.setInput(is2);

        image = new BufferedImage(
                min(reader0.getWidth(0), reader1.getWidth(0), reader2.getWidth(0)),
                min(reader0.getHeight(0), reader1.getHeight(0), reader2.getHeight(0)),
                BufferedImage.TYPE_INT_RGB); // huge memory loose

        minHeight = min(reader0.getHeight(0), reader1.getHeight(0), reader2.getHeight(0));
        minWidth = min(reader0.getWidth(0), reader1.getWidth(0), reader2.getWidth(0));

        tiePoints0 = tiePoints.get(0);
        tiePoints1 = tiePoints.get(1);
        tiePoints2 = tiePoints.get(2);

        startX = tiePoints0[3];
        startY = tiePoints0[4];

        scale1 = pixelScales.get(1);
        scale2 = pixelScales.get(2);

        xscale1 = (int) scale1[0]; // 15
        xscale2 = (int) scale2[0]; // 15
        yscale1 = (int) scale1[1]; // 15
        yscale2 = (int) scale2[1]; // 15

        xoffset1 = (int) ((startX - tiePoints1[3]) / xscale1); // 20
        xoffset2 = (int) ((startX - tiePoints2[3]) / xscale2); // 40
        yoffset1 = (int) ((startY - tiePoints1[4]) / yscale1); // 0
        yoffset2 = (int) ((startY - tiePoints2[4]) / yscale2); // 0

        xrollback = max(xoffset1, xoffset2);

        xstart = 0;
        xstart0 = 0;
        xstart1 = 0;
        xstart2 = 0;

        pair0 = splitImage(reader0, xstart0);
        pair1 = splitImage(reader1, xstart1);
        pair2 = splitImage(reader2, xstart2);

        image0 = pair0.getFst();
        image1 = pair1.getFst();
        image2 = pair2.getFst();

        infoPair0 = pair0.getSnd();
        infoPair1 = pair1.getSnd();
        infoPair2 = pair2.getSnd();

        loop = !infoPair0.getSnd() && !infoPair1.getSnd() && !infoPair2.getSnd();

        while(loop) {

            loop = !infoPair0.getSnd() && !infoPair1.getSnd() && !infoPair2.getSnd();

            for(int y = 0; y < minHeight; y++) {
                for(int x = xstart; x < minWidth; x++) {
                    rgb0 = 0; rgb1 = 0; rgb2 = 0;

                    if(((x - xstart) < image0.getWidth()) && (y < image0.getHeight()))
                        rgb0 = image0.getRGB(x - xstart, y);

                    if(((x + xoffset1 - xstart) < image1.getWidth()) && ((y + yoffset1) < image1.getHeight()))
                        rgb1 = image1.getRGB(x + xoffset1 - xstart, y + yoffset1);


                    if(((x + xoffset2 - xstart) < image2.getWidth()) && ((y + yoffset2) < image2.getHeight()))
                        rgb2 = image2.getRGB(x + xoffset2 - xstart, y + yoffset2);

                    image.setRGB(x, y, rgb0 + rgb1 + rgb2);
                }
            }

            image0.flush();
            image1.flush();
            image2.flush();

            if (loop) {

                xstart0 = infoPair0.getFst();
                println("xstart0:" + String.valueOf(xstart0));

                xstart1 = infoPair1.getFst();
                println("xstart1:" + String.valueOf(xstart1));

                xstart2 = infoPair2.getFst();
                println("xstart2:" + String.valueOf(xstart2));

                xstart = min(xstart0, xstart1, xstart2) - 2 * xrollback;

                // new pairs
                pair0 = splitImage(reader0, xstart0 - 2 * xrollback);
                image0 = pair0.getFst();
                infoPair0 = pair0.getSnd();
                xstart0 = infoPair0.getFst();

                pair1 = splitImage(reader1, xstart1 - 2 * xrollback);
                image1 = pair1.getFst();
                infoPair1 = pair1.getSnd();
                xstart1 = infoPair1.getFst();

                pair2 = splitImage(reader2, xstart2 - 2 * xrollback);
                image2 = pair2.getFst();
                infoPair2 = pair2.getSnd();
                xstart2 = infoPair2.getFst();
            }
        }

        // result
        File outputFile = new File(targetDir);
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();
        ImageIO.write(image, "tiff", outputFile);
    }

    public void splitMergeModified() {
        int rgb0, rgb1, rgb2, xoffset1, xoffset2, xscale1, xscale2,
                yoffset1, yoffset2, yscale1, yscale2, minHeight, minWidth, xstart0, xstart1, xstart2, xstart, xrollback;
        double startX, startY;
        double[] scale1, scale2, tiePoints0, tiePoints1, tiePoints2;
        int[][] pixels;
        BufferedImage image0, image1, image2, image;
        Boolean loop = true;
        Pair<BufferedImage, Pair<Integer, Boolean>> pair0, pair1, pair2;
        Pair<Integer, Boolean> infoPair0, infoPair1, infoPair2;

        try {

            println("Split merge started...");

            ImageInputStream is0 = ImageIO.createImageInputStream(new File(path + fileNames.get(0)));
            ImageInputStream is1 = ImageIO.createImageInputStream(new File(path + fileNames.get(1)));
            ImageInputStream is2 = ImageIO.createImageInputStream(new File(path + fileNames.get(2)));

            Iterator<ImageReader> readers0 = ImageIO.getImageReaders(is0);
            Iterator<ImageReader> readers1 = ImageIO.getImageReaders(is1);
            Iterator<ImageReader> readers2 = ImageIO.getImageReaders(is2);

            if(!readers0.hasNext() && !readers1.hasNext() && !readers2.hasNext()) {
                throw new IOException("No reader available for supplied image stream.");
            }

            ImageReader reader0 = readers0.next();
            ImageReader reader1 = readers1.next();
            ImageReader reader2 = readers2.next();

            reader0.setInput(is0);
            reader1.setInput(is1);
            reader2.setInput(is2);

            minHeight = min(reader0.getHeight(0), reader1.getHeight(0), reader2.getHeight(0));
            minWidth = min(reader0.getWidth(0), reader1.getWidth(0), reader2.getWidth(0));

            tiePoints0 = tiePoints.get(0);
            tiePoints1 = tiePoints.get(1);
            tiePoints2 = tiePoints.get(2);

            startX = tiePoints0[3];
            startY = tiePoints0[4];

            scale1 = pixelScales.get(1);
            scale2 = pixelScales.get(2);

            xscale1 = (int) scale1[0]; // 15
            xscale2 = (int) scale2[0]; // 15
            yscale1 = (int) scale1[1]; // 15
            yscale2 = (int) scale2[1]; // 15

            xoffset1 = (int) ((startX - tiePoints1[3]) / xscale1); // 20
            xoffset2 = (int) ((startX - tiePoints2[3]) / xscale2); // 40
            yoffset1 = (int) ((startY - tiePoints1[4]) / yscale1); // 0
            yoffset2 = (int) ((startY - tiePoints2[4]) / yscale2); // 0

            xrollback = max(xoffset1, xoffset2);

            xstart = 0;
            xstart0 = 0;
            xstart1 = 0;
            xstart2 = 0;

            pair0 = splitImage(reader0, xstart0);
            pair1 = splitImage(reader1, xstart1);
            pair2 = splitImage(reader2, xstart2);

            image0 = pair0.getFst();
            image1 = pair1.getFst();
            image2 = pair2.getFst();

            infoPair0 = pair0.getSnd();
            infoPair1 = pair1.getSnd();
            infoPair2 = pair2.getSnd();

            byte[] img = new byte[minWidth * minHeight * 2];

            loop = !infoPair0.getSnd() && !infoPair1.getSnd() && !infoPair2.getSnd();

            while(loop) {

                loop = !infoPair0.getSnd() && !infoPair1.getSnd() && !infoPair2.getSnd();

                for(int y = 0; y < minHeight; y++) {
                    for(int x = xstart; x < minWidth; x++) {
                        rgb0 = 0; rgb1 = 0; rgb2 = 0;

                        if(((x - xstart) < image0.getWidth()) && (y < image0.getHeight()))
                            rgb0 = image0.getRGB(x - xstart, y);

                        if(((x + xoffset1 - xstart) < image1.getWidth()) && ((y + yoffset1) < image1.getHeight()))
                            rgb1 = image1.getRGB(x + xoffset1 - xstart, y + yoffset1);


                        if(((x + xoffset2 - xstart) < image2.getWidth()) && ((y + yoffset2) < image2.getHeight()))
                            rgb2 = image2.getRGB(x + xoffset2 - xstart, y + yoffset2);

                        img[x * y * 2] = (byte) (rgb0 + rgb1 + rgb2);
                        img[x * y * 2 + 1] = (byte) ((rgb0 + rgb1 + rgb2) >> 8);
                        img[x * y * 2 + 2] = (byte) ((rgb0 + rgb1 + rgb2) >> 16);

                        //    break;
                    }
                    //  break;
                }

                image0.flush();
                image1.flush();
                image2.flush();

                if (loop) {

                    xstart0 = infoPair0.getFst();
                    println("xstart0:" + String.valueOf(xstart0));

                    xstart1 = infoPair1.getFst();
                    println("xstart1:" + String.valueOf(xstart1));

                    xstart2 = infoPair2.getFst();
                    println("xstart2:" + String.valueOf(xstart2));

                    xstart = min(xstart0, xstart1, xstart2) - 2 * xrollback;

                    // new pairs
                    pair0 = splitImage(reader0, xstart0 - 2 * xrollback);
                    image0 = pair0.getFst();
                    infoPair0 = pair0.getSnd();
                    xstart0 = infoPair0.getFst();

                    pair1 = splitImage(reader1, xstart1 - 2 * xrollback);
                    image1 = pair1.getFst();
                    infoPair1 = pair1.getSnd();
                    xstart1 = infoPair1.getFst();

                    pair2 = splitImage(reader2, xstart2 - 2 * xrollback);
                    image2 = pair2.getFst();
                    infoPair2 = pair2.getSnd();
                    xstart2 = infoPair2.getFst();
                }

                //break;
            }


            // result
            int pixelType = FormatTools.UINT16;
            int c = 1;
            int w = minWidth;
            int h = minHeight;

            TiffWriter tw = new TiffWriter();
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();

            MetadataTools.populateMetadata(meta, 0, null, false, "XYZCT",
                    FormatTools.getPixelTypeString(pixelType), w, h, 1, c, 1, c);

            meta.getChannelSamplesPerPixel(0,0);

            tw.setMetadataRetrieve(meta);
            tw.setId(targetDir);
            tw.saveBytes(0, img);
            tw.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (DependencyException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    public Pair<BufferedImage, Pair<Integer, Boolean>> splitImage(ImageReader reader, int xstart)
            throws IOException {
        return splitImage(reader, xstart, 0);
    }

    public Pair<BufferedImage, Pair<Integer, Boolean>> splitImage(ImageReader reader, int xstart, int offset)
            throws IOException {

        int height, width;
        int xoffset = 3000 + offset;
        BufferedImage resampledImage = null;
        Boolean finished = false;

        ImageReadParam imageReaderParams = reader.getDefaultReadParam();

        height = reader.getHeight(0);
        width = reader.getWidth(0);

        if((width - xstart) < 3000) {
            xoffset = width - xstart;
            finished = true;
        }

        imageReaderParams.setSourceRegion(new Rectangle(xstart, 0, xoffset, height));

        resampledImage = reader.read(0, imageReaderParams);

        return new Pair(resampledImage, new Pair(new Integer(xstart + xoffset), finished));
    }

    public void getGrayScalePixel(int x, int y) {
        try {
            BufferedImage image0;

            println("getPixel started...");

            image0 = ImageIO.read(new File(grayScalePath));

            println("GrayScalePixel " + "(" + String.valueOf(x) + ", " + String.valueOf(y) +
                    "): " + String.valueOf(image0.getData().getSample(x, y, 0)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void println(String string) {
        System.out.println(string);
    }
}
