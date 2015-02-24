/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.Locale;
import java.util.Set;

import org.apache.wicket.IConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;

/**
 * Implementation of IConverterLocator which falls back onto the Geotools 
 * converter subsystem.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class GeoToolsConverterLocator implements IConverterLocator {

    public IConverter getConverter(Class type) {
        Set factories = Converters.getConverterFactories( String.class, type );
        if ( !factories.isEmpty() ) {
            return new GeoToolsConverter( factories, type );
        }
   
        return null;
    }

    static class GeoToolsConverter implements IConverter {

        Set<ConverterFactory> factories;
        Class target;
        
        GeoToolsConverter( Set<ConverterFactory> factories, Class target ) {
            this.factories = factories;
            this.target = target;
        }
        
        public Object convertToObject(String value, Locale locale) {
            for ( ConverterFactory factory : factories ) {
                try {
                    Converter converter = factory.createConverter( String.class, target, null );
                    if ( converter != null ) {
                        Object converted = converter.convert( value, target );
                        if ( converted != null ) {
                            return converted;
                        }
                    }
                } 
                catch (Exception e) {
                    //TODO: log this
                }
            }
            
            return null;
        }

        public String convertToString(Object value, Locale locale) {
            Set<ConverterFactory> rconverters =
                (Set<ConverterFactory>) Converters.getConverterFactories( target, String.class );
            for ( ConverterFactory cf : rconverters ) {
                try {
                    Converter converter = cf.createConverter(value.getClass(), String.class,null);
                    if ( converter == null ) {
                        continue;
                    }
                    
                    String converted = converter.convert(value, String.class);
                    if ( converted != null ) {
                        return converted;
                    }
                } 
                catch (Exception e) {
                    //TODO: log this
                }
            }
            
            return value.toString();
        }
    }
}

