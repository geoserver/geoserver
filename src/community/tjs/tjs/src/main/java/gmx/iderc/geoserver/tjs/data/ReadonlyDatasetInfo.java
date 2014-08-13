package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 29/07/13
 * Time: 11:25
 * To change this template use File | Settings | File Templates.
 */
public abstract class ReadonlyDatasetInfo implements DatasetInfo {

    transient protected TJSCatalog tjsCatalog;

    @Override
    public void setFramework(FrameworkInfo frameworkInfo) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setGeoKeyField(String geoKeyField) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDatasetName(String datasetName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setOrganization(String organization) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setReferenceDate(Date refDate) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setVersion(String version) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDocumentation(String documentation) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDefaultStyle(String styleName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAutoJoin(boolean autoJoin) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDescription(String description) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setEnabled(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setCatalog(TJSCatalog catalog) {
        this.tjsCatalog = catalog;
    }
}
