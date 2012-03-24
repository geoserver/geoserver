package org.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.sf.json.JSONException;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class GeoJSONOutputFormat extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging
    .getLogger(this.getClass().toString());

    public static final String FORMAT = "json";

    public GeoJSONOutputFormat(GeoServer gs) {
        super(gs, FORMAT);
    }

    public String getMimeType(Object value, Operation operation)
    throws ServiceException {
        // let's check if a callback has been set, in that case, we need to return a different mime type
        GetFeatureType gft = (GetFeatureType) operation.getParameters()[0];
        String callback = (String) gft.getFormatOptions().get("CALLBACK");
        if(callback != null && !"".equals(callback)) {
            return "text/javascript";
        } else {
            return "application/json";
        }
    }

    public String getCapabilitiesElementName() {
        return "GEOJSON";
    }

    protected String getContentDisposition(
            FeatureCollectionType featureCollection) {

        StringBuffer sb = new StringBuffer();
        for (Iterator f = featureCollection.getFeature().iterator(); f
        .hasNext();) {
            FeatureCollection fc = (FeatureCollection) f.next();
            sb.append(fc.getSchema().getName().getLocalPart() + "_");
        }
        sb.setLength(sb.length() - 1);
        return "inline; filename=" + sb.toString() + ".txt";

    }

    protected void write(FeatureCollectionType featureCollection,
            OutputStream output, Operation getFeature) throws IOException,
            ServiceException {

        WFSInfo wfs = getInfo();
        
        // TODO: investigate setting proper charsets in this
        // it's part of the constructor, just need to hook it up.
        Writer outWriter = new BufferedWriter(
            new OutputStreamWriter(output,wfs.getGeoServer().getGlobal().getCharset()));
        
        // let's check if a callback has been set
        GetFeatureType gft = (GetFeatureType) getFeature.getParameters()[0];
        String callback = (String) gft.getFormatOptions().get("CALLBACK");
        if(callback != null && !"".equals(callback)) {
            outWriter.write(callback + "(");
        }

        GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);

        // execute should of set all the header information
        // including the lockID
        //
        // execute should also fail if all of the locks could not be aquired
        List resultsList = featureCollection.getFeature();

        // FeatureResults[] featureResults = (FeatureResults[]) resultsList
        // .toArray(new FeatureResults[resultsList.size()]);
        LOGGER.info("about to encode JSON");

        // Generate bounds for every feature?
        boolean featureBounding = wfs.isFeatureBounding();
        boolean hasGeom = false;

        try {
            jsonWriter.object().key("type").value("FeatureCollection");
            jsonWriter.key("features");
            jsonWriter.array();

            CoordinateReferenceSystem crs = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = (FeatureCollection) resultsList
                .get(i);
                FeatureIterator iterator = collection.features();

                try {
                    SimpleFeatureType fType;
                    List<AttributeDescriptor> types;

                    while (iterator.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) iterator.next();
                        jsonWriter.object();
                        jsonWriter.key("type").value("Feature");
                        jsonWriter.key("id").value(feature.getID());

                        fType = feature.getFeatureType();
                        types = fType.getAttributeDescriptors();

                        GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();

                        if (crs == null && defaultGeomType != null)
                            crs = fType.getGeometryDescriptor().getCoordinateReferenceSystem();

                        jsonWriter.key("geometry");
                        Geometry aGeom = (Geometry) feature.getDefaultGeometry();

                        if (aGeom == null) {
                            // In case the default geometry is not set, we will
                            // just use the first geometry we find
                            for (int j = 0; j < types.size() && aGeom == null; j++) {
                                Object value = feature.getAttribute(j);
                                if (value != null && value instanceof Geometry) {
                                    aGeom = (Geometry) value;
                                }
                            }
                        }
                        // Write the geometry, whether it is a null or not
                        if (aGeom != null) {
                            jsonWriter.writeGeom(aGeom);
                            hasGeom = true;
                        } else {
                            jsonWriter.value(null);
                        }
                        if (defaultGeomType != null)
                            jsonWriter.key("geometry_name").value(
                                    defaultGeomType.getLocalName());

                        jsonWriter.key("properties");
                        jsonWriter.object();

                        for (int j = 0; j < types.size(); j++) {
                            Object value = feature.getAttribute(j);
                            AttributeDescriptor ad = types.get(j);

                            if (value != null) {
                                if (value instanceof Geometry) {
                                    // This is an area of the spec where they
                                    // decided to 'let convention evolve', 
                                    // that is how to handle multiple
                                    // geometries. My take is to print the
                                    // geometry here if it's not the default. 
                                    // If it's the default that you already
                                    // printed above, so you don't need it here.
                                    if (ad.equals(defaultGeomType)) {
                                        // Do nothing, we wrote it above
                                        // jsonWriter.value("geometry_name");
                                    } else {
                                        jsonWriter.key(ad.getLocalName());
                                        jsonWriter.writeGeom((Geometry) value);
                                    }
                                } else {
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.value(value);
                                }

                            } else {
                                jsonWriter.key(ad.getLocalName());
                                jsonWriter.value(null);
                            }
                        }
                        // Bounding box for feature in properties
                        ReferencedEnvelope refenv = new ReferencedEnvelope(feature.getBounds());
                        if (featureBounding && !refenv.isEmpty())
                            jsonWriter.writeBoundingBox(refenv);

                        jsonWriter.endObject(); // end the properties
                        jsonWriter.endObject(); // end the feature
                    }
                } // catch an exception here?
                finally {
                    collection.close(iterator);
                }

            }

            jsonWriter.endArray(); // end features

            // Coordinate Referense System, currently only if the namespace is
            // EPSG
            if (crs != null) {
                Set<ReferenceIdentifier> ids = crs.getIdentifiers();
                // WKT defined crs might not have identifiers at all
                if(ids != null && ids.size() > 0) {
                    NamedIdentifier namedIdent = (NamedIdentifier) ids.iterator().next();
                    String csStr = namedIdent.getCodeSpace().toUpperCase();
    
                    if (csStr.equals("EPSG")) {
                        jsonWriter.key("crs");
                        jsonWriter.object();
                        jsonWriter.key("type").value(csStr);
                        jsonWriter.key("properties");
                        jsonWriter.object();
                        jsonWriter.key("code");
                        jsonWriter.value(namedIdent.getCode());
                        jsonWriter.endObject(); // end properties
                        jsonWriter.endObject(); // end crs
                    }
                }
            }

            // Bounding box for featurecollection
            if (hasGeom && featureBounding) {
                ReferencedEnvelope e = null;
                for (int i = 0; i < resultsList.size(); i++) {
                    FeatureCollection collection = (FeatureCollection) resultsList
                    .get(i);
                    if (e == null) {
                        e = collection.getBounds();
                    } else {
                        e.expandToInclude(collection.getBounds());
                    }

                }

                if (e != null) {
                    jsonWriter.writeBoundingBox(e);
                }
            }

            jsonWriter.endObject(); // end featurecollection
            outWriter.flush();
            if(callback != null && !"".equals(callback)) {
                outWriter.write(")");
                outWriter.flush();
            }

        } catch (JSONException jsonException) {
            ServiceException serviceException = new ServiceException("Error: "
                    + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }

    }

}
