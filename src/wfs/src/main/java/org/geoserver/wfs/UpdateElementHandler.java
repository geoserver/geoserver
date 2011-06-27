/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Processes standard update elements
 *
 * @author Andrea Aime - TOPP
 *
 */
public class UpdateElementHandler extends AbstractTransactionElementHandler {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");
    
    public UpdateElementHandler(GeoServer gs) {
        super(gs);
    }

    public void checkValidity(EObject element, Map<QName, FeatureTypeInfo> typeInfos)
        throws WFSTransactionException {
        // check inserts are enabled
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_UPDATE) ) {
            throw new WFSException("Transaction Update support is not enabled");
        }

        FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
        
        // check that all required properties have a specified value
        UpdateElementType update = (UpdateElementType) element;

        try {
            FeatureTypeInfo meta = typeInfos.values().iterator().next();
            FeatureType featureType = meta.getFeatureType();

            for (Iterator prop = update.getProperty().iterator(); prop.hasNext();) {
                PropertyType property = (PropertyType) prop.next();

                //check that valus that are non-nillable exist
                if (property.getValue() == null) {
                    String propertyName = property.getName().getLocalPart();
                    AttributeDescriptor attributeType = null;
                    PropertyDescriptor pd = featureType.getDescriptor(propertyName);
                    if(pd instanceof AttributeDescriptor) {
                        attributeType = (AttributeDescriptor) pd;
                    }
                    if ((attributeType != null) && (attributeType.getMinOccurs() > 0)) {
                        String msg = "Property '" + attributeType.getLocalName()
                            + "' is mandatory but no value specified.";
                        throw new WFSException(msg, "MissingParameterValue");
                    }
                }
                
                //check that property names are actually valid
                QName name = property.getName();
                PropertyName propertyName = null;
                
                if ( name.getPrefix() != null && !"".equals( name.getPrefix() )) {
                    propertyName = ff.property( name.getPrefix() + ":" + name.getLocalPart() );
                }
                else {
                    propertyName = ff.property( name.getLocalPart() ); 
                }
                
                if ( propertyName.evaluate( featureType ) == null ) {
                    String msg = "No such property: " + property.getName();
                    throw new WFSException( msg );
                }
            }
        } catch (IOException e) {
            throw new WFSTransactionException("Could not locate feature type information for "
                + update.getTypeName(), e, update.getHandle());
        }
    }

    public void execute(EObject element, TransactionType request,
            @SuppressWarnings("rawtypes") Map<QName, FeatureStore> featureStores,
            TransactionResponseType response, TransactionListener listener)
            throws WFSTransactionException {
        UpdateElementType update = (UpdateElementType) element;
        final QName elementName = update.getTypeName();
        String handle = update.getHandle();
        long updated = response.getTransactionSummary().getTotalUpdated().longValue();

        SimpleFeatureStore store = DataUtilities.simple((FeatureStore) featureStores.get(elementName));

        if (store == null) {
            throw new WFSException("Could not locate FeatureStore for '" + elementName + "'");
        }

        LOGGER.finer("Transaction Update:" + element);

        try {
            Filter filter = (Filter) update.getFilter();

            // make sure all geometric elements in the filter have a crs, and that the filter
            // is reprojected to store's native crs as well
            CoordinateReferenceSystem declaredCRS = WFSReprojectionUtil.getDeclaredCrs(
                    store.getSchema(), request.getVersion());
            if(filter != null) {
                filter = WFSReprojectionUtil.normalizeFilterCRS(filter, store.getSchema(), declaredCRS);
            } else {
                filter = Filter.INCLUDE;
            }


            AttributeDescriptor[] types = new AttributeDescriptor[update.getProperty().size()];
            String[] names = new String[update.getProperty().size()];
            Object[] values = new Object[update.getProperty().size()];

            for (int j = 0; j < update.getProperty().size(); j++) {
                PropertyType property = (PropertyType) update.getProperty().get(j);
                types[j] = store.getSchema().getDescriptor(property.getName().getLocalPart());
                names[j] = property.getName().getLocalPart();
                values[j] = property.getValue();
                
                // if geometry, it may be necessary to reproject it to the native CRS before
                // update
                if (values[j] instanceof Geometry ) {
                    Geometry geometry = (Geometry) values[j];
                    
                    // get the source crs, check the geometry itself first. If not set, assume
                    // the default one
                    CoordinateReferenceSystem source = null;
                    if ( geometry.getUserData() instanceof CoordinateReferenceSystem ) {
                        source = (CoordinateReferenceSystem) geometry.getUserData();
                    } else {
                        geometry.setUserData(declaredCRS);
                        source = declaredCRS;
                    }
                    
                    // see if the geometry has a CRS other than the default one
                    CoordinateReferenceSystem target = null;
                    if (types[j] instanceof GeometryDescriptor) {
                        target = ((GeometryDescriptor)types[j]).getCoordinateReferenceSystem();
                    }
                    
                    if(getInfo().isCiteCompliant())
                        JTS.checkCoordinatesRange(geometry, source != null ? source : target);
                    
                    //if we have a source and target and they are not equal, do 
                    // the reprojection, otherwise just update the value as is
                    if ( source != null && target != null && !CRS.equalsIgnoreMetadata(source, target)) {
                        try {
                            //TODO: this code should be shared with the code
                            // from ReprojectingFeatureCollection --JD
                            MathTransform tx = CRS.findMathTransform(source, target, true);
                            GeometryCoordinateSequenceTransformer gtx = 
                                new GeometryCoordinateSequenceTransformer();
                            gtx.setMathTransform(tx);
                            
                            values[j] = gtx.transform(geometry);    
                        }
                        catch( Exception e ) {
                            String msg = "Failed to reproject geometry:" + e.getLocalizedMessage(); 
                            throw new WFSTransactionException( msg, e );
                        }
                    }
                    
                }
            }

            // Pass through data to collect fids and damaged
            // region
            // for validation
            //
            Set fids = new HashSet();
            LOGGER.finer("Preprocess to remember modification as a set of fids");
            
            SimpleFeatureCollection features = store.getFeatures(filter);
            TransactionEvent event = new TransactionEvent(TransactionEventType.PRE_UPDATE, request,
                    elementName, features);
            event.setSource( update );
            
            listener.dataStoreChange( event );

            Iterator preprocess = features.iterator();

            try {
                while (preprocess.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) preprocess.next();
                    fids.add(feature.getID());
                }
            } catch (NoSuchElementException e) {
                throw new WFSException("Could not aquire FeatureIDs", e);
            } finally {
                features.close(preprocess);
            }

            try {
                store.modifyFeatures(names, values, filter);
            } catch( Exception e) {
                //JD: this is a bit hacky but some of the wfs cite tests require
                // that the 'InvalidParameterValue' code be set on exceptions in 
                // cases where a "bad" value is being suppliedin an update, so 
                // we always set to that code
                throw new WFSTransactionException( "Update error: " + e.getMessage(), e, "InvalidParameterValue");
                
            }
            finally {
                // make sure we unlock
                if ((request.getLockId() != null) && store instanceof FeatureLocking
                        && (request.getReleaseAction() == AllSomeType.SOME_LITERAL)) {
                    SimpleFeatureLocking locking;
                    locking = (SimpleFeatureLocking) store;
                    locking.unLockFeatures(filter);
                }
            }

            // Post process - gather the same features after the update, and  
            if (!fids.isEmpty()) {
                LOGGER.finer("Post process update for boundary update and featureValidation");

                Set featureIds = new HashSet();

                FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
                for (Iterator f = fids.iterator(); f.hasNext();) {
                    featureIds.add(ff.featureId((String) f.next()));
                }

                Id modified = ff.id(featureIds);

                SimpleFeatureCollection changed = store.getFeatures(modified);
                listener.dataStoreChange(new TransactionEvent(TransactionEventType.POST_UPDATE,
                        request, elementName, changed, update));
            }

            // update the update counter
            updated += fids.size();
        } catch (IOException ioException) {
            // JD: changing from throwing service exception to
            // adding action that failed
            throw new WFSTransactionException(ioException, null, handle);
        } catch(PointOutsideEnvelopeException poe) {
            throw new WFSTransactionException(poe, null, handle);
        }

        // update transaction summary
        response.getTransactionSummary().setTotalUpdated(BigInteger.valueOf(updated));
    }

    /**
     * @see org.geoserver.wfs.TransactionElementHandler#getElementClass()
     */
    public Class<UpdateElementType> getElementClass() {
        return UpdateElementType.class;
    }

    /**
     * @see org.geoserver.wfs.TransactionElementHandler#getTypeNames(org.eclipse.emf.ecore.EObject)
     */
    public QName[] getTypeNames(EObject element) throws WFSTransactionException {
        return new QName[] { ((UpdateElementType) element).getTypeName() };
    }
}
