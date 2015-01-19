/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.sf.json.JSONException;

import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * 
 */
public class GeoJSONGetFeatureResponse extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    // store the response type
    private final boolean jsonp;

    public GeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format);
        if (JSONType.isJsonMimeType(format)) {
            jsonp = false;
        } else if (JSONType.isJsonpMimeType(format)) {
            jsonp = true;
        } else {
            throw new IllegalArgumentException(
                    "Unable to create the JSON Response handler using format: " + format
                            + " supported mymetype are: "
                            + Arrays.toString(JSONType.getSupportedTypes()));
        }
    }

    /**
     * capabilities output format string.
     */
    public String getCapabilitiesElementName() {
        return JSONType.getJSONType(getOutputFormat()).toString();
    }

    /**
     * Returns the mime type
     */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if(jsonp) {
            return JSONType.JSONP.getMimeType();
        } else {
            return JSONType.JSON.getMimeType();
        }
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation describeFeatureType) throws IOException {

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("about to encode JSON");
        // Generate bounds for every feature?
        WFSInfo wfs = getInfo();
        boolean featureBounding = wfs.isFeatureBounding();
        
        // include fid?
        String id_option = null; // null - default, "" - none, or "property"
        //GetFeatureRequest request = GetFeatureRequest.adapt(describeFeatureType.getParameters()[0]);
        Request request = Dispatcher.REQUEST.get();
        if (request != null) {
            id_option = JSONType.getIdPolicy( request.getKvp() );
        }
        // prepare to write out
        OutputStreamWriter osw = null;
        Writer outWriter = null;
        boolean hasGeom = false;

        // get feature count for request
        BigInteger featureCount = null;
        // for WFS 1.0.0 and WFS 1.1.0 a request with the query must be executed
        if(describeFeatureType != null) {
            if (describeFeatureType.getParameters()[0] instanceof GetFeatureType) {
                featureCount = BigInteger.valueOf(getFeatureCountFromWFS11Request(describeFeatureType, wfs));
            }
            // for WFS 2.0.0 the total number of features is stored in the featureCollection
            else if (describeFeatureType.getParameters()[0] instanceof net.opengis.wfs20.GetFeatureType){
                featureCount = (featureCollection.getTotalNumberOfFeatures().longValue() < 0)
                        ? null : featureCollection.getTotalNumberOfFeatures();
            }
        }
        
        try {
            osw = new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
            outWriter = new BufferedWriter(osw);

            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
            jsonWriter.object().key("type").value("FeatureCollection");
            if(featureCount != null) {
                jsonWriter.key("totalFeatures").value(featureCount);
            } else {
                jsonWriter.key("totalFeatures").value("unknown");
            }
            jsonWriter.key("features");
            jsonWriter.array();

            // execute should of set all the header information
            // including the lockID
            //
            // execute should also fail if all of the locks could not be aquired
            List<FeatureCollection> resultsList = featureCollection.getFeature();
            CoordinateReferenceSystem crs = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = resultsList.get(i);
                FeatureIterator iterator = collection.features();

                
                try {
                    SimpleFeatureType fType;
                    List<AttributeDescriptor> types;

                    while (iterator.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) iterator.next();
                        jsonWriter.object();
                        jsonWriter.key("type").value("Feature");

                        fType = feature.getFeatureType();
                        types = fType.getAttributeDescriptors();

                        if( id_option == null ){
                            jsonWriter.key("id").value(feature.getID());
                        }
                        else if ( id_option.length() != 0){
                            Object value = feature.getAttribute(id_option);
                            jsonWriter.key("id").value(value);
                        }
                        
                        GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
                        if(defaultGeomType != null) {
                            CoordinateReferenceSystem featureCrs =
                                    defaultGeomType.getCoordinateReferenceSystem();
                            
                            jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                            
                            if (crs == null)
                                crs = featureCrs;
                        } else  {
                            // If we don't know, assume EAST_NORTH so that no swapping occurs
                            jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                        }
                        
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
                            jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());

                        jsonWriter.key("properties");
                        jsonWriter.object();

                        for (int j = 0; j < types.size(); j++) {
                            Object value = feature.getAttribute(j);
                            AttributeDescriptor ad = types.get(j);
                            
                            if( id_option != null && id_option.equals(ad.getLocalName()) ){
                            	continue; // skip this value as it is used as the id
                            }
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
                        ReferencedEnvelope refenv = ReferencedEnvelope.reference(feature.getBounds());
                        if (featureBounding && !refenv.isEmpty())
                            jsonWriter.writeBoundingBox(refenv);

                        jsonWriter.endObject(); // end the properties
                        jsonWriter.endObject(); // end the feature
                    }
                } // catch an exception here?
                finally {
                    iterator.close();
                }
            }
            jsonWriter.endArray(); // end features

            // Coordinate Referense System
            try {
                if ("true".equals(GeoServerExtensions.getProperty("GEOSERVER_GEOJSON_LEGACY_CRS"))){
                    // This is wrong, but GeoServer used to do it this way.
                    writeCrsLegacy(jsonWriter, crs);
                } else {
                    writeCrs(jsonWriter, crs);
                }
            } catch (FactoryException e) {
                throw (IOException) new IOException("Error looking up crs identifier").initCause(e);
            }
            
            // Bounding box for featurecollection
            if (hasGeom && featureBounding) {
                ReferencedEnvelope e = null;
                for (int i = 0; i < resultsList.size(); i++) {
                    FeatureCollection collection = resultsList.get(i);
                    if (e == null) {
                        e = collection.getBounds();
                    } else {
                        e.expandToInclude(collection.getBounds());
                    }

                }

                if (e != null) {
                    jsonWriter.setAxisOrder(CRS.getAxisOrder(e.getCoordinateReferenceSystem()));
                    jsonWriter.writeBoundingBox(e);
                }
            }

            jsonWriter.endObject(); // end featurecollection

            if (jsonp) {
                outWriter.write(")");
            }

            outWriter.flush();

        } catch (JSONException jsonException) {
            ServiceException serviceException = new ServiceException("Error: "
                    + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }

    private void writeCrs(final GeoJSONBuilder jsonWriter,
            CoordinateReferenceSystem crs) throws FactoryException {
        if (crs != null) {
            String identifier = CRS.lookupIdentifier(crs, true);
            // If we get a plain EPSG code, generate a URI as the GeoJSON spec says to 
            // prefer them.
            
            if(identifier.startsWith("EPSG:")) {
                String code = GML2EncodingUtils.epsgCode(crs);
                if (code != null) {
                    identifier = SrsSyntax.OGC_URN.getPrefix() + code;
                }
            }
            jsonWriter.key("crs");
            jsonWriter.object();
            jsonWriter.key("type").value("name");
            jsonWriter.key("properties");
            jsonWriter.object();
            jsonWriter.key("name");
            jsonWriter.value(identifier);
            jsonWriter.endObject(); // end properties
            jsonWriter.endObject(); // end crs
        } else {
            jsonWriter.key("crs");
            jsonWriter.value(null);
        }
    }
    
    // Doesn't follow spec, but GeoServer used to do this.
    private void writeCrsLegacy(final GeoJSONBuilder jsonWriter,
            CoordinateReferenceSystem crs) {
        // Coordinate Referense System, currently only if the namespace is
        // EPSG
        if (crs != null) {
            Set<ReferenceIdentifier> ids = crs.getIdentifiers();
            // WKT defined crs might not have identifiers at all
            if (ids != null && ids.size() > 0) {
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
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    
    /**
     * getFeatureCountFromWFS11Request
     * 
     * Function gets the total number of features from a WFS 1.0.0 or WFS 1.1.0 request and returns it.
     * 
     * @param Operation describeFeatureType
     * @param WFSInfo wfs
     * @return int featurecount 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private int getFeatureCountFromWFS11Request(Operation operation, WFSInfo wfs)
            throws IOException {
        int totalCount = 0;
        Catalog catalog = wfs.getGeoServer().getCatalog();
        
        GetFeatureType request = (GetFeatureType) operation.getParameters()[0];
        List<Map<String, String>> viewParams = new GetFeatureRequest.WFS11(request).getViewParams();
        int idx = 0;
        for (QueryType query :  (EList<QueryType>) request.getQuery()) {
            QName typeName = (QName) query.getTypeName().get(0);
            FeatureTypeInfo meta = catalog.getFeatureTypeByName(typeName.getNamespaceURI(),
                    typeName.getLocalPart());

            FeatureSource<? extends FeatureType, ? extends Feature> source = meta.getFeatureSource(
                    null, null);
            Filter filter = query.getFilter();
            if (filter == null) {
                filter = Filter.INCLUDE;
            }
            Query countQuery = new Query(typeName.getLocalPart(), filter);
            Map<String, String> viewParam = viewParams != null && viewParams.size() > idx ? viewParams
                    .get(idx) : null;
            if (viewParam != null) {
                final Hints hints = new Hints();
                hints.put(Hints.VIRTUAL_TABLE_PARAMETERS, viewParam);
                countQuery.setHints(hints);
            }
            
            int count = 0;
            count = source.getCount(countQuery);
            if (count == -1) {
                // information was not available in the header!
                org.geotools.data.Query gtQuery = new org.geotools.data.Query(countQuery);
                FeatureCollection<? extends FeatureType, ? extends Feature> features = source
                        .getFeatures(gtQuery);
                count = features.size();
            }
            totalCount +=count;
        }

        return totalCount;
    }
    
    @Override
    public String getCharset(Operation operation){
        return gs.getGlobal().getSettings().getCharset();
    }
}
