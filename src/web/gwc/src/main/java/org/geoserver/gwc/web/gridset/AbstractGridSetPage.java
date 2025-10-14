/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import java.io.Serial;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.measure.Unit;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.SRSListPanel;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

// TODO WICKET8 - Verify this page works OK
abstract class AbstractGridSetPage extends GeoServerSecuredPage {

    @Serial
    private static final long serialVersionUID = 2977633539319630433L;

    protected static final Logger LOGGER = Logging.getLogger(AbstractGridSetPage.class);

    /** Name of the page parameter that determines which gridset to edit */
    public static final String GRIDSET_NAME = "gridSet";

    /** Name of page parameter that holds the name of an existing gridset to use as template to create a new one */
    public static final String GRIDSET_TEMPLATE_NAME = "template";

    protected final Form<GridSetInfo> form;

    protected final TextParamPanel name;

    protected final Component description;

    protected final GridSetCRSPanel crs;

    protected final TextParamPanel tileWidth;

    protected final TextParamPanel tileHeight;

    protected final EnvelopePanel bounds;

    protected final Component computeBoundsLink;

    protected final Component cancelLink;

    protected final Component saveLink;

    protected final TileMatrixSetEditor tileMatrixSetEditor;

    protected final Component addLevelLink;

    protected final FeedbackPanel feedback;

    public AbstractGridSetPage(final PageParameters parameters) {

        final String gridSetName;
        final String templateName;
        if (parameters == null) {
            gridSetName = null;
            templateName = null;
        } else {
            gridSetName = parameters.get(GRIDSET_NAME).toOptionalString();
            templateName = parameters.get(GRIDSET_TEMPLATE_NAME).toOptionalString();
        }

        GridSetInfo gridsetInfo;
        if (templateName != null) {
            gridsetInfo = getInfo(templateName);
            gridsetInfo.setName("My_" + gridsetInfo.getName());
            gridsetInfo.setInternal(false);
        } else if (gridSetName != null) {
            gridsetInfo = getInfo(gridSetName);
        } else {
            gridsetInfo = getInfo(null);
        }

        IModel<GridSetInfo> model = new Model<>(gridsetInfo);

        form = new Form<>("gridSetForm", model);
        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        form.add(name = name(model));
        form.add(description = new TextArea<>("description", new PropertyModel<>(model, "description")));
        form.add(crs = crs(model));
        form.add(bounds = bounds(model));
        form.add(computeBoundsLink = computeBoundsLink(form));
        form.add(tileWidth = tileWidth(model));
        form.add(tileHeight = tileHeight(model));

        form.add(tileMatrixSetEditor = new TileMatrixSetEditor("tileMatrixSetEditor", model));
        tileMatrixSetEditor.setOutputMarkupId(true);

        cancelLink = new BookmarkablePageLink<>("cancel", GridSetsPage.class);
        form.add(cancelLink);

        saveLink = saveLink(form);
        form.add(saveLink);
        add(form);

        tileWidth.getFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(tileMatrixSetEditor);
            }
        });
        tileHeight.getFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(tileMatrixSetEditor);
            }
        });

        addLevelLink = new GeoServerAjaxFormLink("addZoomLevel", form) {
            @Serial
            private static final long serialVersionUID = 1202251941625034786L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                crs.processInput();
                bounds.processInput();
                tileWidth.getFormComponent().processInput();
                tileHeight.getFormComponent().processInput();

                addZoomLevel(target);
                target.add(tileMatrixSetEditor);
                target.add(feedback);
            }
        };

        form.add(addLevelLink);
    }

    void addZoomLevel(AjaxRequestTarget target) {
        ReferencedEnvelope bbox = bounds.getModelObject();
        if (null == bbox) {
            String message = new StringResourceModel("AbstractGridSetPage.cantAddZoomLevel").getString();
            feedback.error(message);
            return;
        }
        Integer width = (Integer) tileWidth.getFormComponent().getModelObject();
        Integer height = (Integer) tileHeight.getFormComponent().getModelObject();

        tileMatrixSetEditor.addZoomLevel(bbox, width == null ? 256 : width, height == null ? 256 : height);
    }

    private Component computeBoundsLink(Form<GridSetInfo> form) {

        GeoServerAjaxFormLink link = new GeoServerAjaxFormLink("computeBounds", form) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                crs.processInput();
                computeBounds();
                target.add(bounds);
                target.add(feedback);
                target.add(tileMatrixSetEditor);
            }
        };
        return link;
    }

    void computeBounds() {
        // perform manual processing of the required fields
        CoordinateReferenceSystem coordSys = crs.getModelObject();
        if (coordSys == null) {
            bounds.error(new StringResourceModel("AbstractGridsetPage.computeBounds.crsNotSet").getString());
            return;
        }
        GWC mediator = GWC.get();
        ReferencedEnvelope aov = mediator.getAreaOfValidity(coordSys);
        if (aov == null) {
            bounds.error(new StringResourceModel("AbstractGridsetPage.computeBounds.aovNotSet").getString());
        } else {
            bounds.setModelObject(aov);
        }
    }

    private EnvelopePanel bounds(IModel<GridSetInfo> model) {

        class UpdatingEnvelopePanel extends EnvelopePanel {
            @Serial
            private static final long serialVersionUID = 1L;

            public UpdatingEnvelopePanel(String id, IModel<ReferencedEnvelope> e) {
                super(id, e);

                class UpdateTableBehavior extends AjaxFormSubmitBehavior {
                    @Serial
                    private static final long serialVersionUID = 1L;

                    public UpdateTableBehavior() {
                        super(form, "blur");
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        target.add(AbstractGridSetPage.this.tileMatrixSetEditor);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        UpdatingEnvelopePanel.this.setModelObject(null);
                        target.add(AbstractGridSetPage.this.tileMatrixSetEditor);
                    }
                }

                minXInput.add(new UpdateTableBehavior());
                minYInput.add(new UpdateTableBehavior());
                minZInput.add(new UpdateTableBehavior());
                maxXInput.add(new UpdateTableBehavior());
                maxYInput.add(new UpdateTableBehavior());
                maxZInput.add(new UpdateTableBehavior());
            }
        }

        PropertyModel<ReferencedEnvelope> boundsModel = new PropertyModel<>(model, "bounds");

        EnvelopePanel panel = new UpdatingEnvelopePanel("bounds", boundsModel);
        panel.setRequired(true);
        panel.setOutputMarkupId(true);

        return panel;
    }

    private TextParamPanel tileHeight(IModel<GridSetInfo> model) {
        TextParamPanel<Integer> panel = new TextParamPanel<>(
                "tileHeight",
                new PropertyModel<>(model, "tileHeight"),
                new StringResourceModel("AbstractGridSetPage.tileHeight"),
                true,
                new RangeValidator<>(16, 2048));
        return panel;
    }

    private TextParamPanel tileWidth(IModel<GridSetInfo> model) {
        TextParamPanel<Integer> panel = new TextParamPanel<>(
                "tileWidth",
                new PropertyModel<>(model, "tileWidth"),
                new StringResourceModel("AbstractGridSetPage.tileWidth"),
                true,
                new RangeValidator<>(16, 2048));
        return panel;
    }

    /** @param model */
    private GridSetCRSPanel crs(IModel<GridSetInfo> model) {
        GridSetCRSPanel crsPanel = new GridSetCRSPanel("crs", model);
        return crsPanel;
    }

    protected GridSet toGridSet(AjaxRequestTarget target, Form<?> form, GridSetInfo info) throws Exception {
        final GridSet newGridset = GridSetBuilder.build(info);

        // the creation above can fill in the blanks of empty UI names, here is where we can
        // check if the names are actually unique
        List<String> names = IntStream.range(0, newGridset.getNumLevels())
                .mapToObj(i -> newGridset.getGrid(i).getName())
                .sorted()
                .collect(Collectors.toList());
        Set<String> duplicates = new LinkedHashSet<>();
        for (int i = 1; i < names.size(); i++) {
            String prevName = names.get(i - 1);
            if (prevName.equals(names.get(i))) {
                duplicates.add(prevName);
            }
        }
        if (!duplicates.isEmpty()) {
            // set back to make the duplicates evident
            for (int i = 0; i < newGridset.getNumLevels(); i++) {
                info.getLevels().get(i).setName(names.get(i));
            }
            throw new IllegalArgumentException(
                    "Tile matrix names should not include duplicates, but the following were found: "
                            + duplicates
                            + ". Mind, if you left some names empty, GeoServer has automatically added in some names for you.");
        }
        return newGridset;
    }

    /** @author groldan */
    protected static class GridSetCRSPanel extends CRSPanel {
        @Serial
        private static final long serialVersionUID = 1L;

        private Label units;

        private Label metersPerUnit;

        private IModel<GridSetInfo> infoModel;

        public GridSetCRSPanel(String id, IModel<GridSetInfo> model) {
            super(id, new PropertyModel<>(model, "crs"));
            this.infoModel = model;
            units = new Label("units", new Model<>());
            metersPerUnit = new Label("metersPerUnit", new Model<>());
            units.setOutputMarkupId(true);
            metersPerUnit.setOutputMarkupId(true);

            updateUnits(getModelObject());

            add(units);
            add(metersPerUnit);
        }

        @Override
        protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
            target.add(units);
            target.add(metersPerUnit);
        }

        @Override
        protected SRSListPanel srsListPanel() {
            SRSListPanel srsList = new SRSListPanel(popupWindow.getContentId()) {

                @Serial
                private static final long serialVersionUID = 2869219395676091081L;

                @Override
                protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                    popupWindow.close(target);

                    String srs = "EPSG:" + epsgCode;
                    srsTextField.setModelObject(srs);
                    target.add(srsTextField);

                    CoordinateReferenceSystem crs = fromSRS(srs);
                    wktLabel.setDefaultModelObject(crs.getName().toString());
                    wktLink.setEnabled(true);
                    target.add(wktLink);
                    updateUnits(crs);
                    target.add(units);
                    target.add(metersPerUnit);
                }
            };
            srsList.setCompactMode(true);
            return srsList;
        }

        @Override
        public void convertInput() {
            try {
                super.convertInput();
            } finally {
                updateUnits();
            }
        }

        @Override
        protected void onBeforeRender() {
            // updateUnits();
            super.onBeforeRender();
        }

        private void updateUnits() {
            CoordinateReferenceSystem crs = getConvertedInput();
            updateUnits(crs);
        }

        private void updateUnits(CoordinateReferenceSystem crs) {
            if (crs == null) {
                units.setDefaultModelObject("--");
                metersPerUnit.setDefaultModelObject("--");
            } else {
                Double meters = infoModel.getObject().getMetersPerUnit(crs);
                metersPerUnit.setDefaultModelObject(String.valueOf(meters));

                CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
                final Unit<?> unit = axis.getUnit();
                final String unitStr = unit.toString();
                units.setDefaultModelObject(unitStr);
            }
        }
    }

    private TextParamPanel name(IModel<GridSetInfo> model) {
        TextParamPanel<String> namePanel = new TextParamPanel<>(
                "name",
                new PropertyModel<>(model, "name"),
                new StringResourceModel("AbstractGridSetPage.name"),
                true,
                new UniqueNameValidator(model.getObject().getName()));
        return namePanel;
    }

    private GridSetInfo getInfo(final String gridSetName) {
        GridSetInfo gridsetInfo;

        if (gridSetName == null) {
            gridsetInfo = new GridSetInfo();
        } else {
            GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
            GridSet gridSet = gridSetBroker.get(gridSetName);
            if (gridSet == null) {
                throw new IllegalArgumentException("Requested GridSet does not exist: '" + gridSetName + "'");
            }

            final String name = gridSet.getName();
            final boolean internal = GWC.get().isInternalGridSet(name);

            gridsetInfo = new GridSetInfo(gridSet, internal);
        }
        return gridsetInfo;
    }

    private Component saveLink(Form<GridSetInfo> form) {
        return new AjaxSubmitLink("save", form) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSave(target, form);
            }
        };
    }

    protected abstract void onSave(AjaxRequestTarget target, Form<?> form);

    private static class UniqueNameValidator implements IValidator<String> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String previousName;

        /**
         * @param previousName the initial name of the gridset when the page loaded, may be {@code null} only in case
         *     we're creating a new gridset
         */
        public UniqueNameValidator(final String previousName) {
            this.previousName = previousName;
        }

        @Override
        public void validate(IValidatable<String> iv) {
            final String name = iv.getValue();
            if (name.equals(previousName)) {
                return;
            }
            final GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
            if (previousName != null) {
                gridSetBroker.get(previousName);
            }
            GridSet gridSet = gridSetBroker.get(name);
            if (gridSet != null) {
                ValidationError error = new ValidationError("gridSetAlreadyExists");
                error.setVariable("name", name);
                iv.error(error);
            }
        }
    }
}
