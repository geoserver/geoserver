/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.columns;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author root
 */
public class ColumnsProvider extends GeoServerDataProvider<ColumnInfo> {

    DatasetInfo datasetInfo;

    public static final Property<ColumnInfo> NAME = new BeanProperty<ColumnInfo>("name", "name");
    public static final Property<ColumnInfo> TITLE = new BeanProperty<ColumnInfo>("title", "title");
    public static final Property<ColumnInfo> TYPE = new BeanProperty<ColumnInfo>("type", "type");

    final List<Property<ColumnInfo>> PROPERTIES = Arrays.asList(NAME, TITLE, TYPE);

    public ColumnsProvider(DatasetInfo datasetInfo) {
        super();
        this.datasetInfo = datasetInfo;
    }

    @Override
    protected List<Property<ColumnInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<ColumnInfo> getItems() {
        return datasetInfo.getColumns();
    }

}
