/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.*;

/**
 * Provides a list of resources for a specific data store
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class NewDatasetPageProvider extends GeoServerDataProvider<DatasetInfo> {

    public static final Property<DatasetInfo> NAME = new BeanProperty<DatasetInfo>("name", "name");
    public static final Property<DatasetInfo> TITLE = new BeanProperty<DatasetInfo>("description", "description");
    public static final Property<DatasetInfo> ORGANIZATION = new BeanProperty<DatasetInfo>("organization", "organization");
    public static final Property<DatasetInfo> REF_DATE = new BeanProperty<DatasetInfo>("referenceDate", "referenceDate");
    public static final Property<DatasetInfo> VERSION = new BeanProperty<DatasetInfo>("version", "version");
    public static final Property<DatasetInfo> ENABLED = new BeanProperty<DatasetInfo>("enabled", "enabled");

    public static final List<Property<DatasetInfo>> PROPERTIES = Arrays.asList(NAME, TITLE, ORGANIZATION, REF_DATE, VERSION, ENABLED);

    boolean showPublished;
    List<String> dsNames;

    String dataStoreId;

    protected TJSCatalog getTJSCatalog() {
        return TJSExtension.getTJSCatalog();
    }

    @Override
    protected List<DatasetInfo> getItems() {
        // return an empty list in case we still don't know about the store
        if (dataStoreId == null)
            return new ArrayList<DatasetInfo>();

        // else, grab the resource list
        try {
            List<DatasetInfo> result = getTJSCatalog().getDatasets(dataStoreId);
            Collections.sort(result, new DatasetInfoComparator());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not list Datasources for this store, "
                                               + "an error occurred retrieving them: " + e.getMessage(), e);
        }

    }

    class DatasetInfoComparator implements Comparator<DatasetInfo> {

        public int compare(DatasetInfo ds1, DatasetInfo ds2) {
            return ds1.getName().compareToIgnoreCase(ds2.getName());
        }
    }

    public String getdataStoreId() {
        return dataStoreId;
    }

    public void setdataStoreId(String dataStoreId) {
        this.dataStoreId = dataStoreId;
    }

/*
    @Override
    protected List<DatasetInfo> getFilteredItems() {
        List<DatasetInfo> resources = super.getFilteredItems();
        if(showPublished)
            return resources;

        List<DatasetInfo> unconfigured = new ArrayList<DatasetInfo>();
        for (DatasetInfo resource : resources) {
            if(!resource.isPublished())
                unconfigured.add(resource);
        }
        return unconfigured;
    }
 */

    @Override
    protected List<Property<DatasetInfo>> getProperties() {
        return PROPERTIES;
    }

    public void setShowPublished(boolean showPublished) {
        this.showPublished = showPublished;
    }


}
