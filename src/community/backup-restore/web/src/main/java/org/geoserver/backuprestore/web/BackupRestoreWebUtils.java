/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.ComponentTag;
import org.geoserver.backuprestore.Backup;
import org.geoserver.web.GeoServerApplication;
import org.springframework.security.core.context.SecurityContextHolder;

/** @author afabiani */
public class BackupRestoreWebUtils {

    static Backup backupFacade() {
        Backup backupFacade = GeoServerApplication.get().getBeanOfType(Backup.class);
        backupFacade.setAuth(SecurityContextHolder.getContext().getAuthentication());
        return backupFacade;
    }

    static boolean isDevMode() {
        return RuntimeConfigurationType.DEVELOPMENT
                == GeoServerApplication.get().getConfigurationType();
    }

    static void disableLink(ComponentTag tag) {
        tag.setName("a");
        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
    }

    static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
