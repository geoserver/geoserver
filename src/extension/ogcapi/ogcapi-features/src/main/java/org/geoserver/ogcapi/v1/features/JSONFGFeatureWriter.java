/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.TypeInfoCollectionWrapper;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.SwaggerJSONSchemaMessageConverter;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** GeoJSON feature writer for JSON-FG output format */
public class JSONFGFeatureWriter<T extends FeatureType, F extends Feature> extends RFCGeoJSONFeatureWriter<T, F> {
    public JSONFGFeatureWriter(GeoServer gs, FeatureCollectionResponse response, String featureId) {
        super(gs, response, featureId);
    }

    @Override
    protected void writeExtraCollectionProperties(List<FeatureCollection<T, F>> featureCollections, GeoJSONBuilder jb) {
        super.writeExtraCollectionProperties(featureCollections, jb);

        // assuming single collection here, but since the output format can be used
        // by WFS as well, we need to check
        if (featureCollections.size() != 1) return;

        FeatureCollection collection = featureCollections.get(0);
        FeatureType schema = collection.getSchema();
        if (schema == null) return;

        jb.key("featureType");
        if (collection instanceof TypeInfoCollectionWrapper wrapper) {
            jb.value(wrapper.getFeatureTypeInfo().prefixedName());
        } else {
            jb.value(schema.getName().getLocalPart());
        }

        GeometryDescriptor gd = schema.getGeometryDescriptor();
        if (gd != null) {
            Integer geometryDimension = getGeometryDimension(gd.getType().getBinding());
            if (geometryDimension != null) {
                jb.key("geometryDimension");
                jb.value(geometryDimension);
            }
        }
    }

    private Integer getGeometryDimension(Class<?> binding) {
        if (Point.class.isAssignableFrom(binding) || MultiPoint.class.isAssignableFrom(binding)) {
            return 0;
        } else if (LineString.class.isAssignableFrom(binding) || MultiLineString.class.isAssignableFrom(binding)) {
            return 1;
        } else if (Polygon.class.isAssignableFrom(binding) || MultiPolygon.class.isAssignableFrom(binding)) {
            return 2;
        } else {
            JSONFGFeaturesResponse.LOGGER.log(Level.WARNING, "Could not compute geometry dimension for " + binding);
            return null;
        }
    }

    @Override
    protected void writeGeometry(GeoJSONBuilder jsonWriter, GeometryDescriptor descriptor, Geometry aGeom) {
        boolean otherCRS = descriptor != null
                && descriptor.getCoordinateReferenceSystem() != null
                && !CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, descriptor.getCoordinateReferenceSystem());
        String key = "geometry";
        if (otherCRS) {
            // make sure the JSON writer does not try to un-flip the coordinates
            // as per GeoJSON spec, quoting:
            //  A position is an array of numbers.  There MUST be two or more
            //   elements.  The first two elements are longitude and latitude, or
            //   easting and northing, precisely in that order and using decimal
            //   numbers.  Altitude or elevation MAY be included as an optional third
            //   element.
            jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
            key = "place";
        }
        jsonWriter.key(key);
        if (aGeom != null) {
            jsonWriter.writeGeom(aGeom);
        } else {
            jsonWriter.value(null);
        }
    }

    @Override
    protected void writeCollectionCRS(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs) throws IOException {
        writeCoordRefSys(jsonWriter, crs);
    }

    private void writeCoordRefSys(GeoJSONBuilder jsonWriter, CoordinateReferenceSystem crs) {
        // write the CRS block only if needed
        if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            jsonWriter.key(JSONFGFeaturesResponse.COORD_REF_SYS);
            jsonWriter.value(getCRSURI(crs));
        }
    }

    private String getCRSURI(CoordinateReferenceSystem crs) {
        try {
            // prefer EPSG authority if possible, more commonly understood
            String epsgIdentifier = CRS.lookupIdentifier(Citations.EPSG, crs, false);
            if (epsgIdentifier != null) {
                return "http://www.opengis.net/def/crs/EPSG/0/" + epsgIdentifier;
            }

            // fallback to the first identifier otherwise, with authority
            ReferenceIdentifier identifier = crs.getIdentifiers().iterator().next();
            String authority = identifier.getAuthority().toString();
            String code = identifier.getCode();
            return "http://www.opengis.net/def/crs/" + authority + "/0/" + code;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void writeExtraFeatureProperties(Feature feature, GeoJSONBuilder jb) {
        if (featureId != null) {
            // needed on the single feature when there is no collection wrapper
            writeCoordRefSys(jb, feature.getType().getCoordinateReferenceSystem());
            writeLinks(jb, featureId);
        }
    }

    @Override
    protected void addLinks(
            FeatureCollectionResponse response, GeoJSONBuilder jw, String featureId, FeatureTypeInfo featureType) {
        String baseUrl = APIRequestInfo.get().getBaseURL();
        super.addLinks(response, jw, featureId, featureType);

        // add a link to the schema
        String path =
                "ogc/features/v1/collections/" + ResponseUtils.urlEncode(featureType.prefixedName()) + "/schemas/fg/";
        if (featureId != null) {
            path += "feature.json";
        } else {
            path += "collection.json";
        }

        String href = ResponseUtils.buildURL(baseUrl, path, null, URLMangler.URLType.SERVICE);
        String linkType = "type";
        String linkTitle = "JSON schema";

        jw.writeLink(jw, linkTitle, SwaggerJSONSchemaMessageConverter.SCHEMA_TYPE_VALUE, linkType, href);
    }
}
