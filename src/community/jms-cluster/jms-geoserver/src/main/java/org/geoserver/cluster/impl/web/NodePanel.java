/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.cluster.configuration.JMSConfiguration;

public class NodePanel extends Panel {

    /** serialVersionUID */
    private static final long serialVersionUID = 8112885092637425915L;

    public NodePanel(String id, final JMSConfiguration configuration) {
        super(id);

        add(
                new Label(
                        "instance",
                        (String)
                                configuration.getConfiguration(
                                        JMSConfiguration.INSTANCE_NAME_KEY)));
        add(
                new Label(
                        "group",
                        (String) configuration.getConfiguration(JMSConfiguration.GROUP_KEY)));
    }
}
