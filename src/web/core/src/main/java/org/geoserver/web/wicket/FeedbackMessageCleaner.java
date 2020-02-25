/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * Cleans messages in components recursively
 *
 * @author Andrea Aime
 */
public class FeedbackMessageCleaner<C extends Component, R> implements IVisitor<C, R> {

    int level;

    /**
     * Builds a cleaner removing all messages at or above the specified level. See {@link
     * FeedbackMessage} for a list of levels
     */
    public FeedbackMessageCleaner(int level) {
        this.level = level;
    }

    @Override
    public void component(C component, IVisit<R> visit) {
        if (component.hasFeedbackMessage()) {
            FeedbackMessages messages = component.getFeedbackMessages();
            messages.clear(message -> message.getLevel() >= level);
        }
    }
}
