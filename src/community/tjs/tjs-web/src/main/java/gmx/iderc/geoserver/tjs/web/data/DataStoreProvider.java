/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author root
 */
public class DataStoreProvider extends GeoServerDataProvider<DataStoreInfo> {

    static final Property<DataStoreInfo> NAME = new BeanProperty<DataStoreInfo>("name", "name");

    static final Property<DataStoreInfo> TYPE = new BeanProperty<DataStoreInfo>("type",
                                                                                       "type");

    static final Property<DataStoreInfo> ENABLED = new AbstractProperty<DataStoreInfo>("enabled") {

        public Boolean getPropertyValue(DataStoreInfo item) {
            return Boolean.valueOf(item.getEnabled());
        }

    };

    final List<Property<DataStoreInfo>> PROPERTIES = Arrays.asList(NAME, TYPE, ENABLED);

    @Override
    protected List<Property<DataStoreInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<DataStoreInfo> getItems() {
        return TJSExtension.getTJSCatalog().getDataStores();
    }

}
