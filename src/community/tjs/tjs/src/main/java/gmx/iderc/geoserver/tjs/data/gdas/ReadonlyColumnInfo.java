package gmx.iderc.geoserver.tjs.data.gdas;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 30/07/13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
public abstract class ReadonlyColumnInfo implements ColumnInfo {

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setType(String type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setLength(int length) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDecimals(int decimals) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setTitle(String title) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAbstract(String abstractValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDocumentation(String documentation) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setValueUOM(String valueUOM) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setPurpose(String purpose) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSQLClassBinding(Class sqlClassBinding) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
