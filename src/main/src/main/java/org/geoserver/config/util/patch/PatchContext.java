/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.util.PropertyCopyPolicy;

/**
 * Context for tracking REST patch operations, allowing to determine if a property was explicitly set to null in the XML
 * or JSON (as XStream turns nulls into empty strings). For a complete picture of the problem see the package-info of
 * org.geoserver.config.util.patch.
 */
public final class PatchContext {

    /**
     * Copy policy to be used during REST patch processing, which copies non-null values and also null values if they
     * were explicitly set to null in the XML/JSON (as opposed to properties that are missing from the XML/JSON and
     * should be ignored).
     */
    private static class PatchCopyPolicy implements PropertyCopyPolicy {
        @Override
        public boolean shouldCopy(String propertyName, Object source, Object target, Object newValue) {
            PatchContext pc = PatchContext.get();
            if (pc == null) return newValue != null;

            if (newValue != null) {
                return true;
            }

            return pc.isExplicitNull(source.getClass(), propertyName);
        }

        @Override
        public Object mapValue(String propertyName, Object source, Object target, Object newValue) {
            PatchContext pc = PatchContext.get();

            if (pc != null && pc.isExplicitNull(source.getClass(), propertyName)) {
                return null;
            }

            return newValue;
        }
    }

    public static final PropertyCopyPolicy PATCH_COPY_POLICY = new PatchCopyPolicy();

    private static final ThreadLocal<PatchContext> TL = new ThreadLocal<>();

    /** Checks if the current thread is processing a REST patch operation, i.e. if a PatchContext is active. */
    public static boolean isActive() {
        return TL.get() != null;
    }

    /** Gets the current PatchContext for the thread, or null if not active. Only valid during REST patch processing. */
    public static PatchContext get() {
        return TL.get();
    }

    /**
     * Starts a new PatchContext for the current thread. Should be called at the beginning of REST patch processing, and
     * stop() should be called at the end to clean up the ThreadLocal.
     */
    public static void start() {
        TL.set(new PatchContext());
    }

    /**
     * Stops the current PatchContext for the thread, cleaning up the ThreadLocal. Should be called at the end of REST
     * patch processing.
     */
    public static void stop() {
        TL.remove();
    }

    /**
     * The current serialized path being processed, used to correlate the XML/JSON path with the real member being set
     * in PatchTrackingMapper.
     */
    private String currentSerializedPath;

    /**
     * Mapping of serialized paths to the corresponding member keys (owner class + real member name) being set in
     * PatchTrackingMapper. Used to determine which member is being set when PatchPathTrackingReader detects an explicit
     * null value in the XML/JSON.
     */
    private final Map<String, MemberKey> pathToMember = new HashMap<>();

    /**
     * Set of member keys (owner class + real member name) that were detected as explicitly set to null in the XML/JSON.
     * Used to determine if a property was explicitly set to null when applying the patch.
     */
    private final Set<MemberKey> explicitNullMembers = new HashSet<>();

    /**
     * Since PatchTrackingMapper.realMember is called after the value is deserialized, we may detect explicit nulls in
     * the XML/JSON before we know which member they correspond to. In that case we keep track of the serialized paths
     * that were detected as explicit null, and once we resolve the member for that path we can mark it as explicitly
     * null.
     */
    private final Set<String> pendingNullPaths = new HashSet<>();

    /**
     * During a REST PUT operation (with patch semantic) we track if the original value of a property was null or not,
     * so that if the update value is null we can decide whether to ignore it (if the original value was not null) or
     * set it to null (if the original value was explicitly null). This method returns the copy policy to be used,
     * depending on whether we are in a patch context or not.
     */
    public static PropertyCopyPolicy getCopyPolicy() {
        return PatchContext.isActive() ? PatchContext.PATCH_COPY_POLICY : PropertyCopyPolicy.DEFAULT_POLICY;
    }

    /**
     * Sets the current serialized path being processed, used to correlate with the real member being set in
     * PatchTrackingMapper.
     */
    public void setCurrentSerializedPath(String path) {
        this.currentSerializedPath = path;
    }

    /**
     * Gets the current serialized path being processed, or null if not set. Used to correlate with the real member
     * being set in PatchTrackingMapper.
     */
    public String getCurrentSerializedPath() {
        return currentSerializedPath;
    }

    /**
     * Registers the real member (owner class + member name) corresponding to the current XML/JSON serialized path being
     * processed.
     */
    public void registerCurrentMember(Class<?> ownerType, String realMember) {
        if (currentSerializedPath == null) return;

        MemberKey key = new MemberKey(ownerType, realMember);
        pathToMember.put(currentSerializedPath, key);

        // resolve pending explicit nulls
        if (pendingNullPaths.remove(currentSerializedPath)) {
            explicitNullMembers.add(key);
        }
    }

    /**
     * Marks the current serialized path being processed as explicitly set to null in the XML/JSON. If the real member
     * corresponding to the current path is already known, it is marked as explicitly null immediately. Otherwise, the
     * path is added to pendingNullPaths and will be resolved once the member is registered.
     */
    public void markCurrentAsExplicitNull() {
        if (currentSerializedPath == null) return;

        MemberKey key = pathToMember.get(currentSerializedPath);
        if (key != null) {
            explicitNullMembers.add(key);
        } else {
            pendingNullPaths.add(currentSerializedPath);
        }
    }

    /**
     * Checks if the given member (owner class + member name) was explicitly set to null in the XML/JSON, based on the
     * information collected during patch processing.
     *
     * @return
     */
    public boolean isExplicitNull(Class<?> ownerType, String property) {
        if (property == null || property.isEmpty()) return false;
        if (Character.isUpperCase(property.charAt(0)) && !StringUtils.isAllUpperCase(property)) {
            // OWSUtils uses property names starting with uppercase, normalize it
            property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
        }
        return explicitNullMembers.contains(new MemberKey(ownerType, property));
    }

    /** Simple key class for identifying a member by its owner class and property name */
    private static final class MemberKey {
        private final Class<?> owner;
        private final String property;

        MemberKey(Class<?> owner, String property) {
            this.owner = owner;
            this.property = property;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MemberKey)) return false;
            MemberKey other = (MemberKey) o;
            return owner == other.owner && Objects.equals(property, other.property);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(owner) * 31 + property.hashCode();
        }
    }
}
