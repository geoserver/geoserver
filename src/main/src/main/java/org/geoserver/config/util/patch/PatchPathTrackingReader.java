/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.ReaderWrapper;
import com.thoughtworks.xstream.io.path.PathTracker;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Reader wrapper that tracks the current path in the XML document using PathTracker, and updates the PatchContext with
 * the current path on each moveDown/moveUp. Also tracks xsi:nil="true" attributes to mark explicit nulls in the
 * PatchContext, while preserving existing JSON null handling in collaboration with the {@link NullMarkingStreamReader}.
 */
class PatchPathTrackingReader extends ReaderWrapper {

    private final PathTracker pathTracker;
    private final Deque<Boolean> nilStack = new ArrayDeque<>();

    public PatchPathTrackingReader(HierarchicalStreamReader reader) {
        super(reader);
        this.pathTracker = new PathTracker();
        pathTracker.pushElement(getNodeName());
        // We are at START_TAG for the root here
        nilStack.addLast(isXsiNilTrue());
        sync();
    }

    @Override
    public void moveDown() {
        super.moveDown();
        pathTracker.pushElement(getNodeName());
        boolean nil = isXsiNilTrue();
        nilStack.addLast(nil);
        sync();

        // if explicitly marked as nil, notify the patch context
        PatchContext pc = PatchContext.get();
        if (pc != null && nil) {
            pc.markCurrentAsExplicitNull();
        }
    }

    @Override
    public void moveUp() {
        super.moveUp();
        pathTracker.popElement();
        if (!nilStack.isEmpty()) {
            nilStack.removeLast();
        }
        sync();
    }

    private void sync() {
        PatchContext pc = PatchContext.get();
        if (pc != null) {
            pc.setCurrentSerializedPath(pathTracker.getPath().toString());
        }
    }

    @Override
    public String getValue() {
        String value = super.getValue();

        // JSON marker handling still OK here (no attribute access involved)
        PatchContext pc = PatchContext.get();
        if (pc != null && NullMarkingStreamReader.NULL_MARKER.equals(value)) {
            pc.markCurrentAsExplicitNull();
            return null;
        }

        return value;
    }

    private boolean isXsiNilTrue() {
        try {
            // Depending on namespace handling, attribute can appear as "xsi:nil" or "nil"
            String nil = getAttribute("xsi:nil");
            if (nil == null) {
                nil = getAttribute("nil");
            }
            if (nil == null) {
                return false;
            }
            return "true".equalsIgnoreCase(nil) || "1".equals(nil);
        } catch (IndexOutOfBoundsException e) {
            // Defensive: if some reader implementation ends up not being on START_TAG,
            // treat as "not nil" rather than fail request parsing.
            return false;
        }
    }
}
