/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.symbol;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.junit.Test;
import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.model.font.Font;
import com.boundlessgeo.gsr.model.font.FontDecorationEnum;
import com.boundlessgeo.gsr.model.font.FontStyleEnum;
import com.boundlessgeo.gsr.model.font.FontWeightEnum;

public class SymbolSchemaTest extends JsonSchemaTest {

    @Test
    public void testColorJsonSchema() throws Exception {
        int[] color = { 67, 0, 255, 40 };
        String json = getJson(color);
        assertTrue(validateJSON(json, "gsr/1.0/color.json"));
    }

    @Test
    public void testSimpleMarkerSymbolJsonSchema() throws Exception {
        int[] color = { 67, 0, 255, 40 };
        int[] outlineColor = { 152, 230, 0, 255 };
        Outline outline = new Outline(outlineColor, 1);
        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(SimpleMarkerSymbolEnum.SQUARE, color, 8, 0,
                0, 0, outline);
        String json = getJson(sms);
        assertTrue(validateJSON(json, "gsr/1.0/sms.json"));
    }

    @Test
    public void testSimpleLineSymbolJsonSchema() throws Exception {
        int[] color = { 115, 76, 0, 255 };
        SimpleLineSymbol sls = new SimpleLineSymbol(SimpleLineSymbolEnum.DOT, color, 1);
        String json = getJson(sls);
        assertTrue(validateJSON(json, "gsr/1.0/sls.json"));
    }

    @Test
    public void testSimpleFillSymbolJsonSchema() throws Exception {
        int[] color = { 115, 76, 0, 255 };
        int[] outlineColor = { 110, 110, 110, 255 };
        SimpleLineSymbol sls = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, outlineColor, 1);
        SimpleFillSymbol sfs = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, color, sls);
        String json = getJson(sfs);
        assertTrue(validateJSON(json, "gsr/1.0/sfs.json"));
    }

    @Test
    public void testPictureMarkerSymbolJsonSchema() throws Exception {
        File img = new File(System.getProperty("user.dir")
                + "/src/test/resources/images/hospital10.png");
        BufferedImage bufferedImage = ImageIO.read(img);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", outputStream);
        byte[] rawData = outputStream.toByteArray();
        int[] color = { 255, 255, 255, 0 };
        BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(img.getAbsolutePath()));
        String mimeType = URLConnection.guessContentTypeFromStream(bis);
        URL url = img.toURI().toURL();
        PictureMarkerSymbol pms = new PictureMarkerSymbol(rawData, url.toString(), mimeType, color,
                19.5, 19.5, 0, 0, 0);
        String json = getJson(pms);
        assertTrue(validateJSON(json, "gsr/1.0/pms.json"));
    }

    @Test
    public void testPictureFillSymbolJsonSchema() throws Exception {
        File img = new File(System.getProperty("user.dir")
                + "/src/test/resources/images/hospital10.png");
        BufferedImage bufferedImage = ImageIO.read(img);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", outputStream);
        byte[] rawData = outputStream.toByteArray();
        int[] color = { 255, 255, 255, 0 };
        BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(img.getAbsolutePath()));
        String mimeType = URLConnection.guessContentTypeFromStream(bis);
        URL url = img.toURI().toURL();
        int[] outlineColor = { 110, 110, 110, 255 };
        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, outlineColor, 1);
        PictureFillSymbol pfs = new PictureFillSymbol(rawData, url.toString(), mimeType, color, 63,
                63, 0, 0, 0, outline, 1, 1);
        String json = getJson(pfs);
        assertTrue(validateJSON(json, "gsr/1.0/pfs.json"));
    }

    @Test
    public void testTextSymbolJsonSchema() throws Exception {
        int[] color = { 78, 78, 78, 255 };
        int[] backgroundColor = null;
        int[] borderLineColor = null;
        Font font = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        TextSymbol textSymbol = new TextSymbol(0, 0, 0, color, backgroundColor, borderLineColor,
                VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false, font);
        String json = getJson(textSymbol);
        assertTrue(validateJSON(json,"gsr/1.0/ts.json"));
    }
}
