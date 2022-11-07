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
import org.geoserver.web.data.store.PasswordTextFieldWriteOnlyModel;
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

        add(new TextField<>("bucket").setRequired(true).add(titleModifier("bucket.title")));
        add(
                new TextField<>("awsAccessKey")
                        .setRequired(false)
                        .add(titleModifier("awsAccessKey.title")));
        add(
                new PasswordTextFieldWriteOnlyModel(
                                "awsSecretKey",
                                new PropertyModel<String>(configModel, "awsSecretKey"))
                        .setRequired(false)
                        .add(titleModifier("awsSecretKey.title")));
        add(new TextField<>("prefix").add(titleModifier("prefix.title")));
        add(new TextField<>("endpoint").add(titleModifier("endpoint.title")));
        add(
                new TextField<>("maxConnections")
                        .setRequired(true)
                        .add(titleModifier("maxConnections.title")));
        add(new CheckBox("useHTTPS").add(titleModifier("useHTTPS.title")));
        add(new TextField<>("proxyDomain").add(titleModifier("proxyDomain.title")));
        add(new TextField<>("proxyWorkstation").add(titleModifier("proxyWorkstation.title")));
        add(new TextField<>("proxyHost").add(titleModifier("proxyHost.title")));
        add(new TextField<>("proxyPort").add(titleModifier("proxyPort.title")));
        add(new TextField<>("proxyUsername").add(titleModifier("proxyUsername.title")));
        add(new TextField<>("proxyPassword").add(titleModifier("proxyPassword.title")));
        add(new CheckBox("useGzip").add(titleModifier("useGzip.title")));

        IModel<Access> accessModel = new PropertyModel<>(configModel, "access");

        RadioGroup<Access> accessType = new RadioGroup<>("accessType", accessModel);
        add(accessType);

        IModel<Access> accessPublicModel = new Model<>(Access.PUBLIC);
        IModel<Access> accessPrivateModel = new Model<>(Access.PRIVATE);

        Radio<Access> accessTypePublic = new Radio<>("accessTypePublic", accessPublicModel);
        Radio<Access> accessTypePrivate = new Radio<>("accessTypePrivate", accessPrivateModel);

        accessType.add(accessTypePublic);
        accessType.add(accessTypePrivate);
    }

    private AttributeModifier titleModifier(String s) {
        return new AttributeModifier("title", new ResourceModel(s));
    }
}
