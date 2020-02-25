/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.staticRasterStore;

import java.util.HashMap;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;

/** Format class for the static raster reader. */
final class StaticRasterFormat extends AbstractGridFormat implements Format {

    // add filtering capabilities
    public static final ParameterDescriptor<Filter> FILTER =
            new DefaultParameterDescriptor<>("Filter", Filter.class, null, null);

    StaticRasterFormat() {
        setInfo();
        // reader capabilities
        readParameters =
                new ParameterGroup(
                        new DefaultParameterDescriptorGroup(
                                mInfo,
                                new GeneralParameterDescriptor[] {
                                    AbstractGridFormat.READ_GRIDGEOMETRY2D, FILTER
                                }));
    }

    private void setInfo() {
        HashMap<String, String> info = new HashMap<>();
        info.put("name", "StaticRaster");
        info.put("description", "Static raster store");
        info.put("vendor", "Geotools");
        info.put("docURL", "http://geotools.org/");
        info.put("version", "1.0");
        mInfo = info;
    }

    @Override
    public StaticRasterReader getReader(Object source) {
        // we just create the reader with no hints
        return getReader(source, null);
    }

    @Override
    public StaticRasterReader getReader(Object source, Hints hints) {
        return new StaticRasterReader(source);
    }

    @Override
    public boolean accepts(Object input, Hints hints) {
        // we don't need anything here
        return false;
    }

    @Override
    public ParameterValueGroup getReadParameters() {
        // this will return the read parameters we setup in the constructor
        return super.getReadParameters();
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public GridCoverageWriter getWriter(Object destination) {
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
