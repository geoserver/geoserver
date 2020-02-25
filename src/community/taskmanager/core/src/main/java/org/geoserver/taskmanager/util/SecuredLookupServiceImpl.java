/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecuredLookupServiceImpl<T extends Secured> extends LookupServiceImpl<T>
        implements LookupService<T> {

    @Override
    public T get(String name) {
        T s = super.get(name);
        return canAccess(s) ? s : null;
    }

    @Override
    public <S extends T> S get(String name, Class<S> clazz) {
        S s = super.get(name, clazz);
        return canAccess(s) ? s : null;
    }

    @Override
    public SortedSet<String> names() {
        SortedSet<String> names = new TreeSet<String>(super.names());
        names.removeIf(s -> !canAccess(super.get(s)));
        return names;
    }

    @Override
    public Collection<T> all() {
        Collection<T> all = new ArrayList<T>(super.all());
        all.removeIf(s -> !canAccess(s));
        return all;
    }

    private boolean canAccess(Secured sec) {
        if (sec == null) {
            return true;
        }
        if (sec.getRoles() == null) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return true;
        }
        if (auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (sec.getRoles().contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
