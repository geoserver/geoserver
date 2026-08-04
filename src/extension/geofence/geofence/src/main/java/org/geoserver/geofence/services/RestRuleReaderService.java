/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.geofence.core.services.RuleReaderService;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.AccessTypeDTO;
import org.geofence.core.services.dto.CatalogModeDTO;
import org.geofence.core.services.dto.GrantTypeDTO;
import org.geofence.core.services.dto.LayerAttributeDTO;
import org.geofence.core.services.dto.PermsResult;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.RuleFilter.FilterType;
import org.geofence.core.services.dto.RuleFilter.IdNameFilter;
import org.geofence.core.services.dto.RuleFilter.TextFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geofence.web.rest.api.interfaces.params.RESTRuleFilter;
import org.geofence.web.rest.api.model.RESTAccessInfo;
import org.geofence.web.rest.api.model.RESTLayerAttribute;
import org.geofence.web.rest.api.model.RESTPermsResult;
import org.geofence.web.rest.api.model.RESTShortRule;
import org.geofence.web.rest.api.model.enums.RESTAccessType;
import org.geofence.web.rest.api.model.enums.RESTCatalogMode;
import org.geofence.web.rest.api.model.enums.RESTGrantType;
import org.geofence.web.rest.client.GeoFenceClient;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST-based {@link RuleReaderService}: talks to a remote GeoFence server's {@code /rulereader} endpoint, for
 * deployments where the embedded engine (see {@code geofence-server}) isn't used.
 */
@Component
public class RestRuleReaderService implements RuleReaderService {

    // null for throwaway instances (e.g. GeofencePage's test-connection button); seeds updatedUrl at startup,
    // kept in sync by GeoFenceConfigurationController on save.
    @Autowired(required = false)
    private GeoFenceConfigurationManager configManager;

    private GeoFenceClient client;

    private String currentUrl;
    private String updatedUrl;

    @PostConstruct
    void initServiceUrl() {
        if (configManager != null) {
            updatedUrl = configManager.getConfiguration().getServicesUrl();
        }
    }

    @Override
    public AccessInfo getAccessInfo(RuleFilter filter) {
        RESTAccessInfo restInfo = ruleReaderClient().getAccessInfo(toRestFilter(filter));
        return toAccessInfo(restInfo);
    }

    @Override
    public AccessInfo getAdminAuthorization(RuleFilter filter) {
        RESTAccessInfo restInfo = ruleReaderClient().getAdminAuthorization(toRestFilter(filter));
        return toAccessInfo(restInfo);
    }

    @Override
    public List<ShortRule> getMatchingRules(RuleFilter filter) {
        List<ShortRule> rules = new ArrayList<>();
        ruleReaderClient().getMatchingRules(toRestFilter(filter)).forEach(restRule -> rules.add(toShortRule(restRule)));
        return rules;
    }

    @Override
    public PermsResult getPermissionFilter(RuleFilter filter) {
        RESTPermsResult restResult = ruleReaderClient().getPermissionFilter(toRestFilter(filter));
        return toPermsResult(restResult);
    }

    public void setServiceUrl(String serviceUrl) {
        this.updatedUrl = serviceUrl;
    }

    private synchronized org.geofence.web.rest.api.interfaces.RESTRuleReaderService ruleReaderClient() {
        if (client == null || !updatedUrl.equals(currentUrl)) {
            GeoFenceClient newClient = new GeoFenceClient();
            newClient.setRestUrl(updatedUrl);
            client = newClient;
            currentUrl = updatedUrl;
        }
        return client.getRuleReaderService();
    }

    // ==========================================================================
    // RuleFilter -> RESTRuleFilter

    static RESTRuleFilter toRestFilter(RuleFilter filter) {
        RESTRuleFilter query = new RESTRuleFilter();

        TextQuery user = toQuery(filter.getUser());
        query.userName = user.name();
        query.userDefault = user.includeDefault();

        TextQuery role = toQuery(filter.getRole());
        query.groupName = role.name();
        query.groupDefault = role.includeDefault();

        IdNameQuery instance = toQuery(filter.getInstance());
        query.instanceId = instance.id();
        query.instanceName = instance.name();
        query.instanceDefault = instance.includeDefault();

        TextQuery sourceAddress = toQuery(filter.getSourceAddress());
        query.ipAddress = sourceAddress.name();
        query.ipAddressDefault = sourceAddress.includeDefault();

        TextQuery date = toQuery(filter.getDate());
        query.date = date.name();
        query.dateDefault = date.includeDefault();

        TextQuery service = toQuery(filter.getService());
        query.serviceName = service.name();
        query.serviceDefault = service.includeDefault();

        TextQuery request = toQuery(filter.getRequest());
        query.requestName = request.name();
        query.requestDefault = request.includeDefault();

        TextQuery subfield = toQuery(filter.getSubfield());
        query.subfieldName = subfield.name();
        query.subfieldDefault = subfield.includeDefault();

        TextQuery workspace = toQuery(filter.getWorkspace());
        query.workspace = workspace.name();
        query.workspaceDefault = workspace.includeDefault();

        TextQuery layer = toQuery(filter.getLayer());
        query.layer = layer.name();
        query.layerDefault = layer.includeDefault();

        return query;
    }

    private record TextQuery(String name, Boolean includeDefault) {}

    private static TextQuery toQuery(TextFilter filter) {
        if (filter.getType() == FilterType.NAMEVALUE) {
            return new TextQuery(filter.getText(), filter.isIncludeDefault());
        } else if (filter.getType() == FilterType.DEFAULT) {
            return new TextQuery(null, true);
        } else {
            return new TextQuery(null, null);
        }
    }

    private record IdNameQuery(Long id, String name, Boolean includeDefault) {}

    private static IdNameQuery toQuery(IdNameFilter filter) {
        if (filter.getType() == FilterType.IDVALUE) {
            return new IdNameQuery(filter.getId(), null, filter.isIncludeDefault());
        } else if (filter.getType() == FilterType.NAMEVALUE) {
            return new IdNameQuery(null, filter.getName(), filter.isIncludeDefault());
        } else if (filter.getType() == FilterType.DEFAULT) {
            return new IdNameQuery(null, null, true);
        } else {
            return new IdNameQuery(null, null, null);
        }
    }

    // ==========================================================================
    // RESTAccessInfo -> AccessInfo / RESTShortRule -> ShortRule

    static AccessInfo toAccessInfo(RESTAccessInfo in) {
        AccessInfo out = new AccessInfo(map(in.getGrant()));
        out.setAdminRights(in.isAdminRights());
        out.setAreaWkt(in.getAreaWkt());
        out.setClipAreaWkt(in.getClipAreaWkt());
        out.setCatalogMode(map(in.getCatalogMode()));
        out.setDefaultStyle(in.getDefaultStyle());
        out.setCqlFilterRead(in.getCqlFilterRead());
        out.setCqlFilterWrite(in.getCqlFilterWrite());
        out.setAllowedStyles(in.getAllowedStyles());
        if (in.getAttributes() != null) {
            out.setAttributes(in.getAttributes().stream()
                    .map(RestRuleReaderService::map)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        return out;
    }

    static PermsResult toPermsResult(RESTPermsResult in) {
        PermsResult out = new PermsResult();
        out.setCqlFilter(in.getCqlFilter());
        if (in.getAccessibleResources() != null) {
            out.setAccessibleResources(in.getAccessibleResources());
        }
        return out;
    }

    static ShortRule toShortRule(RESTShortRule in) {
        ShortRule out = new ShortRule();
        out.setId(in.getId());
        out.setPriority(in.getPriority());
        out.setUserName(in.getUserName());
        out.setRoleName(in.getRoleName());
        out.setInstanceId(in.getInstanceId());
        out.setInstanceName(in.getInstanceName());
        out.setAddressRange(in.getAddressRange());
        out.setValidAfter(in.getValidAfter());
        out.setValidBefore(in.getValidBefore());
        out.setService(in.getService());
        out.setRequest(in.getRequest());
        out.setSubfield(in.getSubfield());
        out.setWorkspace(in.getWorkspace());
        out.setLayer(in.getLayer());
        out.setAccess(map(in.getAccess()));
        return out;
    }

    private static GrantTypeDTO map(RESTGrantType in) {
        return in == null ? null : GrantTypeDTO.valueOf(in.name());
    }

    private static AccessTypeDTO map(RESTAccessType in) {
        return in == null ? null : AccessTypeDTO.valueOf(in.name());
    }

    private static CatalogModeDTO map(RESTCatalogMode in) {
        return in == null ? null : CatalogModeDTO.valueOf(in.name());
    }

    private static LayerAttributeDTO map(RESTLayerAttribute in) {
        LayerAttributeDTO out = new LayerAttributeDTO();
        out.setName(in.getName());
        out.setDatatype(in.getDatatype());
        out.setAccess(map(in.getAccess()));
        return out;
    }
}
