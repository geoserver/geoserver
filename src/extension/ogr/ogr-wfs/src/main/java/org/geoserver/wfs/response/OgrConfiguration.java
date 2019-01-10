/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import com.thoughtworks.xstream.XStream;
import java.util.Map;
import org.geoserver.ogr.core.Format;
import org.geoserver.ogr.core.OutputType;
import org.geoserver.ogr.core.ToolConfiguration;

/**
 * Represents the ogr2ogr output format configuration as a whole. Only used for XStream driven
 * de-serialization
 *
 * @author Andrea Aime - OpenGeo
 * @author Stefano Costa - GeoSolutions
 */
public class OgrConfiguration extends ToolConfiguration {

    public static final OgrConfiguration DEFAULT;

    static {
        DEFAULT = new OgrConfiguration();
        // assume it's in the classpath and GDAL_DATA is properly set in the enviroment
        DEFAULT.ogr2ogrLocation = "ogr2ogr";
        // add some default formats
        DEFAULT.formats =
                new OgrFormat[] {
                    new OgrFormat("MapInfo File", "OGR-TAB", ".tab", false, null),
                    new OgrFormat(
                            "MapInfo File", "OGR-MIF", ".mif", false, null, "-dsco", "FORMAT=MIF"),
                    new OgrFormat("CSV", "OGR-CSV", ".csv", true, "text/csv", OutputType.TEXT),
                    new OgrFormat(
                            "KML",
                            "OGR-KML",
                            ".kml",
                            true,
                            "application/vnd.google-earth.kml",
                            OutputType.XML),
                };
    }

    public String ogr2ogrLocation;
    public String gdalData;

    /** Ensures compatibility with old style configuration files. */
    @Override
    public String getExecutable() {
        if (ogr2ogrLocation != null) {
            return ogr2ogrLocation;
        } else {
            return executable;
        }
    }

    /** Ensures compatibility with old style configuration files. */
    @Override
    public Map<String, String> getEnvironment() {
        if (gdalData != null) {
            return java.util.Collections.singletonMap("GDAL_DATA", gdalData);
        } else {
            return environment;
        }
    }

    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) {
        // generates the default configuration xml and prints it to the output
        XStream xstream = new XStream();
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.addImplicitCollection(Format.class, "options", "option", String.class);

        System.out.println(xstream.toXML(OgrConfiguration.DEFAULT));
    }
}
