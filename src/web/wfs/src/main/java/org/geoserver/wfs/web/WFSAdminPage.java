/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSListTextArea;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.response.ShapeZipOutputFormat;

@SuppressWarnings("serial")
public class WFSAdminPage extends BaseServiceAdminPage<WFSInfo> {

    public WFSAdminPage() {
        super();
    }

    public WFSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WFSAdminPage(WFSInfo service) {
        super(service);
    }

    protected Class<WFSInfo> getServiceClass() {
        return WFSInfo.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void build(final IModel info, Form form) {
        // max features
        form.add(new TextField<Integer>("maxFeatures").add(RangeValidator.minimum(0)));
        form.add(new TextField<Integer>("maxNumberOfFeaturesForPreview"));
        form.add(new CheckBox("featureBounding"));
        form.add(new CheckBox("hitsIgnoreMaxFeatures"));

        // service level
        RadioGroup sl = new RadioGroup("serviceLevel");
        form.add(sl);
        sl.add(new Radio("basic", new Model(WFSInfo.ServiceLevel.BASIC)));
        sl.add(new Radio("transactional", new Model(WFSInfo.ServiceLevel.TRANSACTIONAL)));
        sl.add(new Radio("complete", new Model(WFSInfo.ServiceLevel.COMPLETE)));

        IModel gml2Model =
                new LoadableDetachableModel() {
                    public Object load() {
                        return ((WFSInfo) info.getObject()).getGML().get(WFSInfo.Version.V_10);
                    }
                };

        IModel gml3Model =
                new LoadableDetachableModel() {
                    public Object load() {
                        return ((WFSInfo) info.getObject()).getGML().get(WFSInfo.Version.V_11);
                    }
                };

        IModel gml32Model =
                new LoadableDetachableModel() {
                    @Override
                    protected Object load() {
                        return ((WFSInfo) info.getObject()).getGML().get(WFSInfo.Version.V_20);
                    }
                };

        form.add(new GMLPanel("gml2", gml2Model));
        form.add(new GMLPanel("gml3", gml3Model));
        // add GML 3.2. configuration panel with alternative MIME types
        form.add(
                new GMLPanel(
                        "gml32",
                        gml32Model,
                        "application/gml+xml; version=3.2",
                        "text/xml; subtype=gml/3.2",
                        "text/xml"));

        form.add(new CheckBox("canonicalSchemaLocation"));

        // Encode response with one featureMembers element or multiple featureMember elements
        RadioGroup eo = new RadioGroup("encodeFeatureMember");
        form.add(eo);
        eo.add(new Radio("featureMembers", new Model(Boolean.FALSE)));
        eo.add(new Radio("featureMember", new Model(Boolean.TRUE)));

        PropertyModel metadataModel = new PropertyModel(info, "metadata");
        IModel<Boolean> prjFormatModel =
                new MapModel(metadataModel, ShapeZipOutputFormat.SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI);
        CheckBox defaultPrjFormat = new CheckBox("shapeZipPrjFormat", prjFormatModel);
        form.add(defaultPrjFormat);

        try {
            // This is a temporary meassure until we fully implement ESRI WKT support in GeoTools.
            // See discussion in GEOS-4503
            GeoServerResourceLoader resourceLoader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource esriProjs =
                    resourceLoader.get(Paths.path("user_projections", "esri.properties"));
            if (esriProjs.getType() != Type.RESOURCE) {
                defaultPrjFormat.setEnabled(false);
                defaultPrjFormat.getModel().setObject(Boolean.FALSE);
                defaultPrjFormat.add(
                        new AttributeModifier(
                                "title",
                                new Model(
                                        "No esri.properties file "
                                                + "found in the data directory's user_projections folder. "
                                                + "This option is not available")));
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }

        // other srs list
        TextArea srsList =
                new SRSListTextArea(
                        "srs", LiveCollectionModel.list(new PropertyModel(info, "sRS")));
        form.add(srsList);
        form.add(
                new AjaxLink("otherSRSHelp") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel("otherSRS", WFSAdminPage.this, null),
                                new StringResourceModel(
                                        "otherSRS.message", WFSAdminPage.this, null));
                    }
                });

        // allowGlobalQueries checkbox
        form.add(new CheckBox("allowGlobalQueries"));
    }

    static class GMLPanel extends Panel {

        public GMLPanel(String id, IModel gmlModel, String... mimeTypes) {
            super(id, new CompoundPropertyModel(gmlModel));

            // srsNameStyle
            List<GMLInfo.SrsNameStyle> choices = Arrays.asList(SrsNameStyle.values());
            DropDownChoice srsNameStyle =
                    new DropDownChoice("srsNameStyle", choices, new EnumChoiceRenderer());
            add(srsNameStyle);

            add(new CheckBox("overrideGMLAttributes"));

            // GML MIME type overriding section
            GMLInfo gmlInfo = (GMLInfo) gmlModel.getObject();
            boolean mimesTypesProvided = mimeTypes.length != 0;
            boolean activated = gmlInfo.getMimeTypeToForce().isPresent();
            // add MIME type drop down choice
            DropDownChoice<String> mimeTypeToForce =
                    new DropDownChoice<>(
                            "mimeTypeToForce",
                            new Model<>(gmlInfo.getMimeTypeToForce().orElse(null)),
                            Arrays.asList(mimeTypes));
            mimeTypeToForce.add(
                    new AjaxFormComponentUpdatingBehavior("change") {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            // set the MIME type to force
                            String value = mimeTypeToForce.getModelObject();
                            gmlInfo.setMimeTypeToForce(value);
                        }
                    });
            // set the select value if available
            if (mimesTypesProvided) {
                mimeTypeToForce.setModelObject(gmlInfo.getMimeTypeToForce().orElse(mimeTypes[0]));
            }
            // need for Ajax updates
            mimeTypeToForce.setOutputMarkupId(mimesTypesProvided);
            mimeTypeToForce.setOutputMarkupPlaceholderTag(mimesTypesProvided);
            mimeTypeToForce.setVisible(mimesTypesProvided && activated);
            add(mimeTypeToForce);
            // add activate MIME type force checkbox
            CheckBox checkBox =
                    new AjaxCheckBox("forceGmlMimeType", new Model<>(activated)) {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            boolean checked = getModelObject();
                            if (checked) {
                                // force MIME type activated
                                mimeTypeToForce.setVisible(true);
                                String value = mimeTypeToForce.getModelObject();
                                gmlInfo.setMimeTypeToForce(value);
                            } else {
                                // force MIME type deactivated
                                mimeTypeToForce.setVisible(false);
                                gmlInfo.setMimeTypeToForce(null);
                            }
                            // update the drop down choice (requires markup ID and markup
                            // placeholder)
                            target.add(mimeTypeToForce);
                        }
                    };
            checkBox.setVisible(mimesTypesProvided);
            add(checkBox);
            // add check box label
            Label checkBoxLabel =
                    new Label(
                            "forceGmlMimeTypeLabel",
                            new StringResourceModel("WFSAdminPage$GMLPanel.forceGmlMimeTypeLabel"));
            checkBoxLabel.setVisible(mimesTypesProvided);
            add(checkBoxLabel);
        }
    }

    protected String getServiceName() {
        return "WFS";
    }
}
