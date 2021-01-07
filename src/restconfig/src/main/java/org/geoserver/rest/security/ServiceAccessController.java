/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/acl/services")
public class ServiceAccessController
        extends AbstractAclController<ServiceAccessRule, ServiceAccessRuleDAO> {

    public ServiceAccessController() {
        super(ServiceAccessRuleDAO.get());
    }

    @Override
    protected void addRuleToMap(ServiceAccessRule rule, Map<String, String> map) {
        map.put(rule.getKey(), rule.getValue());
    }

    @Override
    protected String keyFor(ServiceAccessRule rule) {
        return rule.getKey();
    }

    @Override
    protected ServiceAccessRule convertEntryToRule(Entry<String, String> entry) {
        String[] parts = parseElements(entry.getKey());
        return new ServiceAccessRule(parts[0], parts[1], parseRoles(entry.getValue()));
    }

    @Override
    protected String validateRuleKey(String ruleKey) {
        String[] elements = parseElements(ruleKey);
        if (elements.length != 2) {
            return "Invalid rule "
                    + ruleKey
                    + ", the expected format is service.method=role1,role2,...";
        }

        if (ANY.equals(elements[0])) {
            if (!ANY.equals(elements[1])) {
                return "Invalid rule "
                        + ruleKey
                        + ", when namespace "
                        + "is * then also layer must be *.";
            }
        }

        return null;
    }

    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

    @Override
    protected String getBasePath() {
        return "/security/acl/services";
    }
}
