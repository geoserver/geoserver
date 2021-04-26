/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.impl.GetFeatureTypeImpl;
import net.opengis.wfs.impl.QueryTypeImpl;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    private String base;
    private String path;
    private Map<String, Object> query;

    /** @param gs */
    public MapMLGetFeatureOutputFormat(GeoServer gs) {
        super(gs, MapMLConstants.FORMAT_NAME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MapMLConstants.MAPML_MIME_TYPE + ";charset=UTF-8";
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollectionResponse,
            OutputStream out,
            Operation getFeature)
            throws IOException, ServiceException {

        List<FeatureCollection> featureCollections = featureCollectionResponse.getFeatures();
        if (featureCollections.size() != 1) {
            throw new ServiceException(
                    "MapML OutputFormat does not support Multiple Feature Type output.");
        }
        FeatureCollection featureCollection = featureCollections.get(0);
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException("MapML OutputFormat does not support Complex Features.");
        }
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;

        LayerInfo layerInfo = gs.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        ResourceInfo resourceInfo = layerInfo.getResource();
        MetadataMap layerMeta = resourceInfo.getMetadata();

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerInfo.getName());
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("UTF-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MapMLConstants.MAPML_MIME_TYPE);
        metas.add(meta);
        metas.addAll(deduceProjectionAndExtent(getFeature, layerInfo));
        List<Link> links = head.getLinks();
        links.addAll(alternateProjections());

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link link = new Link();
            link.setRel(RelType.LICENSE);
            if (licenseLink != null) {
                link.setHref(licenseLink);
            }
            if (licenseTitle != null) {
                link.setTitle(licenseTitle);
            }
            links.add(link);
        }

        String fCaptionTemplate = layerMeta.get("mapml.featureCaption", String.class);
        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);

        List<Feature> features = body.getFeatures();
        MapMLGenerator featureBuilder = new MapMLGenerator();
        int numDecimals = this.getNumDecimals(featureCollections, gs, gs.getCatalog());
        featureBuilder.setNumDecimals(numDecimals);
        try (SimpleFeatureIterator iterator = fc.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                // convert feature to xml

                Feature f = featureBuilder.buildFeature(feature, fCaptionTemplate);
                features.add(f);
            }
        }

        // write to output
        OutputStreamWriter osw = new OutputStreamWriter(out, gs.getSettings().getCharset());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }

    /**
     * @param getFeature the GetFeature operation itself, from which we obtain srsName
     * @param layerInfo metadata for the feature class
     * @return
     */
    private Set<Meta> deduceProjectionAndExtent(Operation getFeature, LayerInfo layerInfo) {
        Set<Meta> metas = new HashSet<>();
        TiledCRSParams tcrs = null;
        CoordinateReferenceSystem sourceCRS = layerInfo.getResource().getCRS();
        CoordinateReferenceSystem responseCRS = sourceCRS;
        String sourceCRSCode = CRS.toSRS(sourceCRS);
        String responseCRSCode = sourceCRSCode;
        Meta projection = new Meta();
        Meta extent = new Meta();
        Meta coordinateSystem = new Meta();
        coordinateSystem.setName("cs");
        extent.setName("extent");
        projection.setName("projection");
        String cite = Citations.fromName("MapML").getTitle().toString();
        String crs;
        try {
            URI srsName = getSrsNameFromOperation(getFeature);
            if (srsName != null) {
                String code = srsName.toString();
                responseCRS = CRS.decode(code);
                responseCRSCode = CRS.toSRS(CRS.decode(code));
                tcrs = TiledCRSConstants.lookupTCRS(responseCRSCode);
                if (tcrs != null) {
                    projection.setContent(tcrs.getName());
                    crs = (responseCRS instanceof GeodeticCRS) ? "gcrs" : "pcrs";
                    coordinateSystem.setContent(crs);
                }
            }
        } catch (Exception e) {
        }
        // if tcrs is not set, either exception encountered deciphering the
        // response CRS or the requested projection is not known to MapML.
        if (tcrs == null) {
            // this crs has no TCRS match, so if it's a gcrs, tag it with the
            // "MapML" CRS 'authority'
            // so that nobody can be surprised by x,y axis order in WGS84 data
            crs = (responseCRS instanceof GeodeticCRS) ? "gcrs" : "pcrs";
            projection.setContent(
                    crs.equalsIgnoreCase("gcrs") ? cite + ":" + responseCRSCode : responseCRSCode);
            coordinateSystem.setContent(crs);
        }
        extent.setContent(getExtent(layerInfo, responseCRSCode, responseCRS));
        metas.add(projection);
        metas.add(coordinateSystem);
        metas.add(extent);
        return metas;
    }

    /**
     * @param getFeature the Operation from which we get the srsName of the request
     * @return the EPSG / whatever code that was requested, or null if not available
     */
    private URI getSrsNameFromOperation(Operation getFeature) {
        URI srsName = null;
        try {
            boolean wfs1 = getFeature.getService().getVersion().toString().startsWith("1");
            srsName =
                    wfs1
                            ? ((QueryTypeImpl)
                                            ((GetFeatureTypeImpl) getFeature.getParameters()[0])
                                                    .getQuery()
                                                    .get(0))
                                    .getSrsName()
                            : ((net.opengis.wfs20.impl.QueryTypeImpl)
                                            ((net.opengis.wfs20.impl.GetFeatureTypeImpl)
                                                            getFeature.getParameters()[0])
                                                    .getAbstractQueryExpressionGroup()
                                                    .get(0)
                                                    .getValue())
                                    .getSrsName();
        } catch (Exception e) {
        }
        return srsName;
    }

    /**
     * @param layerInfo source of bbox info
     * @param responseCRSCode output CRS code
     * @param responseCRS used to transform source bbox to, if necessary
     * @return
     */
    private String getExtent(
            LayerInfo layerInfo, String responseCRSCode, CoordinateReferenceSystem responseCRS) {
        String extent = "";
        ResourceInfo r = layerInfo.getResource();
        ReferencedEnvelope re;
        String gcrsFormat =
                "top-left-longitude=%1$.6f,top-left-latitude=%2$.6f,bottom-right-longitude=%3$.6f,bottom-right-latitude=%4$.6f";
        String pcrsFormat =
                "top-left-easting=%1$.2f,top-left-northing=%2$.2f,bottom-right-easting=%3$.2f,bottom-right-northing=%4$.2f";
        double minLong, minLat, maxLong, maxLat;
        double minEasting, minNorthing, maxEasting, maxNorthing;
        TiledCRSParams tcrs = TiledCRSConstants.lookupTCRS(responseCRSCode);
        try {
            if (responseCRS instanceof GeodeticCRS) {
                re = r.getLatLonBoundingBox();
                minLong = re.getMinX();
                minLat = re.getMinY();
                maxLong = re.getMaxX();
                maxLat = re.getMaxY();
                extent = String.format(gcrsFormat, minLong, maxLat, maxLong, minLat);
            } else {
                re = r.boundingBox().transform(responseCRS, true);
                minEasting = re.getMinX();
                minNorthing = re.getMinY();
                maxEasting = re.getMaxX();
                maxNorthing = re.getMaxY();
                extent =
                        String.format(pcrsFormat, minEasting, maxNorthing, maxEasting, minNorthing);
            }
        } catch (Exception e) {
            if (tcrs != null) {
                if (tcrs.getName().equalsIgnoreCase("WGS84")) {
                    minLong = tcrs.getBounds().getMin().x;
                    minLat = tcrs.getBounds().getMin().y;
                    maxLong = tcrs.getBounds().getMax().x;
                    maxLat = tcrs.getBounds().getMax().y;
                    extent = String.format(gcrsFormat, maxLong, maxLat, minLong, minLat);
                } else {
                    minEasting = tcrs.getBounds().getMin().x;
                    minNorthing = tcrs.getBounds().getMin().y;
                    maxEasting = tcrs.getBounds().getMax().x;
                    maxNorthing = tcrs.getBounds().getMax().y;
                    extent =
                            String.format(
                                    pcrsFormat, minEasting, maxNorthing, maxEasting, minNorthing);
                }
            }
        }
        return extent;
    }
    /**
     * Format TCRS as alternate projection links for use in a WFS response, allowing projection
     * negotiation
     *
     * @return list of link elements with rel=alternate projection=proj-name
     */
    private List<Link> alternateProjections() {
        ArrayList<Link> links = new ArrayList<>();
        Set<String> projections = TiledCRSConstants.tiledCRSBySrsName.keySet();
        projections.forEach(
                (String p) -> {
                    Link l = new Link();
                    TiledCRSParams projection = TiledCRSConstants.lookupTCRS(p);
                    l.setProjection(ProjType.fromValue(projection.getName()));
                    l.setRel(RelType.ALTERNATE);
                    this.query.put("srsName", projection.getCode());
                    HashMap<String, String> kvp = new HashMap<>(this.query.size());
                    this.query
                            .keySet()
                            .forEach(
                                    key -> {
                                        kvp.put(key, this.query.getOrDefault(key, "").toString());
                                    });
                    l.setHref(
                            ResponseUtils.urlDecode(
                                    ResponseUtils.buildURL(
                                            this.base,
                                            this.path,
                                            kvp,
                                            URLMangler.URLType.SERVICE)));
                    links.add(l);
                });

        return links;
    }
    /**
     * Set the base context of the URL of the request for use in output format; set by callback
     *
     * @param base the URL to use as base, corresponds to ResponseUtils.buildURL base parameter
     */
    public void setBase(String base) {
        this.base = base;
    }
    /**
     * Set the query part of the URL to use as input to ResponseUtils.buildURL
     *
     * @param query
     */
    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }
    /**
     * Set the path to be used as the path parameter for input to ResponseUtils.buildURL
     *
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }
}
