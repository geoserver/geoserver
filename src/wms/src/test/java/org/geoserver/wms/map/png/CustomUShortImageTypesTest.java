package org.geoserver.wms.map.png;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ar.com.hjg.pngj.FilterType;

@RunWith(Parameterized.class)
public class CustomUShortImageTypesTest {
    
    private int nbits;
    private int size;

    public CustomUShortImageTypesTest(int nbits, int size) {
        this.nbits = nbits;
        this.size = size;
    }
    
    @Parameters(name = "bits{0}/size{1}")
    public static Collection<Object[]> parameters() {
        List<Object[]> result = new ArrayList<Object[]>();
        for(int nbits : new int[] {1, 2, 4, 8, 16}) {
            for(int size = 1; size <= 32; size++) {
                result.add(new Object[] {nbits, size});
            }
        }

        return result;
    }

    @Test
    public void testCustomUShortImage() throws IOException {
        BufferedImage bi = ImageTypeSpecifier.createGrayscale(nbits, DataBuffer.TYPE_USHORT, false)
                .createBufferedImage(size, size);
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
