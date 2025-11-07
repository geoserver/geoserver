package org.geoserver.acl.plugin.web.support;

import java.util.Objects;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.springframework.context.ApplicationContext;

public final class ApplicationContextSupport {

    private ApplicationContextSupport() {
        // no-op
    }
    /**
     * Obtain the bean from the {@link ApplicationContext}, {@link GeoServerApplication#getBeanOfType(Class)
     * GeoServerApplication.get().getBeanOfType()} calls {@link GeoServerExtensions#bean(Class)} which throws an
     * exception if there are more than one bean of that type, not respecting the one defined as primary (e.g. through
     * {@literal @org.springframework.context.annotation.Primary})
     *
     * @param <T>
     * @param type
     * @return
     */
    public static <T> T getBeanOfType(Class<T> type) {
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        return applicationContext.getBean(Objects.requireNonNull(type));
    }
}
