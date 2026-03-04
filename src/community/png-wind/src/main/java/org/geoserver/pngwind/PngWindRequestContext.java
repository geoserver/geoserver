package org.geoserver.pngwind;

import java.util.List;
import java.util.Objects;

import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Envelope;

/**
 * Request-scoped helper holding validated raster-layer information needed by PNG-WIND.
 *
 * Responsibilities:
 *  - Validate getRequest constraints (single layer, raster, exactly 2 bands)
 *  - Expose CoverageInfo and CoverageDimensionInfo for getMin/getMax/getUom extraction
 *
 * This is reusable from both:
 *  - PngWindMapOutputFormat (produceMap)
 *  - PngWindMapResponse (writeTo/encode)
 */
public final class PngWindRequestContext {

    private final GetMapRequest request;
    private final CoverageInfo coverageInfo;
    private Envelope envelope;
    private final List<CoverageDimensionInfo> dimensions;

    private final BandInfo band1;
    private final BandInfo band2;

    private PngWindRequestContext(
            GetMapRequest request,
            CoverageInfo coverageInfo,
            List<CoverageDimensionInfo> dimensions) {

        this.request = Objects.requireNonNull(request, "getRequest");
        this.coverageInfo = Objects.requireNonNull(coverageInfo, "getCoverageInfo");
        this.dimensions = List.copyOf(Objects.requireNonNull(dimensions, "getDimensions"));

        this.band1 = BandInfo.from(dimensions.get(0));
        this.band2 = BandInfo.from(dimensions.get(1));
    }

    /**
     * Factory: validates and builds the context.
     *
     * Validates:
     *  1) Single requested layer
     *  2) That layer is a raster coverage
     *  3) Coverage has exactly 2 bands
     *
     * Throws ServiceException if not supported.
     */
    public static PngWindRequestContext from(GetMapRequest request) {
        Objects.requireNonNull(request, "getRequest");

        // 1) Exactly one layer requested
        if (request.getLayers() == null || request.getLayers().size() != 1) {
            throw unsupported("requires exactly one raster layer (single-layer GetMap).");
        }

        final Object layerObj = request.getLayers().get(0);

        // 2) Must be a raster layer (coverage)
        final LayerInfo layerInfo = asLayerInfo(layerObj);
        if (layerInfo == null) {
            // Could be a LayerGroupInfo or something else; reject per requirements.
            throw unsupported("requires a single published raster LayerInfo (not a layer group).");
        }

        final ResourceInfo resource = layerInfo.getResource();
        if (!(resource instanceof CoverageInfo coverageInfo)) {
            throw unsupported("requires a raster layer (coverage).");
        }

        // 3) Must have exactly 2 bands (based on configured getDimensions)
        final List<CoverageDimensionInfo> dims;
        try {
            dims = coverageInfo.getDimensions();
        } catch (Exception e) {
            throw unsupported("Unable to read coverage getDimensions for layer '" + coverageInfo.getName()
                    + "': " + e.getMessage());
        }

        if (dims == null || dims.size() != 2) {
            int found = dims == null ? 0 : dims.size();
            throw unsupported("requires a 2-band raster. Found " + found + " band(s).");
        }

        return new PngWindRequestContext(request, coverageInfo, dims);
    }

    public void setBounds(ReferencedEnvelope envelope) {
        this.envelope = envelope;
    }

    /** Map getRequest (original). */
    public GetMapRequest getRequest() {
        return request;
    }

    /** The validated GeoServer CoverageInfo. */
    public CoverageInfo getCoverageInfo() {
        return coverageInfo;
    }

    /** The configured coverage getDimensions (exactly 2). */
    public List<CoverageDimensionInfo> getDimensions() {
        return dimensions;
    }

    /** Convenience accessor for band 1 metadata (getName/getMin/getMax/getUom). */
    public BandInfo band1() {
        return band1;
    }

    /** Convenience accessor for band 2 metadata (getName/getMin/getMax/getUom). */
    public BandInfo band2() {
        return band2;
    }

    public Envelope getEnvelope() {return envelope;}

    /**
     * What PNG-WIND needs from CoverageDimensionInfo to compute quantization metadata.
     *
     * NOTE: CoverageDimensionInfo getMin/getMax fields are often configured in GeoServer UI.
     * Some stores may not populate them; keep your usage defensive.
     */
    public static final class BandInfo {
        private final String name;
        private final Double min;
        private final Double max;
        private final Double noData;
        private final String uom;

        private BandInfo(String name, Double min, Double max, Double noData, String uom) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.uom = uom;
            this.noData = noData;
        }

        public static BandInfo from(CoverageDimensionInfo dim) {
            String name = dim.getName();
            NumberRange<? extends Number> dimensionRange = dim.getRange();
            Double min = dimensionRange != null ? dimensionRange.getMinimum() : null;
            Double max = dimensionRange != null ? dimensionRange.getMaximum() : null;
            String uom = dim.getUnit();
            List<Double> nullValues = dim.getNullValues();
            Double noData = nullValues != null && !nullValues.isEmpty() ? nullValues.get(0) : null;
            return new BandInfo(name, min, max, noData, uom);
        }

        public String getName() {
            return name;
        }

        /** Nullable: may be missing depending on layer configuration. */
        public Double getMin() {
            return min;
        }

        /** Nullable: may be missing depending on layer configuration. */
        public Double getMax() {
            return max;
        }

        /** Nullable: may be missing depending on layer configuration. */
        public String getUom() {
            return uom;
        }

        /** Nullable: may be missing depending on layer configuration. */
        public Double getNodata() {
            return noData;
        }

        public void requireMinMax() {
            if (min == null || max == null) {
                throw unsupported("CoverageDimensionInfo for band '" + name + "' is missing getMin/getMax values.");
            }
            if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
                throw unsupported("Invalid getMin/getMax for band '" + name + "': getMin=" + min + ", getMax=" + max);
            }
        }
    }

    /**
     * Attempts to interpret the getRequest layer entry as a LayerInfo.
     * GeoServer typically uses LayerInfo in getRequest.getLayers() for WMS GetMap,
     * but this keeps us defensive if other types appear.
     */
    private static LayerInfo asLayerInfo(Object layerObj) {
        if (layerObj instanceof LayerInfo layerInfo) {
            return layerInfo;
        } else if (layerObj instanceof MapLayerInfo mapLayerInfo) {
            return mapLayerInfo.getLayerInfo();
        }
        return  null;
    }

    private static ServiceException unsupported(String message) {
        ServiceException se = new ServiceException(PngWindConstants.MIME_TYPE + " " + message);
        se.setCode("InvalidParameterValue");
        return se;
    }
}