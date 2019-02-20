/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataAccessFactory;
import org.h2.store.DataPage;
import org.opengis.coverage.grid.Format;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Page that presents a list of vector and raster store types available in the classpath in order to
 * choose what kind of data source to create, as well as which workspace to create the store in.
 *
 * <p>Meant to be called by {@link DataPage} when about to add a new datastore or coverage.
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class NewDataPage extends GeoServerSecuredPage {

    // do not access directly, it is transient and the instance can be the de-serialized version
    private transient Map<String, DataAccessFactory> dataStores = getAvailableDataStores();

    // do not access directly, it is transient and the instance can be the de-serialized version
    private transient Map<String, Format> coverages = getAvailableCoverageStores();

    /**
     * Creates the page components to present the list of available vector and raster data source
     * types
     */
    public NewDataPage() {

        final boolean thereAreWorkspaces = !getCatalog().getWorkspaces().isEmpty();

        if (!thereAreWorkspaces) {
            super.error(
                    (String) new ResourceModel("NewDataPage.noWorkspacesErrorMessage").getObject());
        }

        final Form storeForm = new Form("storeForm");
        add(storeForm);

        final ArrayList<String> sortedDsNames =
                new ArrayList<String>(getAvailableDataStores().keySet());
        Collections.sort(sortedDsNames);

        final CatalogIconFactory icons = CatalogIconFactory.get();
        final ListView dataStoreLinks =
                new ListView("vectorResources", sortedDsNames) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final String dataStoreFactoryName = item.getDefaultModelObjectAsString();
                        final DataAccessFactory factory =
                                getAvailableDataStores().get(dataStoreFactoryName);
                        final String description = factory.getDescription();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(
                                                new DataAccessNewPage(dataStoreFactoryName));
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(new Label("resourcelabel", dataStoreFactoryName));
                        item.add(link);
                        item.add(new Label("resourceDescription", description));
                        Image icon = new Image("storeIcon", icons.getStoreIcon(factory.getClass()));
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model("")));
                        item.add(icon);
                    }
                };

        final List<String> sortedCoverageNames = new ArrayList<String>();
        sortedCoverageNames.addAll(getAvailableCoverageStores().keySet());
        Collections.sort(sortedCoverageNames);

        final ListView coverageLinks =
                new ListView("rasterResources", sortedCoverageNames) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final String coverageFactoryName = item.getDefaultModelObjectAsString();
                        final Map<String, Format> coverages = getAvailableCoverageStores();
                        Format format = coverages.get(coverageFactoryName);
                        final String description = format.getDescription();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(
                                                new CoverageStoreNewPage(coverageFactoryName));
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(new Label("resourcelabel", coverageFactoryName));
                        item.add(link);
                        item.add(new Label("resourceDescription", description));
                        Image icon = new Image("storeIcon", icons.getStoreIcon(format.getClass()));
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model("")));
                        item.add(icon);
                    }
                };

        final List<OtherStoreDescription> otherStores = getOtherStores();

        final ListView otherStoresLinks =
                new ListView("otherStores", otherStores) {
                    @Override
                    protected void populateItem(ListItem item) {
                        final OtherStoreDescription store =
                                (OtherStoreDescription) item.getModelObject();
                        SubmitLink link;
                        link =
                                new SubmitLink("resourcelink") {
                                    @Override
                                    public void onSubmit() {
                                        setResponsePage(store.configurationPage);
                                    }
                                };
                        link.setEnabled(thereAreWorkspaces);
                        link.add(
                                new Label(
                                        "resourcelabel",
                                        new ParamResourceModel(
                                                "other." + store.key, NewDataPage.this)));
                        item.add(link);
                        item.add(
                                new Label(
                                        "resourceDescription",
                                        new ParamResourceModel(
                                                "other." + store.key + ".description",
                                                NewDataPage.this)));
                        Image icon = new Image("storeIcon", store.icon);
                        // TODO: icons could provide a description too to be used in alt=...
                        icon.add(new AttributeModifier("alt", new Model("")));
                        item.add(icon);
                    }
                };

        storeForm.add(dataStoreLinks);
        storeForm.add(coverageLinks);
        storeForm.add(otherStoresLinks);
    }

    /** @return the name/description set of available datastore factories */
    private Map<String, DataAccessFactory> getAvailableDataStores() {
        // dataStores is transient, a back button may get us to the serialized version so check for
        // it
        if (dataStores == null) {
            final Iterator<DataAccessFactory> availableDataStores;
            availableDataStores = DataStoreUtils.getAvailableDataStoreFactories().iterator();

            Map<String, DataAccessFactory> storeNames = new HashMap<String, DataAccessFactory>();

            while (availableDataStores.hasNext()) {
                DataAccessFactory factory = availableDataStores.next();
                if (factory.getDisplayName() != null) {
                    storeNames.put(factory.getDisplayName(), factory);
                }
            }
            dataStores = storeNames;
        }
        return dataStores;
    }

    /** @return the name/description set of available raster formats */
    private Map<String, Format> getAvailableCoverageStores() {
        if (coverages == null) {
            Format[] availableFormats = GridFormatFinder.getFormatArray();
            Map<String, Format> formatNames = new HashMap<String, Format>();
            for (Format format : availableFormats) {
                formatNames.put(format.getName(), format);
            }
            coverages = formatNames;
        }
        return coverages;
    }

    private List<OtherStoreDescription> getOtherStores() {
        List<OtherStoreDescription> stores = new ArrayList<OtherStoreDescription>();
        PackageResourceReference wmsIcon =
                new PackageResourceReference(
                        GeoServerApplication.class, "img/icons/geosilk/server_map.png");
        stores.add(new OtherStoreDescription("wms", wmsIcon, WMSStoreNewPage.class));
        stores.add(new OtherStoreDescription("wmts", wmsIcon, WMTSStoreNewPage.class));

        return stores;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /** Provides a description for a store that is not a vector nor a raster data source */
    static class OtherStoreDescription implements Serializable {
        String key;

        PackageResourceReference icon;

        Class<? extends Page> configurationPage;

        public OtherStoreDescription(
                String key,
                PackageResourceReference icon,
                Class<? extends Page> configurationPage) {
            super();
            this.key = key;
            this.icon = icon;
            this.configurationPage = configurationPage;
        }
    }
}
