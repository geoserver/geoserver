/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geowebcache.s3.Access;
import org.geowebcache.s3.S3BlobStoreInfo;

/**
 * Panel for S3BlobStore
 *
 * @author Niels Charlier
 */
public class S3BlobStorePanel extends Panel {

    private static final long serialVersionUID = -8237328668463257329L;

    public S3BlobStorePanel(String id, final IModel<S3BlobStoreInfo> configModel) {
        super(id, configModel);

        add(
                new TextField<String>("bucket")
                        .setRequired(true)
                        .add(new AttributeModifier("title", new ResourceModel("bucket.title"))));
        add(
                new TextField<String>("awsAccessKey")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("awsAccessKey.title"))));
        add(
                new TextField<String>("awsSecretKey")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("awsSecretKey.title"))));
        add(
                new TextField<String>("prefix")
                        .add(new AttributeModifier("title", new ResourceModel("prefix.title"))));
        add(
                new TextField<String>("endpoint")
                        .add(new AttributeModifier("title", new ResourceModel("endpoint.title"))));
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
                new TextField<String>("proxyDomain")
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("proxyDomain.title"))));
        add(
                new TextField<String>("proxyWorkstation")
                        .add(
                                new AttributeModifier(
                                        "title", new ResourceModel("proxyWorkstation.title"))));
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
        add(
                new CheckBox("useGzip")
                        .add(new AttributeModifier("title", new ResourceModel("useGzip.title"))));

        IModel<Access> accessModel = new PropertyModel<Access>(configModel, "access");

        RadioGroup<Access> accessType = new RadioGroup<Access>("accessType", accessModel);
        add(accessType);

        IModel<Access> accessPublicModel = new Model<Access>(Access.PUBLIC);
        IModel<Access> accessPrivateModel = new Model<Access>(Access.PRIVATE);

        Radio<Access> accessTypePublic = new Radio<Access>("accessTypePublic", accessPublicModel);
        Radio<Access> accessTypePrivate =
                new Radio<Access>("accessTypePrivate", accessPrivateModel);

        accessType.add(accessTypePublic);
        accessType.add(accessTypePrivate);
    }
}
