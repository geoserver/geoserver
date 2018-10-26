/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wps.process.DelegatingProcessFactory;
import org.geoserver.wps.process.ProcessFilter;
import org.geotools.data.util.DelegateProgressListener;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * A process filter making sure the {@link ProgressListener#started()} method is called upon
 * execution no matter if the process has inputs or not
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessStartupFilter implements ProcessFilter, ExtensionPriority {

    public class ProcessStartupWrapper implements Process {

        Process delegate;

        public ProcessStartupWrapper(Process delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
                throws ProcessException {
            if (monitor != null) {
                monitor.started();
                monitor =
                        new DelegateProgressListener(monitor) {
                            @Override
                            public void started() {
                                // do not pass over, we already called "started"
                            }
                        };
            }
            return delegate.execute(input, monitor);
        }
    }

    static final Logger LOGGER = Logging.getLogger(ProcessStartupFilter.class);

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        return new ProcessStartupFactory(pf);
    }

    class ProcessStartupFactory extends DelegatingProcessFactory {

        public ProcessStartupFactory(ProcessFactory delegate) {
            super(delegate);
        }

        @Override
        public Process create(Name name) {
            Process process = delegate.create(name);
            return new ProcessStartupWrapper(process);
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST + 1;
    }
}
