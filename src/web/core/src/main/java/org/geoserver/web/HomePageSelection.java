/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.catalog.Predicates.acceptAll;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.layer.PublishedChoiceRenderer;
import org.geoserver.web.data.layer.PublishedInfosModel;
import org.geoserver.web.data.workspace.WorkspaceChoiceNameRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

/**
 * Strategy to manage selection of {@link WorkspaceInfo} and {@link PublishedInfo}, as well as
 * describing the selection results.
 */
abstract class HomePageSelection implements Serializable {

    static final Logger LOGGER = Logging.getLogger(HomePageSelection.class);

    /** Maximum time to load workspaces and layers, in milliseconds */

    /**
     * System property used to externally define {@link #HomePageSelection}.
     *
     * <p>If provided this setting will override any configuration option.
     */
    public static String SELECTION_MODE = "GeoServerHomePage.selectionMode";

    enum SelectionMode {

        /** Automatically choose between dropdowns and simple text based on catalog size */
        AUTOMATIC,
        /**
         * Layer autocomplete is only available when workspace prefix provided. Suitable for large
         * catalogues with many workspaces
         */
        DROPDOWN,

        /** Disable autocomplete, use plain text-field for workspace and layer selection */
        TEXT;

        static SelectionMode get() {
            try {
                String mode = GeoServerExtensions.getProperty(SELECTION_MODE);
                if (!Strings.isEmpty(mode)) {
                    return SelectionMode.valueOf(mode.toUpperCase());
                }
            } catch (IllegalArgumentException ignore) {
                LOGGER.fine("Unrecognized GeoServer home page selection mode: " + ignore);
            }
            return SelectionMode.AUTOMATIC;
        }
    }

    /** The chosen selection mode */
    static SelectionMode MODE = SelectionMode.get();

    static long HOME_PAGE_TIMEOUT = Long.getLong("GeoServerHomePage.selectionTimeout", 5000);

    /** Maximum number of workspaces and layers to load */
    static int HOME_PAGE_MAX_ITEMS =
            Integer.getInteger("GeoServerHomePage.selectionMaxItems", 1000);

    public static HomePageSelection getHomePageSelection(GeoServerHomePage page) {
        if (MODE == SelectionMode.DROPDOWN) {
            return new DropDown(page);
        } else if (MODE == SelectionMode.TEXT) {
            return new Text(page);
        } else {
            return new Auto(page);
        }
    }

    protected final GeoServerHomePage page;

    HomePageSelection(GeoServerHomePage page) {
        this.page = page;
    }

    abstract String getDescription();

    abstract FormComponent<WorkspaceInfo> getWorkspaceField(Form form, String componentId);

    static FormComponent<WorkspaceInfo> getWorkspaceSelect2Choice(
            GeoServerHomePage page, Form form, String componentId) {
        Select2DropDownChoice<WorkspaceInfo> component =
                new Select2DropDownChoice<>(
                        "select",
                        new PropertyModel<>(page, "workspaceInfo"),
                        new WorkspacesModel(),
                        new WorkspaceChoiceNameRenderer()) {

                    @Override
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }

                    @Override
                    protected void onSelectionChanged(WorkspaceInfo newSelection) {
                        super.onSelectionChanged(newSelection);
                        if (newSelection != null) {
                            page.selectHomePage(newSelection.getName(), null);
                        } else {
                            String workspaceName = page.getWorkspaceFieldText();
                            page.selectHomePage(workspaceName, null);
                        }
                    }
                };
        component.setNullValid(true);

        Fragment fragment = new Fragment(componentId, "select", page);
        fragment.add(component);
        form.add(fragment);

        return component;
    }

    protected TextField<WorkspaceInfo> getWorkspaceTextField(Form form, String componentId) {
        TextField<WorkspaceInfo> component =
                new TextField<>("text", new PropertyModel<>(page, "workspaceInfo")) {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (WorkspaceInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new WorkspaceInfoConverter();
                        }
                        return null;
                    }
                };
        component.setOutputMarkupId(true);

        Fragment fragment = new Fragment(componentId, "text", page);
        fragment.add(component);
        form.add(fragment);

        return component;
    }

    /**
     * Returns the editor for the GeoServerHomePage.publishedInfo property
     *
     * @param componentId The identifier of the component
     */
    abstract FormComponent<PublishedInfo> getPublishedField(Form form, String componentId);

    static Select2DropDownChoice<PublishedInfo> getPublishedSelect2Choice(
            GeoServerHomePage page, Form form, String componentId) {
        PublishedInfosModel layersModel =
                new PublishedInfosModel() {
                    @Override
                    protected Filter getFilter() {
                        return getLayerFilter(page, page.getWorkspaceInfo());
                    }
                };
        IChoiceRenderer<PublishedInfo> layersRenderer =
                new PublishedChoiceRenderer() {
                    @Override
                    public Object getDisplayValue(PublishedInfo layer) {
                        return page.getWorkspaceInfo() != null
                                ? layer.getName()
                                : layer.prefixedName();
                    }
                };

        Select2DropDownChoice<PublishedInfo> component =
                new Select2DropDownChoice<>(
                        "select",
                        new PropertyModel<>(page, "publishedInfo"),
                        layersModel,
                        layersRenderer) {

                    @Override
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }

                    @Override
                    protected void onSelectionChanged(PublishedInfo newSelection) {
                        super.onSelectionChanged(newSelection);
                        if (newSelection != null) {
                            String prefixed = newSelection.prefixedName();
                            if (prefixed.contains(":")) {
                                String workspaceName = prefixed.substring(0, prefixed.indexOf(":"));
                                String layerName = prefixed.substring(prefixed.indexOf(":") + 1);

                                page.selectHomePage(workspaceName, layerName);
                            } else {
                                page.selectHomePage(null, prefixed);
                            }
                        } else {
                            String workspaceName = page.getWorkspaceFieldText();
                            page.selectHomePage(workspaceName, null);
                        }
                    }
                };
        component.setNullValid(true);

        Fragment fragment = new Fragment(componentId, "select", page);
        fragment.add(component);
        form.add(fragment);

        return component;
    }

    protected TextField<PublishedInfo> getPublishedTextField(Form form, String componentId) {
        TextField<PublishedInfo> component =
                new TextField<>("text", new PropertyModel<>(page, "publishedInfo")) {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (PublishedInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new PublishedInfoConverter();
                        }
                        return null;
                    }
                };
        component.setOutputMarkupId(true);

        Fragment fragment = new Fragment(componentId, "text", page);
        fragment.add(component);
        form.add(fragment);

        return component;
    }

    /**
     * Predicate construct to efficiently query catalog for PublishedInfo suitable for interaction.
     *
     * @param workspace Optional workspace to limit search to a single workspace
     * @return Filter for use with catalog.
     */
    private static Filter getLayerFilter(GeoServerHomePage page, WorkspaceInfo workspace) {

        // need to get only advertised and enabled layers
        Filter isLayerInfo = Predicates.isInstanceOf(LayerInfo.class);
        Filter isLayerGroupInfo = Predicates.isInstanceOf(LayerGroupInfo.class);

        Filter enabledFilter = Predicates.equal("resource.enabled", true);
        Filter storeEnabledFilter = Predicates.equal("resource.store.enabled", true);
        Filter advertisedFilter = Predicates.equal("resource.advertised", true);
        Filter enabledLayerGroup = Predicates.equal("enabled", true);
        Filter advertisedLayerGroup = Predicates.equal("advertised", true);

        // Filter for the Layers
        List<Filter> layerFilters = new ArrayList<>();
        layerFilters.add(isLayerInfo);
        if (workspace != null) {
            layerFilters.add(Predicates.equal("resource.namespace.prefix", workspace.getName()));
        }
        layerFilters.add(enabledFilter);
        layerFilters.add(storeEnabledFilter);
        layerFilters.add(advertisedFilter);

        Filter layerFilter = Predicates.and(layerFilters);

        // Filter for the LayerGroups
        List<Filter> groupFilters = new ArrayList<>();
        groupFilters.add(isLayerGroupInfo);
        if (workspace != null) {
            groupFilters.add(Predicates.equal("workspace.name", workspace.getName()));
        }
        if (!page.getGeoServer().getGlobal().isGlobalServices()) {
            // skip global layer groups if global services are disabled
            groupFilters.add(Predicates.not(Predicates.isNull("workspace.name")));
        }
        groupFilters.add(enabledLayerGroup);
        groupFilters.add(advertisedLayerGroup);

        Filter layerGroupFilter = Predicates.and(groupFilters);

        // Or filter for merging them
        return Predicates.or(layerFilter, layerGroupFilter);
    }

    protected StringResourceModel getFullDescription(int workspaceCount, int layerCount) {
        Locale locale = page.getLocale();
        PublishedInfo publishedInfo = page.getPublishedInfo();
        WorkspaceInfo workspaceInfo = page.getWorkspaceInfo();

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        numberFormat.setGroupingUsed(true);

        String userName = GeoServerSession.get().getUsername();

        HashMap<String, String> params = new HashMap<>();
        params.put("workspaceCount", numberFormat.format(workspaceCount));
        params.put("layerCount", numberFormat.format(layerCount));
        params.put("user", escapeMarkup(userName));

        boolean isGlobal = page.getGeoServer().getGlobal().isGlobalServices();

        if (publishedInfo != null && publishedInfo instanceof LayerInfo) {
            params.put("layerName", escapeMarkup(publishedInfo.prefixedName()));
            return new StringResourceModel(
                    "GeoServerHomePage.descriptionLayer", page, new Model<>(params));
        } else if (publishedInfo != null && publishedInfo instanceof LayerGroupInfo) {
            params.put("layerName", escapeMarkup(publishedInfo.prefixedName()));
            return new StringResourceModel(
                    "GeoServerHomePage.descriptionLayerGroup", page, new Model<>(params));
        } else if (workspaceInfo != null) {
            params.put("workspaceName", escapeMarkup(workspaceInfo.getName()));
            return new StringResourceModel(
                    "GeoServerHomePage.descriptionWorkspace", page, new Model<>(params));
        } else if (isGlobal) {
            return new StringResourceModel(
                    "GeoServerHomePage.descriptionGlobal", page, new Model<>(params));
        } else {
            return new StringResourceModel(
                    "GeoServerHomePage.descriptionGlobalOff", page, new Model<>(params));
        }
    }

    /**
     * Count of PublishedInfo (ie layer or layergroup) taking the current workspace and global
     * services into account.
     *
     * @return Count of addressable layers
     */
    private static int countLayerNames(GeoServerHomePage page, WorkspaceInfo workspaceInfo) {
        return page.getCatalog().count(PublishedInfo.class, getLayerFilter(page, workspaceInfo));
    }

    /**
     * Escape text before being used in formatting (to prevent any raw html being displayed).
     *
     * @param text Text to escape
     * @return escaped text
     */
    private String escapeMarkup(String text) {
        return new StringBuilder(Strings.escapeMarkup(text)).toString();
    }

    /** Forces drop down selection */
    static class DropDown extends HomePageSelection {
        public DropDown(GeoServerHomePage page) {
            super(page);
        }

        @Override
        String getDescription() {
            int workspaceCount = page.getCatalog().count(WorkspaceInfo.class, acceptAll());
            int layerCount = countLayerNames(page, page.getWorkspaceInfo());
            return getFullDescription(workspaceCount, layerCount).getString();
        }

        @Override
        FormComponent<WorkspaceInfo> getWorkspaceField(Form form, String componentId) {
            return getWorkspaceSelect2Choice(page, form, componentId);
        }

        @Override
        FormComponent<PublishedInfo> getPublishedField(Form form, String componentId) {
            return getPublishedSelect2Choice(page, form, componentId);
        }
    }

    static class Text extends HomePageSelection {
        public Text(GeoServerHomePage page) {
            super(page);
        }

        @Override
        String getDescription() {
            return new ParamResourceModel("GeoServerHomePage.description", page).getString();
        }

        @Override
        FormComponent<WorkspaceInfo> getWorkspaceField(Form form, String componentId) {
            return getWorkspaceTextField(form, componentId);
        }

        @Override
        FormComponent<PublishedInfo> getPublishedField(Form form, String componentId) {
            return getPublishedTextField(form, componentId);
        }
    }

    static class Auto extends HomePageSelection {

        private final BoundedCatalogLoader<WorkspaceInfo> workspaceLoader;
        private BoundedCatalogLoader<PublishedInfo> publishedLoader;

        public Auto(GeoServerHomePage page) {
            super(page);
            // load workspaces and layers honoring max total time
            this.workspaceLoader =
                    new BoundedCatalogLoader<>(
                            page.getCatalog(),
                            Predicates.acceptAll(),
                            WorkspaceInfo.class,
                            HOME_PAGE_TIMEOUT,
                            HOME_PAGE_MAX_ITEMS);
            this.publishedLoader =
                    new BoundedCatalogLoader<>(
                            page.getCatalog(),
                            getLayerFilter(page, page.getWorkspaceInfo()),
                            PublishedInfo.class,
                            workspaceLoader.getResidualTime(),
                            HOME_PAGE_MAX_ITEMS);
        }

        @Override
        String getDescription() {
            // if it was too expensive to compute counts, go with
            if (workspaceLoader.isBoundExceeded() || publishedLoader.isBoundExceeded())
                return new ParamResourceModel("GeoServerHomePage.description", page).getString();
            else
                return getFullDescription(
                                workspaceLoader.getResult().size(),
                                publishedLoader.getResult().size())
                        .getString();
        }

        @Override
        FormComponent<WorkspaceInfo> getWorkspaceField(Form form, String componentId) {
            if (workspaceLoader.isBoundExceeded()) return getWorkspaceTextField(form, componentId);
            return getWorkspaceSelect2Choice(page, form, componentId);
        }

        @Override
        FormComponent<PublishedInfo> getPublishedField(Form form, String componentId) {
            if (publishedLoader.isBoundExceeded()) return getPublishedTextField(form, componentId);
            return getPublishedSelect2Choice(page, form, componentId);
        }
    }

    private class WorkspaceInfoConverter implements IConverter<WorkspaceInfo> {
        @Override
        public WorkspaceInfo convertToObject(String s, Locale locale) throws ConversionException {
            return page.getCatalog().getWorkspaceByName(s);
        }

        @Override
        public String convertToString(WorkspaceInfo workspaceInfo, Locale locale) {
            return workspaceInfo.getName();
        }
    }

    private class PublishedInfoConverter implements IConverter<PublishedInfo> {
        @Override
        public PublishedInfo convertToObject(String s, Locale locale) throws ConversionException {
            WorkspaceInfo ws = page.getWorkspaceInfo();
            if (ws != null) {
                PublishedInfo result = page.getCatalog().getLayerGroupByName(ws, s);
                if (result == null && !s.contains(":")) {
                    result = page.getCatalog().getLayerByName(ws.getName() + ":" + s);
                } else {
                    result = page.getCatalog().getLayerByName(s);
                }
                return result;

            } else {
                PublishedInfo result = page.getCatalog().getLayerGroupByName(s);
                if (result == null) {
                    result = page.getCatalog().getLayerByName(s);
                }
                return result;
            }
        }

        @Override
        public String convertToString(PublishedInfo published, Locale locale) {
            if (page.getWorkspaceInfo() != null) return published.getName();
            else return published.prefixedName();
        }
    }
}
