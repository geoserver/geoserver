package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.*;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: thijsb
 * Date: 3/18/14
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
// There seems to be some classes on TJSDataStores alreadye, but these are not for joined results it seems.
//  TODO: figure this out, but nod documentation.
// For now just use another name, since these other classes are DataStores with statistics only
// and this class is for JoinedData

public class TJSJoinedDataStoreFactory implements DataStoreFactorySpi {

    public static final Param FRAMEWORKID = new Param("FrameworkId", String.class,
            "FrameworkId of TJS Framework", true );

    public static final Param NAMESPACE = new Param("namespace", String.class,
            "namespace of datastore", false);

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        TJSCatalog tjsCatalog = TJSExtension.getTJSCatalog();

        // one of the params is the datastoreId, which should be stored / saved...
        String frameworkId = FRAMEWORKID.lookUp(params).toString();
        FrameworkInfo frameworkInfo =  tjsCatalog.getFramework(frameworkId)  ;
        FeatureTypeInfo featureTypeInfo = frameworkInfo.getFeatureType();
        DataStore featureDataStore = (DataStore) featureTypeInfo.getStore().getDataStore(null);

        TJS_1_0_0_DataStore newDataStore = new TJS_1_0_0_DataStore(tjsCatalog, featureDataStore, frameworkInfo);
        return newDataStore;
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException("A TJS Joined Data Store is only created automatically, through a TJS JoinedData operation ");
    }


    public String getDisplayName() {
        return "TJSJoinedData";  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getDescription() {
        return "TJSJoinedData";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[]{ FRAMEWORKID, NAMESPACE };  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
     public boolean canProcess(Map<String, Serializable> params) {
        boolean canProcess = false;
        try {
            TJSCatalog tjsCatalog = TJSExtension.getTJSCatalog();
            String frameworkId = FRAMEWORKID.lookUp(params).toString();
            FrameworkInfo frameworkInfo =  tjsCatalog.getFramework(frameworkId);

            if (frameworkInfo.getFeatureType() != null){
                canProcess = true;
            }
        } catch (NullPointerException ex) {
            canProcess = false;
        } catch (Exception ex) {
            canProcess = false;
        }
        return canProcess;
    }


    @Override
    public boolean isAvailable() {
        // TODO: need some implementation here?
        return true;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return null;
    }
}
