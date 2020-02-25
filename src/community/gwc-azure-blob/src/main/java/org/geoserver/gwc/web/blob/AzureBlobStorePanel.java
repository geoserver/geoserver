/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geowebcache.azure.AzureBlobStoreInfo;

/** Panel for AzureBlobStore */
public class AzureBlobStorePanel extends Panel {

    private static final long serialVersionUID = -8237328668463257329L;

    public AzureBlobStorePanel(String id, final IModel<AzureBlobStoreInfo> configModel) {
        super(id, configModel);

        add(
                new TextField<String>("container")
                        .setRequired(true)
                        .add(new AttributeModifier("title", new ResourceModel("container.title"))));
        add(
                new TextField<String>("accountName")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("accountName.title"))));
        add(
                new TextField<String>("accountKey")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("accountKey.title"))));
        add(
                new TextField<String>("prefix")
                        .add(new AttributeModifier("title", new ResourceModel("prefix.title"))));
        add(
                new TextField<String>("serviceURL")
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("serviceURL.title"))));
        add(
                new TextField<Integer>("maxConnections")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("maxConnections.title"))));
        add(
                new CheckBox("useHTTPS")
                        .add(new AttributeModifier("title", new ResourceModel("useHTTPS.title"))));
        add(
                new TextField<String>("proxyHost")
                        .add(new AttributeModifier("title", new ResourceModel("proxyHost.title"))));
        add(
                new TextField<Integer>("proxyPort")
                        .add(new AttributeModifier("title", new ResourceModel("proxyPort.title"))));
        add(
                new TextField<String>("proxyUsername")
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("proxyUsername.title"))));
        add(
                new TextField<String>("proxyPassword")
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("proxyPassword.title"))));
    }
}
