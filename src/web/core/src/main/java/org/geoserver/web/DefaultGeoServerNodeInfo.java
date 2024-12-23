/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.GeoServerNodeData;
import org.geoserver.web.spring.security.GeoServerSession;

/**
 * Default node id customizer, will respond to a system variable, env variable or servlet context
 * variable named GEOSERVER_NODE_OPTS with the following sytanx:
 * <code>id:<theid>;background:<bgcolor>;color=<color><code>
 * The <code>background</code> and <code>color</code> properties are optional, the id can be a fixed
 * string or can contain the <code>$host_ip</code> or <code>$host_name</code> variable that will be
 * expanded to the first non loopback IP address of the machine, or the equivalent host name
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultGeoServerNodeInfo implements GeoServerNodeInfo, Serializable {

    private static final long serialVersionUID = -8731277645321595181L;

    static final String GEOSERVER_NODE_OPTS = GeoServerNodeData.GEOSERVER_NODE_OPTS;

    static GeoServerNodeData NODE_DATA = null;

    static {
        initializeFromEnviroment();
    }

    @Override
    public GeoServerNodeData getData() {
        return NODE_DATA;
    }

    @Override
    public void customize(WebMarkupContainer container) {
        container.add(AttributeModifier.replace("class", "default-node-info"));
        Map<String, String> properties = NODE_DATA.getIdStyle();
        if (properties != null && !properties.isEmpty()) {
            container.add(new Behavior() {
                private static final long serialVersionUID = -7945010069411202354L;

                @Override
                public void renderHead(Component component, IHeaderResponse response) {
                    String script = toJavaScript(properties);
                    response.render(OnLoadHeaderItem.forScript(script));
                }
            });
        }
        container.setVisible(isNodeIdVisible(container));
    }

    private static String toJavaScript(Map<String, String> properties) {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, String> entry : properties.entrySet()) {
            builder.append("document.getElementsByClassName('default-node-info')[0].style.");
            builder.append(StringEscapeUtils.escapeEcmaScript(entry.getKey()));
            builder.append(" = '");
            builder.append(StringEscapeUtils.escapeEcmaScript(entry.getValue()));
            builder.append("';\n");
        }
        return builder.toString().trim();
    }

    protected static void initializeFromEnviroment() {
        NODE_DATA = GeoServerNodeData.createFromEnvironment();
    }

    /** The element is visible if an admin is logged in, and the id is not null */
    protected boolean isNodeIdVisible(WebMarkupContainer parent) {
        if (NODE_DATA.getId() == null) {
            return false;
        }
        // we don't show the node id to all users, only to the admin
        return ((GeoServerSession) parent.getSession()).isAdmin();
    }
}
