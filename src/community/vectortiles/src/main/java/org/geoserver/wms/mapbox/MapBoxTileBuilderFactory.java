package org.geoserver.wms.mapbox;

import java.awt.Rectangle;
import java.util.Set;

import org.geoserver.wms.vector.VectorTileBuilder;
import org.geoserver.wms.vector.VectorTileBuilderFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.google.common.collect.ImmutableSet;

/**
 * 
 * @author Niels Charlier
 *
 */
public class MapBoxTileBuilderFactory implements VectorTileBuilderFactory {
        
    public static final String MIME_TYPE = "application/x-vtile";

    public static final Set<String> OUTPUT_FORMATS = ImmutableSet.of(MIME_TYPE, "pbf");
    
    private boolean forceCrs;    

    public boolean isForceCrs() {
        return forceCrs;
    }

    public void setForceCrs(boolean forceCrs) {
        this.forceCrs = forceCrs;
    }

    @Override
    public Set<String> getOutputFormats() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public VectorTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea) {
       return new MapBoxTileBuilder(screenSize, mapArea, forceCrs);
    }

}
