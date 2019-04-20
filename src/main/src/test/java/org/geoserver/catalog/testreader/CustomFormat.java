/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.testreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;

/**
 * Coverage format for custom dimensions tests.
 *
 * @author Mike Benowitz
 */
public final class CustomFormat extends AbstractGridFormat {

    public static final String CUSTOM_DIMENSION_NAME = "MY_DIMENSION";
    private static final String TYPE_NAME = "org.geoserver.catalog.testreader.CustomFormat";

    @SuppressWarnings("rawtypes")
    private static final ParameterDescriptor<List> CUSTOM_DIMENSION =
            DefaultParameterDescriptor.create(
                    CUSTOM_DIMENSION_NAME,
                    "Optional list of nonstandard dimension values",
                    List.class,
                    null,
                    false);

    public CustomFormat() {
        this.mInfo = new HashMap<String, String>();
        this.mInfo.put("name", TYPE_NAME);
        this.mInfo.put(
                "description",
                "Test custom coverage format - only visible with test jars in the classpath");
        this.mInfo.put("docURL", "");
        this.mInfo.put("version", "1.0");

        // writing parameters
        this.writeParameters = null;

        // reading parameters
        this.readParameters =
                new ParameterGroup(
                        new DefaultParameterDescriptorGroup(
                                this.mInfo,
                                new GeneralParameterDescriptor[] {
                                    READ_GRIDGEOMETRY2D, INPUT_TRANSPARENT_COLOR, CUSTOM_DIMENSION
                                }));
    }

    @Override
    public boolean accepts(Object source) {
        return accepts(source, null);
    }

    @Override
    public boolean accepts(Object source, Hints hints) {
        if (!(source instanceof File)) {
            return false;
        }
        File dir = (File) source;
        if (dir.isDirectory()) {
            // Look for datastore.properties file with 'type' property
            // specifying this format
            File propertiesFile = new File(dir, "datastore.properties");
            if (propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                    props.load(fis);
                    return TYPE_NAME.equalsIgnoreCase(props.getProperty("type"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object source) {
        return getReader(source, null);
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object source, Hints hints) {
        try {
            return new CustomFormatReader(source, hints);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GridCoverageWriter getWriter(Object source) {
        return getWriter(source, null);
    }

    @Override
    public GridCoverageWriter getWriter(Object source, Hints hints) {
        throw new UnsupportedOperationException("This plugin does not support writing");
    }
}
