package org.geoserver.wps.gs.raster.algebra;

import it.geosolutions.jaiext.bandmerge.BandMergeDescriptor;

import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRenderedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.BandSelectDescriptor;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.jaitools.jiffle.JiffleBuilder;
import org.jaitools.jiffle.JiffleException;

/**
 * This class is a GeoServer process which takes in input a Coverage and a List of Jiffle scripts and executes the scripts on the input coverage
 * bands. The user can even provide only one script that will be calculated on all the bands.
 * 
 * @author Nicola Lagomarsini GeoSolutions SAS
 * 
 */
public class JiffleScriptListProcess implements GSProcess {

    private final static Logger LOGGER = Logging.getLogger(JiffleScriptListProcess.class);

    /**
     * @param data representing the input coverage (Mandatory)
     * @param scripts representing a list of the jiffle scripts to execute (Mandatory)
     * @return coverage representing the output elaborated coverage
     */
    @DescribeResult(name = "result", description = "JiffleProcess", type = GridCoverage2D.class)
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", description = "Input Raster(mandatory)", min = 1) GridCoverage2D coverage,
            @DescribeParameter(name = "scripts", description = "Jiffle Script List to use on the raster bands (mandatory)", min = 1) List<String> scripts)
            throws Exception {

        // hints for tiling
        final Hints hints = GeoTools.getDefaultHints().clone();
        // Selection of the GridGeometry associated to the input coverage
        GridGeometry2D destGridGeometry = coverage.getGridGeometry();
        // create jiffle script
        final JiffleBuilder jb = new JiffleBuilder();
        // RenderedImage associated to the input coverage
        RenderedImage coverageIMG = coverage.getRenderedImage();
        // Number of Bands of the input coverage
        int numBands = coverageIMG.getSampleModel().getNumBands();

        // Has Alpha Band?
        ColorModel colorModel = coverageIMG.getColorModel();
        if (colorModel != null) {
            // Check if the alpha band is present and if so, the last band is not considered
            if ((numBands > 1) && (colorModel instanceof ComponentColorModel)
                    && colorModel.hasAlpha() && !colorModel.isAlphaPremultiplied()) {
                numBands--;
            }
        }

        // Setting of the single script operation if only one operation is present
        String script = null;
        int scriptNum = scripts.size();
        boolean singleScript = scriptNum == 1;
        if (singleScript) {
            script = scripts.get(0);
        } else {
            if (scriptNum != numBands) {
                throw new IllegalArgumentException(
                        "Number of scripts is different from the total bands number");
            }
        }

        RenderedImage[] inputs;
        int[] bandIndices;
        // Check on the input bands
        if (numBands == 1) {
            // If only one band is present, no band manipulation is performed
            inputs = new RenderedImage[] { coverageIMG };
        } else {
            bandIndices = new int[1];
            inputs = new RenderedImage[numBands];

            for (int i = 0; i < numBands; i++) {
                bandIndices[0] = i;
                inputs[i] = BandSelectDescriptor.create(coverageIMG, bandIndices, hints);
            }
        }
        try {

            for (int i = 0; i < numBands; i++) {
                // We pass the script to the builder and associate the source images
                // with the variable names. Note the use of method chaining.
                if (singleScript) {
                    jb.script(script);
                } else {
                    jb.script(scripts.get(i));
                }
                // Calculation of the result on the selected band
                inputs[i] = jiffleProcessExecution(inputs[i], jb, destGridGeometry);
                jb.clear();
            }

            RenderedOp img = BandMergeDescriptor.create(null, 0, hints, inputs);

            // Creation of the GridGeometry associated to the final image
            final GridGeometry2D gg2d = new GridGeometry2D(new GridEnvelope2D(PlanarImage
                    .wrapRenderedImage(img).getBounds()), destGridGeometry.getGridToCRS(),
                    destGridGeometry.getCoordinateReferenceSystem());

            // create the final coverage reusing origin envelope
            final GridCoverage2D retValue = new GridCoverageFactory(hints).create(
                    coverage.getName() + "_modified", img, gg2d.getEnvelope());

            return retValue;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            // jiffle
            jb.clear();
            // Coverage disposal
            coverage.dispose(true);
        }

        // If an exception is thrown, null is returned
        return null;
    }

    /**
     * Private method used for executing the script operation on an input image with the selected GridGeometry2D.
     * 
     * @param input RenderedImage to process
     * @param jb jiffleBuilder object with the script to execute
     * @param destGridGeometry GridGeometry object associated to the output image
     * @return img output image generated from the script
     * @throws JiffleException
     */
    private RenderedImage jiffleProcessExecution(RenderedImage input, JiffleBuilder jb,
            GridGeometry2D destGridGeometry) throws JiffleException {

        // Setting of the source
        jb.source("image", input, null, false);

        // Now we specify the tile dimensions of the final image
        int tileWidth = input.getTileWidth();
        int tileHeight = input.getTileHeight();
        // Creation of a SampleModel associated with the final image
        SampleModel sm = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE,
                tileWidth, tileHeight, 1);
        // Selection of the GridEnvelope associated to the input coverage
        final GridEnvelope2D gr2d = destGridGeometry.getGridRange2D();
        // Final image creation
        final WritableRenderedImage img = new TiledImage(gr2d.x, gr2d.y, gr2d.width, gr2d.height,
                0, 0, sm, PlanarImage.createColorModel(sm));
        // Setting of the final image
        jb.dest("dest", img);

        // Finally we run the script and retrieve the resulting image.
        jb.run();

        return img;
    }

}
