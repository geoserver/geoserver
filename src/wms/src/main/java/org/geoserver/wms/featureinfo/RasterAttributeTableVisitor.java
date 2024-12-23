/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.RasterLayerIdentifier.INCLUDE_RAT;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.geoserver.wms.map.RasterSymbolizerVisitor;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.ChannelSelection;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.SelectedChannelType;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.util.logging.Logging;

/**
 * Extracts the first attribute table from raster symbolizers that expose the {@link RasterLayerIdentifier#INCLUDE_RAT}
 * vendor option, and whose selected channels actually have a RAT in the reader PAM dataset.
 */
class RasterAttributeTableVisitor extends RasterSymbolizerVisitor {

    static final Logger LOGGER = Logging.getLogger(RasterAttributeTableVisitor.class);
    private final GridCoverage2DReader reader;
    private final String coverageName;

    public RasterAttributeTableVisitor(double scaleDenominator, String nativeName, GridCoverage2DReader reader) {
        super(scaleDenominator, null);
        this.reader = reader;
        this.coverageName = nativeName;
    }

    /**
     * Builds an attribute
     *
     * @return
     */
    public AttributeTableEnricher getAttributeTableEnricher() {
        return Optional.ofNullable(getAttributeTableBand())
                .map(b -> new AttributeTableEnricher(b))
                .orElse(null);
    }

    /**
     * Returns an attribute tables, from the first raster symbolizers that are active at the current scale denominator,
     * and that expose the {@link RasterLayerIdentifier#INCLUDE_RAT} vendor option. In normal conditions there would be
     * just one, but if there are various raster sybolizers or various channel selections, all the mappings found are
     * returned.
     *
     * @return The
     */
    private PAMRasterBand getAttributeTableBand() {
        List<PAMRasterBand> pamRasterBands = null;
        for (RasterSymbolizer rs : getRasterSymbolizers()) {
            boolean addAttributeTable = Boolean.valueOf(rs.getOptions().getOrDefault(INCLUDE_RAT, "false"));
            if (addAttributeTable) {
                // lazy load PAM dataset, but return early if it's not found
                if (pamRasterBands == null) {
                    ResourceInfo info = reader.getInfo(coverageName);
                    if (!(info instanceof PAMResourceInfo)) {
                        LOGGER.fine("No PAM dataset found in raster attribute table, even if "
                                + INCLUDE_RAT
                                + " is set to true.");
                        return null;
                    }
                    PAMResourceInfo pamInfo = (PAMResourceInfo) info;
                    PAMDataset pam = pamInfo.getPAMDataset();
                    // bail out early also if there are no PAM bands
                    if (pam == null
                            || pam.getPAMRasterBand() == null
                            || pam.getPAMRasterBand().isEmpty()) {
                        LOGGER.fine(
                                "No Raster bands found in PAM dataset, even if " + INCLUDE_RAT + " is set to true.");
                        return null;
                    }
                    pamRasterBands = pam.getPAMRasterBand();
                }

                ChannelSelection cs = rs.getChannelSelection();
                if (cs == null || (cs.getGrayChannel() == null && cs.getRGBChannels() == null)) {
                    // no channel selection, use the first band
                    PAMRasterBand band = pamRasterBands.get(0);
                    if (band.getGdalRasterAttributeTable() != null) return band;
                } else if (cs.getGrayChannel() != null) {
                    Expression channelName = cs.getGrayChannel().getChannelName();
                    PAMRasterBand band = getBandWithRAT(channelName, pamRasterBands);
                    if (band != null && band.getGdalRasterAttributeTable() != null) return band;
                    else
                        LOGGER.fine("No RAT found in PAM dataset for the gray channel even if "
                                + INCLUDE_RAT
                                + " is set to true.");

                } else if (cs.getRGBChannels() != null && cs.getRGBChannels().length > 0) {
                    for (SelectedChannelType sct : cs.getRGBChannels()) {
                        Expression channelName = sct.getChannelName();
                        PAMRasterBand band = getBandWithRAT(channelName, pamRasterBands);
                        if (band != null && band.getGdalRasterAttributeTable() != null) return band;
                    }
                    LOGGER.fine("No RAT found in PAM dataset for any of the RGB channels"
                            + " even if "
                            + INCLUDE_RAT
                            + " is set to true.");
                }
            }
        }

        // no RAT found
        return null;
    }

    /**
     * Returns PAMRasterBand, ensuring it has a valid index, or <code>null</code>, if the channel name does not resolve
     * to a valid band index, or if the band does not have a valid RAT.
     */
    private static PAMRasterBand getBandWithRAT(Expression channelName, List<PAMRasterBand> pamRasterBands) {
        Integer bandIdx = channelName.evaluate(null, Integer.class);
        if (bandIdx != null) {
            bandIdx--;
            if (bandIdx >= 0
                    && bandIdx < pamRasterBands.size()
                    && pamRasterBands.get(bandIdx).getGdalRasterAttributeTable() != null) {
                PAMRasterBand band = pamRasterBands.get(bandIdx);
                if (band.getBand() == null) band.setBand(bandIdx);
                return band;
            }
        }

        return null;
    }
}
