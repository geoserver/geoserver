/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import com.thoughtworks.xstream.XStream;

/**
 * Represents the ogr2ogr output format configuration as a whole.
 * Only used for XStream driven de-serialization 
 * @author Andrea Aime - OpenGeo

 */
class OgrConfiguration {
    public static final OgrConfiguration DEFAULT;
    static {
        DEFAULT = new OgrConfiguration();
        // assume it's in the classpath and GDAL_DATA is properly set in the enviroment
        DEFAULT.ogr2ogrLocation = "ogr2ogr";
        // add some default formats
        DEFAULT.formats = new OgrFormat[] {
                new OgrFormat("MapInfo File", "OGR-TAB", ".tab", false, null),
                new OgrFormat("MapInfo File", "OGR-MIF", ".mif", false, null, "-dsco", "FORMAT=MIF"),
                new OgrFormat("CSV", "OGR-CSV", ".csv", true, "text/csv"),
                new OgrFormat("KML", "OGR-KML", ".kml", true, "application/vnd.google-earth.kml"),
        };
    }
    
    String ogr2ogrLocation;
    String gdalData;
    OgrFormat[] formats;
    
    public static void main(String[] args) {
        // generates the default configuration xml and prints it to the output
        XStream xstream = Ogr2OgrConfigurator.buildXStream();
        System.out.println(xstream.toXML(OgrConfiguration.DEFAULT));
    }
}
