package org.geoserver.wps.gs.raster.algebra;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.BandSelectDescriptor;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.jaitools.jiffle.JiffleBuilder;

/**
 * This class is a GeoServer process which takes in input a Coverage and a Jiffle script and executes the script on the input coverage. The user can
 * select which band of the input coverage must be elaborated, by default the first image is calculated.
 * 
 * @author Nicola Lagomarsini GeoSolutions SAS
 */
public class JiffleScriptProcess implements GSProcess {

    private final static Logger LOGGER = Logging.getLogger(JiffleScriptProcess.class);

    /**
     * @param data representing the input coverage (Mandatory)
     * @param script representing the jiffle script to execute (Mandatory)
     * @param band representing the band index of the input coverage (Optional)
     * @return coverage representing the output elaborated coverage
     */
    @DescribeResult(name = "JiffleProcess", description = "JiffleProcess", type = GridCoverage2D.class)
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", description = "Input Raster(mandatory)", min = 1) GridCoverage2D coverage,
            @DescribeParameter(name = "script", description = "Jiffle Script to use on the raster data (mandatory)", min = 1) String script,
            @DescribeParameter(name = "dataEnv", description = "Jiffle Script environment variables (optional)", min = 0) String env,
            @DescribeParameter(name = "band", description = "Band Index to use (optional)", min = 0) Integer bandIndex)
            throws Exception {

        // hints for tiling
        final Hints hints = GeoTools.getDefaultHints().clone();

        // create jiffle script
        final JiffleBuilder jb = new JiffleBuilder();
        // Going to check if we need to substitute some value from the environment
        if (env != null && env.length() > 0) {
            String[] envSlices = env.split(",");
            for (String kvp : envSlices) {
                String[] envData = kvp.split("=");
                if (envData.length == 2) {
                    script = script.replace("{" + envData[0] + "}", envData[1]);
                }
            }
        }
        // We pass the script to the builder and associate the source images
        // with the variable names. Note the use of method chaining.
        jb.script(script);
        // RenderedImage associated to the input coverage
        RenderedImage coverageIMG = coverage.getRenderedImage();
        // Number of Bands of the input coverage
        int numBands = coverageIMG.getSampleModel().getNumBands();

        RenderedImage input;
        // Check on the input bands
        if (numBands == 1) {
            // If only one band is present, no band manipulation is performed
            input = coverageIMG;
        } else {
            // If the bandIndex is not defined, the first band is taken
            if (bandIndex == null) {
                bandIndex = Integer.valueOf(0);
                // If bandIndex is not valid, an exception is thrown
            } else if (bandIndex > numBands || bandIndex < 0) {
                throw new ProcessException("Band Index is not correct");
            }
            // Then the image at the selected band is taken
            int[] bandIndices = new int[] { bandIndex };
            input = BandSelectDescriptor.create(coverageIMG, bandIndices, hints);
        }
        try {
            // Setting of the source
            jb.source("image", input, null, false);

            // Now we specify the tile dimensions of the final image
            int tileWidth = input.getTileWidth();
            int tileHeight = input.getTileHeight();
            // Creation of a SampleModel associated with the final image
            SampleModel sm = RasterFactory.createPixelInterleavedSampleModel(
                    DataBuffer.TYPE_DOUBLE, tileWidth, tileHeight, 1);
            // Selection of the GridGeometry associated to the input coverage
            GridGeometry2D destGridGeometry = coverage.getGridGeometry();
            // Selection of the GridEnvelope associated to the input coverage
            final GridEnvelope2D gr2d = destGridGeometry.getGridRange2D();
            // Final image creation
            final WritableRenderedImage img = new TiledImage(gr2d.x, gr2d.y, gr2d.width,
                    gr2d.height, 0, 0, sm, PlanarImage.createColorModel(sm));
            // Setting of the final image
            jb.dest("dest", img);

            // Finally we run the script and retrieve the resulting image.
            jb.run();
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
}
