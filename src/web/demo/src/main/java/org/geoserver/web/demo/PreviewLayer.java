/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wfs.xml.GML32OutputFormat;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.util.NullProgressListener;
import org.geotools.util.ProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * A model class for the UI, hides the difference between simple layers and groups, centralizes the
 * computation of a valid preview request
 */
public class PreviewLayer {
    static final Logger LOGGER = Logging.getLogger(PreviewLayer.class);

    public enum PreviewLayerType {
        Raster,
        Vector,
        Remote,
        Group
    };

    LayerInfo layerInfo;

    LayerGroupInfo groupInfo;

    transient GetMapRequest request;

    public PreviewLayer(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    public PreviewLayer(LayerGroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    public String getName() {
        if (layerInfo != null) {
            return layerInfo.getResource().prefixedName();
        } else {
            return groupInfo.prefixedName();
        }
    }

    public String getWorkspace() {
        if (layerInfo != null) {
            return layerInfo.getResource().getStore().getWorkspace().getName();
        } else if (groupInfo != null && groupInfo.getWorkspace() != null) {
            return groupInfo.getWorkspace().getName();
        }
        return null;
    }

    public PackageResourceReference getIcon() {
        if (layerInfo != null) return CatalogIconFactory.get().getSpecificLayerIcon(layerInfo);
        else return CatalogIconFactory.GROUP_ICON;
    }

    public PackageResourceReference getTypeSpecificIcon() {
        if (layerInfo != null) return CatalogIconFactory.get().getSpecificLayerIcon(layerInfo);
        else return CatalogIconFactory.GROUP_ICON;
    }

    public String getTitle() {
        if (layerInfo != null) {
            return layerInfo.getResource().getTitle();
        } else if (groupInfo != null) {
            return groupInfo.getTitle();
        } else {
            return "";
        }
    }

    public String getAbstract() {
        if (layerInfo != null) {
            return layerInfo.getResource().getAbstract();
        } else if (groupInfo != null) {
            return groupInfo.getAbstract();
        } else {
            return "";
        }
    }

    public String getKeywords() {
        if (layerInfo != null) {
            return layerInfo.getResource().getKeywords().toString();
        } else {
            return "";
        }
    }

    public PreviewLayer.PreviewLayerType getType() {
        if (layerInfo != null) {
            if (layerInfo.getType() == PublishedType.RASTER) return PreviewLayerType.Raster;
            else if (layerInfo.getType() == PublishedType.VECTOR) return PreviewLayerType.Vector;
            else return PreviewLayerType.Remote;
        } else {
            return PreviewLayerType.Group;
        }
    }

    /**
     * Builds a fake GetMap request
     *
     * @param prefixedName
     */
    GetMapRequest getRequest() {
        if (request == null) {
            GeoServerApplication app = GeoServerApplication.get();
            request = new GetMapRequest();
            Catalog catalog = app.getCatalog();
            List<MapLayerInfo> layers = expandLayers(catalog);
            request.setLayers(layers);
            request.setFormat("application/openlayers");

            // in the case of groups we already know about the envelope and the target SRS
            if (groupInfo != null) {
                ReferencedEnvelope bounds = groupInfo.getBounds();
                request.setBbox(bounds);
                String epsgCode = GML2EncodingUtils.epsgCode(bounds.getCoordinateReferenceSystem());
                if (epsgCode != null) request.setSRS("EPSG:" + epsgCode);
            }
            try {
                DefaultWebMapService.autoSetBoundsAndSize(request);
            } catch (Exception e) {
                LOGGER.log(
                        Level.INFO,
                        "Could not set figure out automatically a good preview link for "
                                + getName(),
                        e);
            }
        }
        return request;
    }

    /**
     * Expands the specified name into a list of layer info names
     *
     * @param name
     * @param catalog
     */
    private List<MapLayerInfo> expandLayers(Catalog catalog) {
        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();

        if (layerInfo != null) {
            layers.add(new MapLayerInfo(layerInfo));
        } else {
            for (LayerInfo l : Iterables.filter(groupInfo.getLayers(), LayerInfo.class)) {
                layers.add(new MapLayerInfo(l));
            }
        }
        return layers;
    }

    String getBaseUrl(String service) {
        return getBaseUrl(service, false);
    }

    String getBaseUrl(String service, boolean useGlobalRef) {
        HttpServletRequest req = GeoServerApplication.get().servletRequest();
        String base = ResponseUtils.baseURL(req);

        String ws = getWorkspace();
        if (ws == null || useGlobalRef) {
            // global reference
            return ResponseUtils.buildURL(base, service, null, URLType.SERVICE);
        } else {
            return ResponseUtils.buildURL(base, ws + "/" + service, null, URLType.SERVICE);
        }
    }

    /**
     * Given a request and a target format, builds the WMS request
     *
     * @param request
     * @param string
     */
    public String getWmsLink() {
        GetMapRequest request = getRequest();
        final Envelope bbox = request.getBbox();
        if (bbox == null) return null;

        return getBaseUrl("wms")
                + "?service=WMS&version=1.1.0&request=GetMap" //
                + "&layers="
                + getName() //
                + "&styles=" //
                + "&bbox="
                + bbox.getMinX()
                + ","
                + bbox.getMinY() //
                + ","
                + bbox.getMaxX()
                + ","
                + bbox.getMaxY() //
                + "&width="
                + request.getWidth() //
                + "&height="
                + request.getHeight()
                + "&srs="
                + request.getSRS();
    }

    /**
     * Returns the default GML link for this layer.
     *
     * @param gmlParamsCache optional map where computed GML output params are cached
     */
    public String getGmlLink(Map<String, GMLOutputParams> gmlParamsCache) {
        GMLOutputParams gmlParams = new GMLOutputParams();

        if (layerInfo != null) {
            if (layerInfo.getResource() instanceof FeatureTypeInfo) {
                FeatureTypeInfo ftInfo = (FeatureTypeInfo) layerInfo.getResource();
                if (ftInfo.getStore() != null) {
                    Map<String, Serializable> connParams =
                            ftInfo.getStore().getConnectionParameters();
                    if (connParams != null) {
                        String dbtype = (String) connParams.get("dbtype");
                        // app-schema feature types need special treatment
                        if ("app-schema".equals(dbtype)) {
                            String mappingUrl = connParams.get("url").toString();
                            if (gmlParamsCache != null && gmlParamsCache.containsKey(mappingUrl)) {
                                // avoid looking up the GML version again
                                gmlParams = gmlParamsCache.get(mappingUrl);
                            } else {
                                // use global OWS service to make sure all secondary namespaces
                                // are accessible
                                gmlParams.baseUrl = getBaseUrl("ows", true);
                                // always use WFS 1.1.0 for app-schema layers
                                gmlParams.wfsVersion =
                                        org.geotools.wfs.v1_1.WFS.getInstance().getVersion();
                                // determine GML version by inspecting the feature type and its
                                // super types
                                try {
                                    gmlParams.gmlVersion = findGmlVersion(ftInfo);
                                } catch (IOException e) {
                                    LOGGER.log(
                                            Level.FINE,
                                            "Could not determine GML version, using default",
                                            e);
                                    gmlParams.gmlVersion = null;
                                }
                                // store params in cache
                                if (gmlParamsCache != null) {
                                    gmlParamsCache.put(mappingUrl, gmlParams);
                                }
                            }
                        }
                        // TODO: do other data stores have any special needs?
                        else {
                        }
                    }
                }
            }
        }

        return buildGmlLink(gmlParams);
    }

    /**
     * Returns the GML version used in the feature type's definition.
     *
     * <p>The method recursively climbs up the type hierarchy of the provided feature type, until it
     * finds AbstractFeatureType. Then, the GML version is determined by looking at the namespace
     * URI.
     *
     * <p>Please note that this method does not differentiate between GML 2 and GML 3.1.1, but
     * assumes that "http://www.opengis.net/gml" namespace always refers to GML 3.1.1.
     *
     * @param ftInfo the feature type info
     * @return the GML version used in the feature type definition
     * @throws IOException if the underlying datastore instance cannot be retrieved
     */
    String findGmlVersion(FeatureTypeInfo ftInfo) throws IOException {
        ProgressListener listener = new NullProgressListener();
        Name qName = ftInfo.getQualifiedName();
        FeatureType fType = ftInfo.getStore().getDataStore(listener).getSchema(qName);
        return findFeatureTypeGmlVersion(fType);
    }

    private String findFeatureTypeGmlVersion(AttributeType featureType) {
        if (featureType == null) {
            return null;
        }

        if (isAbstractFeatureType(featureType)) {
            String gmlNamespace = featureType.getName().getNamespaceURI();
            if (org.geotools.gml3.GML.NAMESPACE.equals(gmlNamespace)) {
                // GML 3.1.1
                return "gml3";
            } else if (org.geotools.gml3.v3_2.GML.NAMESPACE.equals(gmlNamespace)) {
                // GML 3.2
                return GML32OutputFormat.FORMATS.get(0);
            } else {
                // should never happen
                LOGGER.log(
                        Level.FINE, "Cannot determine GML version from AbstractFeatureType type");
                return null;
            }
        }

        // recursively check super types
        AttributeType parent = featureType.getSuper();
        return findFeatureTypeGmlVersion(parent);
    }

    private boolean isAbstractFeatureType(AttributeType type) {
        if (type == null) {
            return false;
        }

        Name qName = type.getName();
        String localPart = qName.getLocalPart();
        String ns = qName.getNamespaceURI();
        if ("AbstractFeatureType".equals(localPart)
                && (org.geotools.gml3.GML.NAMESPACE.equals(ns)
                        || org.geotools.gml3.v3_2.GML.NAMESPACE.equals(ns))) {
            return true;
        } else {
            return false;
        }
    }

    String buildGmlLink(GMLOutputParams gmlParams) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(gmlParams.baseUrl).append("?");
        urlBuilder.append("service=WFS").append("&");
        urlBuilder.append("version=").append(gmlParams.wfsVersion).append("&");
        urlBuilder.append("request=GetFeature").append("&");
        urlBuilder.append("typeName=").append(getName());
        if (gmlParams.gmlVersion != null) {
            urlBuilder.append("&");
            urlBuilder.append("outputFormat=").append(gmlParams.gmlVersion);
        }

        return urlBuilder.toString();
    }

    class GMLOutputParams {
        String wfsVersion;
        String gmlVersion;
        String baseUrl;

        private GMLOutputParams() {
            // by default, use WFS 1.0.0
            wfsVersion = org.geotools.wfs.v1_0.WFS.getInstance().getVersion();
            // by default, infer GML version from WFS version
            gmlVersion = null;
            // by default, use virtual ows services
            baseUrl = getBaseUrl("ows");
        }
    }
}
