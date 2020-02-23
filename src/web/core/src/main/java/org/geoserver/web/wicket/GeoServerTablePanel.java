/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * An abstract filterable, sortable, pageable table with associated filtering form and paging
 * navigator.
 *
 * <p>The construction of the page is driven by the properties returned by a {@link
 * GeoServerDataProvider}, subclasses only need to build a component for each property by
 * implementing the {@link #getComponentForProperty(String, IModel, Property)} method
 *
 * @param <T>
 */
public abstract class GeoServerTablePanel<T> extends Panel {

    private static final long serialVersionUID = -5275268446479549108L;

    private static final int DEFAULT_ITEMS_PER_PAGE = 25;

    public static final String FILTER_PARAM = "filter";

    /** METADATA MAP inside user session that remembers the filters user input inside the form */
    private static final String FILTER_INPUTS = "userInput";

    private static final String SORT_INPUTS = "userSort";

    // filter form components
    TextField<String> filter;

    // table components
    DataView<T> dataView;

    WebMarkupContainer listContainer;

    PagerDelegate pagerDelegate;

    Pager navigatorTop;

    Pager navigatorBottom;

    GeoServerDataProvider<T> dataProvider;

    Form<?> filterForm;

    CheckBox selectAll;

    AjaxButton hiddenSubmit;

    AjaxLink clearFilter;

    boolean sortable = true;

    boolean selectable = true;

    /**
     * An array of the selected items in the current page. Gets wiped out each time the current
     * page, the sorting or the filtering changes.
     */
    boolean[] selection;

    boolean selectAllValue;
    boolean pageable;

    /** Builds a non selectable table */
    public GeoServerTablePanel(final String id, final GeoServerDataProvider<T> dataProvider) {
        this(id, dataProvider, false);
    }

    /** Builds a new table panel */
    public GeoServerTablePanel(
            final String id,
            final GeoServerDataProvider<T> dataProvider,
            final boolean selectable) {
        super(id);
        this.dataProvider = dataProvider;
        // check if the request came from left menu link
        // if so reset any previously used filter and treat it as a REST
        // if param not found, treat as keep the filter intact
        Boolean keepFilter =
                getRequest().getRequestParameters().getParameterValue(FILTER_PARAM).toBoolean(true);

        String previousInput = loadPreviousInput();
        if (previousInput != null && keepFilter) {
            // setting the previous filter UP FRONT
            if (!previousInput.isEmpty()) {
                dataProvider.setKeywords(loadPreviousInput().split("\\s+"));
            }
        } else if (!keepFilter) {
            // panel was invoke from Left Grid menu
            // clear the filter from session
            // previousInput to null to hide clear button
            clearFilterFromSession();
            previousInput = null;
        }

        SortParam previousSort = loadPreviousSort();

        if (previousSort != null && keepFilter) dataProvider.setSort(previousSort);
        else if (!keepFilter) {
            // panel was invoke from Left Grid menu
            // clear the filter from session
            // previousInput to null to hide clear button
            clearSortFromSession();
            previousInput = null;
        }

        // prepare the selection array
        selection = new boolean[DEFAULT_ITEMS_PER_PAGE];

        // layer container used for ajax-y udpates of the table
        listContainer = new WebMarkupContainer("listContainer");

        // build the filter form
        filterForm = new Form<>("filterForm");
        filterForm.setOutputMarkupId(true);
        add(filterForm);
        filter =
                new TextField<String>("filter", new Model<String>()) {
                    private static final long serialVersionUID = -1252520208030081584L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put(
                                "onkeypress",
                                "if(event.keyCode == 13) {document.getElementById('"
                                        + hiddenSubmit.getMarkupId()
                                        + "').click();return false;}");
                    }

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();

                        String previousInput = loadPreviousInput();
                        if (previousInput != null)
                            if (!previousInput.isEmpty()) {
                                this.setModelObject(previousInput);
                                // dataProvider.setKeywords(previousInput.split("\\s+"));
                            }
                    }
                };
        filterForm.add(filter);
        filter.add(
                AttributeModifier.replace(
                        "title",
                        String.valueOf(
                                new ResourceModel("GeoServerTablePanel.search", "Search")
                                        .getObject())));
        filterForm.add(hiddenSubmit = hiddenSubmit());
        filterForm.setDefaultButton(hiddenSubmit);

        clearFilter = getClearFilterLink(previousInput);
        filterForm.add(clearFilter);

        // setup the table
        listContainer.setOutputMarkupId(true);
        add(listContainer);
        dataView =
                new DataView<T>("items", dataProvider) {
                    private static final long serialVersionUID = 7201317388415148823L;

                    @Override
                    protected Item<T> newItem(String id, int index, IModel<T> model) {
                        OddEvenItem<T> item = new OddEvenItem<T>(id, index, model);
                        item.setOutputMarkupId(true);
                        return item;
                    }

                    @Override
                    protected void populateItem(Item<T> item) {
                        final IModel<T> itemModel = item.getModel();

                        // add row selector (visible only if selection is active)
                        WebMarkupContainer cnt = new WebMarkupContainer("selectItemContainer");
                        cnt.add(selectOneCheckbox(item));
                        cnt.setVisible(selectable);
                        item.add(cnt);

                        buildRowListView(dataProvider, item, itemModel);
                    }
                };
        dataView.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        listContainer.add(dataView);

        // add select all checkbox
        WebMarkupContainer cnt = new WebMarkupContainer("selectAllContainer");
        cnt.add(selectAll = selectAllCheckbox());
        cnt.setVisible(selectable);
        listContainer.add(cnt);

        // add the sorting links
        listContainer.add(buildLinksListView(dataProvider));

        // add the paging navigator and set the items per page
        dataView.setItemsPerPage(DEFAULT_ITEMS_PER_PAGE);
        pagerDelegate = new PagerDelegate();

        filterForm.add(navigatorTop = new Pager("navigatorTop"));
        navigatorTop.setOutputMarkupId(true);
        add(navigatorBottom = new Pager("navigatorBottom"));
        navigatorBottom.setOutputMarkupId(true);
    }

    protected ListView<Property<T>> buildLinksListView(
            final GeoServerDataProvider<T> dataProvider) {
        return new ListView<Property<T>>("sortableLinks", dataProvider.getVisibleProperties()) {

            private static final long serialVersionUID = -7565457802398721254L;

            @Override
            protected void populateItem(ListItem<Property<T>> item) {
                Property<T> property = (Property<T>) item.getModelObject();

                // build a sortable link if the property is sortable, a label otherwise
                IModel<String> titleModel = getPropertyTitle(property);
                if (sortable && property.getComparator() != null) {
                    Fragment f = new Fragment("header", "sortableHeader", GeoServerTablePanel.this);
                    AjaxLink<Property<T>> link = sortLink(dataProvider, item);
                    link.add(new Label("label", titleModel));
                    f.add(link);
                    item.add(f);
                } else {
                    item.add(new Label("header", titleModel));
                }
            }
        };
    }

    protected void buildRowListView(
            final GeoServerDataProvider<T> dataProvider, Item<T> item, final IModel<T> itemModel) {
        // make sure we don't serialize the list, but get it fresh from the dataProvider,
        // to avoid serialization issues seen in GEOS-8273
        IModel propertyList =
                new LoadableDetachableModel() {

                    @Override
                    protected Object load() {
                        return dataProvider.getVisibleProperties();
                    }
                };
        // create one component per viewable property
        ListView<Property<T>> items =
                new ListView<Property<T>>("itemProperties", propertyList) {

                    private static final long serialVersionUID = -4552413955986008990L;

                    @Override
                    protected void populateItem(ListItem<Property<T>> item) {
                        Property<T> property = item.getModelObject();

                        Component component =
                                getComponentForProperty("component", itemModel, property);

                        if (component == null) {
                            // show a plain label if the the subclass did not create any component
                            component = new Label("component", property.getModel(itemModel));
                        } else if (!"component".equals(component.getId())) {
                            // add some checks for the id, the error message
                            // that wicket returns in case of mismatch is not
                            // that helpful
                            throw new IllegalArgumentException(
                                    "getComponentForProperty asked "
                                            + "to build a component "
                                            + "with id = 'component' "
                                            + "for property '"
                                            + property.getName()
                                            + "', but got '"
                                            + component.getId()
                                            + "' instead");
                        }
                        item.add(component);
                        onPopulateItem(property, item);
                    }
                };
        items.setReuseItems(true);
        item.add(items);
    }

    /**
     * Sets the item reuse strategy for the table. Should be {@link ReuseIfModelsEqualStrategy} if
     * you're building an editable table, {@link DefaultItemReuseStrategy} otherwise
     */
    public void setItemReuseStrategy(IItemReuseStrategy strategy) {
        dataView.setItemReuseStrategy(strategy);
    }

    /** Whether this table will have sortable headers, or not */
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    /** Returns pager above the table */
    public Component getTopPager() {
        return navigatorTop;
    }

    /** Returns the pager below the table */
    public Component getBottomPager() {
        return navigatorBottom;
    }

    /** Returns the data provider feeding this table */
    public GeoServerDataProvider<T> getDataProvider() {
        return dataProvider;
    }

    /**
     * Called each time selection checkbox changes state due to a user action. By default it does
     * nothing, subclasses can implement this to provide extra behavior
     */
    protected void onSelectionUpdate(AjaxRequestTarget target) {
        // by default do nothing
    }

    /**
     * Returns a model for this property title. Default behaviour is to lookup for a resource name
     * <page>.th.<propertyName>
     */
    protected IModel<String> getPropertyTitle(Property<T> property) {
        ResourceModel resMod = new ResourceModel("th." + property.getName(), property.getName());
        return resMod;
    }

    /** @return the number of items selected in the current page */
    public int getNumSelected() {
        int selected = 0;
        for (boolean itemSelected : selection) {
            if (itemSelected) {
                selected++;
            }
        }
        return selected;
    }

    /** Returns the items that have been selected by the user */
    @SuppressWarnings("unchecked")
    public List<T> getSelection() {
        List<T> result = new ArrayList<T>();
        int i = 0;
        for (Iterator<Component> it = dataView.iterator(); it.hasNext(); ) {
            Component item = it.next();
            if (selection[i]) {
                result.add((T) item.getDefaultModelObject());
            }
            i++;
        }
        return result;
    }

    CheckBox selectAllCheckbox() {
        CheckBox sa = new CheckBox("selectAll", new PropertyModel<Boolean>(this, "selectAllValue"));
        sa.setOutputMarkupId(true);
        sa.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    private static final long serialVersionUID = 1154921156065269691L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // select all the checkboxes
                        setSelection(selectAllValue);

                        // update table and the checkbox itself
                        target.add(getComponent());
                        target.add(listContainer);

                        // allow subclasses to play on this change as well
                        onSelectionUpdate(target);
                    }
                });
        return sa;
    }

    protected CheckBox selectOneCheckbox(Item<T> item) {
        CheckBox cb = new CheckBox("selectItem", new SelectionModel(item.getIndex()));
        cb.setOutputMarkupId(true);
        cb.setVisible(selectable);
        cb.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    private static final long serialVersionUID = -2419184741329470638L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (Boolean.FALSE.equals(getComponent().getDefaultModelObject())) {
                            selectAllValue = false;
                            target.add(selectAll);
                        }
                        onSelectionUpdate(target);
                    }
                });
        return cb;
    }

    /** When set to false, will prevent the selection checkboxes from showing up */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        selectAll.setVisible(selectable);
    }

    void setSelection(boolean selected) {
        for (int i = 0; i < selection.length; i++) {
            selection[i] = selected;
        }
        selectAllValue = selected;
    }

    /** Clears the current selection */
    public void clearSelection() {
        setSelection(false);
    }

    /** Selects all the items in the current page */
    public void selectAll() {
        setSelection(true);
    }

    /** Selects a single item by object. */
    public void selectObject(T object) {
        int i = 0;
        for (Iterator<Component> it = dataView.iterator(); it.hasNext(); ) {
            @SuppressWarnings("unchecked")
            Item<T> item = (Item<T>) it.next();
            if (object.equals(item.getModelObject())) {
                selection[i] = true;
                return;
            }
            i++;
        }
    }

    /** Selects a single item by index. */
    public void selectIndex(int i) {
        validateSelectionIndex(i);
        selection[i] = true;
    }

    /** Un-selects a single item by index. */
    public void unseelectIndex(int i) {
        validateSelectionIndex(i);
        selection[i] = false;
    }

    public void validateSelectionIndex(int i) {
        if (selection.length <= i) {
            if (dataProvider.size() <= i) {
                throw new ArrayIndexOutOfBoundsException(i);
            } else {
                // expand selection array, the data provider likely resized and the two got
                // misaligned
                boolean[] newSelection = new boolean[(int) dataProvider.size()];
                System.arraycopy(selection, 0, newSelection, 0, selection.length);
                this.selection = newSelection;
            }
        }
    }

    /** The hidden button that will submit the form when the user presses enter in the text field */
    AjaxButton hiddenSubmit() {
        return new AjaxButton("submit") {

            static final long serialVersionUID = 5334592790005438960L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateFilter(target, filter.getDefaultModelObjectAsString());
                rememeberFilter();
            }
        };
    }

    /** The hidden button that will submit the form when the user presses enter in the text field */
    AjaxLink getClearFilterLink(String previousInput) {
        AjaxLink clearButton =
                new AjaxLink("clear") {

                    static final long serialVersionUID = 5334592790005438960L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        updateFilter(target, "");
                        filter.setModelObject("");
                        rememeberFilter();
                        clearFilter.setVisible(false);
                        target.add(filterForm);
                    }
                };
        // decide if it should be visible
        // null and empty checks
        boolean visible = false;
        if (previousInput != null) visible = !previousInput.isEmpty();

        clearButton.setVisible(visible);
        return clearButton;
    }

    /**
     * Number of visible items per page, should the default {@link #DEFAULT_ITEMS_PER_PAGE} not
     * satisfy the programmer needs. Calling this will wipe out the selection
     */
    public void setItemsPerPage(int items) {
        dataView.setItemsPerPage(items);
        selection = new boolean[items];
    }

    /**
     * Enables/disables filtering for this table. When no filtering is enabled, the top form with
     * the top pager and the search box will disappear. Returns self for chaining.
     */
    public GeoServerTablePanel<T> setFilterable(boolean filterable) {
        filterForm.setVisible(filterable);
        return this;
    }

    /**
     * Builds a sort link that will force sorting on a certain column, and flip it to the other
     * direction when clicked again
     */
    <S> AjaxLink<S> sortLink(final GeoServerDataProvider<T> dataProvider, ListItem<S> item) {
        return new AjaxLink<S>("link", item.getModel()) {

            private static final long serialVersionUID = -6180419488076488737L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SortParam<?> currSort = dataProvider.getSort();
                @SuppressWarnings("unchecked")
                Property<T> property = (Property<T>) getModelObject();
                if (currSort == null || !property.getName().equals(currSort.getProperty())) {
                    dataProvider.setSort(new SortParam<Object>(property.getName(), true));
                } else {
                    dataProvider.setSort(
                            new SortParam<Object>(property.getName(), !currSort.isAscending()));
                }
                setSelection(false);
                target.add(listContainer);
                rememeberSort();
            }
        };
    }

    /**
     * Parses the keywords and sets them into the data provider, forces update of the components
     * that need to as a result of the different filtering
     */
    private void updateFilter(AjaxRequestTarget target, String flatKeywords) {
        if ("".equals(flatKeywords)) {
            dataProvider.setKeywords(null);
            filter.setModelObject("");
            dataView.setCurrentPage(0);
        } else {
            String[] keywords = flatKeywords.split("\\s+");
            dataProvider.setKeywords(keywords);
            dataView.setCurrentPage(0);
        }
        pagerDelegate.updateMatched();
        navigatorTop.updateMatched();
        navigatorBottom.updateMatched();
        setSelection(false);
        clearFilter.setVisible(true);

        target.add(listContainer);
        target.add(navigatorTop);
        target.add(navigatorBottom);
        target.add(filterForm);
    }

    /** Sets back to the first page, clears the selection and */
    public void reset() {
        dataView.setCurrentPage(0);
        clearSelection();
        dataProvider.setSort(null);
    }

    /** Turns filtering abilities on/off. */
    public void setFilterVisible(boolean filterVisible) {
        filterForm.setVisible(filterVisible);
    }

    public void processInputs() {
        this.visitChildren(
                FormComponent.class,
                (component, visit) -> {
                    ((FormComponent<?>) component).processInput();
                    visit.dontGoDeeper();
                });
    }

    /**
     * Returns the component that will represent a property of a table item. Usually it should be a
     * label, or a link, but you can return pretty much everything. The subclass can also return
     * null, in that case a label will be created
     */
    protected abstract Component getComponentForProperty(
            String id, IModel<T> itemModel, Property<T> property);

    /**
     * Called each time a new table item/column is created.
     *
     * <p>By default this method does nothing, subclasses may override for instance to add an
     * attribute to the &lt;td&gt; element created for the column.
     */
    protected void onPopulateItem(Property<T> property, ListItem<Property<T>> item) {}

    IModel<String> showingAllRecords(long first, long last, long size) {
        return new ParamResourceModel("showingAllRecords", this, first, last, size);
    }

    IModel<String> matchedXOutOfY(long first, long last, long size, long fullSize) {
        return new ParamResourceModel("matchedXOutOfY", this, first, last, size, fullSize);
    }

    protected class PagerDelegate implements Serializable {
        private static final long serialVersionUID = -6928477338531850338L;
        long fullSize, size, first, last;

        public PagerDelegate() {
            updateMatched();
        }

        /** Updates the label given the current page and filtering status */
        void updateMatched() {
            size = dataProvider.size();
            fullSize = dataProvider.fullSize();
            first = first(size);
            last = last(size);
        }

        public IModel<String> model() {
            if (dataProvider.getKeywords() == null) {
                return showingAllRecords(first, last, fullSize);
            } else {
                return matchedXOutOfY(first, last, size, fullSize);
            }
        }

        /**
         * User oriented index of the first item in the current page
         *
         * @param size The total number of items matched by the current filter
         */
        long first(long size) {
            if (dataProvider.getKeywords() != null) {
                size = dataView.getDataProvider().size();
            }
            if (size > 0) return dataView.getItemsPerPage() * dataView.getCurrentPage() + 1;
            else return 0;
        }

        /**
         * User oriented index of the last item in the current page
         *
         * @param size The total number of items matched by the current filter
         */
        long last(long size) {

            long count = optGetPageCount(size);
            long page = dataView.getCurrentPage();
            if (page < (count - 1)) return dataView.getItemsPerPage() * (page + 1);
            else {
                return dataProvider.getKeywords() != null
                        ? dataView.getDataProvider().size()
                        : size;
            }
        }

        long optGetPageCount(long total) {
            long page = dataView.getItemsPerPage();
            long count = total / page;

            if (page * count < total) {
                count++;
            }

            return count;
        }
    }
    /**
     * The two pages in the table panel. Includes a paging navigator and a status label telling the
     * user what she is seeing
     */
    protected class Pager extends Panel {

        private static final long serialVersionUID = 6128188748404971154L;

        GeoServerPagingNavigator navigator;

        Label matched;

        Pager(String id) {
            super(id);

            add(navigator = updatingPagingNavigator());
            add(matched = new Label("filterMatch", new Model<String>()));
            updateMatched();
        }

        /** Builds a paging navigator that will update both of the labels when the page changes. */
        private GeoServerPagingNavigator updatingPagingNavigator() {
            return new GeoServerPagingNavigator("navigator", dataView) {
                private static final long serialVersionUID = -1795278469204272385L;

                @Override
                protected void onAjaxEvent(AjaxRequestTarget target) {
                    super.onAjaxEvent(target);
                    setSelection(false);
                    pagerDelegate.updateMatched();
                    navigatorTop.updateMatched();
                    navigatorBottom.updateMatched();
                    target.add(navigatorTop);
                    target.add(navigatorBottom);
                }
            };
        }

        /** Updates the label given the current page and filtering status */
        void updateMatched() {
            matched.setDefaultModel(pagerDelegate.model());
        }
    }

    public class SelectionModel implements IModel<Boolean> {
        private static final long serialVersionUID = 7681891298556441330L;
        int index;

        public SelectionModel(int index) {
            this.index = index;
            validateSelectionIndex(index);
        }

        public Boolean getObject() {
            return selection[index];
        }

        public void setObject(Boolean object) {
            selection[index] = object.booleanValue();
        }

        public void detach() {
            // nothing to do
        }
    }

    /**
     * Sets the table into pageable/non pageable mode. The default is pageable, in non pageable mode
     * both pagers will be hidden and the number of items per page is set to the DataView default
     * (Integer.MAX_VALUE)
     */
    public void setPageable(boolean pageable) {
        if (!pageable) {
            navigatorTop.setVisible(false);
            navigatorBottom.setVisible(false);
            dataView.setItemsPerPage(Integer.MAX_VALUE);
            selection = new boolean[getDataProvider().getItems().size()];
        } else {
            navigatorTop.setVisible(true);
            navigatorBottom.setVisible(true);
            dataView.setItemsPerPage(DEFAULT_ITEMS_PER_PAGE);
            selection = new boolean[DEFAULT_ITEMS_PER_PAGE];
        }
    }

    private void rememeberFilter() {
        MetadataMap filters = getMetaDataMapFromSession(FILTER_INPUTS);
        // if empty ignore and clear any previously saved filter against this dataprovider
        if (filter.getDefaultModelObjectAsString().isEmpty() && filters != null) {
            // clear
            filters.put(dataProvider.getClass().getName(), null);
            return;
        }
        // create and populate user session
        if (filters == null) {
            filters = new MetadataMap();
            getSession().setAttribute(FILTER_INPUTS, filters);
        }

        filters.put(dataProvider.getClass().getName(), filter.getDefaultModelObjectAsString());
    }

    private void rememeberSort() {
        MetadataMap sorts = getMetaDataMapFromSession(SORT_INPUTS);

        // create and populate user session
        if (sorts == null) {
            sorts = new MetadataMap();
            getSession().setAttribute(SORT_INPUTS, sorts);
        }

        sorts.put(dataProvider.getClass().getName(), dataProvider.getSort());
    }

    private String loadPreviousInput() {
        MetadataMap filters = getMetaDataMapFromSession(FILTER_INPUTS);
        if (filters == null) return null;
        else if (filters.get(dataProvider.getClass().getName(), String.class) == null) return null;
        return filters.get(dataProvider.getClass().getName(), String.class);
    }

    private SortParam<Object> loadPreviousSort() {
        MetadataMap filters = getMetaDataMapFromSession(SORT_INPUTS);
        if (filters == null) return null;
        else if (filters.get(dataProvider.getClass().getName(), SortParam.class) == null)
            return null;
        return filters.get(dataProvider.getClass().getName(), SortParam.class);
    }

    private void clearFilterFromSession() {
        MetadataMap filters = getMetaDataMapFromSession(FILTER_INPUTS);
        // if empty ignore and clear any previously saved filter against this dataprovider
        if (filters != null) filters.put(dataProvider.getClass().getName(), null);
        dataProvider.setKeywords(null);
    }

    private void clearSortFromSession() {
        MetadataMap sorts = getMetaDataMapFromSession(SORT_INPUTS);
        // if empty ignore and clear any previously saved filter against this dataprovider
        if (sorts != null) sorts.put(dataProvider.getClass().getName(), null);
        dataProvider.setKeywords(null);
    }

    private MetadataMap getMetaDataMapFromSession(String key) {
        return (MetadataMap) getSession().getAttribute(key);
    }
}
