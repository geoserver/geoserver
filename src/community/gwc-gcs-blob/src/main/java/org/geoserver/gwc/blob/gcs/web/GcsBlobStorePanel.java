/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.blob.gcs.web;

import java.io.Serial;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;

/** Panel for GoogleCloudStorageBlobStore */
public class GcsBlobStorePanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    public GcsBlobStorePanel(String id, final IModel<GoogleCloudStorageBlobStoreInfo> configModel) {
        super(id, configModel);

        add(new TextField<String>("bucket").setRequired(true).add(title("bucket.title")));

        add(new TextField<String>("prefix").add(title("prefix.title")));

        add(new TextField<String>("projectId").add(title("projectId.title")));

        add(new TextField<String>("quotaProjectId").add(title("quotaProjectId.title")));

        add(new TextField<String>("endpointUrl").add(title("endpointUrl.title")));

        add(new PasswordTextField("apiKey")
                .setResetPassword(false)
                .setRequired(false)
                .add(title("apiKey.title")));

        add(new CheckBox("useDefaultCredentialsChain").add(title("useDefaultCredentialsChain.title")));
    }

    private AttributeModifier title(String titleResourceKey) {
        return new AttributeModifier("title", new ResourceModel(titleResourceKey));
    }
}
