/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.Geometry;

/**
 * Inputs and outputs feature collections in GeoJSON format using gt-geojson
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class GeoJSONPPIO extends CDataPPIO {

    static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        JtsModule module = new JtsModule(6);
        MAPPER.registerModule(module);
    }

    GeoServer gs;

    protected GeoJSONPPIO(Class<?> clazz) {
        super(clazz, clazz, "application/json");
        this.gs = (GeoServer) GeoServerExtensions.bean("geoServer");
    }

    protected GeoJSONPPIO(Class<?> clazz, GeoServer gs) {
        super(clazz, clazz, "application/json");
        this.gs = gs;
    }

    @Override
    public abstract void encode(Object value, OutputStream os) throws IOException;

    @Override
    public abstract Object decode(InputStream input) throws Exception;

    @Override
    public abstract Object decode(String input) throws Exception;

    @Override
    public final String getFileExtension() {
        return "json";
    }

    public static class FeatureCollections extends GeoJSONPPIO {
        public FeatureCollections() {
            super(FeatureCollection.class);
        }

        protected FeatureCollections(GeoServer gs) {
            super(FeatureCollection.class, gs);
        }

        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            int decimals = gs.getSettings().getNumDecimals();
            try (GeoJSONWriter writer = new GeoJSONWriter(os)) {
                writer.setMaxDecimals(decimals);
                writer.writeFeatureCollection((SimpleFeatureCollection) value);
            }
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            try (GeoJSONReader reader = new GeoJSONReader(input)) {
                return reader.getFeatures();
            }
        }

        @Override
        public Object decode(String input) throws Exception {
            try (GeoJSONReader reader = new GeoJSONReader(input)) {
                return reader.getFeatures();
            }
        }
    }

    public static class Geometries extends GeoJSONPPIO {
        public Geometries() {
            super(Geometry.class);
        }

        protected Geometries(GeoServer gs) {
            super(Geometry.class, gs);
        }

        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            int decimals = gs.getSettings().getNumDecimals();
            ObjectMapper mapper = getMapper(decimals);
            mapper.writeValue(os, value);
        }

        private ObjectMapper getMapper(int decimals) {
            if (decimals == 6) {
                return MAPPER;
            }
            ObjectMapper mapper = new ObjectMapper();
            JtsModule module = new JtsModule(decimals);
            mapper.registerModule(module);
            return mapper;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            return MAPPER.readValue(input, Geometry.class);
        }

        @Override
        public Object decode(String input) throws Exception {
            return MAPPER.readValue(input, Geometry.class);
        }
    }
}
