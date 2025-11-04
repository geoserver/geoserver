/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.data.store.graticule;

import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.graticule.GraticuleDataStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

public class GraticulePanel extends Panel {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GraticuleDataStore.class);

    FormComponent steps;

    private static CoordinateReferenceSystem DEFAULT_CRS;

    static {
        try {
            DEFAULT_CRS = CRS.decode("EPSG:4326", true);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    private final EnvelopePanel bounds;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GraticulePanel(final String id, final IModel paramsModel, final Form storeEditForm) {

        super(id);
        steps = addTextPanel(paramsModel, "steps", true);

        // bounding box
        // deseialise bounds

        add(
                bounds = new EnvelopePanel(
                        "bounds", new MapModel<org.geotools.geometry.jts.ReferencedEnvelope>(paramsModel, "bounds")));
        if (bounds.getModelObject() == null) {
            bounds.setDefaultModelObject(new ReferencedEnvelope(DEFAULT_CRS));
        }
        bounds.setRequired(true);
        bounds.setCRSFieldVisible(true);
        bounds.setCrsRequired(true);
        bounds.setOutputMarkupId(true);

        add(new GeoServerAjaxFormLink("generateBoundsFromCRS") {
            @Serial
            private static final long serialVersionUID = -7907583302556368270L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                LOGGER.log(Level.FINE, "Computing bounds for graticule based off CRS");

                CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
                Bounds crsEnvelope = CRS.getEnvelope(crs);
                if (crsEnvelope != null) {
                    ReferencedEnvelope refEnvelope = new ReferencedEnvelope(crsEnvelope);
                    bounds.setDefaultModelObject(refEnvelope);
                }

                bounds.modelChanged();
                target.add(bounds);
            }
        });

        steps.setOutputMarkupId(true);
    }

    private FormComponent addTextPanel(final IModel paramsModel, final String paramName, final boolean required) {
        return addTextPanel(paramsModel, paramName, paramName, required);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private FormComponent addTextPanel(
            final IModel paramsModel, final String paramName, final String paramTitle, final boolean required) {
        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final TextParamPanel textParamPanel = new TextParamPanel<>(
                paramName,
                new MapModel<>(paramsModel, paramTitle),
                new ResourceModel(resourceKey, paramName),
                required);
        textParamPanel.getFormComponent().setType(String.class /*param.type*/);

        String defaultTitle = paramTitle;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[] {steps, bounds};
    }
}
