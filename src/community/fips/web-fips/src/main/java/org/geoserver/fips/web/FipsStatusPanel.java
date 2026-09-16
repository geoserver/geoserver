/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips.web;

import java.io.Serial;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.fips.FipsSetup;

/**
 * Reports how cryptography is set up in a FIPS deployment: what is in use, and whether the algorithms GeoServer needs
 * are there. Seeing this tab at all tells the admin that the FIPS module is installed, which is the first thing they
 * want to know.
 */
public class FipsStatusPanel extends Panel {

    @Serial
    private static final long serialVersionUID = 6011862838392561144L;

    public FipsStatusPanel(String id) {
        super(id);
        add(new ListView<>("facts", FipsSetup.getFacts()) {
            @Override
            protected void populateItem(ListItem<FipsSetup.Fact> item) {
                item.add(new Label("label", item.getModelObject().label()));
                item.add(new Label("value", item.getModelObject().value()));
            }
        });
        add(new ListView<>("algorithms", FipsSetup.getRequiredAlgorithms()) {
            @Override
            protected void populateItem(ListItem<FipsSetup.AlgorithmStatus> item) {
                FipsSetup.AlgorithmStatus algorithm = item.getModelObject();
                item.add(new Label("use", algorithm.use()));
                item.add(new Label("type", algorithm.type()));
                item.add(new Label("algorithm", algorithm.algorithm()));
                item.add(new Label("available", algorithm.available() ? "yes" : "no"));
            }
        });
    }
}
