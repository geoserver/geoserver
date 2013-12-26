package org.geoserver.wms.map.png;


import static org.junit.Assert.assertEquals;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ar.com.hjg.pngj.FilterType;

@RunWith(Parameterized.class)
public class PngSuiteImagesTest {

    private File sourceFile;


    public PngSuiteImagesTest(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> result = new ArrayList<Object[]>();
        File source = new File("./src/test/resources/pngsuite");
        File[] files = source.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".png");
            }
        });
        Arrays.sort(files);
        for (File file : files) {
            result.add(new Object[] { file });
        }

        return result;
    }

    @Test
    public void testRoundTripFilterNone() throws Exception {
        BufferedImage input = ImageIO.read(sourceFile);

        roundTripPNGJ(input, input);
    }

    @Test
    public void testRoundTripTiledImage() throws Exception {
        BufferedImage input = ImageIO.read(sourceFile);

        // prepare a tiled image layout
        ImageLayout il = new ImageLayout(input);
        il.setTileWidth(8);
        il.setTileHeight(8);

        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);
        RenderedOp tiled = FormatDescriptor.create(input, input.getSampleModel().getDataType(),
                hints);
        assertEquals(8, tiled.getTileWidth());
        assertEquals(8, tiled.getTileHeight());

        roundTripPNGJ(input, tiled);
    }

    private void roundTripPNGJ(BufferedImage original, RenderedImage source) throws IOException {
        // write the PNG
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new PNGJWriter().writePNG(original, bos, 4, FilterType.FILTER_NONE);

        // write the output to file for eventual visual comparison
        byte[] bytes = bos.toByteArray();
        writeToFile(new File("./target/roundTripNone", sourceFile.getName()), bytes);

        // read it back
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(bis);

        ImageAssert.assertImagesEqual(original, image);
    }

    private void writeToFile(File file, byte[] bytes) throws IOException {
        File parent = file.getParentFile();
        parent.mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

}
