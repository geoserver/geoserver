package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.JoinedMapInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogVisitor;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Alvaro Javier
 * Date: 3/27/13
 * Time: 12:20 a.m.
 * To change this template use File | Settings | File Templates.
 */
public class JoinedMapInfoImpl extends TJSCatalogObjectImpl implements JoinedMapInfo, Serializable {

    private String frameworkURI;
    private String getDataURL;
    private boolean updatable;
    private long creationTime;
    private long lifeTime;
    private String serverUrl;
    private String datasetUri;

    public JoinedMapInfoImpl(TJSCatalog catalog) {
        super(catalog);
        lifeTime = DEFAULT_LIFE_TIME;
        frameworkURI = "";
        getDataURL = "";
        updatable = false;
    }

    @Override
    public void loadDefault() {
        setId(TJSCatalogFactoryImpl.getIdForObject(this));
    }

    @Override
    public void accept(TJSCatalogVisitor visitor) {
        visitor.visit((JoinedMapInfo) this);
    }

    @Override
    public String getFrameworkURI() {
        return frameworkURI;
    }

    @Override
    public void setFrameworkURI(String frameworkURI) {
        this.frameworkURI = frameworkURI;
    }

    @Override
    public String getGetDataURL() {
        return getDataURL;
    }

    @Override
    public void setGetDataURL(String getDataURL) {
        this.getDataURL = getDataURL;
    }

    @Override
    public boolean isUpdatable() {
        return updatable;
    }

    @Override
    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void setCreationTime(long time) {
        creationTime = time;
    }

    @Override
    public long getLifeTime() {
        return lifeTime;
    }

    @Override
    public void setLifeTime(long time) {
        lifeTime = time;
    }

    @Override
    public void setServerURL(String url) {
        serverUrl = url;
    }

    @Override
    public String getServerURL() {
        return serverUrl;
    }

    @Override
    public void setDatasetUri(String uri) {
        datasetUri = uri;
    }

    @Override
    public String getDatasetUri() {
        return datasetUri;
    }

    @Override
    public String toString() {
        return "[FrameworkUri=" + frameworkURI + ",\ngetDataURL=" + getDataURL + ",\nupdatable=" + updatable + "]";
    }
}
