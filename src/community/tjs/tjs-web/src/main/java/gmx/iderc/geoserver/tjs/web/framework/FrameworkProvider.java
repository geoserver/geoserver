/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.framework;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author root
 */
public class FrameworkProvider extends GeoServerDataProvider<FrameworkInfo> {

    static final Property<FrameworkInfo> FEATURE_TYPE = new AbstractProperty<FrameworkInfo>("featureType") {

        @Override
        public String getName() {
            //TODO: localizar esto, Alvaro Javier Fuentes Suarez
            return "Feature type";
        }

        public Object getPropertyValue(FrameworkInfo item) {
            if (item.getFeatureType() != null) {
                return item.getFeatureType().getName();
            } else {
                return new String("");
            }
        }
    };

    static final Property<FrameworkInfo> NAME = new BeanProperty<FrameworkInfo>("name", "name");

    static final Property<FrameworkInfo> ENABLED = new AbstractProperty<FrameworkInfo>("enabled") {

        public Boolean getPropertyValue(FrameworkInfo item) {
            return Boolean.valueOf(item.getEnabled());
        }

    };

    final List<Property<FrameworkInfo>> PROPERTIES = Arrays.asList(NAME, FEATURE_TYPE, ENABLED);

    @Override
    protected List<Property<FrameworkInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<FrameworkInfo> getItems() {
        return TJSExtension.getTJSCatalog().getFrameworks();
    }

}
