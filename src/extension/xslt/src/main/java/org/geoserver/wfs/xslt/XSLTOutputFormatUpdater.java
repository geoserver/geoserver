/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.geotools.util.logging.Logging;

/**
 * Keeps the list of output formats XSLT can handle updated, without flooding the disk with multiple
 * accesses for each and every request in order to check which output formats are available now (as
 * we are trying to also support direct modifications on disk given that there is no UI)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class XSLTOutputFormatUpdater extends TimerTask {

    static final Logger LOGGER = Logging.getLogger(XSLTOutputFormatUpdater.class);

    private TransformRepository repository;

    long lastModified;

    public XSLTOutputFormatUpdater(TransformRepository repository) {
        this.repository = repository;
        run();
    }

    @Override
    public void run() {
        try {
            List<TransformInfo> infos = this.repository.getAllTransforms();
            Set<String> formats = new HashSet<String>();
            for (TransformInfo tx : infos) {
                formats.add(tx.getOutputFormat());
            }

            XSLTOutputFormat.updateFormats(formats);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to update XSLT output format list", e);
        }
    }
}
