/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

import gmx.iderc.geoserver.tjs.data.TJSDatasource;

import java.util.Date;
import java.util.List;

/**
 * @author root
 */
public interface DatasetInfo extends TJSCatalogObject {

    FrameworkInfo getFramework();

    void setFramework(FrameworkInfo frameworkInfo);

    DataStoreInfo getDataStore();

    void setDataStore(DataStoreInfo dataStoreInfo);

    String getDatasetUri();

    String getDatasetName();

    void setDatasetName(String datasetName);

    String getGeoKeyField();

    void setGeoKeyField(String geoKeyField);

    String getOrganization();

    void setOrganization(String organization);

    Date getReferenceDate();

    void setReferenceDate(Date refDate);

    String getVersion();

    void setVersion(String version);

    String getDocumentation();

    void setDocumentation(String documentation);

    TJSDatasource getTJSDatasource();

    List<ColumnInfo> getColumns();

    ColumnInfo getColumn(String name);

    String getDefaultStyle();

    void setDefaultStyle(String styleName);

    boolean getAutoJoin();

    void setAutoJoin(boolean autoJoin);

}
