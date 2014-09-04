/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web.publish;

import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.web.publish.LayerConfigurationPanel;

@SuppressWarnings("serial")
public class WFSLayerConfig extends LayerConfigurationPanel {

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
    }
}
