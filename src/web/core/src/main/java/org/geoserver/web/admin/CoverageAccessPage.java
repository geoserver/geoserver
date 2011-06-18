/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.Arrays;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.web.GeoServerHomePage;

/**
 * Edits the Coverage configuration parameters
 */
public class CoverageAccessPage extends ServerAdminPage {

    public CoverageAccessPage(){
        final IModel geoServerModel = getGeoServerModel();
        final IModel coverageModel = getCoverageAccessModel();

        // form and submit
        Form form = new Form("form", new CompoundPropertyModel(coverageModel));
        add( form );

        // All the fields
        TextField corePoolSize = new TextField("corePoolSize");
        corePoolSize.add(new MinimumValidator(1));
        form.add(corePoolSize);
        
        TextField maxPoolSize = new TextField("maxPoolSize");
        maxPoolSize.add(new MinimumValidator(1));
        form.add(maxPoolSize);
        
        TextField keepAliveTime = new TextField("keepAliveTime");
        keepAliveTime.add(new MinimumValidator(1));
        form.add(keepAliveTime);
        
        final DropDownChoice queueType = new DropDownChoice("queueType", Arrays.asList(CoverageAccessInfo.QueueType.values()), new QueueTypeRenderer());
        form.add(queueType);
        
        TextField imageIOCacheThreshold = new TextField("imageIOCacheThreshold");
        imageIOCacheThreshold.add(new MinimumValidator(0l));
        form.add(imageIOCacheThreshold);
                
        Button submit = new Button("submit", new StringResourceModel("submit", this, null)) {
            @Override
            public void onSubmit() {
                GeoServer gs = (GeoServer) geoServerModel.getObject();
                gs.getGlobal().setCoverageAccess( (CoverageAccessInfo)coverageModel.getObject() );
                gs.save( gs.getGlobal() ); 
                setResponsePage(GeoServerHomePage.class);
            }
        };
        form.add(submit);
        
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                setResponsePage(GeoServerHomePage.class);
            }
        };
        form.add(cancel);
    }
    
    private class QueueTypeRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((QueueType) object).name(), CoverageAccessPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((QueueType) object).name();
        }
    }
}
