/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.style.Style;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * WMS dispatcher callback that enforces ACL authorization rules on WMS requests.
 *
 * <p>This callback intercepts WMS operations (GetMap, GetFeatureInfo, GetLegendGraphic) and applies ACL-based style
 * restrictions. It checks user permissions for requested layers and styles, applying default styles when no specific
 * style is requested, and validating that explicitly requested styles are allowed by the ACL rules.
 *
 * @author Andrea Aime - GeoSolutions - Originally as part of GeoFence's GeoServer extension
 * @author Emanuele Tajariol- GeoSolutions - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public class WmsRequestAclEnforcerInterceptor extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(WmsRequestAclEnforcerInterceptor.class);

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);

    private AuthorizationService authorizationService;

    private Catalog catalog;

    public WmsRequestAclEnforcerInterceptor(AuthorizationService aclService, LocalWorkspaceCatalog catalog) {

        this.authorizationService = aclService;
        this.catalog = catalog;
    }

    /**
     * Intercepts WMS operations to apply ACL-based authorization and style restrictions.
     *
     * <p>Admin users bypass all restrictions. For non-admin users, this method processes:
     *
     * <ul>
     *   <li>GetMap and GetFeatureInfo requests - validates and overrides styles based on ACL rules
     *   <li>GetLegendGraphic requests - ensures only allowed styles are used for legend generation
     * </ul>
     *
     * @param gsRequest the incoming GeoServer request
     * @param operation the operation being dispatched
     * @return the operation, potentially modified with ACL-enforced style restrictions
     */
    @Override
    public Operation operationDispatched(Request gsRequest, Operation operation) {
        // service and request
        String service = gsRequest.getService();
        String request = gsRequest.getRequest();

        // get the user
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        // shortcut, if the user is the admin, he can do everything
        if (AclResourceAccessManager.isAdmin(user)) {
            LOGGER.finer("Admin level access, not applying default style for this request");
            return operation;
        }

        if ((request != null)
                && "WMS".equalsIgnoreCase(service)
                && ("GetMap".equalsIgnoreCase(request) || "GetFeatureInfo".equalsIgnoreCase(request))) {
            // extract the getmap part
            Object ro = operation.getParameters()[0];
            GetMapRequest getMap;
            if (ro instanceof GetMapRequest mapRequest) {
                getMap = mapRequest;
            } else if (ro instanceof GetFeatureInfoRequest infoRequest) {
                getMap = infoRequest.getGetMapRequest();
            } else {
                throw new ServiceException("Unrecognized request object: " + ro);
            }

            overrideGetMapRequest(gsRequest, service, request, user, getMap);
        } else if ((request != null)
                && "WMS".equalsIgnoreCase(service)
                && "GetLegendGraphic".equalsIgnoreCase(request)) {
            overrideGetLegendGraphicRequest(gsRequest, operation, service, request, user);
        }

        return operation;
    }

    /**
     * Applies ACL style restrictions to GetLegendGraphic requests.
     *
     * <p>For each layer in the request (including layers within layer groups), this method:
     *
     * <ul>
     *   <li>Retrieves ACL access rules for the layer
     *   <li>If no style is specified, applies the ACL-defined default style
     *   <li>If a style is specified, validates it against the allowed styles from ACL
     * </ul>
     *
     * @param gsRequest the GeoServer request containing layer and style parameters
     * @param operation the operation containing the GetLegendGraphicRequest
     * @param service the service name (WMS)
     * @param request the request type (GetLegendGraphic)
     * @param user the authenticated user making the request
     * @throws ServiceException if a required default style cannot be found or loaded
     */
    void overrideGetLegendGraphicRequest(
            Request gsRequest, Operation operation, String service, String request, Authentication user) {
        // get the layer
        String layerName = (String) gsRequest.getKvp().get("LAYER");
        String reqStyle = (String) gsRequest.getKvp().get("STYLE");
        List<String> styles = new ArrayList<>();
        List<LayerInfo> layers = new ArrayList<>();
        LayerInfo candidateLayer = catalog.getLayerByName(layerName);
        if (candidateLayer == null) {
            LayerGroupInfo layerGroup = catalog.getLayerGroupByName(layerName);
            if (layerGroup != null) {
                boolean emptyStyleName = reqStyle == null || "".equals(reqStyle);
                layers.addAll(emptyStyleName ? layerGroup.layers() : layerGroup.layers(reqStyle));
                addGroupStyles(layerGroup, styles, reqStyle);
            }
        } else {
            layers.add(candidateLayer);
            styles.add(reqStyle);
        }

        // get the request object
        GetLegendGraphicRequest getLegend = (GetLegendGraphicRequest) operation.getParameters()[0];
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo layer = layers.get(i);
            ResourceInfo resource = layer.getResource();

            AccessInfo layerConstraints = getLayerAccessConstraints(user, resource, service, request);

            // get the requested style
            String styleName = styles.get(i);
            if (styleName == null) {
                if (layerConstraints.getDefaultStyle() != null) {
                    try {
                        StyleInfo si = catalog.getStyleByName(layerConstraints.getDefaultStyle());
                        if (si == null) {
                            throw new ServiceException("Could not find default style suggested "
                                    + "by GeoRepository: "
                                    + layerConstraints.getDefaultStyle());
                        }
                        getLegend.setStyle(si.getStyle());
                    } catch (IOException e) {
                        throw new ServiceException(
                                "Unable to load the style suggested by GeoRepository: "
                                        + layerConstraints.getDefaultStyle(),
                                e);
                    }
                }
            } else {
                checkStyleAllowed(layerConstraints, styleName);
            }
        }
    }

    /**
     * Applies ACL style restrictions to GetMap and GetFeatureInfo requests.
     *
     * <p>This method enforces security by:
     *
     * <ul>
     *   <li>Forbidding POST requests without proper parameters (security measure)
     *   <li>Parsing the requested layers and styles from the KVP parameters
     *   <li>For each layer, checking ACL rules to determine allowed styles
     *   <li>Applying ACL default styles when no specific style is requested
     *   <li>Validating explicitly requested styles are in the ACL allowed list
     * </ul>
     *
     * @param gsRequest the GeoServer request containing KVP parameters
     * @param service the service name (WMS)
     * @param request the request type (GetMap or GetFeatureInfo)
     * @param user the authenticated user making the request
     * @param getMap the GetMapRequest object to be modified with ACL-enforced styles
     * @throws ServiceException if POST requests are forbidden, or if styles cannot be found/loaded
     */
    void overrideGetMapRequest(
            final Request gsRequest,
            final String service,
            final String request,
            final Authentication user,
            GetMapRequest getMap) {

        if (gsRequest.getKvp().get("layers") == null
                && gsRequest.getKvp().get("sld") == null
                && gsRequest.getKvp().get("sld_body") == null) {
            throw new ServiceException("GetMap POST requests are forbidden");
        }

        // parse the styles param like the kvp parser would (since we have no way,
        // to know if a certain style was requested explicitly or defaulted, and
        // we need to tell apart the default case from the explicit request case
        List<String> styleNameList = extractRequestedStyles(gsRequest, getMap);

        // apply the override/security check for each layer in the request
        List<MapLayerInfo> layers = getMap.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayerInfo layer = layers.get(i);
            ResourceInfo resource = null;
            if (layer.getType() == MapLayerInfo.TYPE_VECTOR || layer.getType() == MapLayerInfo.TYPE_RASTER) {
                resource = layer.getResource();
            }

            // obtain default and allowed styles
            AccessInfo layerConstraints = getLayerAccessConstraints(user, resource, service, request);

            // get the requested style name
            String styleName = styleNameList.get(i);

            // if default use ACL's default
            if (styleName != null) {
                checkStyleAllowed(layerConstraints, styleName);
            } else if ((layerConstraints.getDefaultStyle() != null)) {
                try {
                    StyleInfo si = catalog.getStyleByName(layerConstraints.getDefaultStyle());
                    if (si == null) {
                        throw new ServiceException(
                                "Could not find default style suggested by ACL: " + layerConstraints.getDefaultStyle());
                    }

                    Style style = si.getStyle();
                    getMap.getStyles().set(i, style);
                } catch (IOException e) {
                    throw new ServiceException(
                            "Unable to load the style suggested by ACL: " + layerConstraints.getDefaultStyle(), e);
                }
            }
        }
    }

    private AccessInfo getLayerAccessConstraints(
            Authentication user, ResourceInfo info, String service, String request) {
        AccessRequest layerAccessRequest;

        String workspace = info == null ? null : info.getStore().getWorkspace().getName();
        String layerName = info == null ? null : info.getName();
        layerAccessRequest = AuthorizationRequestBuilder.data()
                .user(user)
                .service(service)
                .request(request)
                .workspace(workspace)
                .layer(layerName)
                .build();

        return authorizationService.getAccessInfo(layerAccessRequest);
    }

    /**
     * Validates that a requested style is allowed by ACL rules.
     *
     * <p>A style is considered allowed if it appears in either the default style or the allowed styles list from the
     * ACL AccessInfo. If ACL defines allowed styles and the requested style is not in that list, a ServiceException is
     * thrown.
     *
     * @param accessInfo the ACL access information containing allowed and default styles
     * @param styleName the name of the style being requested
     * @throws ServiceException if the style is not in the allowed styles list
     */
    private void checkStyleAllowed(AccessInfo accessInfo, String styleName) {
        // otherwise check if the requested style is allowed
        Set<String> allowedStyles = new HashSet<>();
        if (accessInfo.getDefaultStyle() != null) {
            allowedStyles.add(accessInfo.getDefaultStyle());
        }
        if (accessInfo.getAllowedStyles() != null) {
            allowedStyles.addAll(accessInfo.getAllowedStyles());
        }

        if ((!allowedStyles.isEmpty()) && !allowedStyles.contains(styleName)) {
            throw new ServiceException("The '" + styleName + "' style is not available on this layer");
        }
    }

    /**
     * Extracts and expands the requested styles from the request parameters.
     *
     * <p>Returns a list that contains the requested styles corresponding to the layers in GetMap.getLayers(). This
     * method handles layer groups by expanding them into individual layers and their associated styles. If fewer styles
     * are specified than layers, the missing entries are filled with null (indicating default style should be used).
     *
     * @param gsRequest the request containing the STYLES KVP parameter
     * @param getMap the GetMapRequest containing layer information
     * @return a list of style names (or null for default style) matching each layer
     */
    private List<String> extractRequestedStyles(final Request gsRequest, final GetMapRequest getMap) {
        List<String> requestedStyles = new ArrayList<>();
        int styleIndex = 0;
        List<String> parsedStyles = parseStylesParameter(gsRequest);
        for (Object layer : parseLayersParameter(gsRequest, getMap)) {
            boolean outOfBound = styleIndex >= parsedStyles.size();
            if (layer instanceof LayerGroupInfo info) {
                String styleName = outOfBound ? null : parsedStyles.get(styleIndex);
                addGroupStyles(info, requestedStyles, styleName);
            } else {
                // the layer is a LayerInfo or MapLayerInfo (if it is a remote layer)
                if (outOfBound) {
                    requestedStyles.add(null);
                } else {
                    requestedStyles.add(parsedStyles.get(styleIndex));
                }
            }
            styleIndex++;
        }
        return requestedStyles;
    }

    /**
     * Expands a layer group into individual layer styles and adds them to the requested styles list.
     *
     * <p>If a specific style name is provided, it retrieves the styles associated with that named style from the layer
     * group. Otherwise, it uses the default styles for the group.
     *
     * @param groupInfo the layer group to expand
     * @param requestedStyles the list to add the expanded styles to
     * @param styleName the specific style name for the group, or null/empty for default styles
     */
    private void addGroupStyles(LayerGroupInfo groupInfo, List<String> requestedStyles, String styleName) {
        List<StyleInfo> groupStyles;
        if (styleName != null && !"".equals(styleName)) groupStyles = groupInfo.styles(styleName);
        else groupStyles = groupInfo.styles();

        requestedStyles.addAll(groupStyles.stream()
                .map(s -> s != null ? s.prefixedName() : null)
                .collect(Collectors.toList()));
    }

    /**
     * Parses the LAYERS parameter from the request into a list of layer objects.
     *
     * <p>Returns a list of LayerInfo, LayerGroupInfo, or MapLayerInfo objects (for remote layers) based on the LAYERS
     * KVP parameter. Uses the GetMapKvpRequestReader to handle the parsing.
     *
     * @param gsRequest the request containing the LAYERS parameter
     * @param getMap the GetMapRequest containing remote OWS information
     * @return a list of layer objects (LayerInfo, LayerGroupInfo, or MapLayerInfo)
     */
    private List<Object> parseLayersParameter(Request gsRequest, GetMapRequest getMap) {
        String rawLayersParameter = (String) gsRequest.getRawKvp().get("LAYERS");
        if (rawLayersParameter != null) {
            List<String> layersNames = KvpUtils.readFlat(rawLayersParameter);
            return LayersKvpParser.getInstance()
                    .parseLayers(layersNames, getMap.getRemoteOwsURL(), getMap.getRemoteOwsType());
        }
        return new ArrayList<>();
    }

    /**
     * Parses the STYLES parameter from the request into a list of style names.
     *
     * @param gsRequest the request containing the STYLES parameter
     * @return a list of style names (may be empty if parameter is not present)
     */
    private List<String> parseStylesParameter(Request gsRequest) {
        String rawStylesParameter = (String) gsRequest.getRawKvp().get("STYLES");
        if (rawStylesParameter != null) {
            return KvpUtils.readFlat(rawStylesParameter);
        }
        return new ArrayList<>();
    }

    /**
     * Helper class that provides access to the protected parseLayers method from GetMapKvpRequestReader.
     *
     * <p>This avoids duplicating the layer parsing logic and reuses the standard GeoServer implementation for parsing
     * layer names into layer objects.
     */
    static final class LayersKvpParser extends GetMapKvpRequestReader {

        private static LayersKvpParser singleton = null;

        public static synchronized LayersKvpParser getInstance() {
            if (singleton == null) singleton = new LayersKvpParser();
            return singleton;
        }

        private LayersKvpParser() {
            super(WMS.get());
        }

        /**
         * Parses layer names into layer objects, handling both local and remote layers.
         *
         * @param requestedLayerNames the list of layer names from the request
         * @param remoteOwsUrl URL of remote OWS service, if applicable
         * @param remoteOwsType type of remote OWS service (WMS, WFS, etc.)
         * @return list of layer objects (LayerInfo, LayerGroupInfo, or MapLayerInfo)
         * @throws ServiceException if parsing fails
         */
        @Override
        public List<Object> parseLayers(List<String> requestedLayerNames, URL remoteOwsUrl, String remoteOwsType) {
            try {
                return super.parseLayers(requestedLayerNames, remoteOwsUrl, remoteOwsType);
            } catch (Exception exception) {
                throw new ServiceException("Error parsing requested layers.", exception);
            }
        }
    }
}
