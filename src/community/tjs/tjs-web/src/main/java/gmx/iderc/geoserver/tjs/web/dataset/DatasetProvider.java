/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author root
 */
public class DatasetProvider extends GeoServerDataProvider<DatasetInfo> {

    static final Property<DatasetInfo> FRAMEWORK = new AbstractProperty<DatasetInfo>("framework") {

        public Object getPropertyValue(DatasetInfo item) {
            if (item.getFramework() != null) {
                return item.getFramework().getName();
            } else {
                return new String("");
            }
        }

    };

    static final Property<DatasetInfo> DATASTORE = new AbstractProperty<DatasetInfo>("dataStore") {

        public Object getPropertyValue(DatasetInfo item) {
            if (item.getDataStore() != null) {
                return item.getDataStore().getName();
            } else {
                return new String("");
            }
        }

    };

    public static final Property<DatasetInfo> NAME = new BeanProperty<DatasetInfo>("name", "name");

    //pa que tanta informacion?, Alvaro Javier Fuentes Suarez
    public static final Property<DatasetInfo> TITLE = new BeanProperty<DatasetInfo>("description", "description");
    public static final Property<DatasetInfo> ORGANIZATION = new BeanProperty<DatasetInfo>("organization", "organization");
    public static final Property<DatasetInfo> REF_DATE = new BeanProperty<DatasetInfo>("referenceDate", "referenceDate");
    public static final Property<DatasetInfo> VERSION = new BeanProperty<DatasetInfo>("version", "version");


    static final Property<DatasetInfo> ENABLED = new AbstractProperty<DatasetInfo>("enabled") {

        public Boolean getPropertyValue(DatasetInfo item) {
            return Boolean.valueOf(item.getEnabled());
        }

    };
    //pa que tanta iformacion?, Alvaro Javier Fuentes Suarez
    //final List<Property<DatasetInfo>> PROPERTIES = Arrays.asList(NAME, FRAMEWORK, TITLE, ORGANIZATION, REF_DATE, VERSION, DATASTORE, ENABLED);
    final List<Property<DatasetInfo>> PROPERTIES = Arrays.asList(NAME, FRAMEWORK, DATASTORE, ENABLED);

    @Override
    protected List<Property<DatasetInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<DatasetInfo> getItems() {
        return TJSExtension.getTJSCatalog().getDatasets(null);
    }

}
