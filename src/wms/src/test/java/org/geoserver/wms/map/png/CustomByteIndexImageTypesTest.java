package org.geoserver.wms.map.png;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ar.com.hjg.pngj.FilterType;

@RunWith(Parameterized.class)
public class CustomByteIndexImageTypesTest {

    private int ncolors;

    private int size;

    public CustomByteIndexImageTypesTest(int ncolors, int size) {
        this.ncolors = ncolors;
        this.size = size;
    }

    @Parameters(name = "colors{0}/size{1}")
    public static Collection<Object[]> parameters() {
        List<Object[]> result = new ArrayList<Object[]>();
        for (int ncolors : new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 
                19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 
                107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 
                197, 199, 211, 223, 227, 229, 233, 239, 241, 255, 256}) {
            for (int size = 1; size <= 8; size++) {
                result.add(new Object[] { ncolors, size });
            }
        }

        return result;
    }

    @Test
    public void testCustomIndexedImage() throws IOException {
        byte[] colors = new byte[ncolors];
        for (int i = 0; i < ncolors; i++) {
            colors[i] = (byte) i;
        }
        int nbits;
        if(ncolors <= 2) {
            nbits = 1;
        } else {
            nbits = (int) Math.ceil(Math.log(ncolors) / Math.log(2));
            if((nbits & (nbits - 1)) != 0) {
                int nextPower = (int) (Math.floor(Math.log(nbits) / Math.log(2)) + 1);
                nbits = (int) Math.pow(2, nextPower);
            }
        }
        
        IndexColorModel icm = new IndexColorModel(nbits, ncolors, colors, colors, colors);
        SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, size, size, nbits);
        int pixelsPerByte = 8 / nbits;
        int bytesPerRow = (int) Math.max(1, Math.ceil(1d * size / pixelsPerByte));
        int bytes = bytesPerRow * size;
        DataBufferByte dataBuffer = new DataBufferByte(bytes);
        WritableRaster wr = (WritableRaster) Raster.createWritableRaster(sm, dataBuffer, new Point(0, 0));
        BufferedImage bi = new BufferedImage(icm, wr, false, null);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 16, 32);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(16, 0, 16, 32);
        graphics.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new PNGJWriter().writePNG(bi, bos, 5, FilterType.FILTER_NONE);

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
        ImageAssert.assertImagesEqual(bi, read);
    }
}
