/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.gridset.GridSetListTablePanel;
import org.geoserver.gwc.web.gridset.GridSetTableProvider;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

/**
 * Form component that edits the default {@link GWCConfig#getDefaultCachingGridSetIds() cached
 * gridsets} for {@link CachingOptionsPanel}.
 *
 * @author groldan
 */
class DefaultGridsetsEditor extends FormComponentPanel<Set<String>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private final IModel<? extends List<String>> selection;

    private DefaultGridSetsTable defaultGridsetsTable;

    private final DropDownChoice<String> availableGridSets;

    private class DefaultGridSetsTable extends GridSetListTablePanel {
        private static final long serialVersionUID = -3301795024743630393L;

        public DefaultGridSetsTable(String id, GridSetTableProvider provider) {
            super(id, provider, false);
            setOutputMarkupId(true);
            setPageable(false);
            setFilterable(false);
        }

        @Override
        protected Component nameLink(final String id, final GridSet gridSet) {
            Label label = new Label(id, gridSet.getName());
            label.add(new AttributeModifier("title", new Model<String>(gridSet.getDescription())));
            return label;
        }

        @Override
        protected Component actionLink(final String id, String gridSetName) {

            @SuppressWarnings("rawtypes")
            Component removeLink =
                    new ImageAjaxLink(id, GWCIconFactory.DELETE_ICON) {
                        private static final long serialVersionUID = 1L;

                        /** Removes the selected item from the provider's model */
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            final String gridsetName = getDefaultModelObjectAsString();
                            List<String> selection =
                                    DefaultGridsetsEditor.this.selection.getObject();
                            selection.remove(gridsetName);
                            List<String> choices =
                                    new ArrayList<String>(availableGridSets.getChoices());
                            choices.add(gridsetName);
                            Collections.sort(choices);
                            availableGridSets.setChoices(choices);
                            target.add(defaultGridsetsTable);
                            target.add(availableGridSets);
                        }
                    };
            removeLink.setDefaultModel(new Model<String>(gridSetName));

            return removeLink;
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<GridSet> itemModel, Property<GridSet> property) {
            // Property objects are package access, so we can't statically reference them here
            // see org.geoserver.gwc.web.gridset.ACTION_LINK
            final String propertyName = property.getName();
            // the Remove link property name is the empty string. If that is the property name,
            // return the actionLink here.
            if (Strings.isEmpty(propertyName)) {
                return actionLink(id, itemModel.getObject().getName());
            }
            return null;
        }
    }

    public DefaultGridsetsEditor(final String id, final IModel<Set<String>> model) {
        super(id, model);
        selection = new Model<ArrayList<String>>(new ArrayList<String>(model.getObject()));

        GridSetTableProvider provider =
                new GridSetTableProvider() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<GridSet> getItems() {
                        GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
                        List<String> list = selection.getObject();
                        List<GridSet> gridsets = new ArrayList<GridSet>(list.size());
                        for (String id : list) {
                            GridSet gridSet = gridSetBroker.get(id);
                            if (gridSet != null) {
                                gridsets.add(gridSet);
                            }
                        }
                        return gridsets;
                    }
                };

        defaultGridsetsTable = new DefaultGridSetsTable("table", provider);
        add(defaultGridsetsTable);

        IModel<List<String>> availableModel =
                new LoadableDetachableModel<List<String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<String> load() {
                        List<String> gridSetNames =
                                new ArrayList<String>(GWC.get().getGridSetBroker().getNames());
                        for (String gsId : selection.getObject()) {
                            gridSetNames.remove(gsId);
                        }
                        Collections.sort(gridSetNames);
                        return gridSetNames;
                    }
                };

        availableGridSets =
                new DropDownChoice<String>(
                        "availableGridsets", new Model<String>(), availableModel);
        availableGridSets.setOutputMarkupId(true);
        add(availableGridSets);

        GeoServerAjaxFormLink addGridsubsetLink =
                new GeoServerAjaxFormLink("addGridset") {
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

                        List<String> selectedIds = selection.getObject();
                        selectedIds.add(selectedGridset);
                        // Execute setPageable() in order to re-create the inner record list
                        // updated.
                        defaultGridsetsTable.setPageable(false);
                        target.add(defaultGridsetsTable);
                        target.add(availableGridSets);
                    }
                };
        addGridsubsetLink.add(new Icon("addIcon", GWCIconFactory.ADD_ICON));
        add(addGridsubsetLink);
    }

    @Override
    public void convertInput() {
        List<String> defaultGridsetIds = selection.getObject();
        Set<String> convertedInput = new HashSet<String>();
        convertedInput.addAll(defaultGridsetIds);
        setConvertedInput(convertedInput);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }
}
