/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;
import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.gml.producer.FeatureTransformer;
import org.geotools.gml.producer.FeatureTransformer.FeatureTypeNamespaces;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.geotools.wfs.WFS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Encodes features in Geographic Markup Language (GML) version 2.
 *
 * <p>GML2-GZIP format is just GML2 with gzip compression. If GML2-GZIP format was requested, <code>
 * getContentEncoding()</code> will retutn <code>"gzip"</code>, otherwise will return <code>null
 * </code>
 *
 * @author Gabriel Rold?n
 * @version $Id$
 */
public class GML2OutputFormat extends WFSGetFeatureOutputFormat {
    private static final int NO_FORMATTING = -1;
    private static final int INDENT_SIZE = 2;
    public static final String formatName = "GML2";
    public static final String MIME_TYPE = "text/xml; subtype=gml/2.1.2";

    /**
     * This is a "magic" class provided by Geotools that writes out GML for an array of
     * FeatureResults.
     *
     * <p>This class seems to do all the work, if you have a problem with GML you will need to hunt
     * it down. We supply all of the header information in the execute method, and work through the
     * featureList in the writeTo method.
     *
     * <p>This value will be <code>null</code> until execute is called.
     */
    private FeatureTransformer transformer;

    /** GeoServer configuration */
    private GeoServer geoServer;

    /** The catalog */
    protected Catalog catalog;

    /** Creates the producer with a reference to the GetFeature operation using it. */
    public GML2OutputFormat(GeoServer geoServer) {
        super(geoServer, new HashSet(Arrays.asList(new String[] {"GML2", MIME_TYPE})));

        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME_TYPE;
    }

    public String getCapabilitiesElementName() {
        return "GML2";
    }

    /** prepares for encoding into GML2 format */
    @SuppressWarnings("unchecked")
    public void prepare(
            String outputFormat, FeatureCollectionResponse results, GetFeatureRequest request)
            throws IOException {
        transformer = createTransformer();

        FeatureTypeNamespaces ftNames = transformer.getFeatureTypeNamespaces();
        Map ftNamespaces = new HashMap();

        // TODO: the srs is a back, it only will work property when there is
        // one type, we really need to set it on the feature level
        int srs = -1;
        int numDecimals = -1;
        boolean padWithZeros = false;
        boolean forcedDecimal = false;
        for (int i = 0; i < results.getFeature().size(); i++) {
            // FeatureResults features = (FeatureResults) f.next();
            FeatureCollection features = (FeatureCollection) results.getFeature().get(i);
            SimpleFeatureType featureType = (SimpleFeatureType) features.getSchema();

            ResourceInfo meta =
                    catalog.getResourceByName(featureType.getName(), ResourceInfo.class);

            String prefix = meta.getNamespace().getPrefix();
            String uri = meta.getNamespace().getURI();

            ftNames.declareNamespace(features.getSchema(), prefix, uri);

            if (ftNamespaces.containsKey(uri)) {
                String location = (String) ftNamespaces.get(uri);
                ftNamespaces.put(uri, location + "," + urlEncode(meta.prefixedName()));
            } else {
                // don't blindly assume it's a feature type, this class is used also by WMS
                // FeatureInfo
                // meaning it might be a coverage or a remote wms layer
                if (meta instanceof FeatureTypeInfo) {
                    String location =
                            typeSchemaLocation(
                                    geoServer.getGlobal(),
                                    (FeatureTypeInfo) meta,
                                    request.getBaseUrl());
                    ftNamespaces.put(uri, location);
                }
            }

            // JD: wfs reprojection: should not set srs form metadata but from
            // the request
            // srs = Integer.parseInt(meta.getSRS());
            Query query = request.getQueries().get(i);
            try {
                String srsName = query.getSrsName() != null ? query.getSrsName().toString() : null;
                if (srsName == null) {
                    // no SRS in query...asking for the default?
                    srsName = meta.getSRS();
                }
                if (srsName != null) {
                    CoordinateReferenceSystem crs = CRS.decode(srsName);
                    String epsgCode = GML2EncodingUtils.epsgCode(crs);
                    srs = Integer.parseInt(epsgCode);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Problem encoding:" + query.getSrsName(), e);
            }

            // track num decimals, in cases where the query has multiple types we choose the max
            // of all the values (same deal as above, might not be a vector due to GetFeatureInfo
            // reusing this)
            if (meta instanceof FeatureTypeInfo) {
                int ftiDecimals = ((FeatureTypeInfo) meta).getNumDecimals();
                if (ftiDecimals > 0) {
                    numDecimals =
                            numDecimals == -1 ? ftiDecimals : Math.max(numDecimals, ftiDecimals);
                }
                boolean pad = ((FeatureTypeInfo) meta).getPadWithZeros();
                if (pad) {
                    padWithZeros = true;
                }
                boolean force = ((FeatureTypeInfo) meta).getForcedDecimal();
                if (force) {
                    forcedDecimal = true;
                }
            }
        }

        SettingsInfo settings = geoServer.getSettings();

        if (numDecimals == -1) {
            numDecimals = settings.getNumDecimals();
        }

        WFSInfo wfs = getInfo();

        transformer.setIndentation(wfs.isVerbose() ? INDENT_SIZE : (NO_FORMATTING));
        transformer.setNumDecimals(numDecimals);
        transformer.setPadWithZeros(padWithZeros);
        transformer.setForceDecimalEncoding(forcedDecimal);
        transformer.setFeatureBounding(wfs.isFeatureBounding());
        transformer.setCollectionBounding(wfs.isFeatureBounding());
        transformer.setEncoding(Charset.forName(settings.getCharset()));

        if (wfs.isCanonicalSchemaLocation()) {
            transformer.addSchemaLocation(WFS.NAMESPACE, wfsCanonicalSchemaLocation());
        } else {
            String wfsSchemaloc = wfsSchemaLocation(request.getBaseUrl());
            transformer.addSchemaLocation(WFS.NAMESPACE, wfsSchemaloc);
        }

        for (Iterator it = ftNamespaces.keySet().iterator(); it.hasNext(); ) {
            String uri = (String) it.next();
            transformer.addSchemaLocation(uri, (String) ftNamespaces.get(uri));
        }

        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_10);
        transformer.setGmlPrefixing(wfs.isCiteCompliant() || !gml.getOverrideGMLAttributes());

        if (results.getLockId() != null) {
            transformer.setLockId(results.getLockId());
        }

        if (srs != -1) {
            transformer.setSrsName(gml.getSrsNameStyle().getPrefix() + srs);
        }
    }

    /** */
    public void encode(
            OutputStream output, FeatureCollectionResponse results, GetFeatureRequest request)
            throws ServiceException, IOException {
        if (results == null) {
            throw new IllegalStateException(
                    "It seems prepare() has not been called" + " or has not succeed");
        }

        GZIPOutputStream gzipOut = null;

        // execute should of set all the header information
        // including the lockID
        //
        // execute should also fail if all of the locks could not be aquired
        List resultsList = results.getFeature();
        FeatureCollection[] featureResults =
                (FeatureCollection[])
                        resultsList.toArray(new FeatureCollection[resultsList.size()]);

        try {
            transformer.transform(featureResults, output);

            // we need to "finish" here because if not,it is possible that the gzipped
            // content do not gets completely written
            if (gzipOut != null) {
                gzipOut.finish();
                gzipOut.flush();
            }
        } catch (TransformerException gmlException) {
            String msg = " error:" + gmlException.getMessage();
            throw new ServiceException(msg, gmlException);
        }
    }

    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        GetFeatureRequest request = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

        prepare(request.getOutputFormat(), featureCollection, request);
        encode(output, featureCollection, request);
    }

    protected FeatureTransformer createTransformer() {
        return new FeatureTransformer();
    }

    protected String wfsSchemaLocation(String baseUrl) {
        return buildSchemaURL(baseUrl, "wfs/1.0.0/WFS-basic.xsd");
    }

    protected String wfsCanonicalSchemaLocation() {
        return org.geoserver.wfs.xml.v1_0_0.WFS.CANONICAL_SCHEMA_LOCATION_BASIC;
    }

    protected String typeSchemaLocation(
            GeoServerInfo global, FeatureTypeInfo meta, String baseUrl) {
        Map<String, String> params =
                params(
                        "service",
                        "WFS",
                        "version",
                        "1.0.0",
                        "request",
                        "DescribeFeatureType",
                        "typeName",
                        meta.prefixedName());
        return buildURL(baseUrl, "wfs", params, URLType.SERVICE);
    }
}
