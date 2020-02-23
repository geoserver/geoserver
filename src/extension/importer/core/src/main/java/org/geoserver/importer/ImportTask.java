/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geoserver.importer.ImporterUtils.resolve;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.TransformChain;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * A unit of work during an import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportTask implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String TYPE_NAME = "typeName";
    public static final String TYPE_SPEC = "typeSpec";

    public static enum State {
        PENDING,
        READY,
        RUNNING,
        NO_CRS,
        NO_BOUNDS,
        NO_FORMAT,
        BAD_FORMAT,
        ERROR,
        CANCELED,
        COMPLETE
    }

    /** task id */
    long id;

    /** the context this task is part of */
    ImportContext context;

    /** source of data for the import */
    ImportData data;

    /** The target store for the import */
    StoreInfo store;

    /** state */
    State state = State.PENDING;

    /** id generator for items */
    int itemid = 0;

    /** flag signalling direct/indirect import */
    boolean direct;

    /** how data should be applied to the target, during ingest/indirect import */
    UpdateMode updateMode;

    /** The original layer name assigned to the task */
    String originalLayerName;

    /** the layer/resource */
    LayerInfo layer;

    /** Any error associated with the resource */
    Exception error;

    /** transform to apply to this import item */
    TransformChain transform;

    /** messages logged during proessing */
    List<LogRecord> messages = new ArrayList<LogRecord>();

    /** various metadata */
    transient Map<Object, Object> metadata;

    /** used to track progress */
    int totalToProcess;

    int numberProcessed;

    String typeName;

    String typeSpec;

    public ImportTask() {
        updateMode = UpdateMode.CREATE;
    }

    public ImportTask(ImportData data) {
        this();
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ImportContext getContext() {
        return context;
    }

    public void setContext(ImportContext context) {
        this.context = context;
    }

    public ImportData getData() {
        return data;
    }

    public void setData(ImportData data) {
        this.data = data;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
    }

    public StoreInfo getStore() {
        return store;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public LayerInfo getLayer() {
        return layer;
    }

    public void setLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public TransformChain getTransform() {
        return transform;
    }

    public void setTransform(TransformChain transform) {
        this.transform = transform;
    }

    /**
     * Returns a transient metadata map, useful for caching information that's expensive to compute.
     * The map won't be stored in the {@link ImportStore} so don't use it for anything that needs to
     * be persisted.
     */
    public Map<Object, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<Object, Object>();
        }
        return metadata;
    }

    public void clearMessages() {
        if (messages != null) {
            messages.clear();
        }
    }

    public void addMessage(Level level, String msg) {
        if (messages == null) {
            messages = new ArrayList<LogRecord>();
        }
        messages.add(new LogRecord(level, msg));
    }

    public List<LogRecord> getMessages() {
        List<LogRecord> retval;
        if (messages == null) {
            retval = Collections.emptyList();
        } else {
            retval = Collections.unmodifiableList(messages);
        }
        return retval;
    }

    public String getOriginalLayerName() {
        return originalLayerName == null ? layer.getResource().getNativeName() : originalLayerName;
    }

    public void setOriginalLayerName(String originalLayerName) {
        this.originalLayerName = originalLayerName;
    }

    public int getNumberProcessed() {
        return numberProcessed;
    }

    public void setNumberProcessed(int numberProcessed) {
        this.numberProcessed = numberProcessed;
    }

    public int getTotalToProcess() {
        return totalToProcess;
    }

    public void setTotalToProcess(int totalToProcess) {
        this.totalToProcess = totalToProcess;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public void reattach(Catalog catalog) {
        reattach(catalog, false);
    }

    public void reattach(Catalog catalog, boolean lookupByName) {
        store = resolve(store, catalog, lookupByName);
        layer = resolve(layer, catalog, lookupByName);
    }

    public boolean readyForImport() {
        return state == State.READY || state == State.CANCELED;
    }

    public ProgressMonitor progress() {
        return context.progress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImportTask other = (ImportTask) obj;
        if (context == null) {
            if (other.context != null) return false;
        } else if (!context.equals(other.context)) return false;
        if (id != other.id) return false;
        return true;
    }

    public SimpleFeatureType getFeatureType() {
        SimpleFeatureType schema = (SimpleFeatureType) getMetadata().get(FeatureType.class);
        if (schema == null) {
            if (typeName != null && typeSpec != null) {
                try {
                    schema = DataUtilities.createType(typeName, typeSpec);
                    getMetadata().put(FeatureType.class, schema);
                } catch (SchemaException e) {
                    // ignore
                }
            }
        }

        return schema;
    }

    public void setFeatureType(SimpleFeatureType featureType) {
        getMetadata().put(FeatureType.class, featureType);
        if (featureType != null) {
            typeName = featureType.getTypeName();
            typeSpec = DataUtilities.encodeType(featureType);
        }
    }
}
