package org.geoserver.gss.impl.query;

import org.geoserver.ows.KvpParser;
import org.geotools.geometry.jts.WKTReader2;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Parses the {@code GEOM} parameter from a Geometry WKT representation to a JTS Geometry.
 * 
 * @author groldan
 * 
 */
public class GeomKvpParser extends KvpParser {

    public GeomKvpParser() {
        super("GEOM", Geometry.class);
    }

    @Override
    public Geometry parse(final String value) throws Exception {
        Geometry geometry = new WKTReader2().read(value);
        return geometry;
    }

}
