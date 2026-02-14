/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

/** Holds metatile related context information in a thread local variable. */
public class MetatileContextHolder {

    private MetatileContextHolder() {}

    /** Meta information about the metatile being processed. */
    public static final class MetaInfo {
        final int width;
        final int height;
        final int tileSize;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getTileSize() {
            return tileSize;
        }

        public MetaInfo(int w, int h, int ts) {
            this.width = w;
            this.height = h;
            this.tileSize = ts;
        }
    }

    private static final ThreadLocal<MetaInfo> TL = new ThreadLocal<>();

    public static void set(MetaInfo mi) {
        TL.set(mi);
    }

    public static MetaInfo get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
