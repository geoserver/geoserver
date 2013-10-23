/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v2_0;

import javax.xml.namespace.QName;

import net.opengis.wfs20.Wfs20Factory;

import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.Configuration;

/**
 * Custom binding class to support bounding box on each feature flag
 * at aggregated level.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class FeatureCollectionTypeBinding extends org.geotools.wfs.v2_0.FeatureCollectionTypeBinding {
    boolean generateBounds;

    public FeatureCollectionTypeBinding(Wfs20Factory factory, Configuration configuration) {
        super(factory);
        this.generateBounds = !configuration.getProperties().contains(GMLConfiguration.NO_FEATURE_BOUNDS);
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        Object val = super.getProperty(object, name);
        // remove aggregated bounds if asked to do so
        if( "boundedBy".equals( name.getLocalPart() ) && !generateBounds) {
            return null;
        }        
        return val;
    }

    

}
