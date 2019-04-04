/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.xml.namespace.QName;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;

/**
 * Event carrying information about a change that happened/that is about to occur.
 *
 * <p>The feature collection may be an in-memory one, or may be based on a real data store with a
 * filter.
 *
 * <p><b>Note</b> that care should be taken when relying on feature identifiers from a {@link
 * TransactionEventType#POST_INSERT} event. Depending on the type of store those identifiers may be
 * reliable. Essentially they can only be relied upon in the case of a spatial dbms (such as
 * PostGIS) is being used.
 */
public class TransactionEvent {
    private TransactionEventType type;
    private SimpleFeatureCollection affectedFeatures;
    private QName layerName;
    private Object source;
    private final TransactionRequest request;

    public TransactionEvent(
            TransactionEventType type,
            TransactionRequest request,
            QName layerName,
            SimpleFeatureCollection affectedFeatures) {
        this(type, request, layerName, affectedFeatures, null);
    }

    public TransactionEvent(
            TransactionEventType type,
            TransactionRequest request,
            QName layerName,
            SimpleFeatureCollection affectedFeatures,
            Object source) {
        this.type = type;
        this.request = request;
        this.layerName = layerName;
        this.affectedFeatures = affectedFeatures;
        this.source = source;
    }

    /** The type of change occurring */
    public TransactionEventType getType() {
        return type;
    }

    /**
     * A collection of the features that are being manipulated. Accessible and usable only when the
     * event is being thrown, if you store the event and try to access the collection later there is
     * no guarantee it will still be usable.
     */
    public SimpleFeatureCollection getAffectedFeatures() {
        return affectedFeatures;
    }

    /** The name of the layer / feature type that this transaction effects. */
    public QName getLayerName() {
        return layerName;
    }

    /** Sets the source of the transction. */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Returns the source of the transaction.
     *
     * <p>One of:
     *
     * <ul>
     *   <li>{@link InsertElementType}
     *   <li>{@link UpdateElementType}
     *   <li>{@link DeleteElementType}
     * </ul>
     */
    public Object getSource() {
        return source;
    }

    public TransactionType getRequest() {
        return TransactionRequest.WFS11.unadapt(request);
    }

    /**
     * Returns the current GeoTools Data {@link Transaction} associated with this event. May be
     * {@code null} for post-commit events.
     */
    public Transaction getTransaction() {
        return request.getTransaction();
    }
}
