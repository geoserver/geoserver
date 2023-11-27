package org.geoserver.web.data.store.graticule;

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.graticule.GraticuleDataStore;
import org.geotools.referencing.CRS;

public class GraticulePanel extends Panel {

    private static final String RESOURCE_KEY_PREFIX = GraticulePanel.class.getSimpleName();

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(GraticuleDataStore.class);

    FormComponent steps;

    private static CoordinateReferenceSystem DEFAULT_CRS;

    static {
        try {
            DEFAULT_CRS = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    private EnvelopePanel envelopePanel;

    public GraticulePanel(final String id, final IModel paramsModel, final Form storeEditForm) {

        super(id);
        steps = addTextPanel(paramsModel, "steps", true);

        // bounding box
        add(envelopePanel = new EnvelopePanel("bounds") /*.setReadOnly(true)*/);
        envelopePanel.setRequired(true);
        envelopePanel.setCRSFieldVisible(true);
        envelopePanel.setCrsRequired(true);
        envelopePanel.setOutputMarkupId(true);

        steps.setOutputMarkupId(true);
    }

    private FormComponent addTextPanel(
            final IModel paramsModel, final String paramName, final boolean required) {
        return addTextPanel(paramsModel, paramName, paramName, required);
    }

    private FormComponent addTextPanel(
            final IModel paramsModel,
            final String paramName,
            final String paramTitle,
            final boolean required) {
        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final TextParamPanel textParamPanel =
                new TextParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramTitle),
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
        return new FormComponent[] {steps, envelopePanel};
    }
}
