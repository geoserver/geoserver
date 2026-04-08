/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat;

import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Generic;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Max;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Min;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.MinMax;
import static it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldUsage.Name;

import it.geosolutions.imageio.pam.PAMDataset;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.util.logging.Logging;

/** Provides access to the PAMDataset for a coverage and support to extract {@link RasterAttributeTable} from it. */
public class CoverageRATs {

    static final Logger LOGGER = Logging.getLogger(CoverageRATs.class);

    private Catalog catalog;
    private final PAMDataset pamDataset;
    private CoverageInfo coverage;

    public CoverageRATs(Catalog catalog, CoverageInfo coverage) {
        this.catalog = catalog;
        this.coverage = coverage;
        this.pamDataset = getPAMDataset(coverage);
    }

    /**
     * Constructor for test usage only, some methods will not work
     *
     * @param pamDataset
     */
    protected CoverageRATs(PAMDataset pamDataset) {
        this.pamDataset = pamDataset;
    }

    /**
     * Returns the full PAM dataset for the coverage, or null if none was available.
     *
     * @return
     */
    public PAMDataset getPAMDataset() {
        return pamDataset;
    }

    /** Returns a PAMDataset, if available in the given coverage. */
    private PAMDataset getPAMDataset(CoverageInfo coverage) {
        try {
            // grab the reader
            GridCoverageReader reader = coverage.getGridCoverageReader(null, null);
            if (!(reader instanceof GridCoverage2DReader)) return null;

            // see if it can provide coverage info
            GridCoverage2DReader reader2D = (GridCoverage2DReader) reader;
            ResourceInfo info = reader2D.getInfo(coverage.getNativeCoverageName());
            if (!(info instanceof PAMResourceInfo)) return null;

            return ((PAMResourceInfo) info).getPAMDataset();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Read failed while attempting to check if reader has a PAMDataset", e);
        }
        return null;
    }

    /** Converts the PAM dataset to XML */
    public String toXML() {
        try {
            JAXBContext ctx = JAXBContext.newInstance("it.geosolutions.imageio.pam");
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(pamDataset, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOGGER.log(Level.SEVERE, "Failed to write the final PAM file", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link RasterAttributeTable} for the given band, or {@code null} if no RAT is available for the band.
     *
     * @param bandIdx
     * @return
     */
    public RasterAttributeTable getRasterAttributeTable(int bandIdx) {
        List<PAMDataset.PAMRasterBand> bands = pamDataset.getPAMRasterBand();
        if (bands == null || bands.size() < bandIdx)
            throw new IllegalArgumentException("Band " + bandIdx + " not found");

        PAMDataset.PAMRasterBand band = bands.get(bandIdx);
        PAMDataset.PAMRasterBand.GDALRasterAttributeTable rat = band.getGdalRasterAttributeTable();
        if (rat == null) {
            LOGGER.fine("No raster attribute table found for band " + bandIdx);
            return null;
        }

        List<PAMDataset.PAMRasterBand.FieldDefn> fields = rat.getFieldDefn();
        if (fields == null || fields.isEmpty()) {
            LOGGER.fine("No field definitions found in RAT for band " + bandIdx);
            return null;
        }

        Set<PAMDataset.PAMRasterBand.FieldUsage> fieldUsages =
                fields.stream().map(f -> f.getUsage()).collect(Collectors.toSet());

        if (!fieldUsages.contains(Name) && !fieldUsages.contains(Generic)) {
            LOGGER.fine("No classification names and no generic fields found in RAT for band " + bandIdx);
            return null;
        }

        boolean valueClassification = fieldUsages.contains(MinMax);
        boolean rangeClassification = fieldUsages.contains(Min) && fieldUsages.contains(Max);
        if (!valueClassification && !rangeClassification) {
            LOGGER.fine("No value columns found in RAT for band " + bandIdx);
            return null;
        }

        if (valueClassification) {
            return new RasterAttributeTable.Recode(rat, bandIdx);
        } else {
            return new RasterAttributeTable.Categorize(rat, bandIdx);
        }
    }

    /**
     * Returns a style in the same workspace as the coverage, with the given name, or {@code null}
     *
     * @param name
     * @return
     */
    public StyleInfo getCoverageStyle(String name) {
        WorkspaceInfo workspace = coverage.getStore().getWorkspace();
        return catalog.getStyleByName(workspace, name);
    }

    /** Adds a style with the given name into the catalog, or updates a same named style otherwise */
    public StyleInfo saveStyle(Style style, String name) throws IOException {
        StyleInfo si = getCoverageStyle(name);
        if (si == null) {
            si = catalog.getFactory().createStyle();
            si.setName(name);
            si.setWorkspace(coverage.getStore().getWorkspace());
            si.setFilename(name + ".sld");
            si.setFormat("sld");
            catalog.add(si);
        } else {
            LOGGER.warning("Overwriting existing style: " + si.prefixedName());
        }

        catalog.getResourcePool().writeStyle(si, style, true);
        return si;
    }

    /** Returns the default style name for the given band and classification */
    public String getDefaultStyleName(int band, String classification) {
        return coverage.getName() + "_b" + band + "_" + classification;
    }
}
