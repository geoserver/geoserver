package gmx.iderc.geoserver.tjs.catalog;

/**
 * Created with IntelliJ IDEA.
 * User: Alvaro Javier
 * Date: 3/27/13
 * Time: 12:16 a.m.
 */
public interface JoinedMapInfo extends TJSCatalogObject {

    //3 días de vida, en milisegundos
    public static final long DEFAULT_LIFE_TIME = 3 * 24 * 60 * 60 * 1000;

    String getGetDataURL();

    void setGetDataURL(String getDataURL);

    String getFrameworkURI();

    void setFrameworkURI(String frameworkURI);

    public boolean isUpdatable();

    public void setUpdatable(boolean updateable);

    public long getCreationTime();

    public void setCreationTime(long time);

    public long getLifeTime();

    public void setLifeTime(long time);

    public void setServerURL(String url);

    public String getServerURL();

    public void setDatasetUri(String uri);

    public String getDatasetUri();

}
