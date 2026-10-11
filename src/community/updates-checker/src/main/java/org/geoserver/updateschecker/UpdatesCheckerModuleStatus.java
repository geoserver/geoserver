/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.lang.reflect.Method;
import org.geoserver.platform.ModuleStatusImpl;

/**
 * The module's entry on the Status page. The category and contact only exist from GeoServer 3.0, so they are set only
 * where {@link ModuleStatusImpl} has them, which keeps the same jar usable on older releases.
 */
public class UpdatesCheckerModuleStatus extends ModuleStatusImpl {

    private static final long serialVersionUID = 1L;

    public UpdatesCheckerModuleStatus() {
        super("gs-updates-checker", "Check for Updates");
        setDocumentation("community/updates-checker/index.html");
        setIfPresent("setContact", String.class, "gs-updates-checker@geoguru.africa");
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object community =
                    Enum.valueOf((Class) Class.forName("org.geoserver.platform.ModuleStatus$Category"), "COMMUNITY");
            setIfPresent("setCategory", community.getClass(), community);
        } catch (ClassNotFoundException e) {
            // older GeoServer: modules have no category
        }
    }

    private void setIfPresent(String setter, Class<?> type, Object value) {
        try {
            Method method = ModuleStatusImpl.class.getMethod(setter, type);
            method.invoke(this, value);
        } catch (ReflectiveOperationException e) {
            // older GeoServer: the property does not exist
        }
    }
}
