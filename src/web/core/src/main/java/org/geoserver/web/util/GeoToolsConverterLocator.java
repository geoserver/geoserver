/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Implementation of IConverterLocator which falls back onto the Geotools converter subsystem.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class GeoToolsConverterLocator implements IConverterLocator {

    private static final long serialVersionUID = -8704868281264763254L;
    static final Logger LOGGER = Logging.getLogger(GeoToolsConverterLocator.class);

    public <C> IConverter<C> getConverter(Class<C> type) {
        Set<ConverterFactory> factories = Converters.getConverterFactories(String.class, type);
        if (!factories.isEmpty()) {
            return new GeoToolsConverter<C>(factories, type);
        }

        return null;
    }

    static class GeoToolsConverter<T> implements IConverter<T> {

        private static final long serialVersionUID = 3463117432947622403L;
        Set<ConverterFactory> factories;
        Class<T> target;

        GeoToolsConverter(Set<ConverterFactory> factories, Class<T> target) {
            this.factories = factories;
            this.target = target;
        }

        public T convertToObject(String value, Locale locale) {
            for (ConverterFactory factory : factories) {
                try {
                    Converter converter = factory.createConverter(String.class, target, null);
                    if (converter != null) {
                        T converted = converter.convert(value, target);
                        if (converted != null) {
                            return converted;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error converting \"" + value + "\" to " + target.getName(),
                            e);
                }
            }

            return null;
        }

        public String convertToString(Object value, Locale locale) {
            Set<ConverterFactory> rconverters =
                    (Set<ConverterFactory>) Converters.getConverterFactories(target, String.class);
            for (ConverterFactory cf : rconverters) {
                try {
                    Converter converter = cf.createConverter(value.getClass(), String.class, null);
                    if (converter == null) {
                        continue;
                    }

                    String converted = converter.convert(value, String.class);
                    if (converted != null) {
                        return converted;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to String", e);
                }
            }

            return value.toString();
        }
    }
}
