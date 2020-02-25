/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.testreader;

import java.awt.RenderingHints.Key;
import java.util.Collections;
import java.util.Map;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

/**
 * Coverage format SPI for custom dimensions tests.
 *
 * @author Mike Benowitz
 */
public final class CustomFormatFactory implements GridFormatFactorySpi {

    @Override
    public AbstractGridFormat createFormat() {
        return new CustomFormat();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
}
