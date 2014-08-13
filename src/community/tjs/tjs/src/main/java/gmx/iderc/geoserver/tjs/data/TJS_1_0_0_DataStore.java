package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import org.geotools.data.*;
import org.geotools.data.store.ContentEntry;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.type.Name;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 9/22/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJS_1_0_0_DataStore extends AbstractDataStore {        // AbstractDataStore or ContentDataStore?
    // WFSDataStore wfsDataStore;
    DataStore featureDataStore;
    FrameworkInfo frameworkInfo;
    TJSCatalog catalog;
    HashMap<String, SimpleFeatureType> storeTypeNames = new HashMap<String, SimpleFeatureType>();

    // This datastore does not allow writing features
    protected boolean isWritable = false;

    public FrameworkInfo getFrameworkInfo() {
        return frameworkInfo;
    }

    public TJS_1_0_0_DataStore(TJSCatalog catalog, DataStore featureDataStore, FrameworkInfo frameworkInfo) {
        this.catalog = catalog;
        this.featureDataStore = featureDataStore;
        this.frameworkInfo = frameworkInfo;
    }

    public String[] getTypeNames() throws IOException {
        List<String> typeNames = new ArrayList<String>();
        if (catalog!=null && frameworkInfo != null) {
            List<DatasetInfo> datasets = catalog.getDatasetsByFramework(frameworkInfo.getId());
            for (DatasetInfo dataset : datasets) {
                typeNames.add(dataset.getName());
            }
        }
        String[] typeNamesArray = new String[typeNames.size()];
        typeNames.toArray(typeNamesArray);
        return typeNamesArray;
    }

    // interface method of AbstractDataStore
    public String[] getFeatureTypes() throws IOException {
          return this.getTypeNames();
    }

    public SimpleFeatureType getSchema(String typeName) {
        if (storeTypeNames.containsKey(typeName)) {
            return storeTypeNames.get(typeName);
        }

        if (frameworkInfo != null )    {
            String wfsTypeName = frameworkInfo.getFeatureType().getNativeName();
            SimpleFeatureType wfsFeatureType = null;
            try {
                wfsFeatureType = featureDataStore.getSchema(wfsTypeName);
            } catch (IOException ex) {

            }

            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
            featureTypeBuilder.setName(typeName);

            if (wfsFeatureType != null) {
                featureTypeBuilder.addAll(wfsFeatureType.getAttributeDescriptors());
            }
            DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);

            for (ColumnInfo column : datasetInfo.getColumns()) {
                Class attrClass = String.class;
                // TODO: this class mapping is a workaround to avoid cutting off Strings (because the SQL Class Binding seems to be java.lang.Character)
                if (column.getSQLClassBinding().equals( (Class)java.lang.Character.class ) ) {
                    attrClass = String.class;
                }   else {
                    // assume the rest of the classes and bindings is correct now..
                    attrClass = column.getSQLClassBinding();
                }
                featureTypeBuilder.add(column.getName(), attrClass);
            }
            // TODO: deal with the namespace. Set it in the current namespace? Is this needed?
            featureTypeBuilder.setNamespaceURI("");
            SimpleFeatureType newFt = featureTypeBuilder.buildFeatureType();
            storeTypeNames.put(typeName, newFt);
            return storeTypeNames.get(typeName);
        } else {
            // TODO: how to deal with this?
            return null;
        }
    }

    // getSchema for a Name
    public SimpleFeatureType getSchema(Name name) {
        // TODO: is local part okay? For now it works, not sure what happens if we have the same typenames, in different namespaces
        return getSchema(name.getLocalPart());
    }

    // or  create a feature source here? Dynamically or persist data in database and create a new datasource from that?
    @Override
    public TJSFeatureSource getFeatureSource(String typeName) {
        return new TJSFeatureSource(this, typeName);
    }

    // dummy method?
    protected FeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new TJSFeatureSource(this, entry.getTypeName());
    }


    // Thijs: was protected, but need it elsewhere.
    // Is this appropriate?  Should this be implemented somewhere else?
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        String wfsTypeName = frameworkInfo.getFeatureType().getNativeName();
        FeatureReader<SimpleFeatureType, SimpleFeature> wfsFeatureReader = featureDataStore.getFeatureReader(new DefaultQuery(wfsTypeName), new DefaultTransaction());
        DatasetInfo datasetInfo = catalog.getDatasetByFramework(frameworkInfo.getId(), typeName);
        return new TJSFeatureReader(getSchema(typeName), wfsFeatureReader, datasetInfo);
    }

}
