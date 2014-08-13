/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web.publish;

import java.awt.Checkbox;

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
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSListTextArea;

@SuppressWarnings("serial")
public class WFSLayerConfig extends LayerConfigurationPanel {
    
    protected GeoServerDialog dialog;

    public WFSLayerConfig(String id, IModel model){
        super(id, model);

        TextField maxFeatures = new TextField("perReqFeatureLimit", new PropertyModel(model, "resource.maxFeatures"));
        maxFeatures.add(NumberValidator.minimum(0));
        Border mfb = new FormComponentFeedbackBorder("perReqFeaturesBorder");
        mfb.add(maxFeatures);
        add(mfb);
        TextField maxDecimals = new TextField("maxDecimals", new PropertyModel(model, "resource.numDecimals"));
        maxFeatures.add(NumberValidator.minimum(0));
        Border mdb = new FormComponentFeedbackBorder("maxDecimalsBorder");
        mdb.add(maxDecimals);
        add(mdb);
        
        // other srs list
        dialog = new GeoServerDialog("wfsDialog");
        add(dialog);
        PropertyModel overrideServiceSRSModel = new PropertyModel(model, "resource.overridingServiceSRS");
        final CheckBox overrideServiceSRS = new CheckBox("overridingServiceSRS", overrideServiceSRSModel);
        add(overrideServiceSRS);
        final WebMarkupContainer otherSrsContainer = new WebMarkupContainer("otherSRSContainer");
        otherSrsContainer.setOutputMarkupId(true);
        add(otherSrsContainer);
        final TextArea srsList = new SRSListTextArea("srs", LiveCollectionModel.list(new PropertyModel(model, "resource.responseSRS")));
        srsList.setOutputMarkupId(true);
        srsList.setVisible(Boolean.TRUE.equals(overrideServiceSRSModel.getObject())); 
        otherSrsContainer.add(srsList);
        overrideServiceSRS.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean visible = overrideServiceSRS.getConvertedInput();
                srsList.setVisible(visible);
                target.addComponent(otherSrsContainer);
            }
        });
        add(new AjaxLink("otherSRSHelp") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.showInfo(target, 
                    new StringResourceModel("otherSRS", WFSLayerConfig.this, null), 
                    new StringResourceModel("otherSRS.message", WFSLayerConfig.this, null));
            }
        });

    }
}
