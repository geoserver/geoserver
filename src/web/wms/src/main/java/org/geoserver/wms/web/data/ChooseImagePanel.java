/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.ParamResourceModel;

class ChooseImagePanel extends Panel {
    private static final long serialVersionUID = 7564545298131010218L;

    private WorkspaceInfo ws;
    private String[] extensions;

    public ChooseImagePanel(String id, WorkspaceInfo ws, String[] extensions) {
        super(id);
        this.ws = ws;
        this.extensions = extensions;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        SortedSet<String> imageSet = new TreeSet<>();
        GeoServerDataDirectory dd =
                GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
        for (Resource r : dd.getStyles(ws).list()) {
            if (ArrayUtils.contains(
                    extensions, FilenameUtils.getExtension(r.name()).toLowerCase())) {
                imageSet.add(r.name());
            }
        }

        FileUploadField upload = new FileUploadField("upload", new Model<ArrayList<FileUpload>>());

        Model<String> imageModel = new Model<>();
        DropDownChoice<String> image =
                new DropDownChoice<>("image", imageModel, new ArrayList<>(imageSet));

        Image display =
                new Image(
                        "display",
                        new ResourceStreamResource(
                                new AbstractResourceStream() {
                                    private static final long serialVersionUID =
                                            9031811973994305485L;

                                    transient InputStream is;

                                    @Override
                                    public InputStream getInputStream()
                                            throws ResourceStreamNotFoundException {
                                        GeoServerDataDirectory dd =
                                                GeoServerApplication.get()
                                                        .getBeanOfType(
                                                                GeoServerDataDirectory.class);
                                        is = dd.getStyles(ws).get(imageModel.getObject()).in();
                                        return is;
                                    }

                                    @Override
                                    public void close() throws IOException {
                                        if (is != null) {
                                            is.close();
                                        }
                                    }
                                }));
        display.setOutputMarkupPlaceholderTag(true).setVisible(false);

        image.setNullValid(true)
                .setOutputMarkupId(true)
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = 6320466559337730660L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                upload.setModelObject(null);
                                display.setVisible(image.getModelObject() != null);
                                target.add(upload);
                                target.add(display);
                            }
                        });

        upload.setOutputMarkupId(true)
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = 5905505859401520055L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                image.setModelObject(null);
                                display.setVisible(false);
                                target.add(image);
                                target.add(display);
                            }
                        });

        add(image);
        add(display);
        add(upload);

        findParent(Form.class)
                .add(
                        new AbstractFormValidator() {
                            private static final long serialVersionUID = 1388363954282359884L;

                            @Override
                            public FormComponent<?>[] getDependentFormComponents() {
                                return new FormComponent<?>[] {image, upload};
                            }

                            @Override
                            public void validate(Form<?> form) {
                                if (image.getConvertedInput() == null
                                        && (upload.getConvertedInput() == null
                                                || upload.getConvertedInput().isEmpty())) {
                                    form.error(
                                            new ParamResourceModel("missingImage", getPage())
                                                    .getString());
                                }
                            }
                        });
    }

    public String getChoice() {
        return get("image").getDefaultModelObjectAsString();
    }

    public FileUpload getFileUpload() {
        return ((FileUploadField) get("upload")).getFileUpload();
    }

    public FeedbackPanel getFeedback() {
        return (FeedbackPanel) get("feedback");
    }
}
