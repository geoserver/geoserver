package org.geoserver.bxml.gml_3_1;

import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.bxml.AbstractEncoder;
import org.geoserver.bxml.Context;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.gvsig.bxml.geoserver.EncoderConfig;
import org.gvsig.bxml.geoserver.Gml3Encoder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;

public abstract class AbstractGMLEncoder<T> extends AbstractEncoder<T> {

    protected static final Logger LOGGER = Logging.getLogger(AbstractGMLEncoder.class.getPackage()
            .getName());

    private Gml3Encoder gmlEncoder;

    public AbstractGMLEncoder() {
        super();
    }

    protected Gml3Encoder getGmlEncoder() {
        if (gmlEncoder == null) {
            EncoderConfig config;
            try {
                config = Context.get(EncoderConfig.class);
            } catch (NoSuchElementException e) {
                config = GeoServerExtensions.bean(EncoderConfig.class);
                Preconditions.checkState(config != null, EncoderConfig.class.getName()
                        + " not provided through " + Context.class.getName()
                        + " not GeoServerExtensions");
            }
            gmlEncoder = new Gml3Encoder(config);
        }
        return gmlEncoder;
    }

    protected final CoordinateReferenceSystem guessCRS(final Geometry geometry) {

        final Object userData = geometry.getUserData();
        CoordinateReferenceSystem crs = null;
        if (userData instanceof CoordinateReferenceSystem) {
            crs = (CoordinateReferenceSystem) userData;
        } else if (userData instanceof String) {
            String mayBeACrsId = (String) userData;
            try {
                crs = CRS.decode(mayBeACrsId);
            } catch (Exception e) {
                // looks like it wasn't
                crs = null;
            }
        }
        if (crs == null && geometry.getSRID() > 0) {
            // may it have been set as a srid property?
            try {
                CRS.decode("urn:ogc:def:crs:EPSG::" + geometry.getSRID());
            } catch (Exception e) {
                // bad luck
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Geometry has srid property = " + geometry.getSRID()
                            + " but it coudln't be parsed to a CoordinateReferenceSystem", e);
                }
            }
        }
        if (crs == null) {
            // fallback to default value
            LOGGER.fine("Geometry contains no CRS information, defaulting to urn:x-ogc:def:crs:EPSG:4326");
            try {
                CRS.decode("urn:x-ogc:def:crs:EPSG:4326");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return crs;
    }

}