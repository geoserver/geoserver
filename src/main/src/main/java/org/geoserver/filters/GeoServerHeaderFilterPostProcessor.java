/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.header.HeaderWriterFilter;

/** Ensures any {@link GeoServerFilter} that happens to extend {@link HeaderWriterFilter} writes headers eagerly */
public class GeoServerHeaderFilterPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = Logging.getLogger(GeoServerHeaderFilterPostProcessor.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof GeoServerFilter && bean instanceof HeaderWriterFilter) {
            LOGGER.log(Level.CONFIG, "Switching HeaderWriterFilter to eager header writing for bean {0}", beanName);
            ((HeaderWriterFilter) bean).setShouldWriteHeadersEagerly(true);
        }
        return bean;
    }
}
