package com.dc.core;


import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class GeoTIFF {
    private String path = "/mnt/disk2/subversions/git/github/geotrellis-spray-tutorial/data/geotiff/";
    private String filepath = "";
    private PlanarImage[] images = new PlanarImage[3];
    private OutputStream os = null;
    private RenderedImage renderedImage;

    public GeoTIFF() {
        try {
            /*renderedImage = javax.imageio.ImageIO.read(new File(path + "LC81770282014145LGN00.TIF"));
            images[0] = (PlanarImage) new RenderedImageAdapter(renderedImage);

            renderedImage = javax.imageio.ImageIO.read(new File(path + "LC81770282014209LGN00.TIF"));
            images[1] = (PlanarImage) new RenderedImageAdapter(renderedImage);

            renderedImage = javax.imageio.ImageIO.read(new File(path + "LC81770282014241LGN00.TIF"));
            images[2] = (PlanarImage) new RenderedImageAdapter(renderedImage);*/


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void imageIO(Boolean f) {
        try {
            System.out.println(new Date());
            saveTiffImageIO(images);
            if(f) saveTiffPng();
            System.out.println(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void JAI(Boolean f) {
        try {
            System.out.println(new Date());
            saveTiffJAI(os, images);
            if(f) saveTiffPng();
            System.out.println(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTiffImageIO(PlanarImage[] images)
            throws IOException {
        Iterator<ImageWriter> writers = ImageIO
                .getImageWritersByFormatName("tiff");
        ImageWriter imageWriter = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(new File(path + "result.TIF"));
        imageWriter.setOutput(ios);

        imageWriter.prepareWriteSequence(null);
        for (PlanarImage image : images) {
            imageWriter.writeToSequence(new IIOImage(image, null, null), null);
        }
        imageWriter.endWriteSequence();

        imageWriter.dispose();
        ios.flush();
        ios.close();
    }

    private void saveTiffPng() throws IOException {
        final BufferedImage tif = ImageIO.read(new File(path + "result.TIF"));
        ImageIO.write(tif, "png", new File(path + "result.png"));
    }

    private void saveTiffJAI(OutputStream out, PlanarImage[] images)
            throws IOException {
        TIFFEncodeParam param = new TIFFEncodeParam();
        ImageEncoder encoder = ImageCodec
                .createImageEncoder("TIFF", out, param);
        List<PlanarImage> list = new ArrayList<PlanarImage>();
        for (int i = 1; i < images.length; i++) {
            list.add(images[i]);
        }
        param.setExtraImages(list.iterator());
        encoder.encode(images[0]);
    }

    public double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    public void merge() {
        try {
            String targetDir = path + "result2.tiff";

            BufferedImage image0 = ImageIO.read(new File(path + "LC81770282014145LGN00.TIF"));
            BufferedImage image1 = ImageIO.read(new File(path + "LC81770282014209LGN00.TIF"));
            BufferedImage image2 = ImageIO.read(new File(path + "LC81770282014241LGN00.TIF"));

            BufferedImage img2 = new BufferedImage(image0.getWidth(), image0.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            double minHeight = min(image0.getHeight(), image1.getHeight(), image2.getHeight());
            double minWidth = min(image0.getWidth(), image1.getWidth(), image2.getWidth());

            for(int y = 0; y < minHeight; y++) {
                for(int x = 0; x < minWidth; x++) {
                    img2.setRGB(x, y, image0.getRGB(x, y) + image1.getRGB(x, y) + image2.getRGB(x, y));
                }
            }
            File outputFile = new File(targetDir);
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
            ImageIO.write(img2, "tiff", outputFile);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void toPng() {
        try {
            File file = new File(path + "result2.tiff");
            String targetDir = path + "result2.png";
            BufferedImage image = ImageIO.read(file);
            if(image != null) {
                BufferedImage img2 = new BufferedImage(image.getWidth(), image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                for(int y = 0; y < image.getHeight(); y++) {
                    for(int x = 0; x < image.getWidth(); x++) {
                        img2.setRGB(x, y, image.getRGB(x, y));
                    }
                }
                File outputFile = new File(targetDir);
                outputFile.getParentFile().mkdirs();
                outputFile.createNewFile();
                ImageIO.write(img2, "png", outputFile);
            } else {
                System.out.println("image is null for file " + file.getAbsolutePath());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }


}
