package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.impl.TJSCatalogFactoryImpl;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/8/12
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TJSStore extends StoreInfoImpl implements DataStoreInfo, Serializable {

    transient TJS_1_0_0_DataStore store;
    transient Catalog catalog;
    String id;
    String type;

    public TJSStore(TJS_1_0_0_DataStore store, Catalog catalog) {
        this.store = store;
        this.catalog = catalog;
        this.id = TJSCatalogFactoryImpl.getIdForObject(this);
        this.type = this.getType();
    }

    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(ProgressListener listener) throws IOException {
        return store;
    }

    public TJS_1_0_0_DataStore getStore() {
        return store;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public String getName() {
        // Thijs: trying to avoid NPEs when reloading the config.xml
        if (store != null) {
            return store.getFrameworkInfo().getName();
        }   else {
            return null;
        }
    }

    public void setName(String name) {
        // this.name = name;
    }

    public String getDescription() {
        // Thijs: trying to avoid NPEs when reloading the config.xml
        if (store != null) {
            return store.getFrameworkInfo().getDescription();
        }   else {
            return null;
        }
    }

    public void setDescription(String description) {
        //To change body of implemented methods use File | Settings | File Templates.
        // TODO: how to deal with this, since it is not about the framework?
        store.getFrameworkInfo().setDescription(description);
    }

    public String getType() {
        // TODO: make this a configurable name, constant
        return "TJSJoinedData";
    }

    public void setType(String type) {
        // TODO: implement this?
    }

    public MetadataMap getMetadata() {
        return null;
    }

    public boolean isEnabled() {
        // Thijs: trying to avoid NPEs when reloading the config.xml
        if (store != null) {
            return store.getFrameworkInfo().getEnabled();
        }   else {
            return false;
        }
    }

    public void setEnabled(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
        store.getFrameworkInfo().setEnabled(enabled);
    }

    // TODO: the try/catch is a workaround to avoid an NPE when reloading he confog.
    // TODO: refactor this, to make sure the workspaceinfo could be found
    public WorkspaceInfo getWorkspace() {
        WorkspaceInfo wi = null;
        try {
            wi = catalog.getWorkspaceByName(TJSExtension.TJS_TEMP_WORKSPACE) ;
        }   catch (Exception e) {

        }
        return wi;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Serializable> getConnectionParameters() {
        // TODO: workaround to provide some params?
        Map<String, Serializable> params = new HashMap<String, Serializable>();

        // Thijs: workaround to try to get around the exceptions when reloading the config
        try {
            params.put("FrameworkId", store.getFrameworkInfo().getId());
        } catch (Exception e) {

        }
        // params.put("FrameworkId", store.getFrameworkInfo().getId());
        return params;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Throwable getError() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setError(Throwable t) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void accept(CatalogVisitor visitor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getId() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
