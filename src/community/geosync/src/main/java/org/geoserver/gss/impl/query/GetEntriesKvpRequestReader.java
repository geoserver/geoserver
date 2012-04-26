package org.geoserver.gss.impl.query;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.service.FeedType;
import org.geoserver.gss.service.GetEntries;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.ServiceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.Geometry;

public class GetEntriesKvpRequestReader extends KvpRequestReader {

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    public GetEntriesKvpRequestReader() {
        super(GetEntries.class);
    }

    @Override
    public GetEntries createRequest() throws Exception {
        return (GetEntries) super.createRequest();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GetEntries read(Object reqObj, Map kvp, Map rawKvp) throws Exception {
        GetEntries request = (GetEntries) super.read(reqObj, kvp, rawKvp);

        // Mandatory, as of OGC 10-069r2, Table 11, page 63.
        if (null == request.getFeed()) {
            if (rawKvp.get("FEED") == null) {
                throw new ServiceException("FEED parameter was not provided",
                        "MissingParameterValue", "FEED");
            } else {
                throw new ServiceException("Invalid FEED parameter '" + rawKvp.get("FEED")
                        + "'. Expected one of " + Arrays.asList(FeedType.values()),
                        "InvalidParameterValue", "FEED");
            }
        }

        if (rawKvp.containsKey("filter") && request.getFilter() == null) {
            throw new ServiceException("Filter failed to be parsed. Unknown reason",
                    "InvalidParameterValue", "FILTER");
        }

        // Default value as of OGC 10-069r2, Table 11, page 63.
        if (null == request.getOutputFormat()) {
            request.setOutputFormat("application/atom+xml");
        }
        // Default value as of OGC 10-069r2, Table 11, page 63.
        if (null == request.getMaxEntries()) {
            request.setMaxEntries(Long.valueOf(25));
        }

        final Filter entryIdFitler = parseEntryId(kvp, rawKvp);
        final Filter temporalFitler = parseTemporalFilter(kvp, rawKvp);

        // Generalized predicate
        Filter generalizedPredicate = request.getFilter();
        if (generalizedPredicate == null) {
            generalizedPredicate = Filter.INCLUDE;
        }
        // Spatial parameters
        Filter spatialParamsFilter = buildSpatialParamsFilter(kvp, rawKvp);
        if (!Filter.INCLUDE.equals(spatialParamsFilter)
                && !Filter.INCLUDE.equals(generalizedPredicate)) {
            // Spatial Parameters and Generalized Predicate are mutually exclusive, as of OGC
            // 10-069r2, Table 11, page 63.
            throw new ServiceException("Precense of Spatial Parameters and Generalized Predicate "
                    + "are mutually exclusive", "InvalidParameterValue", "FILTER");
        }

        Filter filter = ff.and(Arrays.asList(entryIdFitler, temporalFitler, spatialParamsFilter,
                generalizedPredicate));

        SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
        filter = (Filter) filter.accept(visitor, null);

        request.setFilter(filter);

        return request;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Filter parseEntryId(Map kvp, Map rawKvp) throws Exception {
        final String entryId = (String) kvp.get("ENTRYID");
        if (entryId == null) {
            return Filter.INCLUDE;
        }
        List<FeatureId> ids = (List<FeatureId>) new EntryIdKvpParser(ff).parse(entryId);
        if (ids.size() > 0) {
            Id entryIdFilter = ff.id(new LinkedHashSet<FeatureId>(ids));
            return entryIdFilter;
        }
        return Filter.INCLUDE;
    }

    private static class EntryIdKvpParser extends FlatKvpParser {

        private final FilterFactory2 ff;

        public EntryIdKvpParser(FilterFactory2 ff) {
            super("entryid", FeatureId.class);
            this.ff = ff;
        }

        @Override
        protected Object parseToken(final String token) throws Exception {
            return ff.featureId(token);
        }
    }

    /**
     * Parses the spatial parameters {@code GEOM}, {@code BBOX}, {@code SPATIALOP}, {@code CRS}
     * <p>
     * <ul>
     * <li><b>GEOM</b>: if present, raw value is a geometry WKT, and must have resolved to a
     * {@link Geometry}, and is mutually exclusive with {@code BBOX}
     * <li><b>BBOX</b>: if present, must have resolved to a {@link ReferencedEnvelope}, and is
     * mutually exclusive with {@code GEOM}.
     * <li><b>CRS</b>: if present, a String of the form {@code "urn:ogc:def:crs:EPSG::XXXX"},
     * otherwise defaults to {@code "urn:ogc:def:crs:EPSG::4326"}. If {@code GEOM} was given it's
     * the Geometry. CRS. If BBOX was given and it has no CRS, the CRS of the BBOX, otherwise
     * ignored.
     * <li><b>SPATIALOP</b>: the {@link SpatialOp} to apply to the given geometry/bbox. If absent
     * defaults to {@link SpatialOp#Intersects Intersects}.
     * </ul>
     * </p>
     * 
     * @param kvp
     * @param rawKvp
     * @return {@code Filter#INCLUDE} if no spatial parameters where given, the parsed filter
     *         otherwise.
     */
    @SuppressWarnings("rawtypes")
    private Filter buildSpatialParamsFilter(final Map kvp, final Map rawKvp) {
        final boolean hasSpatialParameters = kvp.containsKey("BBOX") || kvp.containsKey("GEOM")
                || kvp.containsKey("SPATIALOP") || kvp.containsKey("CRS");
        if (!hasSpatialParameters) {
            return Filter.INCLUDE;
        }

        Geometry geom = (Geometry) kvp.get("GEOM");
        ReferencedEnvelope bbox = (ReferencedEnvelope) kvp.get("BBOX");

        SpatialOp spatialOp = null;
        if (rawKvp.get("SPATIALOP") != null) {
            if (kvp.get("SPATIALOP") instanceof SpatialOp) {
                spatialOp = (SpatialOp) kvp.get("SPATIALOP");
            } else {
                throw new ServiceException("Invalid SPATIALOP parameter value: '"
                        + rawKvp.get("SPATIALOP") + "'. Expected one of "
                        + Arrays.asList(SpatialOp.values()), "InvalidParameterValue", "SPATIALOP");
            }
        }
        String srs = (String) kvp.get("CRS");
        if (srs == null) {
            if (bbox != null && bbox.getCoordinateReferenceSystem() != null) {
                final boolean simple = false;
                srs = CRS.toSRS(bbox.getCoordinateReferenceSystem(), simple);
            } else {
                srs = "urn:ogc:def:crs:EPSG::4326";
            }
        }
        if (geom != null && bbox != null) {
            throw new ServiceException("Parameters GEOM and BBOX" + "are mutually exclusive",
                    "InvalidParameterValue", "GEOM/BBOX");
        }

        CoordinateReferenceSystem crs;
        try {
            crs = CRS.decode(srs);
        } catch (Exception e) {
            throw new ServiceException("Unable to parse CRS parameter value '" + srs + "': "
                    + e.getMessage(), "InvalidParameterValue", "CRS");
        }

        if (spatialOp == null) {
            spatialOp = SpatialOp.Intersects;
        }
        if (geom == null && !SpatialOp.Intersects.equals(spatialOp)) {
            geom = JTS.toGeometry(bbox);
            bbox = null;
        }

        Filter filter = null;
        NamespaceSupport nscontext = new NamespaceSupport();
        nscontext.declarePrefix("atom", Atom.NAMESPACE);
        nscontext.declarePrefix("georss", "http://www.georss.org/georss");
        final PropertyName propertyName = ff.property("atom:entry/georss:where", nscontext);
        if (geom != null) {
            geom.setUserData(crs);
            Expression geometryLiteral = ff.literal(geom);
            switch (spatialOp) {
            case Contains:
                filter = ff.contains(propertyName, geometryLiteral);
                break;
            case Crosses:
                filter = ff.crosses(propertyName, geometryLiteral);
                break;
            case Disjoint:
                filter = ff.disjoint(propertyName, geometryLiteral);
                break;
            case Equals:
                filter = ff.equal(propertyName, geometryLiteral);
                break;
            case Intersects:
                filter = ff.intersects(propertyName, geometryLiteral);
                break;
            case Overlaps:
                filter = ff.overlaps(propertyName, geometryLiteral);
                break;
            case Touches:
                filter = ff.touches(propertyName, geometryLiteral);
                break;
            case Within:
                filter = ff.within(propertyName, geometryLiteral);
                break;
            }
        } else if (bbox != null) {
            filter = ff.bbox(propertyName, bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(),
                    bbox.getMaxY(), srs);
        }
        return filter;
    }

    @SuppressWarnings("rawtypes")
    private Filter parseTemporalFilter(Map kvp, Map rawKvp) {
        final Date startTime = (Date) kvp.get("STARTTIME");
        final Date endTime = (Date) kvp.get("ENDTIME");
        TemporalOp temporalOp = (TemporalOp) kvp.get("TEMPORALOP");

        // Check validity of Temporal Parameters, as per OGC 10-069r2, Table 11, page 63.
        if (temporalOp != null && startTime == null && endTime == null) {
            throw new ServiceException(
                    "TEMPORALOP cannot be specified if either STARTTIME and/or ENDTIME are specified",
                    "MissingParameterValue", "STARTTIME");
        }
        if (null != endTime) {
            if (null == startTime) {
                throw new ServiceException(
                        "ENDTIME is only valid if STARTTIME and a period related temporal operation are also provided",
                        "MissingParameterValue", "STARTTIME");
            }
            if (null == temporalOp) {
                throw new ServiceException(
                        "TEMPORALOP is mandatory if STARTTIME and ENDTIME were provided",
                        "MissingParameterValue", "TEMPORALOP");
            }
            if (!temporalOp.requiresPeriod()) {
                throw new ServiceException(
                        "STARTTIME and ENDTIME shall only be requested with one of the following values for TEMPORALOP: "
                                + TemporalOp.periodRelated(), "InvalidParameterValue", "TEMPORALOP");
            }
        } else if (null == startTime) {
            return Filter.INCLUDE;
        }

        if (null == temporalOp) {
            // no temportal op provided, assume after
            temporalOp = TemporalOp.After;
        }

        final String updated = "updated";
        final PropertyName property = ff.property(updated);
        Filter temporalFilter;
        switch (temporalOp) {
        case After: {
            temporalFilter = ff.greater(property, ff.literal(startTime));
            break;
        }
        case Before: {
            temporalFilter = ff.less(property, ff.literal(startTime));
            break;
        }
        case During: {
            Expression greaterThan = ff.literal(startTime);
            Expression lowerThan = ff.literal(endTime);
            temporalFilter = ff.between(property, greaterThan, lowerThan);
            break;
        }
        case TEquals: {
            temporalFilter = ff.equals(property, ff.literal(startTime));
            break;
        }
        case Begins: {
            temporalFilter = ff.greaterOrEqual(property, ff.literal(startTime));
            break;
        }
        case Ends: {
            temporalFilter = ff.lessOrEqual(property, ff.literal(startTime));
            break;
        }
        case OverlappedBy:
        case BegunBy:
        case EndedBy:
        case Meets:
        case MetBy:
        case TContains:
        case TOverlaps:
        default:
            throw new ServiceException("Unsupported temporalOp: " + temporalOp,
                    "InvalidParameterValue", "TEMPORALOP");
        }

        return temporalFilter;
    }
}
