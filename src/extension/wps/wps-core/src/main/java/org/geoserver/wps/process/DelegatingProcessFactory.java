/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.process;

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import java.awt.RenderingHints.Key;
import java.util.Map;
import java.util.Set;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * A simple process factory delegating to another factory. Meant to be a base class for process
 * factory wrappers that need to modify some of the wrapped process factory behavior.
 */
public abstract class DelegatingProcessFactory implements ProcessFactory {

    protected ProcessFactory delegate;

    public DelegatingProcessFactory(ProcessFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Process create(Name name) {
        return delegate.create(name);
    }

    @Override
    public InternationalString getDescription(Name name) {
        return delegate.getDescription(name);
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return delegate.getImplementationHints();
    }

    @Override
    public Set<Name> getNames() {
        return delegate.getNames();
    }

    @Override
    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        return delegate.getParameterInfo(name);
    }

    @Override
    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters)
            throws IllegalArgumentException {
        return delegate.getResultInfo(name, parameters);
    }

    @Override
    public InternationalString getTitle() {
        return delegate.getTitle();
    }

    @Override
    public InternationalString getTitle(Name name) {
        return delegate.getTitle(name);
    }

    @Override
    public String getVersion(Name name) {
        return delegate.getVersion(name);
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public boolean supportsProgress(Name name) {
        return delegate.supportsProgress(name);
    }

    /** Returns the original process factory */
    public ProcessFactory getDelegate() {
        return delegate;
    }

    /**
     * Returns the innermost delegate, this method can be used to check what the original factory
     * was
     */
    public ProcessFactory getInnermostDelegate() {
        ProcessFactory pf = delegate;
        while (pf instanceof DelegatingProcessFactory) {
            pf = ((DelegatingProcessFactory) pf).delegate;
        }

        return pf;
    }
}
