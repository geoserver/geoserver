/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.UpdateElementType;

import org.geotools.data.simple.SimpleFeatureCollection;


/**
 * Event carrying information about a change that happened/that is about to
 * occur. The feature collection may be an in-memory one, or may be based on a
 * real data store with a filter.
 */
public class TransactionEvent {
    private TransactionEventType type;
    private SimpleFeatureCollection affectedFeatures;
    private QName layerName;
    private Object source;

    public TransactionEvent(TransactionEventType type, QName layerName,
            SimpleFeatureCollection affectedFeatures) {
        this( type, layerName, affectedFeatures, null );
    }
    
    public TransactionEvent(TransactionEventType type, QName layerName,
            SimpleFeatureCollection affectedFeatures, Object source) {
        this.type = type;
        this.layerName = layerName;
        this.affectedFeatures = affectedFeatures;
        this.source = source;
    }

    /**
     * The type of change occurring
     */
    public TransactionEventType getType() {
        return type;
    }

    /**
     * A collection of the features that are being manipulated. Accessible and usable only
     * when the event is being thrown, if you store the event and try to access the collection later
     * there is no guarantee it will still be usable.
     */
    public SimpleFeatureCollection getAffectedFeatures() {
        return affectedFeatures;
    }
    
    /**
     * The name of the layer / feature type that this transaction effects.
     */
    public QName getLayerName() {
        return layerName;
    }
    
    /**
     * Sets the source of the transction.
     */
    public void setSource(Object source) {
        this.source = source;
    }
    
    /**
     * Returns the source of the transaction.
     * <p>
     * One of:
     * <ul>
     *  <li>{@link InsertElementType}
     *  <li>{@link UpdateElementType}
     *  <li>{@link DeleteElementType}
     * </ul>
     * </p>
     */
    public Object getSource() {
        return source;
    }
}
