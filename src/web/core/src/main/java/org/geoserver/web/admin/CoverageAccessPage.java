/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.Arrays;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Edits the Coverage configuration parameters
 */
public class CoverageAccessPage extends ServerAdminPage {

    class PoolSizeValidator extends AbstractFormValidator {
    
        private static final long serialVersionUID = -3435198454570572665L;
        
        public FormComponent<?>[] getDependentFormComponents() {
            return null;
        }
        
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("submit")) {
                return;
            }
        
            // Getting pool components
            final Component maxPoolComponent = form.get("maxPoolSize");
            final Component corePoolComponent = form.get("corePoolSize");
        
            int maxPool;
            int corePool;
        
            // checking limits are properly set
            if (maxPoolComponent != null && maxPoolComponent instanceof TextField<?>
                    && corePoolComponent != null
                    && corePoolComponent instanceof TextField<?>) {
                final TextField maxPoolField = (TextField) maxPoolComponent;
                final TextField corePoolField = (TextField) corePoolComponent;
                final String mp = maxPoolField.getValue();
                final String cp = corePoolField.getValue();
                if (!(mp == null || cp == null || mp.trim().isEmpty() || cp.trim()
                        .isEmpty())) {
                    try {
                        maxPool = Integer.valueOf(mp);
                    } catch (NumberFormatException nfe) {
                        // The MinimumValidator(1) should already deal with that
                        return;
                    }
        
                    try {
                        corePool = Integer.valueOf(cp);
                    } catch (NumberFormatException nfe) {
                        // The MinimumValidator(1) should already deal with that
                        return;
                    }
        
                    if (maxPool >= 1 && corePool >= 1 && maxPool < corePool) {
                        form.error(new ParamResourceModel("poolSizeCheck", getPage())
                                .getString());
                    }
                }
            }
        }
    }

    public CoverageAccessPage(){
        final IModel geoServerModel = getGeoServerModel();
        
        // this invokation will trigger a clone of the CoverageAccessInfo,
        // which will allow the modification proxy seeing changes on the
        // CoverageAccess page with respect to the original CoverageAccessInfo object
        final IModel coverageModel = getCoverageAccessModel();

        // form and submit
        Form form = new Form("form", new CompoundPropertyModel(coverageModel));
        add( form );
        form.add(new PoolSizeValidator());
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
                GeoServerInfo global = gs.getGlobal();
                global.setCoverageAccess( (CoverageAccessInfo)coverageModel.getObject() );
                gs.save(global);
                doReturn();
            }
        };
        form.add(submit);
        
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
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
