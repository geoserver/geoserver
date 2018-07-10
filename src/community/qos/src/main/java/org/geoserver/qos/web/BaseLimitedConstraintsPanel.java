/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.qos.QosData;
import org.geoserver.qos.xml.AreaConstraint;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class BaseLimitedConstraintsPanel<T extends LimitedAreaRequestConstraints>
        extends Panel {

    protected SerializableConsumer<AjaxTargetAndModel<T>> onDelete;
    protected IModel<T> mainModel;
    protected WebMarkupContainer mainDiv;
    protected ModalWindow selectLayerModal;
    protected WebMarkupContainer layersDiv;
    protected WebMarkupContainer innerConstraintsDiv;
    protected EnvelopePanel envelopePanel;
    protected IModel<ReferencedEnvelope> envelopeModel;
    protected LayersListBuilder<T> layersBuilder;

    public BaseLimitedConstraintsPanel(
            String id, IModel<T> model, LayersListBuilder<T> layersBuilder) {
        super(id, model);
        mainModel = model;
        if (mainModel.getObject().getLayerNames() == null)
            mainModel.getObject().setLayerNames(new ArrayList<String>());
        this.layersBuilder = layersBuilder;
        initAllComponents();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected void initAllComponents() {
        selectLayerModal = new ModalWindow("selectLayerModal");
        add(selectLayerModal);

        mainDiv = new WebMarkupContainer("mainDiv");
        mainDiv.setOutputMarkupId(true);
        add(mainDiv);

        innerConstraintsDiv =
                new WebMarkupContainer("innerConstraintsDiv") {
                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        if (CollectionUtils.isNotEmpty(getSelectedLayers())) {
                            this.setVisible(true);
                        } else {
                            this.setVisible(false);
                        }
                    }
                };
        innerConstraintsDiv.setOutputMarkupId(true);
        innerConstraintsDiv.setOutputMarkupPlaceholderTag(true);
        mainDiv.add(innerConstraintsDiv);

        initLayersComponents();
        initFormatsComponents();
        initBoundsComponent();
    }

    private void initBoundsComponent() {
        envelopeModel =
                new Model<ReferencedEnvelope>() {
                    @Override
                    public ReferencedEnvelope getObject() {
                        if (mainModel.getObject().getAreaConstraint() == null) {
                            mainModel.getObject().setAreaConstraint(new AreaConstraint());
                        }
                        AreaConstraint ac = mainModel.getObject().getAreaConstraint();
                        // if no complete values present, return null
                        // if (ac == null || mainModel.getObject().getCrs() == null) return null;
                        Envelope env;
                        if (ac.getMinX() == null
                                || ac.getMaxX() == null
                                || ac.getMinY() == null
                                || ac.getMaxY() == null) {
                            env = new Envelope();
                        } else {
                            env =
                                    new Envelope(
                                            ac.getMinX(), ac.getMaxX(), ac.getMinY(), ac.getMaxY());
                        }
                        CoordinateReferenceSystem crs = null;
                        if (StringUtils.isNotEmpty(mainModel.getObject().getCrs())) {
                            try {
                                crs = CRS.decode(mainModel.getObject().getCrs());
                            } catch (FactoryException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return ReferencedEnvelope.create(env, crs);
                    }

                    @Override
                    public void setObject(ReferencedEnvelope env) {
                        if (env == null) {
                            mainModel.getObject().setAreaConstraint(null);
                            mainModel.getObject().setCrs(null);
                        }
                        mainModel
                                .getObject()
                                .setAreaConstraint(
                                        new AreaConstraint(
                                                env.getMinX(),
                                                env.getMinY(),
                                                env.getMaxX(),
                                                env.getMaxY()));
                        try {
                            mainModel
                                    .getObject()
                                    .setCrs(
                                            CRS.lookupIdentifier(
                                                    env.getCoordinateReferenceSystem(), true));
                        } catch (FactoryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
        envelopePanel = new EnvelopePanel("bounds", envelopeModel);
        envelopePanel.setRequired(true);
        envelopePanel.setCRSFieldVisible(true);
        envelopePanel.setCrsRequired(true);
        envelopePanel.setOutputMarkupId(true);
        innerConstraintsDiv.add(envelopePanel);

        final GeoServerAjaxFormLink generateBoundsLink =
                new GeoServerAjaxFormLink("generateBounds") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (CollectionUtils.isEmpty(getSelectedLayers())) return;
                        GeoServerApplication gsa = (GeoServerApplication) getApplication();
                        LayerGroupInfo lg = gsa.getCatalog().getFactory().createLayerGroup();
                        getSelectedLayers()
                                .forEach(
                                        l -> {
                                            lg.getLayers().add(gsa.getCatalog().getLayerByName(l));
                                        });
                        try {
                            CoordinateReferenceSystem crs =
                                    envelopePanel.getCoordinateReferenceSystem();
                            // CRS.decode(envelopePanel.getCoordinateReferenceSystem());
                            if (crs != null) {
                                new CatalogBuilder(gsa.getCatalog())
                                        .calculateLayerGroupBounds(lg, crs);
                            } else {
                                // calculate from scratch
                                new CatalogBuilder(gsa.getCatalog()).calculateLayerGroupBounds(lg);
                            }
                            ReferencedEnvelope renv = lg.getBounds();
                            envelopePanel.setModelObject(renv);
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                        target.add(mainDiv);
                    }

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {}
                };
        innerConstraintsDiv.add(generateBoundsLink);

        final GeoServerAjaxFormLink generateBoundsFromCRSLink =
                new GeoServerAjaxFormLink("generateBoundsFromCRS") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        try {
                            CoordinateReferenceSystem crs =
                                    envelopePanel.getCoordinateReferenceSystem();
                            if (crs == null) return;
                            ReferencedEnvelope refEnv =
                                    new ReferencedEnvelope(CRS.getEnvelope(crs));
                            envelopePanel.setModelObject(refEnv);
                            envelopePanel.modelChanged();
                            target.add(envelopePanel);
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                    }

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {}
                };
        innerConstraintsDiv.add(generateBoundsFromCRSLink);
    }

    public void setOnDelete(SerializableConsumer<AjaxTargetAndModel<T>> onDelete) {
        this.onDelete = onDelete;
    }

    private void initFormatsComponents() {
        final WebMarkupContainer formatsDiv = new WebMarkupContainer("formatsDiv");
        formatsDiv.setOutputMarkupId(true);
        innerConstraintsDiv.add(formatsDiv);

        // multi select
        final ListMultipleChoice<String> formatsChoice =
                new ListMultipleChoice<>(
                        "formatsChoice",
                        new PropertyModel<>(mainModel, "outputFormat"),
                        getOutputFormats());
        formatsDiv.add(formatsChoice);
    }

    protected List<String> getOutputFormats() {
        return QosData.instance().getWmsOutputFormats();
    }

    protected IModel<T> getMainModel() {
        return mainModel;
    }

    public void setMainModel(IModel<T> mainModel) {
        this.mainModel = mainModel;
    }

    protected WebMarkupContainer getMainDiv() {
        return mainDiv;
    }

    protected void initLayersComponents() {
        layersDiv = layersBuilder.build(mainDiv, selectLayerModal, mainModel);
    }

    protected abstract List<String> getSelectedLayers();
}
