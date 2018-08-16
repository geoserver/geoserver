/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web.publish;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSListTextArea;

public class WFSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 4264296611272179367L;

    protected GeoServerDialog dialog;

    public WFSLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        TextField<Integer> maxFeatures =
                new TextField<Integer>(
                        "perReqFeatureLimit",
                        new PropertyModel<Integer>(model, "resource.maxFeatures"));
        maxFeatures.add(RangeValidator.minimum(0));
        Border mfb = new FormComponentFeedbackBorder("perReqFeaturesBorder");
        mfb.add(maxFeatures);
        add(mfb);
        TextField<Integer> maxDecimals =
                new TextField<Integer>(
                        "maxDecimals", new PropertyModel<Integer>(model, "resource.numDecimals"));
        maxFeatures.add(RangeValidator.minimum(0));
        Border mdb = new FormComponentFeedbackBorder("maxDecimalsBorder");
        mdb.add(maxDecimals);
        add(mdb);
        CheckBox skipNumberMatched =
                new CheckBox(
                        "skipNumberMatched",
                        new PropertyModel<Boolean>(model, "resource.skipNumberMatched"));
        add(skipNumberMatched);

        // coordinates measures encoding
        CheckBox encodeMeasures =
                new CheckBox(
                        "encodeMeasures", new PropertyModel<>(model, "resource.encodeMeasures"));
        add(encodeMeasures);

        // other srs list
        dialog = new GeoServerDialog("wfsDialog");
        add(dialog);
        PropertyModel<Boolean> overrideServiceSRSModel =
                new PropertyModel<Boolean>(model, "resource.overridingServiceSRS");
        final CheckBox overrideServiceSRS =
                new CheckBox("overridingServiceSRS", overrideServiceSRSModel);
        add(overrideServiceSRS);
        final WebMarkupContainer otherSrsContainer = new WebMarkupContainer("otherSRSContainer");
        otherSrsContainer.setOutputMarkupId(true);
        add(otherSrsContainer);
        final TextArea<List<String>> srsList =
                new SRSListTextArea(
                        "srs",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(model, "resource.responseSRS")));
        srsList.setOutputMarkupId(true);
        srsList.setVisible(Boolean.TRUE.equals(overrideServiceSRSModel.getObject()));
        otherSrsContainer.add(srsList);
        overrideServiceSRS.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = -6590810763209350915L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean visible = overrideServiceSRS.getConvertedInput();
                        srsList.setVisible(visible);
                        target.add(otherSrsContainer);
                    }
                });
        add(
                new AjaxLink<String>("skipNumberMatchedHelp") {
                    private static final long serialVersionUID = 9222171216768726057L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel(
                                        "skipNumberMatched", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "skipNumberMatched.message", WFSLayerConfig.this, null));
                    }
                });
        add(
                new AjaxLink<String>("otherSRSHelp") {
                    private static final long serialVersionUID = -1239179491855142211L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel("otherSRS", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "otherSRS.message", WFSLayerConfig.this, null));
                    }
                });
        add(
                new AjaxLink<String>("coordinatesEncodingHelp") {
                    private static final long serialVersionUID = 926171216768726057L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel(
                                        "coordinatesEncodingTitle", WFSLayerConfig.this, null),
                                new StringResourceModel(
                                        "coordinatesEncodingHelp.message",
                                        WFSLayerConfig.this,
                                        null));
                    }
                });
    }
}
