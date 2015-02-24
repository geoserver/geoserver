/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSToCRSModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A generic configuration panel for all basic ResourceInfo properties
 */
@SuppressWarnings("serial")
public class BasicResourceConfig extends ResourceConfigurationPanel {

    DropDownChoice projectionPolicy;

    CRSPanel declaredCRS;

    public BasicResourceConfig(String id, IModel model) {
        super(id, model);

        TextField name = new TextField("name");
        name.setRequired(true);
        add(name);
        add(new CheckBox("enabled"));
        add(new CheckBox("advertised"));
        add(new TextField("title"));
        add(new TextArea("abstract"));
        add(new KeywordsEditor("keywords", LiveCollectionModel.list(new PropertyModel(model, "keywords"))));
        add(new MetadataLinkEditor("metadataLinks", model));

        final Form refForm = new Form("referencingForm");
        add(refForm);

        // native bbox
        PropertyModel nativeBBoxModel = new PropertyModel(model, "nativeBoundingBox");
        final EnvelopePanel nativeBBox = new EnvelopePanel("nativeBoundingBox", nativeBBoxModel);
        nativeBBox.setOutputMarkupId(true);
        refForm.add(nativeBBox);
        refForm.add(computeNativeBoundsLink(refForm, nativeBBox));

        // lat/lon bbox
        final EnvelopePanel latLonPanel = new EnvelopePanel("latLonBoundingBox", new PropertyModel(
                model, "latLonBoundingBox"));
        latLonPanel.setOutputMarkupId(true);
        latLonPanel.setRequired(true);
        refForm.add(latLonPanel);
        refForm.add(computeLatLonBoundsLink(refForm, nativeBBox, latLonPanel));

        // native srs , declared srs, and srs handling dropdown
        CRSPanel nativeCRS = new CRSPanel("nativeSRS", new PropertyModel(model, "nativeCRS"));
        nativeCRS.setReadOnly(true);
        refForm.add(nativeCRS);
        declaredCRS = new CRSPanel("declaredSRS",
                new SRSToCRSModel(new PropertyModel(model, "sRS")));
        declaredCRS.setRequired(true);
        refForm.add(declaredCRS);

        projectionPolicy = new DropDownChoice("srsHandling", new PropertyModel(model,
                "projectionPolicy"), Arrays.asList(ProjectionPolicy.values()),
                new ProjectionPolicyRenderer());
        ResourceInfo ri = (ResourceInfo) model.getObject();
        if (((ResourceInfo) model.getObject()).getCRS() == null) {
            // no native, the only meaningful policy is to force
            ri.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        }
        refForm.add(projectionPolicy);
        
        
        refForm.add(new ReprojectionIsPossibleValidator(nativeCRS, declaredCRS, projectionPolicy));

    }

    AjaxSubmitLink computeNativeBoundsLink(final Form refForm,
            final EnvelopePanel nativeBBox) {
        return new AjaxSubmitLink("computeNative", refForm) {

            @Override
            public void onSubmit(final AjaxRequestTarget target, Form form) {
                // perform manual processing otherwise the component contents won't be updated
                form.process();
                ResourceInfo resource = (ResourceInfo) BasicResourceConfig.this.getDefaultModelObject();
                try {
                    CatalogBuilder cb = new CatalogBuilder(GeoServerApplication.get().getCatalog());
                    ReferencedEnvelope bounds = cb.getNativeBounds(resource);
                    resource.setNativeBoundingBox(bounds);
                    nativeBBox.setModelObject(bounds);
                } catch(IOException e) {
                    LOGGER.log(Level.SEVERE, "Error computing the native BBOX", e);
                    error("Error computing the native BBOX:" + e.getMessage());
                }
                target.addComponent(nativeBBox);
            }
            
            public boolean getDefaultFormProcessing() {
                // disable the default processing or the link won't trigger
                // when any validation fails
                return false;
            }

        };
    }

    GeoServerAjaxFormLink computeLatLonBoundsLink(final Form refForm,
            final EnvelopePanel nativeBBox, final EnvelopePanel latLonPanel) {
        return new GeoServerAjaxFormLink("computeLatLon", refForm) {

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                // perform manual processing of the required fields
                nativeBBox.processInput();
                declaredCRS.processInput();
                
                ReferencedEnvelope nativeBounds = (ReferencedEnvelope) nativeBBox.getModelObject();
                try {
                    // if the native bounds are not around compute them
                    if(nativeBounds == null) {
                        ResourceInfo resource = (ResourceInfo) BasicResourceConfig.this.getDefaultModelObject();
                        CatalogBuilder cb = new CatalogBuilder(GeoServerApplication.get().getCatalog());
                        nativeBounds = cb.getNativeBounds(resource);
                        resource.setNativeBoundingBox(nativeBounds);
                        nativeBBox.setModelObject(nativeBounds);
                        target.addComponent(nativeBBox);
                    }
                
                    CatalogBuilder cb = new CatalogBuilder(GeoServerApplication.get().getCatalog());
                    latLonPanel.setModelObject(cb.getLatLonBounds(nativeBounds, declaredCRS.getCRS()));
                } catch(IOException e) {
                    LOGGER.log(Level.SEVERE, "Error computing the geographic BBOX", e);
                    error("Error computing the geographic bounds:" + e.getMessage());
                }
                target.addComponent(latLonPanel);
            }
        };
    }

    class ProjectionPolicyRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((ProjectionPolicy) object).name(),
                    BasicResourceConfig.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((ProjectionPolicy) object).name();
        }
    }
    
    /**
     * Checks a resource name is actually a valid one (WFS/WMS wise),
     * in particular, only word chars
     */
    static class ResourceNameValidator extends PatternValidator {
        public ResourceNameValidator() {
            super("[\\w][\\w.-]*");
        }
    }
    
    /**
     * Form validator that checks whether the native CRS can be projected to the declared one
     * whenever the projection policy chosen is "reproject" 
     */
    private static class ReprojectionIsPossibleValidator implements IFormValidator {

        private FormComponent[] dependentFormComponents;

        private FormComponent nativeCRS;

        private FormComponent declaredCRS;

        private FormComponent projectionPolicy;

        public ReprojectionIsPossibleValidator(final FormComponent nativeCRS,
                final FormComponent declaredCRS, final FormComponent projectionPolicy) {
            this.nativeCRS = nativeCRS;
            this.declaredCRS = declaredCRS;
            this.projectionPolicy = projectionPolicy;
            this.dependentFormComponents = new FormComponent[] { nativeCRS, declaredCRS,
                    projectionPolicy };
        }

        public FormComponent[] getDependentFormComponents() {
            return dependentFormComponents;
        }

        public void validate(final Form form) {
            CoordinateReferenceSystem nativeCrs;
            CoordinateReferenceSystem declaredCrs;
            ProjectionPolicy policy;

            nativeCrs = (CoordinateReferenceSystem) nativeCRS.getConvertedInput();
            declaredCrs = (CoordinateReferenceSystem) declaredCRS.getConvertedInput();
            policy = (ProjectionPolicy) projectionPolicy.getConvertedInput();
            if (policy == ProjectionPolicy.REPROJECT_TO_DECLARED) {
                final boolean lenient = true;
                try {
                    CRS.findMathTransform(nativeCrs, declaredCrs, lenient);
                } catch (FactoryException e) {
                    String msgKey = "BasicResourceConfig.noTransformFromNativeToDeclaredCRS";
                    String errMsg = e.getMessage();
                    String message =(String) new ResourceModel(msgKey).getObject();
                    form.error(message, Collections.singletonMap("error", errMsg));
                }
            }
        }
    }
}
