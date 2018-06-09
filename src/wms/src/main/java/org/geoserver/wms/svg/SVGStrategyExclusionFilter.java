/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import org.geoserver.platform.ExtensionFilter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * An extension filter that tells {@link GeoServerExtensions#extensions(Class)} (where {@code Class
 * == GetMapOutputFormat.class}) whether the {@link SVGStreamingMapOutputFormat} or the {@link
 * SVGBatikMapOutputFormat} is to be excluded based on the {@link WMS#getSvgRenderer()} config
 * option.
 *
 * <p>Implementation note: it would be better if this bean received a reference to the {@link WMS}
 * facade instead of looking it up through {@link GeoServerExtensions} but if so, unit tests break
 * while setting up the mock application context when {@code SecureCatalogImpl}'s constructor
 * performs an extension lookup with the following error: <i> Error creating bean with name 'wms':
 * Requested bean is currently in creation: Is there an unresolvable circular reference?</i>
 *
 * @author Gabriel Roldan
 */
public class SVGStrategyExclusionFilter implements ExtensionFilter, ApplicationContextAware {

    private String wmsBeanName;

    private WMS wms;

    private ApplicationContext ctx;

    /**
     * @param wmsBeanName name of the bean of type {@link WMS} to be lazily looked up in the
     *     application context
     */
    public SVGStrategyExclusionFilter(final String wmsBeanName) {
        this.wmsBeanName = wmsBeanName;
    }

    /**
     * @param ctx context where to look for the {@link WMS} bean named as specified to this class'
     *     constructor
     * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    /**
     * @return {@code true} if {@code bean} is one of the SVG rendering strategies but not the one
     *     that shall be used as per {@link WMS#getSvgRenderer()}
     * @see ExtensionFilter#exclude(String, Object)
     */
    public boolean exclude(String beanId, Object bean) {
        boolean exclude = false;
        /*
         * Lazy lookup of the WMS bean is performed here because this method is often called while
         * the application context is still being built
         */
        if (bean instanceof SVGStreamingMapOutputFormat) {
            exclude = !SVG.canHandle(getWMS(), WMS.SVG_SIMPLE);
        } else if (bean instanceof SVGBatikMapOutputFormat) {
            exclude = !SVG.canHandle(getWMS(), WMS.SVG_BATIK);
        }
        return exclude;
    }

    private WMS getWMS() {
        if (wms == null) {
            wms = (WMS) ctx.getBean(wmsBeanName);
        }
        return wms;
    }
}
