/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;

@JsonPropertyOrder({"type", "domainType", "axes", "referencing"})
public abstract class Domain extends CoverageJson {

    private static final String ISO8601_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String TYPE = "Domain";

    @JsonProperty protected String domainType;

    @JsonProperty(required = true)
    private Map<String, Axis> axes;

    @JsonProperty(required = true)
    private List<Referencing> referencing;

    public Domain(
            String domainType,
            CoordinateReferenceSystem crs,
            List<DimensionBean> dimensions,
            GridGeometry2D gridGeometry,
            GranuleStack granuleStack) {
        super(TYPE);
        this.domainType = domainType;
        axes = buildAxes(crs, dimensions, gridGeometry, granuleStack);
        referencing = buildReferencing(crs, dimensions, axes);
    }

    protected List<Referencing> buildReferencing(
            CoordinateReferenceSystem crs, List<DimensionBean> dimensions, Map<String, Axis> axes) {
        List<Referencing> referencing = new ArrayList<>();
        ReferenceIdentifier identifier = crs.getIdentifiers().iterator().next();
        String code = identifier.getCode();
        if (crs instanceof GeographicCRS) {
            Referencing ref = new Referencing(new Referencing.GeographicCRS(code), XY_COORD);
            referencing.add(ref);
        } else if (crs instanceof ProjectedCRS) {
            Referencing ref = new Referencing(new Referencing.ProjectedCRS(code), XY_COORD);
            referencing.add(ref);
        }
        if (dimensions != null && !dimensions.isEmpty()) {
            buildZetaSystem(dimensions, referencing);
            buildTimeSystem(dimensions, referencing);
        }
        return referencing;
    }

    private void buildTimeSystem(List<DimensionBean> dimensions, List<Referencing> referencing) {
        for (DimensionBean dimension : dimensions) {
            if (isTime(dimension)) {
                Referencing ref = new Referencing(new Referencing.TemporalRS(), T_COORD);
                referencing.add(ref);
                return;
            }
        }
    }

    private void buildZetaSystem(List<DimensionBean> dimensions, List<Referencing> referencing) {
        for (DimensionBean dimension : dimensions) {
            if (isElevation(dimension)) {
                Referencing ref = new Referencing(new Referencing.VerticalCRS(), Z_COORD);
                referencing.add(ref);
                return;
            }
        }
    }

    protected Map<String, Axis> buildAxes(
            CoordinateReferenceSystem crs,
            List<DimensionBean> dimensions,
            GridGeometry2D gridGeometry,
            GranuleStack granuleStack) {
        Map<String, Axis> axes = new LinkedHashMap<>();
        // Build the 2D x,y axis
        buildGeoAxis(crs, gridGeometry, axes);

        // Add additional axis based on dimensions
        buildZetaAxis(dimensions, granuleStack, axes);
        buildTimeAxis(dimensions, granuleStack, axes);
        return axes;
    }

    protected abstract void buildGeoAxis(
            CoordinateReferenceSystem crs, GridGeometry2D gridGeometry, Map<String, Axis> axes);

    private void buildZetaAxis(
            List<DimensionBean> dimensions, GranuleStack granuleStack, Map<String, Axis> axes) {
        for (DimensionBean dimension : dimensions) {
            if (isElevation(dimension)) {
                String dimensionName = dimension.getName();
                String axisKey = getKey(dimension);
                Axis axis = new Axis(axisKey);
                Set<Object> uniqueValues = new LinkedHashSet<>();
                for (GridCoverage2D granule : granuleStack.getGranules()) {
                    Object object = granule.getProperty(dimensionName);
                    Number value = null;
                    if (object instanceof Number) {
                        value = (Number) object;
                    } else if (object instanceof NumberRange) {
                        value = ((NumberRange) object).getMinimum();
                    }
                    if (value != null) {
                        uniqueValues.add(value);
                    }
                }
                if (!uniqueValues.isEmpty()) {
                    List<Object> dimValues = new ArrayList<>(uniqueValues);
                    axis.setValues(dimValues);
                    axes.put(axisKey, axis);
                }
                return;
            }
        }
    }

    private void buildTimeAxis(
            List<DimensionBean> dimensions, GranuleStack granuleStack, Map<String, Axis> axes) {
        for (DimensionBean dimension : dimensions) {
            if (isTime(dimension)) {
                String dimensionName = dimension.getName();
                String axisKey = getKey(dimension);
                Axis axis = new Axis(axisKey);
                Set<Object> uniqueValues = new LinkedHashSet<>();
                DateFormat isoFormatter = new SimpleDateFormat(ISO8601_UTC_PATTERN);
                isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                for (GridCoverage2D granule : granuleStack.getGranules()) {
                    Object object = granule.getProperty(dimensionName);
                    Date value = null;
                    if (object instanceof Date) {
                        value = (Date) object;
                    } else if (object instanceof DateRange) {
                        value = ((DateRange) object).getMinValue();
                    }
                    uniqueValues.add(isoFormatter.format(value));
                }
                if (!uniqueValues.isEmpty()) {
                    List<Object> dimValues = new ArrayList<>(uniqueValues);
                    axis.setValues(dimValues);
                    axes.put(axisKey, axis);
                }
                return;
            }
        }
    }

    static class DomainBuilder {

        private List<DimensionBean> dimensions;
        private CoordinateReferenceSystem crs;
        private GridGeometry2D gridGeometry;
        private GranuleStack granuleStack;

        public void setDimensions(List<DimensionBean> dimensions) {
            this.dimensions = dimensions;
        }

        public void setCrs(CoordinateReferenceSystem coordinateReferenceSystem) {
            crs = coordinateReferenceSystem;
        }

        public void setGridGeometry(GridGeometry2D gridGeometry) {
            this.gridGeometry = gridGeometry;
        }

        public Domain build() {
            GridEnvelope range = gridGeometry.getGridRange();
            int xSpan = range.getSpan(0);
            int ySpan = range.getSpan(1);
            if (xSpan < 2 && ySpan < 2) {
                // Use PointSeries for coverages having 2D extent made of a single point
                return new PointSeries(crs, dimensions, gridGeometry, granuleStack);
            }
            return new Grid(crs, dimensions, gridGeometry, granuleStack);
        }

        public void setGranuleStack(GranuleStack granuleStack) {
            this.granuleStack = granuleStack;
        }
    }

    private boolean isTime(DimensionBean dimension) {
        return dimension != null
                && DimensionBean.DimensionType.TIME.equals(dimension.getDimensionType());
    }

    private boolean isElevation(DimensionBean dimension) {
        return dimension != null
                && DimensionBean.DimensionType.ELEVATION.equals(dimension.getDimensionType());
    }

    protected String getKey(CoordinateSystemAxis axis) {
        AxisDirection direction = axis.getDirection();
        if (direction.equals(AxisDirection.EAST)) {
            return X;
        }
        if (direction.equals(AxisDirection.NORTH)) {
            return Y;
        }
        return null;
    }

    protected String getKey(DimensionBean dimension) {
        if (isTime(dimension)) {
            return T;
        } else if (isElevation(dimension)) {
            return Z;
        }

        return null;
    }

    public Map<String, Axis> getAxes() {
        return axes;
    }

    private static final String X = "x";
    private static final String Y = "y";
    private static final String T = "t";
    private static final String Z = "z";

    private static final List<String> T_COORD = Collections.singletonList(T);
    private static final List<String> Z_COORD = Collections.singletonList(Z);
    private static final List<String> XY_COORD = Arrays.asList(X, Y);
}
