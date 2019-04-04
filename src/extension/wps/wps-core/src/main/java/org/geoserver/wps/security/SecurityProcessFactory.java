/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.geoserver.wps.process.DelegatingProcessFactory;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class SecurityProcessFactory extends DelegatingProcessFactory {

    SecurityProcessFilter selector;

    public SecurityProcessFactory(ProcessFactory delegate, SecurityProcessFilter selector) {
        super(delegate);
        this.selector = selector;
    }

    @Override
    public Set<Name> getNames() {
        // filter out the processes we want to hide
        Set<Name> names = new LinkedHashSet<Name>(super.getNames());
        for (Iterator<Name> it = names.iterator(); it.hasNext(); ) {
            Name name = it.next();
            if (!selector.allowProcess(name)) {
                it.remove();
            }
        }

        return names;
    }

    public Process create(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.create(name);
        } else {
            return null;
        }
    }

    public InternationalString getDescription(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.getDescription(name);
        } else {
            return null;
        }
    }

    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.getParameterInfo(name);
        } else {
            return null;
        }
    }

    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters)
            throws IllegalArgumentException {
        if (selector.allowProcess(name)) {
            return delegate.getResultInfo(name, parameters);
        } else {
            return null;
        }
    }

    public InternationalString getTitle(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.getTitle(name);
        } else {
            return null;
        }
    }

    public String getVersion(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.getVersion(name);
        } else {
            return null;
        }
    }

    public boolean supportsProgress(Name name) {
        if (selector.allowProcess(name)) {
            return delegate.supportsProgress(name);
        } else {
            return false;
        }
    }
}
