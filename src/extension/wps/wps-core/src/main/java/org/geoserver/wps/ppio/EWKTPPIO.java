/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;

/**
 * Used to represent geometries in EWKT format Reads geometry and crs from string and sets this crs
 * to created geometry
 */
public class EWKTPPIO extends CDataPPIO {

    private static final WKTReader2 reader = new WKTReader2();

    private static final Pattern SRID_REGEX = Pattern.compile("SRID=[0-9].*");

    public EWKTPPIO() {
        super(Geometry.class, Geometry.class, "application/ewkt");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }

    @Override
    public Object decode(String input) throws Exception {
        String[] wktContents = input.split(";");
        Geometry geom = reader.read(wktContents[wktContents.length - 1]);
        // parse SRID if passed
        // looking for a pattern srid=4326:LineString(...)
        if (geom == null) {
            throw new IllegalArgumentException("Input should contain geometry");
        }
        CoordinateReferenceSystem geomCRS = null;
        if (wktContents.length == 2 && SRID_REGEX.matcher(wktContents[0].toUpperCase()).matches()) {
            String sridString = wktContents[0].split("=")[1];
            geomCRS = CRS.decode("EPSG:" + sridString, true);
        }
        if (geomCRS != null) {
            geom.setUserData(geomCRS);
        }
        return geom;
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Writer w = new OutputStreamWriter(os);
        try {
            Geometry g = (Geometry) value;
            new WKTWriter().write(g, w);
        } finally {
            w.flush();
        }
    }

    @Override
    public String getFileExtension() {
        return "ewkt";
    }
}
