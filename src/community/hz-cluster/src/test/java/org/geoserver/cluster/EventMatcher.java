/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

public class EventMatcher implements IArgumentMatcher {

    Object source;

    public EventMatcher(Object source) {
        super();
        this.source = source;
    }

    public static Event event(Object source) {
        EasyMock.reportMatcher(new EventMatcher(source));
        return null;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof Event) {
            Event evt = (Event) argument;
            return source == null || evt.getSource().equals(source);
        } else {
            return false;
        }
    }

    @Override
    public void appendTo(StringBuffer buffer) {
        buffer.append("event(").append(source.toString()).append(")");
    }
}
