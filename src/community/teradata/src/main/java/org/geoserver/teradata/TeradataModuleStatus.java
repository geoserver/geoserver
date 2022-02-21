/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.teradata;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.data.teradata.TeradataDataStoreFactory;

public class TeradataModuleStatus extends ModuleStatusImpl implements ModuleStatus {

    private static final long serialVersionUID = -8193285933110449870L;
    static TeradataDataStoreFactory fac = new TeradataDataStoreFactory();

    @Override
    public String getModule() {
        return "gs-teradata";
    }

    @Override
    public Optional<String> getComponent() {

        return Optional.of("Teradata Database");
    }

    @Override
    public String getName() {
        return "Teradata Datastore";
    }

    @Override
    public boolean isAvailable() {
        return fac.isAvailable();
    }

    @Override
    public boolean isEnabled() {
        return fac.isAvailable();
    }

    @Override
    public Optional<String> getMessage() {
        String message = "";
        if (!fac.isAvailable()) {
            message =
                    "This plugin requires com.teradata.terajdbc4.jar and com.teradata.tdgssconfig.jar to be present.";
        }
        return Optional.of(message);
    }
}
