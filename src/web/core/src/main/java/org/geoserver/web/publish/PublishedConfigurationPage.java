/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.resource.LayerModel;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;

/**
 * Page allowing to configure a layer(group) (and its resource).
 *
 * <p>The page is completely pluggable, the UI will be made up by scanning the Spring context for
 * implementations of {@link ResourceConfigurationPanel} and {@link PublishedConfigurationPanel}.
 *
 * <p>WARNING: one crucial aspect of this page is its ability to not loose edits when one switches
 * from one tab to the other. I did not find any effective way to unit test this, so _please_, if
 * you do modify anything in this class (especially the models), manually retest that the edits are
 * not lost on tab switch.
 *
 * @author Niels Charlier
 */
public abstract class PublishedConfigurationPage<T extends PublishedInfo>
        extends GeoServerSecuredPage {

    private static final long serialVersionUID = 7870938096047218989L;

    public static final String NAME = "name";

    public static final String WORKSPACE = "wsName";

    protected IModel<T> myModel;

    protected boolean isNew;

    protected TabbedPanel tabbedPanel;

    /**
     * {@link PublishedEditTabPanel} contributions may need to edit something different than the
     * LayerInfo and ResourceInfo this page holds models to. In such cases {@link
     * PublishedEditTabPanelInfo#createOwnModel} will return a non null model and well pass it back
     * to the concrete LayerEditTabPanel constructor. This is so because LayerEditTabPanel are
     * re-created everytime the user switches tabs.
     */
    private LinkedHashMap<Class<? extends PublishedEditTabPanel<T>>, IModel<?>>
            tabPanelCustomModels;

    private boolean inputEnabled = true;

    protected PublishedConfigurationPage(boolean isNew) {
        this.isNew = isNew;
    }

    protected PublishedConfigurationPage(T info, boolean isNew) {
        this.isNew = isNew;
        setupPublished(info);
    }

    @SuppressWarnings("unchecked")
    protected void setupPublished(T info) {
        setupPublished(
                info instanceof LayerInfo
                        ? (IModel<T>) new LayerModel((LayerInfo) info)
                        : new Model<T>(info));
    }

    protected void setupPublished(IModel<T> infoModel) {
        myModel = new CompoundPropertyModel<T>(infoModel);
        initComponents();
    }

    /** */
    @SuppressWarnings("rawtypes")
    private void initComponents() {
        this.tabPanelCustomModels =
                new LinkedHashMap<Class<? extends PublishedEditTabPanel<T>>, IModel<?>>();

        add(new Label("publishedinfoname", getPublishedInfo().prefixedName()));
        Form<T> theForm = new Form<T>("publishedinfo", myModel);
        add(theForm);

        List<ITab> tabs = new ArrayList<ITab>();

        // add the "well known" tabs
        tabs.add(
                new AbstractTab(
                        new org.apache.wicket.model.ResourceModel(
                                "ResourceConfigurationPage.Data")) {
                    private static final long serialVersionUID = 1L;

                    public Panel getPanel(String panelID) {
                        return createMainTab(panelID).setInputEnabled(inputEnabled);
                    }
                });

        tabs.add(
                new AbstractTab(
                        new org.apache.wicket.model.ResourceModel(
                                "ResourceConfigurationPage.Publishing")) {
                    private static final long serialVersionUID = 1L;

                    public Panel getPanel(String panelID) {
                        return new PublishingEditTabPanel(panelID).setInputEnabled(inputEnabled);
                    }
                });

        // add the tabs contributed via extension point
        List<PublishedEditTabPanelInfo> tabPanels =
                getGeoServerApplication().getBeansOfType(PublishedEditTabPanelInfo.class);

        // sort the tabs based on order
        Collections.sort(
                tabPanels,
                new Comparator<PublishedEditTabPanelInfo>() {
                    public int compare(PublishedEditTabPanelInfo o1, PublishedEditTabPanelInfo o2) {
                        Integer order1 = o1.getOrder() >= 0 ? o1.getOrder() : Integer.MAX_VALUE;
                        Integer order2 = o2.getOrder() >= 0 ? o2.getOrder() : Integer.MAX_VALUE;

                        return order1.compareTo(order2);
                    }
                });

        for (PublishedEditTabPanelInfo ttabPanelInfo : tabPanels) {
            if (ttabPanelInfo.supports(getPublishedInfo())) {

                @SuppressWarnings("unchecked")
                PublishedEditTabPanelInfo<T> tabPanelInfo =
                        (PublishedEditTabPanelInfo<T>) ttabPanelInfo;

                String titleKey = tabPanelInfo.getTitleKey();
                IModel<String> titleModel = null;
                if (titleKey != null) {
                    titleModel = new org.apache.wicket.model.ResourceModel(titleKey);
                } else {
                    titleModel =
                            new Model<String>(tabPanelInfo.getComponentClass().getSimpleName());
                }

                final Class<PublishedEditTabPanel<T>> panelClass = tabPanelInfo.getComponentClass();
                IModel<?> panelCustomModel = tabPanelInfo.createOwnModel(myModel, isNew);
                tabPanelCustomModels.put(panelClass, panelCustomModel);

                tabs.add(
                        new AbstractTab(titleModel) {
                            private static final long serialVersionUID = -6637277497986497791L;
                            private final Class<PublishedEditTabPanel<T>> panelType = panelClass;

                            @Override
                            public Panel getPanel(String panelId) {
                                PublishedEditTabPanel<?> tabPanel;
                                final IModel<?> panelCustomModel =
                                        tabPanelCustomModels.get(panelType);
                                try {
                                    // if this tab needs a custom model instead of just our layer
                                    // model, then
                                    // let it create it once
                                    if (panelCustomModel == null) {
                                        tabPanel =
                                                panelClass
                                                        .getConstructor(String.class, IModel.class)
                                                        .newInstance(panelId, myModel);
                                    } else {
                                        tabPanel =
                                                panelClass
                                                        .getConstructor(
                                                                String.class,
                                                                IModel.class,
                                                                IModel.class)
                                                        .newInstance(
                                                                panelId, myModel, panelCustomModel);
                                    }
                                } catch (Exception e) {
                                    throw new WicketRuntimeException(e);
                                    // LOGGER.log(Level.WARNING, "Error creating resource panel",
                                    // e);
                                }
                                return tabPanel;
                            }
                        });
            }
        }

        // we need to override with submit links so that the various form
        // element
        // will validate and write down into their
        tabbedPanel =
                new TabbedPanel("tabs", tabs) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer newLink(String linkId, final int index) {
                        return new SubmitLink(linkId) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onSubmit() {
                                setSelectedTab(index);
                            }
                        };
                    }
                };
        theForm.add(tabbedPanel);
        theForm.add(saveLink());
        theForm.add(cancelLink());
    }

    protected abstract PublishedEditTabPanel<T> createMainTab(String panelID);

    protected abstract void doSaveInternal() throws IOException;

    public void setSelectedTab(Class<? extends PublishedEditTabPanel<?>> selectedTabClass) {
        int selectedTabIndex;
        // relying on LinkedHashMap here
        selectedTabIndex =
                new ArrayList<Class<? extends PublishedEditTabPanel<T>>>(
                                tabPanelCustomModels.keySet())
                        .indexOf(selectedTabClass);
        if (selectedTabIndex > -1) {
            tabbedPanel.setSelectedTab(selectedTabIndex);
        }
    }

    public void selectDataTab() {
        tabbedPanel.setSelectedTab(0);
    }

    public void selectPublishingTab() {
        tabbedPanel.setSelectedTab(1);
    }

    protected void disableForm() {
        get("publishedinfo").setEnabled(false);
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            private static final long serialVersionUID = 1839992481355433705L;

            @Override
            public void onSubmit() {
                doSave();
            }
        };
    }

    /**
     * Performs the necessary operation on save.
     *
     * <p>This implementation adds the necessary objects to the catalog, respecting the isNew flag,
     * and calls {@link #onSuccessfulSave()} upon success.
     */
    protected void doSave() {
        try {
            doSaveInternal();
            if (hasErrorMessage()) {
                return;
            }

            for (Entry<Class<? extends PublishedEditTabPanel<T>>, IModel<?>> e :
                    tabPanelCustomModels.entrySet()) {
                Class<? extends PublishedEditTabPanel<T>> panelClass = e.getKey();
                IModel<?> customModel = e.getValue();
                if (customModel == null) {
                    continue;
                }
                PublishedEditTabPanel<?> tabPanel =
                        panelClass
                                .getConstructor(String.class, IModel.class, IModel.class)
                                .newInstance("temp", myModel, customModel);
                tabPanel.save();
            }

            onSuccessfulSave();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error saving layer", e);
            error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private Link<?> cancelLink() {
        return new Link("cancel") {
            private static final long serialVersionUID = -9007727127569731882L;

            @Override
            public void onClick() {
                onCancel();
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<PublishedConfigurationPanelInfo<T>> filterPublishedPanels(
            List<PublishedConfigurationPanelInfo> list) {
        List<PublishedConfigurationPanelInfo<T>> result =
                new ArrayList<PublishedConfigurationPanelInfo<T>>();
        for (PublishedConfigurationPanelInfo info : list) {
            if (info.canHandle(getPublishedInfo())) {
                result.add((PublishedConfigurationPanelInfo<T>) info);
            }
        }
        return result;
    }

    /** Returns the {@link PublishedInfo} contained in this page */
    public T getPublishedInfo() {
        return (T) myModel.getObject();
    }

    /** By default brings back the user to LayerPage, subclasses can override this behavior */
    protected void onSuccessfulSave() {
        doReturn();
    }

    /** By default brings back the user to LayerPage, subclasses can override this behavior */
    protected void onCancel() {
        doReturn();
    }
    /**
     * Allows collaborating pages to update the published info object
     *
     * @param info
     */
    public void updatePublishedInfo(T info) {
        myModel.setObject(info);
    }

    protected abstract class ListEditTabPanel extends PublishedEditTabPanel<T> {

        private static final long serialVersionUID = -7279044666531992361L;

        public ListEditTabPanel(String id) {
            super(id, myModel);

            ListView<?> list = createList("theList");

            // do this or die on validation (the form element contents will
            // reset, the edit will be lost)
            list.setReuseItems(true);
            add(list);
        }

        protected abstract ListView<?> createList(String id);
    }

    protected class PublishingEditTabPanel extends ListEditTabPanel {
        private static final long serialVersionUID = -6575960326680386479L;

        public PublishingEditTabPanel(String id) {
            super(id);
        }

        @Override
        public ListView<PublishedConfigurationPanelInfo<T>> createList(String id) {
            List<PublishedConfigurationPanelInfo<T>> pubPanels =
                    filterPublishedPanels(
                            getGeoServerApplication()
                                    .getBeansOfType(PublishedConfigurationPanelInfo.class));
            ListView<PublishedConfigurationPanelInfo<T>> pubPanelList =
                    new ListView<PublishedConfigurationPanelInfo<T>>(id, pubPanels) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(
                                ListItem<PublishedConfigurationPanelInfo<T>> item) {
                            PublishedConfigurationPanelInfo<T> panelInfo =
                                    (PublishedConfigurationPanelInfo<T>) item.getModelObject();
                            try {
                                PublishedConfigurationPanel<T> panel =
                                        panelInfo
                                                .getComponentClass()
                                                .getConstructor(String.class, IModel.class)
                                                .newInstance("content", myModel);
                                item.add((Component) panel);
                            } catch (Exception e) {
                                throw new WicketRuntimeException(
                                        "Failed to add pluggable layer configuration panels", e);
                            }
                        }
                    };
            return pubPanelList;
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    public void setInputEnabled(boolean inputEnabled) {
        this.inputEnabled = inputEnabled;
        get("publishedinfo:save").setVisible(inputEnabled);
    }
}
