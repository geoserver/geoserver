/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

class GridSubsetsEditor extends FormComponentPanel<Set<XMLGridSubset>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private final WebMarkupContainer table;

    private final ListView<XMLGridSubset> grids;

    private final DropDownChoice<String> availableGridSets;

    private final GridSubsetListValidator validator;

    private class GridSubsetListValidator implements IValidator<Set<XMLGridSubset>> {

        private static final long serialVersionUID = -2646310164736911748L;

        private boolean validate;

        public GridSubsetListValidator() {
            this.setEnabled(true);
        }

        @Override
        public void validate(IValidatable<Set<XMLGridSubset>> validatable) {
            if (!validate) {
                return;
            }
            Set<XMLGridSubset> gridSubsets = validatable.getValue();
            if (gridSubsets == null || gridSubsets.size() == 0) {
                error(validatable, "GridSubsetsEditor.validation.empty");
                return;
            }

            final GWC gwc = GWC.get();
            for (XMLGridSubset subset : gridSubsets) {
                final String gridSetName = subset.getGridSetName();
                final Integer zoomStart = subset.getZoomStart();
                final Integer zoomStop = subset.getZoomStop();
                final BoundingBox extent = subset.getExtent();

                if (gridSetName == null) {
                    throw new IllegalStateException("GridSet name is null");
                }

                if (zoomStart != null && zoomStop != null) {
                    if (zoomStart.intValue() > zoomStop.intValue()) {
                        error(validatable, "GridSubsetsEditor.validation.zoomLevelsError");
                        return;
                    }
                }

                final GridSetBroker gridSetBroker = gwc.getGridSetBroker();
                final GridSet gridSet = gridSetBroker.get(gridSetName);

                if (null == gridSet) {
                    error(validatable, "GridSubsetsEditor.validation.gridSetNotFound", gridSetName);
                    return;
                }

                if (extent != null) {
                    if (extent.isNull() || !extent.isSane()) {
                        error(validatable, "GridSubsetsEditor.validation.invalidBounds");
                    }
                    final BoundingBox fullBounds = gridSet.getOriginalExtent();
                    final boolean intersects = fullBounds.intersects(extent);
                    if (!intersects) {
                        error(validatable, "GridSubsetsEditor.validation.boundsOutsideCoverage");
                    }
                }
            }
        }

        private void error(
                IValidatable<Set<XMLGridSubset>> validatable,
                final String resourceKey,
                final String... params) {

            ValidationError error = new ValidationError();
            String message;
            if (params == null) {
                message = new ResourceModel(resourceKey).getObject();
            } else {
                message =
                        new ParamResourceModel(
                                        resourceKey, GridSubsetsEditor.this, (Object[]) params)
                                .getObject();
            }
            error.setMessage(message);
            validatable.error(error);
        }

        public void setEnabled(boolean validate) {
            this.validate = validate;
        }
    }

    public GridSubsetsEditor(final String id, final IModel<Set<XMLGridSubset>> model) {
        super(id, model);
        add(validator = new GridSubsetListValidator());

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);

        container.add(table);

        grids =
                new ListView<XMLGridSubset>(
                        "gridSubsets", new ArrayList<XMLGridSubset>(model.getObject())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                    }

                    @Override
                    protected void populateItem(final ListItem<XMLGridSubset> item) {
                        // odd/even style
                        final int index = item.getIndex();
                        item.add(
                                AttributeModifier.replace(
                                        "class", index % 2 == 0 ? "even" : "odd"));

                        final XMLGridSubset gridSubset = item.getModelObject();
                        GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();

                        String gridsetDescription = null;
                        int gridsetLevels;
                        boolean gridsetExists;
                        {
                            final GridSet gridSet = gridSetBroker.get(gridSubset.getGridSetName());
                            gridsetExists = gridSet != null;
                            if (gridsetExists) {
                                gridsetLevels = gridSet.getNumLevels();
                                gridsetDescription = gridSet.getDescription();
                            } else {
                                gridsetLevels =
                                        gridSubset.getZoomStop() == null
                                                ? 1
                                                : gridSubset.getZoomStop().intValue();
                            }
                        }
                        final Label gridSetLabel;
                        final Component gridSetBounds;

                        gridSetLabel =
                                new Label(
                                        "gridSet",
                                        new PropertyModel<String>(item.getModel(), "gridSetName"));
                        if (!gridsetExists) {
                            gridSetLabel.add(
                                    new AttributeModifier(
                                            "style",
                                            new Model<String>(
                                                    "color:red;text-decoration:line-through;")));
                            getPage()
                                    .warn(
                                            "GridSet "
                                                    + gridSubset.getGridSetName()
                                                    + " does not exist");
                        }
                        item.add(gridSetLabel);
                        if (null != gridsetDescription) {
                            gridSetLabel.add(
                                    new AttributeModifier(
                                            "title", new Model<String>(gridsetDescription)));
                        }

                        final Component removeLink;

                        final int maxZoomLevel = gridsetLevels - 1;
                        final ArrayList<Integer> zoomLevels =
                                new ArrayList<Integer>(maxZoomLevel + 1);
                        for (int z = 0; z <= maxZoomLevel; z++) {
                            zoomLevels.add(Integer.valueOf(z));
                        }

                        // zoomStart has all zoom levels as choices
                        // zoomStop choices start at zoomStart's selection
                        // minCachedLevel start at zoomStart's selection and ends at zoomStop's
                        // selection
                        // maxCachedLevel start at minCachedLevels' and ends at zoomStop's selection

                        final IModel<Integer> zoomStartModel;
                        final IModel<Integer> zoomStopModel;
                        final IModel<Integer> minCachedLevelModel;
                        final IModel<Integer> maxCachedLevelModel;

                        final ZoomLevelDropDownChoice zoomStart;
                        final ZoomLevelDropDownChoice zoomStop;
                        final ZoomLevelDropDownChoice minCachedLevel;
                        final ZoomLevelDropDownChoice maxCachedLevel;

                        zoomStartModel = new PropertyModel<Integer>(item.getModel(), "zoomStart");
                        zoomStopModel = new PropertyModel<Integer>(item.getModel(), "zoomStop");
                        minCachedLevelModel =
                                new PropertyModel<Integer>(item.getModel(), "minCachedLevel");
                        maxCachedLevelModel =
                                new PropertyModel<Integer>(item.getModel(), "maxCachedLevel");

                        @SuppressWarnings({"rawtypes", "unchecked"})
                        final IModel<List<Integer>> allLevels = new Model(zoomLevels);

                        zoomStart =
                                new ZoomLevelDropDownChoice("zoomStart", zoomStartModel, allLevels);
                        zoomStart.setEnabled(gridsetExists);
                        item.add(zoomStart);

                        zoomStop =
                                new ZoomLevelDropDownChoice("zoomStop", zoomStopModel, allLevels);
                        zoomStop.setEnabled(gridsetExists);
                        item.add(zoomStop);

                        minCachedLevel =
                                new ZoomLevelDropDownChoice(
                                        "minCachedLevel", minCachedLevelModel, allLevels);
                        minCachedLevel.setEnabled(gridsetExists);
                        item.add(minCachedLevel);

                        maxCachedLevel =
                                new ZoomLevelDropDownChoice(
                                        "maxCachedLevel", maxCachedLevelModel, allLevels);
                        maxCachedLevel.setEnabled(gridsetExists);
                        item.add(maxCachedLevel);

                        for (ZoomLevelDropDownChoice dropDown :
                                Arrays.asList(
                                        zoomStart, zoomStop, minCachedLevel, maxCachedLevel)) {
                            dropDown.add(
                                    new OnChangeAjaxBehavior() {
                                        private static final long serialVersionUID = 1L;

                                        // cascades to zoomStop, min and max cached levels
                                        @Override
                                        protected void onUpdate(AjaxRequestTarget target) {
                                            updateValidZoomRanges(
                                                    zoomStart,
                                                    zoomStop,
                                                    minCachedLevel,
                                                    maxCachedLevel,
                                                    target);
                                        }
                                    });
                        }

                        updateValidZoomRanges(
                                zoomStart, zoomStop, minCachedLevel, maxCachedLevel, null);

                        // TODO Should probably use a convertor instead of an if but this should
                        // work until we decide to make this editable.
                        if (Objects.nonNull(item.getModelObject().getExtent())) {
                            gridSetBounds =
                                    new Label(
                                            "bounds",
                                            new PropertyModel<Integer>(item.getModel(), "extent"));
                        } else {
                            gridSetBounds =
                                    new Label(
                                            "bounds",
                                            new ResourceModel("GridSubsetsEditor.bounds.dynamic"));
                        }
                        item.add(gridSetBounds);

                        removeLink =
                                new ImageAjaxLink<Object>(
                                        "removeLink", GWCIconFactory.DELETE_ICON) {

                                    private static final long serialVersionUID =
                                            -5072597940769821889L;

                                    @Override
                                    protected void onClick(AjaxRequestTarget target) {
                                        List<XMLGridSubset> list;
                                        list = new ArrayList<XMLGridSubset>(grids.getModelObject());
                                        final XMLGridSubset subset =
                                                (XMLGridSubset) getDefaultModelObject();

                                        list.remove(subset);

                                        grids.setModelObject(list);

                                        List<String> choices =
                                                new ArrayList<String>(
                                                        availableGridSets.getChoices());
                                        choices.add(subset.getGridSetName());
                                        Collections.sort(choices);
                                        availableGridSets.setChoices(choices);

                                        target.add(container);
                                        target.add(availableGridSets);
                                    }
                                };
                        removeLink.setDefaultModel(item.getModel());
                        removeLink.add(
                                new AttributeModifier(
                                        "title",
                                        new ResourceModel("GridSubsetsEditor.removeLink")));
                        item.add(removeLink);
                    }
                };

        grids.setOutputMarkupId(true);
        // this is necessary to avoid loosing item contents on edit/validation checks
        grids.setReuseItems(true);
        table.add(grids);

        List<String> gridSetNames = new ArrayList<String>(GWC.get().getGridSetBroker().getNames());
        for (XMLGridSubset gs : model.getObject()) {
            gridSetNames.remove(gs.getGridSetName());
        }
        Collections.sort(gridSetNames);

        GeoServerAjaxFormLink addGridsubsetLink =
                new GeoServerAjaxFormLink("addGridSubset") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        availableGridSets.processInput();

                        final String selectedGridset = availableGridSets.getModelObject();
                        if (null == selectedGridset) {
                            return;
                        }

                        List<String> choices =
                                new ArrayList<String>(availableGridSets.getChoices());
                        choices.remove(selectedGridset);
                        availableGridSets.setChoices(choices);
                        availableGridSets.setEnabled(!choices.isEmpty());

                        XMLGridSubset newSubset = new XMLGridSubset();
                        newSubset.setGridSetName(selectedGridset);
                        grids.getModelObject().add(newSubset);

                        target.add(table);
                        target.add(availableGridSets);
                    }
                };
        addGridsubsetLink.add(new Icon("addIcon", GWCIconFactory.ADD_ICON));
        add(addGridsubsetLink);

        availableGridSets =
                new DropDownChoice<String>("availableGridsets", new Model<String>(), gridSetNames);
        availableGridSets.setOutputMarkupId(true);
        add(availableGridSets);
    }

    @Override
    public void convertInput() {
        grids.visitChildren(
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                });
        List<XMLGridSubset> info = grids.getModelObject();
        HashSet<XMLGridSubset> convertedInput = new HashSet<XMLGridSubset>(info);
        setConvertedInput(convertedInput);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }

    public void setValidating(final boolean validate) {
        validator.setEnabled(validate);
    }

    private static class ZoomLevelDropDownChoice extends DropDownChoice<Integer> {
        private static final long serialVersionUID = -1312406093015271637L;

        public ZoomLevelDropDownChoice(
                final String id, IModel<Integer> model, IModel<List<Integer>> allChoices) {
            super(id, model, allChoices);
            setNullValid(true); // show null option even if model value isn't null
            setOutputMarkupId(true);
        }

        @Override
        protected String getNullValidKey() {
            String nullValidKey = "GridSubsetsEditor." + getId() + ".nullValid";
            return nullValidKey;
        }

        public void setAllowedMin(int newMin) {
            List<? extends Integer> choices = getChoices();
            final int max = choices.get(choices.size() - 1).intValue();
            setChoices(newMin, Math.max(newMin, max));
        }

        public void setAllowedMax(int max) {
            List<? extends Integer> choices = getChoices();
            final int min = choices.get(0).intValue();
            setChoices(Math.min(min, max), max);
        }

        private void setChoices(int min, int max) {
            List<Integer> choices = new ArrayList<Integer>();
            for (int i = min; i <= max; i++) {
                choices.add(Integer.valueOf(i));
            }
            Integer modelObject = getModelObject();
            setChoices(choices);
            if (modelObject != null && modelObject >= min && modelObject <= max) {
                setModelObject(modelObject);
            } else {
                setModelObject(null);
            }
        }
    }

    private void updateValidZoomRanges(
            final ZoomLevelDropDownChoice zoomStart,
            final ZoomLevelDropDownChoice zoomStop,
            final ZoomLevelDropDownChoice minCachedLevel,
            final ZoomLevelDropDownChoice maxCachedLevel,
            AjaxRequestTarget target) {

        // zoomStart.processInput();
        Integer min = zoomStart.getModelObject();
        if (min != null) {
            zoomStop.setAllowedMin(min.intValue());
            minCachedLevel.setAllowedMin(min.intValue());
            maxCachedLevel.setAllowedMin(min.intValue());
        }

        Integer max = zoomStop.getModelObject();
        if (max != null) {
            minCachedLevel.setAllowedMax(max.intValue());
            maxCachedLevel.setAllowedMax(max.intValue());
        }

        Integer minCached = minCachedLevel.getModelObject();
        if (minCached != null) {
            maxCachedLevel.setAllowedMin(minCached.intValue());
        }

        if (null != target) {
            target.add(zoomStop);
            target.add(minCachedLevel);
            target.add(maxCachedLevel);
        }
    }
}
