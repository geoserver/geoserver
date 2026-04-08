/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * JSON-only: turns an empty element (START .. END with no CHARACTERS and no child elements) into START,
 * CHARACTERS(NULL_MARKER), END.
 *
 * <p>No peeking: we inject the marker when we *reach* END_ELEMENT, by returning a virtual CHARACTERS event first and
 * buffering the END for the next call.
 *
 * <p>This keeps the StAX contract intact (no delegate desync).
 */
public class NullMarkingStreamReader extends StreamReaderDelegate implements XMLStreamConstants {

    /**
     * Marker string to indicate a JSON null value. Must be something that cannot appear in normal JSON content, and
     * should be long enough to avoid collisions with user content. (We use a UUID to be extra safe.) This allows the
     * downstream code to distinguish between an empty string ("") and a null value, which is important for JSON
     * deserialization where nulls are represented as empty elements.
     */
    public static final String NULL_MARKER = "\u0000__GS_JSON_NULL____550e8400-e29b-41d4-a716-446655440000\u0000";

    private static final class Frame {
        boolean sawChildStart;
        boolean sawCharacters; // true even for "" (empty string), if a CHARACTERS event occurred
    }

    private final Deque<Frame> stack = new ArrayDeque<>();

    // When we inject a virtual CHARACTERS, we buffer the END_ELEMENT to return next
    private boolean pendingEnd = false;

    // Current virtual event (only CHARACTERS is virtual here)
    private Integer virtualEventType = null;

    public NullMarkingStreamReader(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public int next() throws XMLStreamException {
        // If we previously returned a virtual CHARACTERS, now return the buffered END_ELEMENT
        if (pendingEnd) {
            pendingEnd = false;
            virtualEventType = null;
            // Delegate is already positioned at END_ELEMENT from the previous underlying next()
            return END_ELEMENT;
        }

        // Advance delegate normally
        int t = super.next();
        virtualEventType = null;

        switch (t) {
            case START_ELEMENT: {
                // Mark parent had a child element
                if (!stack.isEmpty()) {
                    stack.peekLast().sawChildStart = true;
                }
                // Push new frame for this element
                stack.addLast(new Frame());
                return t;
            }
            case CHARACTERS:
            case CDATA: {
                // Any CHARACTERS event counts as "not null" (distinguishes "" from null)
                if (!stack.isEmpty()) {
                    stack.peekLast().sawCharacters = true;
                }
                return t;
            }
            case END_ELEMENT: {
                // Pop the frame for the element that just ended
                Frame f = stack.pollLast();

                // If it's a leaf (no child starts, no chars), treat as JSON null
                // (Apply this wrapper only for JSON driver so XML semantics are not changed.)
                if (f != null && !f.sawChildStart && !f.sawCharacters) {
                    // Inject virtual CHARACTERS marker before END
                    pendingEnd = true;
                    virtualEventType = CHARACTERS;
                    return CHARACTERS;
                }

                return t;
            }
            default:
                return t;
        }
    }

    @Override
    public int getEventType() {
        return virtualEventType != null ? virtualEventType : super.getEventType();
    }

    @Override
    public String getText() {
        if (virtualEventType != null && virtualEventType == CHARACTERS) {
            return NULL_MARKER;
        }
        return super.getText();
    }

    @Override
    public char[] getTextCharacters() {
        String t = getText();
        return t != null ? t.toCharArray() : new char[0];
    }

    @Override
    public int getTextLength() {
        String t = getText();
        return t != null ? t.length() : 0;
    }

    @Override
    public int getTextStart() {
        return 0;
    }
}
