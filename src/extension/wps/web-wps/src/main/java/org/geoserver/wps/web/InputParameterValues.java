/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.CoordinateReferenceSystemPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.ProcessFactory;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.Name;

/**
 * Contains the set of values for a single parameter. For most input parameters it will be just one
 * value actually
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class InputParameterValues implements Serializable {
    public enum ParameterType {
        LITERAL,
        TEXT,
        VECTOR_LAYER,
        RASTER_LAYER,
        REFERENCE,
        SUBPROCESS;
    };

    Name processName;

    String paramName;

    List<ParameterValue> values = new ArrayList<ParameterValue>();

    public InputParameterValues(Name processName, String paramName) {
        this.processName = processName;
        this.paramName = paramName;
        Parameter<?> p = getParameter();
        final ParameterType type = guessBestType();
        final String mime = getDefaultMime();
        for (int i = 0; i < Math.max(1, p.minOccurs); i++) {
            values.add(new ParameterValue(type, mime, null));
        }
    }

    private ParameterType guessBestType() {
        if (!isComplex()) return ParameterType.LITERAL;
        if (FeatureCollection.class.isAssignableFrom(getParameter().type)) {
            return ParameterType.VECTOR_LAYER;
        } else if (GridCoverage2D.class.isAssignableFrom(getParameter().type)) {
            return ParameterType.RASTER_LAYER;
        } else {
            return ParameterType.TEXT;
        }
    }

    public List<ParameterType> getSupportedTypes() {
        if (!isComplex()) {
            return Collections.singletonList(ParameterType.LITERAL);
        } else {
            Set<ParameterType> result = new LinkedHashSet<ParameterType>();
            result.add(ParameterType.TEXT);
            result.add(ParameterType.REFERENCE);
            result.add(ParameterType.SUBPROCESS);
            for (ProcessParameterIO ppio : getProcessParameterIO()) {
                if (FeatureCollection.class.isAssignableFrom(ppio.getType())) {
                    result.add(ParameterType.VECTOR_LAYER);
                } else if (GridCoverage.class.isAssignableFrom(ppio.getType())) {
                    result.add(ParameterType.RASTER_LAYER);
                }
            }
            return new ArrayList<ParameterType>(result);
        }
    }

    String getDefaultMime() {
        if (!isComplex()) {
            return null;
        } else {
            return ((ComplexPPIO) getProcessParameterIO().get(0)).getMimeType();
        }
    }

    public List<String> getSupportedMime() {
        List<String> results = new ArrayList<String>();
        for (ProcessParameterIO ppio : getProcessParameterIO()) {
            ComplexPPIO cp = (ComplexPPIO) ppio;
            results.add(cp.getMimeType());
        }
        return results;
    }

    public boolean isEnum() {
        return Enum.class.isAssignableFrom(getParameter().type);
    }

    public boolean isComplex() {
        List<ProcessParameterIO> ppios = getProcessParameterIO();
        return ppios.size() > 0 && ppios.get(0) instanceof ComplexPPIO;
    }

    public boolean isBoundingBox() {
        List<ProcessParameterIO> ppios = getProcessParameterIO();
        return ppios.size() > 0 && ppios.get(0) instanceof BoundingBoxPPIO;
    }

    public boolean isCoordinateReferenceSystem() {
        List<ProcessParameterIO> ppios = getProcessParameterIO();
        return ppios.size() > 0 && ppios.get(0) instanceof CoordinateReferenceSystemPPIO;
    }

    List<ProcessParameterIO> getProcessParameterIO() {
        return ProcessParameterIO.findAll(getParameter(), null);
    }

    ProcessFactory getProcessFactory() {
        return GeoServerProcessors.createProcessFactory(processName, false);
    }

    Parameter<?> getParameter() {
        return getProcessFactory().getParameterInfo(processName).get(paramName);
    }

    /** A single value, along with the chosen editor and its mime type */
    static class ParameterValue implements Serializable {
        ParameterType type;

        String mime;

        Serializable value;

        public ParameterValue(ParameterType type, String mime, Serializable value) {
            this.type = type;
            this.mime = mime;
            this.value = value;
        }

        public ParameterType getType() {
            return type;
        }

        public void setType(ParameterType type) {
            this.type = type;
        }

        public String getMime() {
            return mime;
        }

        public void setMime(String mime) {
            this.mime = mime;
        }

        public Serializable getValue() {
            return value;
        }

        public void setValue(Serializable value) {
            this.value = value;
        }
    }
}
