/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.feature;

import java.rmi.server.UID;

import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * This class provides some common functionality for builders and defines an abstraction
 * for Feature builders' public interfaces. 
 * @author bro879
 *  
 * @param <FT>
 * 		The kind of FeatureType whose feature the builder will build. Allows you to enforce a stricter specialist type; eg. SimpleFeatureType.
 * @param <F>
 * 		The kind of Feature that the builder will build. Allows you to enforce a stricter specialist type; eg. SimpleFeature.
 */
public abstract class FeatureBuilder<FT extends FeatureType, F extends Feature> {
	/** the feature type */
    protected FT featureType;

    /** the feature factory */
    protected FeatureFactory factory;

    public abstract F buildFeature(String id);

    protected FeatureBuilder(FT featureType, FeatureFactory factory)
    {
    	this.featureType = featureType;
        this.factory = factory;
    }
    
    /**
     * Returns the feature type used by this builder as a feature template
     * @return
     */
    public FT getFeatureType() {
        return featureType;
    }

    protected Object convert(Object value, PropertyDescriptor descriptor) {
        // make sure the type of the value and the binding of the type match up
        if ( value != null ) {
            Class<?> target = descriptor.getType().getBinding(); 
            Object converted = Converters.convert(value, target);
            if(converted != null) {
                value = converted;
            }
        }
        
        return value;
    }
    
    /**
     * Internal method for creating feature id's when none is specified.
     */
    public static String createDefaultFeatureId() {
        // According to GML and XML schema standards, FID is a XML ID
        // (http://www.w3.org/TR/xmlschema-2/#ID), whose acceptable values are those that match an
        // NCNAME production (http://www.w3.org/TR/1999/REC-xml-names-19990114/#NT-NCName):
        // NCName ::= (Letter | '_') (NCNameChar)* /* An XML Name, minus the ":" */
        // NCNameChar ::= Letter | Digit | '.' | '-' | '_' | CombiningChar | Extender
        // We have to fix the generated UID replacing all non word chars with an _ (it seems
        // they area all ":")
        //return "fid-" + NON_WORD_PATTERN.matcher(new UID().toString()).replaceAll("_");
        // optimization, since the UID toString uses only ":" and converts long and integers
        // to strings for the rest, so the only non word character is really ":"
        return "fid-" + new UID().toString().replace(':', '_');
    }
    /**
     * Internal method for a temporary FeatureId that can be assigned
     * a real value after a commit.
     * @param suggestedId suggested id
     */
    public static FeatureIdImpl createDefaultFeatureIdentifier( String suggestedId ) {
    	if( suggestedId != null ){
    		return new FeatureIdImpl( suggestedId );	
    	}
    	return new FeatureIdImpl( createDefaultFeatureId() );
    }
}
