/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/acl/layers")
public class DataAccessController extends AbstractAclController {

    DataAccessController() {
        super(DataAccessRuleDAO.get());
    }

    @Override
    protected void addRuleToMap(Comparable rule, Map map) {
        DataAccessRule ruleObject = (DataAccessRule) rule;
        map.put(ruleObject.getKey(), ruleObject.getValue());
    }

    @Override
    protected String keyFor(Comparable rule) {
        return ((DataAccessRule) rule).getKey();
    }

    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

    @Override
    protected Comparable convertEntryToRule(Entry entry) {
        String[] parts = parseElements(((String) entry.getKey()));

        AccessMode accessMode = AccessMode.getByAlias(parts[2]);

        return new DataAccessRule(
                parts[0], parts[1], accessMode, parseRoles((String) entry.getValue()));
    }

    @Override
    protected String validateRuleKey(String ruleKey) {
        String[] elements = parseElements(ruleKey);
        if (elements.length != 3) {
            return "Invalid rule "
                    + ruleKey
                    + ", the expected format is workspace.layer.mode=role1,role2,...";
        }

        String workspace = elements[0];
        String layerName = elements[1];
        String modeAlias = elements[2];

        AccessMode mode = AccessMode.getByAlias(modeAlias);
        if (mode == null) {
            return "Unknown access mode " + modeAlias + " in " + ruleKey;
        }

        if (ANY.equals(workspace)) {
            if (!ANY.equals(layerName)) {
                return "Invalid rule "
                        + ruleKey
                        + ", when namespace "
                        + "is * then also layer must be *.";
            }
        }
        if (mode == AccessMode.ADMIN && !ANY.equals(layerName)) {
            return "Invalid rule "
                    + ruleKey
                    + ", admin (a) privileges may only be applied "
                    + "globally to a workspace, layer must be *.";
        }

        return null;
    }

    @Override
    protected String getBasePath() {
        return "/security/acl/layers";
    }
}
