/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.gml3.simple.GML3FeatureCollectionEncoderDelegate;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:FeatureCollectionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="FeatureCollectionType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              This type defines a container for the response to a
 *              GetFeature or GetFeatureWithLock request.  If the
 *              request is GetFeatureWithLock, the lockId attribute
 *              must be populated.  The lockId attribute can otherwise
 *              be safely ignored.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="gml:AbstractFeatureCollectionType"&gt;
 *              &lt;xsd:attribute name="lockId" type="xsd:string" use="optional"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                    The value of the lockId attribute is an identifier
 *                    that a Web Feature Service generates when responding
 *                    to a GetFeatureWithLock request.  A client application
 *                    can use this value in subsequent operations (such as a
 *                    Transaction request) to reference the set of locked
 *                    features.
 *                 &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *              &lt;xsd:attribute name="timeStamp" type="xsd:dateTime" use="optional"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                    The timeStamp attribute should contain the date and time
 *                    that the response was generated.
 *                 &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *              &lt;xsd:attribute name="numberOfFeatures"
 *                  type="xsd:nonNegativeInteger" use="optional"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                    The numberOfFeatures attribute should contain a
 *                    count of the number of features in the response.
 *                    That is a count of all features elements dervied
 *                    from gml:AbstractFeatureType.
 *                 &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class FeatureCollectionTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;
    Catalog catalog;
    boolean generateBounds;
    /**
     * Boolean property which controls whether the FeatureCollection should be encoded with multiple featureMember as
     * opposed to a single featureMembers
     */
    boolean encodeFeatureMember;

    private Encoder encoder;

    public FeatureCollectionTypeBinding(WfsFactory wfsfactory, Catalog catalog, Configuration configuration) {
        this(wfsfactory, catalog, configuration, null);
    }

    public FeatureCollectionTypeBinding(
            WfsFactory wfsfactory, Catalog catalog, Configuration configuration, Encoder encoder) {
        this.wfsfactory = wfsfactory;
        this.catalog = catalog;
        this.encoder = encoder;
        this.generateBounds = !configuration.getProperties().contains(GMLConfiguration.NO_FEATURE_BOUNDS);
        this.encodeFeatureMember = configuration.getProperties().contains(GMLConfiguration.ENCODE_FEATURE_MEMBER);
    }

    @Override
    public int getExecutionMode() {
        return OVERRIDE;
    }

    /** @generated */
    @Override
    public QName getTarget() {
        return WFS.FEATURECOLLECTIONTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Class<FeatureCollectionType> getType() {
        return FeatureCollectionType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        return value;
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        // check for feature collection members
        if (GML.featureMembers.equals(name)) {
            // check the WFS configuration, if encode featureMember is selected on WFS configuration
            // page, return null;
            if (encodeFeatureMember) {
                return null;
            }
            FeatureCollectionType featureCollection = (FeatureCollectionType) object;

            if (!featureCollection.getFeature().isEmpty()) {
                return handleFeatureCollection(featureCollection);
            }

        } else if (GML.featureMember.equals(name)) {
            // check the WFS configuration, if encode featureMembers is selected on WFS
            // configuration page, return null;
            if (!encodeFeatureMember) {
                return null;
            }
            FeatureCollectionType featureCollection = (FeatureCollectionType) object;

            if (!featureCollection.getFeature().isEmpty()) {
                return handleFeatureCollection(featureCollection);
            }
        } else if (GML.boundedBy.equals(name) && generateBounds) {
            FeatureCollectionType featureCollection = (FeatureCollectionType) object;

            ReferencedEnvelope env = null;
            for (Object o : featureCollection.getFeature()) {
                FeatureCollection fc = (FeatureCollection) o;
                if (env == null) {
                    env = fc.getBounds();
                } else {
                    env.expandToInclude(fc.getBounds());
                }

                // workaround bogus collection implementation that won't return the crs
                if (env != null && env.getCoordinateReferenceSystem() == null) {
                    CoordinateReferenceSystem crs = fc.getSchema().getCoordinateReferenceSystem();
                    if (crs == null) {
                        // fall back on catalog
                        FeatureTypeInfo info =
                                catalog.getFeatureTypeByName(fc.getSchema().getName());
                        if (info != null) {
                            crs = info.getCRS();
                        }
                    }
                    env = new ReferencedEnvelope(env, crs);
                }

                if (env != null) {
                    // JD: here we don't return the envelope if it is null or empty, this is to work
                    // around an issue with validation in the cite engine. I have opened a jira task
                    // to track this, and hopefully eventually fix the cite engine
                    //    https://osgeo-org.atlassian.net/browse/GEOS-2700
                    return !(env.isNull() || env.isEmpty()) ? env : null;
                }
            }
        }

        // delegate to parent lookup
        return super.getProperty(object, name);
    }

    @SuppressWarnings("unchecked") // EMF model without generics
    private Object handleFeatureCollection(FeatureCollectionType featureCollection) {
        FeatureCollection result = null;
        if (featureCollection.getFeature().size() > 1) {
            // wrap in a single
            result = new CompositeFeatureCollection<>(featureCollection.getFeature());
        } else {
            // just return the single
            result = (FeatureCollection)
                    featureCollection.getFeature().iterator().next();
        }

        if (isSimpleFeatureCollection(result)
                && encoder.getConfiguration().hasProperty(GMLConfiguration.OPTIMIZED_ENCODING)) {
            if (result instanceof CompositeFeatureCollection collection) {
                return new GML3FeatureCollectionEncoderDelegate(collection.simple(), encoder);
            }
            return new GML3FeatureCollectionEncoderDelegate(DataUtilities.simple(result), encoder);
        } else {
            return result;
        }
    }

    private boolean isSimpleFeatureCollection(FeatureCollection result) {
        if (result instanceof CompositeFeatureCollection collection) {
            return collection.isSimple();
        } else {
            return result instanceof SimpleFeatureCollection;
        }
    }
}
