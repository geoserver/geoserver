/*
 *  Copyright (C) 2007 - 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geofence;

import it.geosolutions.geofence.core.model.LayerAttribute;
import it.geosolutions.geofence.core.model.enums.AccessType;
import it.geosolutions.geofence.core.model.enums.GrantType;
import it.geosolutions.geofence.services.RuleReaderService;
import it.geosolutions.geofence.services.dto.AccessInfo;
import it.geosolutions.geofence.services.dto.RuleFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.LayerGroupAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.StyleAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import it.geosolutions.geofence.config.GeoFenceConfiguration;
import it.geosolutions.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.catalog.PublishedInfo;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * Makes GeoServer use the Geofence to assess data access rules
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GeofenceAccessManager implements ResourceAccessManager, DispatcherCallback
{

    private static final Logger LOGGER = Logging.getLogger(GeofenceAccessManager.class);

    /**
     * The role given to the administrators
     */
    static final String ROOT_ROLE = "ROLE_ADMINISTRATOR";

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);

    enum PropertyAccessMode
    {
        READ,
        WRITE
    }

    static final CatalogMode DEFAULT_CATALOG_MODE = CatalogMode.HIDE;

    RuleReaderService rules;

    Catalog catalog;

    private final GeoFenceConfigurationManager configurationManager;

            
    // list of accepted roles, for the useRolesToFilter option
//    List<String> roles = new ArrayList<String>();

    public GeofenceAccessManager(RuleReaderService rules, Catalog catalog,
            GeoFenceConfigurationManager configurationManager) {

        this.rules = rules;
        this.catalog = catalog;
        this.configurationManager = configurationManager;
    }

    boolean isAdmin(Authentication user) {
        if (user.getAuthorities() != null) {
            for (GrantedAuthority authority : user.getAuthorities()) {
                final String userRole = authority.getAuthority();
                if (ROOT_ROLE.equals(userRole) || GeoServerRole.ADMIN_ROLE.getAuthority().equals(userRole) ) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        LOGGER.log(Level.FINE, "Getting access limits for workspace {0}", workspace.getName());

        if ((user != null) && !(user instanceof AnonymousAuthenticationToken)) {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user)) {
                LOGGER.log(Level.FINE, "Admin level access, returning "
                        + "full rights for workspace {0}", workspace.getName());

                return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, true);
            }
            return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, configurationManager.getConfiguration().isGrantWriteToWorkspacesToAuthenticatedUsers());
        }

        // further logic disabled because of https://github.com/geosolutions-it/geofence/issues/6
        return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, false);
    }

    String getSourceAddress(HttpServletRequest http) {
        try {
            if (http == null) {
                LOGGER.log(Level.WARNING, "No HTTP connection available.");
                return null;
            }

            String forwardedFor = http.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String[] ips = forwardedFor.split(", ");

                return InetAddress.getByName(ips[0]).getHostAddress();
            } else {
                return http.getRemoteAddr();
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to get remote address", e);
            return null;
        }
    }


    private String retrieveCallerIpAddress() {

        // is this an OWS request
        Request owsRequest = Dispatcher.REQUEST.get();
        if(owsRequest != null ) {
            HttpServletRequest httpReq = owsRequest.getHttpRequest();
            String sourceAddress = getSourceAddress(httpReq);
            if(sourceAddress == null) {
                LOGGER.log(Level.WARNING, "Could not retrieve source address from OWSRequest");
            }
            return sourceAddress;
        }
        
        // try Spring
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        String sourceAddress = getSourceAddress(request);
        if(sourceAddress == null) {
            LOGGER.log(Level.WARNING, "Could not retrieve source address from Spring Request");
        }
        return sourceAddress;
    }


    private WorkspaceAccessLimits buildAccessLimits(WorkspaceInfo workspace, AccessInfo rule) {
        if (rule == null) {
            return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, true);
        } else {
            return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, rule.getGrant() == GrantType.ALLOW, rule.getGrant() == GrantType.ALLOW);
        }
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style)
    {
        //return getAccessLimits(user, style.getResource());
        LOGGER.fine("Not limiting styles");
    	return null;
    	// TODO
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user,
        LayerGroupInfo layerInfo)
    {
        //return getAccessLimits(user, layerInfo.getResource());
        LOGGER.fine("Not limiting layergroups");
    	return null;
    	// TODO
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer)
    {
        LOGGER.log(Level.FINE, "Getting access limits for Layer {0}", layer.getName());
        return getAccessLimits(user, layer.getResource());
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource)
    {
        LOGGER.log(Level.FINE, "Getting access limits for Resource {0}", resource.getName());
        // extract the user name
        String username = null;
        if ((user != null) && !(user instanceof AnonymousAuthenticationToken))
        {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user))
            {
                LOGGER.log(Level.FINE, "Admin level access, returning " +
                    "full rights for layer {0}", resource.getPrefixedName());

                return buildAccessLimits(resource, AccessInfo.ALLOW_ALL);
            }

            username = user.getName();
        }

        // get info from the current request
        String service = null;
        String request = null;
        Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest != null)
        {
            service = owsRequest.getService();
            request = owsRequest.getRequest();
        }

        // get the resource info
        String layer = resource.getName();
        StoreInfo store = resource.getStore();
        WorkspaceInfo ws = store.getWorkspace();
        String workspace = ws.getName();

        // get the request infos
        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
        setRuleFilterUserOrRole(user, ruleFilter);
        
        ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
        if (service != null)
        {
            if ("*".equals(service))
            {
                ruleFilter.setService(RuleFilter.SpecialFilterType.ANY);
            }
            else
            {
                ruleFilter.setService(service);
            }
        } else {
            ruleFilter.setService(RuleFilter.SpecialFilterType.DEFAULT);
        }

        if (request != null)
        {
            if ("*".equals(request))
            {
                ruleFilter.setRequest(RuleFilter.SpecialFilterType.ANY);
            }
            else
            {
                ruleFilter.setRequest(request);
            }
        } else {
            ruleFilter.setRequest(RuleFilter.SpecialFilterType.DEFAULT);
        }
        ruleFilter.setWorkspace(workspace);
        ruleFilter.setLayer(layer);

        String sourceAddress = retrieveCallerIpAddress();
        if(sourceAddress != null) {
            ruleFilter.setSourceAddress(sourceAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
            ruleFilter.setSourceAddress(RuleFilter.SpecialFilterType.DEFAULT);
        }

        LOGGER.log(Level.FINE, "ResourceInfo filter: {0}", ruleFilter);

        AccessInfo rule = rules.getAccessInfo(ruleFilter);

        if (rule == null)
        {
            rule = AccessInfo.DENY_ALL;
        }

        DataAccessLimits limits = buildAccessLimits(resource, rule);
        LOGGER.log(Level.FINE, "Returning {0} for layer {1} and user {2}",
            new Object[] { limits, resource.getPrefixedName(), username });

        return limits;
    }

    /**
     * @param user
     */
    private void setRuleFilterUserOrRole(Authentication user, RuleFilter ruleFilter) {
        if (user != null) {
            GeoFenceConfiguration config = configurationManager.getConfiguration();
            if (config.isUseRolesToFilter() && config.getRoles().size() > 0) {
                
                String role = "UNKNOWN";
                for (GrantedAuthority authority : user.getAuthorities()) {
                    if (config.getRoles().contains(authority.getAuthority())) {
                        role = authority.getAuthority();
                        
                    }
                }
                LOGGER.log(Level.FINE, "Setting role for filter: {0}",
                        new Object[] { role });
                ruleFilter.setUserGroup(role);
            } else {
                String username = user.getName();
                if (username == null) {
                    ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
                } else {
                    LOGGER.log(Level.FINE, "Setting user for filter: {0}",
                            new Object[] { username });                    
                    ruleFilter.setUser(username);
                }
            }
        } else {
            ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
        }
    
    }

	/**
     * @param resource
     * @param rule
     * @return
     */
    DataAccessLimits buildAccessLimits(ResourceInfo resource, AccessInfo rule)
    {
        // basic filter
        Filter readFilter = (rule.getGrant() == GrantType.ALLOW) ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = (rule.getGrant() == GrantType.ALLOW) ? Filter.INCLUDE : Filter.EXCLUDE;
        try
        {
            if (rule.getCqlFilterRead() != null)
            {
                readFilter = ECQL.toFilter(rule.getCqlFilterRead());
            }
            if (rule.getCqlFilterWrite() != null)
            {
                writeFilter = ECQL.toFilter(rule.getCqlFilterWrite());
            }
        }
        catch (CQLException e)
        {
            throw new IllegalArgumentException("Invalid cql filter found: " + e.getMessage(), e);
        }

        // get the attributes
        List<PropertyName> readAttributes = toPropertyNames(rule.getAttributes(),
                PropertyAccessMode.READ);
        List<PropertyName> writeAttributes = toPropertyNames(rule.getAttributes(),
                PropertyAccessMode.WRITE);

        // reproject the area if necessary
        Geometry area = null;
        String areaWkt = rule.getAreaWkt();
        if(areaWkt != null) {
            try {

//            Geometry area = rule.getArea();
                WKTReader wktReader = new WKTReader();
                area = wktReader.read(areaWkt);

                if ((area != null) && (area.getSRID() > 0)) {
                    CoordinateReferenceSystem geomCrs = CRS.decode("EPSG:" + area.getSRID());
                    CoordinateReferenceSystem resourceCrs = resource.getCRS();
                    if ((resourceCrs != null) && !CRS.equalsIgnoreMetadata(geomCrs, resourceCrs)) {
                        MathTransform mt = CRS.findMathTransform(geomCrs, resourceCrs, true);
                        area = JTS.transform(area, mt);
//                        rule.setArea(area);
                        rule.setAreaWkt(area.toString());
                    }
                }
            } catch (ParseException e) {
                throw new RuntimeException("Failed to unmarshal the restricted area wkt", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to reproject the restricted area to the layer's native SRS", e);
            }
        }

        CatalogMode catalogMode = DEFAULT_CATALOG_MODE;
        
        if(rule.getCatalogMode() != null) {
            switch(rule.getCatalogMode()) {
                case CHALLENGE: 
                    catalogMode = CatalogMode.CHALLENGE;
                    break;
                case HIDE:
                    catalogMode = CatalogMode.HIDE;
                    break;
                case MIXED:
                    catalogMode = CatalogMode.MIXED;
                    break;
            }
        }

        LOGGER.log(Level.FINE, "Returning mode {0} for resource {1}", new Object[] { catalogMode, resource });

        if (resource instanceof FeatureTypeInfo)
        {
            // merge the area among the filters
            if (area != null)
            {
                Filter areaFilter = FF.intersects(FF.property(""), FF.literal(area));
                readFilter = mergeFilter(readFilter, areaFilter);
                writeFilter = mergeFilter(writeFilter, areaFilter);
            }

            return new VectorAccessLimits(catalogMode, readAttributes, readFilter, writeAttributes,
                    writeFilter);
        }
        else if (resource instanceof CoverageInfo)
        {
            MultiPolygon rasterFilter = buildRasterFilter(rule);

            return new CoverageAccessLimits(catalogMode, readFilter, rasterFilter, null);
        }
        else if (resource instanceof WMSLayerInfo)
        {
            MultiPolygon rasterFilter = buildRasterFilter(rule);

            return new WMSAccessLimits(catalogMode, readFilter, rasterFilter, true);
        }
        else
        {
            throw new IllegalArgumentException("Don't know how to handle resource " + resource);
        }
    }

    private MultiPolygon buildRasterFilter(AccessInfo rule)
    {
        MultiPolygon rasterFilter = null;
        if (rule.getAreaWkt() != null)
        {
            WKTReader reader = new WKTReader();
            Geometry area = null;
            try {
                area = reader.read(rule.getAreaWkt());
            } catch (ParseException e) {
                throw new RuntimeException("Failed to unmarshal the restricted area wkt", e);
            }
            rasterFilter = Converters.convert(area, MultiPolygon.class);
            if (rasterFilter == null)
            {
                throw new RuntimeException("Error applying security rules, cannot convert " +
                    "the Geofence area restriction " + rule.getAreaWkt() +
                    " to a multi-polygon");
            }
        }

        return rasterFilter;
    }

    /**
     * Merges the two filters into one by AND
     *
     * @param filter
     * @param areaFilter
     * @return
     */
    private Filter mergeFilter(Filter filter, Filter areaFilter) {
        if ((filter == null) || (filter == Filter.INCLUDE)) {
            return areaFilter;
        } else if (filter == Filter.EXCLUDE) {
            return filter;
        } else {
            return FF.and(filter, areaFilter);
        }
    }

    /**
     * Builds the equivalent {@link PropertyName} list for the specified access mode
     *
     * @param attributes
     * @param mode
     * @return
     */
    private List<PropertyName> toPropertyNames(Set<LayerAttribute> attributes,
            PropertyAccessMode mode) {
        // handle simple case
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        // filter and translate
        List<PropertyName> result = new ArrayList<PropertyName>();
        for (LayerAttribute attribute : attributes) {
            if ((attribute.getAccess() == AccessType.READWRITE)
                    || ((mode == PropertyAccessMode.READ)
                    && (attribute.getAccess() == AccessType.READONLY))) {
                PropertyName property = FF.property(attribute.getName());
                result.add(property);
            }
        }

        return result;
    }

    @Override
    public void finished(Request request) {
        // nothing to do
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Operation operationDispatched(Request gsRequest, Operation operation) {
        // service and request
        String service = gsRequest.getService();
        String request = gsRequest.getRequest();

        // get the user
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if ((user != null) && !(user instanceof AnonymousAuthenticationToken)) {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user)) {
                LOGGER.log(Level.FINE, "Admin level access, not applying default style for this request");

                return operation;
            } else {
                username = user.getName();
            }
        }

        if ((request != null) && "WMS".equalsIgnoreCase(service) && ("GetMap".equalsIgnoreCase(request)
                || "GetFeatureInfo".equalsIgnoreCase(request))) {
            // extract the getmap part
            Object ro = operation.getParameters()[0];
            GetMapRequest getMap;
            if (ro instanceof GetMapRequest) {
                getMap = (GetMapRequest) ro;
            } else if (ro instanceof GetFeatureInfoRequest) {
                getMap = ((GetFeatureInfoRequest) ro).getGetMapRequest();
            } else {
                throw new ServiceException("Unrecognized request object: " + ro);
            }

            overrideGetMapRequest(gsRequest, service, request, user, getMap);
        } else if ((request != null) && "WMS".equalsIgnoreCase(service) && "GetLegendGraphic".equalsIgnoreCase(request)) {
            overrideGetLegendGraphicRequest(gsRequest, operation, service, request, user);

        }

        return operation;
    }

    void overrideGetLegendGraphicRequest(Request gsRequest, Operation operation,
        String service, String request, Authentication user) {
        // get the layer
        String layerName = (String) gsRequest.getKvp().get("LAYER");
        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        LayerInfo candidateLayer = catalog.getLayerByName(layerName);
        if(candidateLayer == null) {
        	if(layerName.indexOf(":") == -1) {
        		// add namespace info to candidate layer group name
        		if(LocalWorkspace.get() != null) {
             		layerName = LocalWorkspace.get().getName() + ":" + layerName;
             	} else if(catalog.getDefaultWorkspace() != null) {
             		layerName = catalog.getDefaultWorkspace().getName() + ":" + layerName;
             	}
        	}
        	LayerGroupInfo layerGroup = catalog.getLayerGroupByName(layerName);
        	if(layerGroup != null) {
                for (PublishedInfo publishedInfo : layerGroup.getLayers()) {
                    if(publishedInfo instanceof LayerInfo) {
                        layers.add((LayerInfo)publishedInfo);
                    } else {
                        if(LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Skipping publishable " + publishedInfo);
                        }
                    }
                }
        	}
        } else {
        	layers.add(candidateLayer);
        }
        
        // get the request object
        GetLegendGraphicRequest getLegend = (GetLegendGraphicRequest) operation.getParameters()[0];
        
        for(LayerInfo layer : layers) {
        	ResourceInfo resource = layer.getResource();

            // get the rule, it contains default and allowed styles
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
            setRuleFilterUserOrRole(user, ruleFilter);            
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            ruleFilter.setWorkspace(resource.getStore().getWorkspace().getName());
            ruleFilter.setLayer(resource.getName());

            LOGGER.log(Level.FINE, "Getting access limits for getLegendGraphic", ruleFilter);

            AccessInfo rule = rules.getAccessInfo(ruleFilter);
            
            // get the requested style
            String styleName = (String) gsRequest.getKvp().get("STYLE");
            if (styleName == null) {
                if (rule.getDefaultStyle() != null) {
                    try {
                        StyleInfo si = catalog.getStyleByName(rule.getDefaultStyle());
                        if (si == null) {
                            throw new ServiceException("Could not find default style suggested "
                                    + "by GeoRepository: " + rule.getDefaultStyle());
                        }
                        getLegend.setStyle(si.getStyle());
                    } catch (IOException e) {
                        throw new ServiceException("Unable to load the style suggested by GeoRepository: "
                                + rule.getDefaultStyle(), e);
                    }
                }
            } else {
                checkStyleAllowed(rule, styleName);
            }
        }
    }

    private void overrideGetMapRequest(Request gsRequest, String service, String request, 
    		Authentication user, GetMapRequest getMap)
    {
		if (gsRequest.getKvp().get("layers") == null
				&& gsRequest.getKvp().get("sld") == null
				&& gsRequest.getKvp().get("sld_body") == null) {
            throw new ServiceException("GetMap POST requests are forbidden");
        }
		
        // check for dynamic style
        if ((getMap.getSld() != null) || (getMap.getSldBody() != null)) {
            if( !configurationManager.getConfiguration().isAllowDynamicStyles() ) {
                throw new ServiceException("Dynamic style usage is forbidden");
            }
        }

        // parse the styles param like the kvp parser would (since we have no way,
        // to know if a certain style was requested explicitly or defaulted, and
        // we need to tell apart the default case from the explicit request case
        String stylesParam = (String) gsRequest.getRawKvp().get("STYLES");
        List<String> styleNameList = new ArrayList<String>();
        if (stylesParam != null)
        {
            styleNameList.addAll(KvpUtils.readFlat(stylesParam));
        }

        // apply the override/security check for each layer in the request
        List<MapLayerInfo> layers = getMap.getLayers();
        for (int i = 0; i < layers.size(); i++)
        {
            MapLayerInfo layer = layers.get(i);
            ResourceInfo info = null;
            if(layer.getType() == MapLayerInfo.TYPE_VECTOR || layer.getType() == MapLayerInfo.TYPE_RASTER) {
            	info = layer.getResource();
            } else if(!configurationManager.getConfiguration().isAllowRemoteAndInlineLayers()) {
                throw new ServiceException("Remote layers are not allowed");                
            }

            // get the rule, it contains default and allowed styles
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);

            setRuleFilterUserOrRole(user, ruleFilter);
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            if(info != null) {
	            ruleFilter.setWorkspace(info.getStore().getWorkspace().getName());
	            ruleFilter.setLayer(info.getName());
	            
            } else {
            	ruleFilter.setWorkspace(RuleFilter.SpecialFilterType.ANY);
            	ruleFilter.setLayer(RuleFilter.SpecialFilterType.ANY);
            }

            LOGGER.log(Level.FINE, "Getting access limits for getMap", ruleFilter);

            AccessInfo rule = rules.getAccessInfo(ruleFilter);

            // get the requested style name
            String styleName = (styleNameList.size() > 0) ? styleNameList.get(i) : null;

            // if default use geofence default
            if (styleName != null) {
                checkStyleAllowed(rule, styleName);
            } else if((rule.getDefaultStyle() != null)) {
                try
                {
                    StyleInfo si = catalog.getStyleByName(rule.getDefaultStyle());
                    if (si == null)
                    {
                        throw new ServiceException("Could not find default style suggested " +
                            "by Geofence: " + rule.getDefaultStyle());
                    }

                    Style style = si.getStyle();
                    getMap.getStyles().set(i, style);
                }
                catch (IOException e)
                {
                    throw new ServiceException("Unable to load the style suggested by Geofence: " +
                        rule.getDefaultStyle(), e);
                }
            }
        }
    }

    /**
     *
     * Allow dynamic styling only when there's no style restriction.
     *
     * We may update the model by adding a "allow dyn style" permission,
     * but it would break current installations.
     * We can infer this authorization by verifying if any style constraint is in place:
     * It means that both of these conditions should be verified:
     * 1) rule.getAllowedStyles() != null
     *    - -> no style restriction has been defined in the rule details
     * 2) the number of allowed styles should be less than than the number of total styles
     *    --> generally, you can't delete the restricted style list once it has been defined.
     *      > We assume that if all the available styles have been allowed, then there should be no
     *      > style restrictions. We're only comparing the number of the available styles, but a
     *      > better approach would be to make sure all of the available styles (maybe identified by name)
     *      > are currently allowed.
     *
     * @param getMap
     * @param rule
     * @param layer
     * @throws ServiceException if dyn style usage is forbidden
     */
    protected void checkDynStyles(GetMapRequest getMap, AccessInfo rule, MapLayerInfo layer) throws ServiceException {
        if ((getMap.getSld() != null) || (getMap.getSldBody() != null))
        {
            if( !configurationManager.getConfiguration().isAllowDynamicStyles() ) {
                LOGGER.info("Denying dynamic style; allowed#"+rule.getAllowedStyles().size() + " avail#"+layer.getLayerInfo().getStyles().size());
                throw new ServiceException("Dynamic style usage is forbidden");
            }
        }
    }

    private void checkStyleAllowed(AccessInfo rule, String styleName) {
        // otherwise check if the requested style is allowed
        Set<String> allowedStyles = new HashSet<String>();
        if (rule.getDefaultStyle() != null) {
            allowedStyles.add(rule.getDefaultStyle());
        }
        if (rule.getAllowedStyles() != null) {
            allowedStyles.addAll(rule.getAllowedStyles());
        }

        if ((allowedStyles.size() > 0) && !allowedStyles.contains(styleName)) {
            throw new ServiceException("The '" + styleName + "' style is not available on this layer");
        }
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        return response;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    
 
}
