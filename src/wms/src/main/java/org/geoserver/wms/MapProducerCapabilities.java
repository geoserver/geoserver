/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

/**
 * Class to record capabilities for a {@link RasterMapProducer}.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class MapProducerCapabilities {

    private final boolean tiledRequestsSupported;

    private final boolean paletteSupported;

    private final boolean transparencySupported;

    public MapProducerCapabilities(
            boolean tiledRequestsSupported,
            boolean paletteSupported,
            boolean transparencySupported) {
        super();
        this.tiledRequestsSupported = tiledRequestsSupported;
        this.paletteSupported = paletteSupported;
        this.transparencySupported = transparencySupported;
    }

    /** If the map producer can be used in a meta-tiling context */
    public boolean isTiledRequestsSupported() {
        return tiledRequestsSupported;
    }

    /** Returns true if paletted images are supported */
    public boolean isPaletteSupported() {
        return paletteSupported;
    }

    /** Returns true if background transparency is supported */
    public boolean isTransparencySupported() {
        return transparencySupported;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (paletteSupported ? 1231 : 1237);
        result = prime * result + (tiledRequestsSupported ? 1231 : 1237);
        result = prime * result + (transparencySupported ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MapProducerCapabilities)) {
            return false;
        }
        MapProducerCapabilities other = (MapProducerCapabilities) obj;
        if (paletteSupported != other.paletteSupported) {
            return false;
        }
        if (tiledRequestsSupported != other.tiledRequestsSupported) {
            return false;
        }
        if (transparencySupported != other.transparencySupported) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MapProducerCapabilities [framesMimeType="
                + paletteSupported
                + ", tiledRequestsSupported="
                + tiledRequestsSupported
                + ", transparencySupported="
                + transparencySupported
                + "]";
    }
}
