/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import it.geosolutions.imageio.pam.PAMDataset;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.ResourceInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.data.DefaultResourceInfo;

/** A ResourceInfo wrapper for CoverageViews that provides access to the PAMDataset */
public class CoverageViewPamResourceInfo extends DefaultResourceInfo implements PAMResourceInfo {
    private final PAMDataset viewPam;

    /**
     * Constructor that populates the PAM dataset from the view bands
     *
     * @param viewInputs input alpha non-null coverages
     */
    public CoverageViewPamResourceInfo(CoverageViewReader.ViewInputs viewInputs) {
        viewPam = new PAMDataset();
        populatePAMFromViewBands(viewInputs, viewPam);
    }

    @Override
    public PAMDataset getPAMDataset() {
        return viewPam;
    }

    @Override
    public boolean reloadPAMDataset() throws IOException {
        return PAMResourceInfo.super.reloadPAMDataset();
    }

    /**
     * Populates the PAM dataset from the view bands
     *
     * @param info convenience object to access the coverage view input readers
     * @param viewPam the PAM dataset to populate
     */
    private void populatePAMFromViewBands(CoverageViewReader.ViewInputs info, PAMDataset viewPam) {
        Map<String, GridCoverageReader> readers = info.getInputReaders();
        // iterate over the view bands and populate the PAM dataset
        for (CoverageView.CoverageBand band : info.getBands()) {
            for (CoverageView.InputCoverageBand inputCoverageBand : band.getInputCoverageBands()) {
                // get the reader for the coverage associated with this input coverage band
                GridCoverageReader reader = readers.get(inputCoverageBand.getCoverageName());
                if (reader instanceof GridCoverage2DReader) {
                    GridCoverage2DReader bandReader = (GridCoverage2DReader) reader;
                    ResourceInfo resourceInfoBand = bandReader.getInfo(inputCoverageBand.getCoverageName());
                    // reader is associated with a PAM
                    if (resourceInfoBand instanceof PAMResourceInfo) {
                        PAMDataset bandPam = ((PAMResourceInfo) resourceInfoBand).getPAMDataset();
                        if (bandPam != null) {
                            List<PAMDataset.PAMRasterBand> pamRasterBands = bandPam.getPAMRasterBand();
                            // find the PAMRasterBand for the given band index and put in the output
                            Optional<PAMDataset.PAMRasterBand> pamRasterBandOptional =
                                    getPAMRasterBandByBandIndex(pamRasterBands, inputCoverageBand.getBand());
                            pamRasterBandOptional.ifPresent(
                                    pamRasterBand -> viewPam.getPAMRasterBand().add(pamRasterBand));
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the PAMRasterBand for the given band index, or {@code null} if not found
     *
     * @param pamRasterBands PAM raster bands
     * @param band band index to search for
     * @return PAMRasterBand for the given band index, or {@code null} if not found
     */
    private Optional<PAMDataset.PAMRasterBand> getPAMRasterBandByBandIndex(
            List<PAMDataset.PAMRasterBand> pamRasterBands, String band) {
        for (PAMDataset.PAMRasterBand pamRasterBand : pamRasterBands) {
            // PAM bands are 1-based, so we need to adjust the band index
            Integer adjustedBand = pamRasterBand.getBand() - 1;
            if (adjustedBand.toString().equals(band)) {
                return Optional.of(pamRasterBand);
            }
        }
        return Optional.empty();
    }
}
