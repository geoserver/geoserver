/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * XML/DTO wrapper used by the REST layer to transport the enabled authentication provider order. The controller writes
 * this list into {@code <authProviderNames>} of {@code security.xml}.
 *
 * <p>XStream is configured with an implicit collection on the {@code order} field, so it serializes like:
 *
 * <pre>{@code
 * <order>
 *   <order>name1</order>
 *   <order>name2</order>
 * </order>
 * }</pre>
 *
 * <p>Names are carried verbatim here; normalization/validation is handled by the controller.
 */
@XStreamAlias("order")
public class AuthProviderOrder {

    /** Backing list used by XStream implicit collection. Do not make final. */
    private List<String> order = new ArrayList<>();

    /** Required by XStream. */
    public AuthProviderOrder() {}

    /** Convenience ctor; null treated as empty. */
    public AuthProviderOrder(List<String> order) {
        setOrder(order);
    }

    /** Returns an unmodifiable view of the order; never {@code null}. */
    public List<String> getOrder() {
        return Collections.unmodifiableList(order);
    }

    /** Replaces the order list. {@code null} becomes empty. A defensive copy is taken to prevent external mutation. */
    public void setOrder(List<String> order) {
        this.order = (order == null) ? new ArrayList<>() : new ArrayList<>(order);
    }

    /** @return number of names (never negative). */
    public int size() {
        return order.size();
    }

    /** @return {@code true} if empty. */
    public boolean isEmpty() {
        return order.isEmpty();
    }

    /** Append a name; no-op on {@code null}. */
    public void add(String name) {
        if (name != null) order.add(name);
    }

    @Override
    public String toString() {
        return "AuthProviderOrder{order=" + order + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthProviderOrder)) return false;
        return Objects.equals(order, ((AuthProviderOrder) o).order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order);
    }
}
