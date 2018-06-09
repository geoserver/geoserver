/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.localIPAsString;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class NodeLinkPanel extends Panel {

    GeoServerDialog dialog;

    @SuppressWarnings("unchecked")
    public NodeLinkPanel(String id, final HzCluster cluster) {
        super(id);

        add(
                new SimpleAjaxLink("link", new Model(localIPAsString(cluster.getHz()))) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        // dialog.show(target);
                        dialog.showOkCancel(
                                target,
                                new GeoServerDialog.DialogDelegate() {

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        return true;
                                    }

                                    @Override
                                    protected Component getContents(String id) {
                                        return new NodeInfoDialog(id);
                                    }
                                });
                    }
                });

        add(new Label("cluster", cluster.getHz().getConfig().getGroupConfig().getName()));

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(350);
        dialog.setInitialWidth(300);
    }
}
