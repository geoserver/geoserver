/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    http://java.net/projects/imageio-ext/
 *    (C) 2007 - 2009, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.map.png;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.ConstantDescriptor;

import org.junit.Test;

import ar.com.hjg.pngj.FilterType;

public class GrayAlpha8bitTest {

    @Test
    public void testGrayAlpha8Bit() throws Exception {
        BufferedImage bi = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 16, 32);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(16, 0, 16, 32);
        graphics.dispose();
        
        final ImageLayout tempLayout = new ImageLayout(bi);
        tempLayout.unsetValid(ImageLayout.COLOR_MODEL_MASK).unsetValid(
                ImageLayout.SAMPLE_MODEL_MASK);
        RenderedImage alpha = ConstantDescriptor.create(Float.valueOf(bi.getWidth()),
                Float.valueOf(bi.getHeight()), new Byte[] { Byte.valueOf((byte) 255) },
                new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));
        RenderedImage grayAlpha = BandMergeDescriptor.create(bi, alpha, null);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new PNGJWriter().writePNG(grayAlpha, bos, 5, FilterType.FILTER_NONE);
        
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
        BufferedImage gaBuffered = PlanarImage.wrapRenderedImage(grayAlpha).getAsBufferedImage();
        ImageAssert.assertImagesEqual(gaBuffered, read);
    }
}
