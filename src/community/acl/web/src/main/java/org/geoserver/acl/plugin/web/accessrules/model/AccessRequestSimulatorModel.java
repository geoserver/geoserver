/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.plugin.web.components.AbstractRulesModel;
import org.geoserver.acl.plugin.web.support.ApplicationContextSupport;
import org.geoserver.acl.plugin.web.support.SerializablePredicate;

@SuppressWarnings("serial")
public class AccessRequestSimulatorModel extends AbstractRulesModel {

    private final @Getter CompoundPropertyModel<MutableAccessRequest> model =
            new CompoundPropertyModel<>(new MutableAccessRequest());

    private final @Getter IModel<AccessInfo> accessInfoModel = new LoadableDetachableModel<>() {
        protected @Override AccessInfo load() {
            return getAccessInfo();
        }
    };

    @Override
    protected String getSelectedRoleName() {
        return null;
    }

    @Override
    protected String getSelectedWorkspace() {
        return getModel().getObject().getWorkspace();
    }

    public Iterator<String> getServiceChoices(String input) {
        final Pattern test = caseInsensitiveStartsWith(input);
        return findServiceNames().stream()
                .filter(service -> inputMatches(test, service))
                .iterator();
    }

    public Iterator<String> getRequestChoices(String input) {
        String service = getModel().getObject().getService();
        List<String> requests = findOperationNames(service);
        final Pattern test = caseInsensitiveStartsWith(input);
        return requests.stream().filter(request -> inputMatches(test, request)).iterator();
    }

    public Set<String> findUserRoles() {
        String userName = getModel().getObject().getUser();
        return super.findUserRoles(userName).collect(Collectors.toSet());
    }

    private Set<String> matchingRules = Set.of();

    public boolean runSimulation() {
        AccessInfo accessInfo = getAccessInfo();
        getAccessInfoModel().setObject(accessInfo);
        Set<String> newMatchingRules = Set.copyOf(accessInfo.getMatchingRules());
        if (matchingRules.equals(newMatchingRules)) {
            return false;
        }
        this.matchingRules = newMatchingRules;
        return true;
    }

    private AccessInfo getAccessInfo() {
        AccessRequest request = getModel().getObject().toRequest();
        AuthorizationService authorizationService = authorizationService();
        return authorizationService.getAccessInfo(request);
    }

    public boolean isMatchingRules() {
        return !matchingRules.isEmpty();
    }

    private AuthorizationService authorizationService() {
        return ApplicationContextSupport.getBeanOfType(AuthorizationService.class);
    }

    public SerializablePredicate<MutableRule> getMatchingRulesFilter() {
        return rule -> matchingRules.contains(rule.getId());
    }

    public void clear() {
        getModel().setObject(new MutableAccessRequest());
        getAccessInfoModel().detach();
    }
}
