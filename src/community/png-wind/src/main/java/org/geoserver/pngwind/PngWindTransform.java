package org.geoserver.pngwind;

import java.awt.image.RenderedImage;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

public final class PngWindTransform {

    private static final Logger LOGGER = Logging.getLogger(PngWindTransform.class);

    public enum Kind { UV, VU, SPEED_DIR }

    public static class PngWindTransformResult {


        private final RenderedImage uv;
        private final Kind kind;
        private final String uName;
        private final String vName;

        public PngWindTransformResult(RenderedImage uv, Kind kind, String uName, String vName) {
            this.uv = uv;
            this.kind = kind;
            this.uName = uName;
            this.vName = vName;
        }

        public RenderedImage getUv() { return uv; }
        public Kind getKind() { return kind; }
        public String getUName() { return uName; }
        public String getVName() { return vName; }
    }

    public enum DirConvention { FROM, TO }
    public enum DirUnit { DEG, RAD }

    private static final Set<String> SPEED_EXACT = Set.of("speed", "wind_speed", "windspeed", "ws");
    private static final Set<String> DIR_EXACT   = Set.of("direction", "wind_direction", "winddir", "wd");

    public PngWindTransformResult toUvIfNeeded(RenderedImage twoBands, String band1Name, String band2Name) {
        String n1 = norm(band1Name);
        String n2 = norm(band2Name);
        Kind defaultKind = Kind.UV;

        WindBandType r1 = classify(n1);
        WindBandType r2 = classify(n2);

        // Case A: already U/V (any order)
        if ((r1 == WindBandType.U && r2 == WindBandType.V) || (r1 == WindBandType.V && r2 == WindBandType.U)) {
            Kind kind = r1 == WindBandType.U ? defaultKind : Kind.VU;
            return new PngWindTransformResult(twoBands, kind, PngWindConstants.U, PngWindConstants.V);
        }

        // Case B: speed/direction (any order)
        if ((r1 == WindBandType.SPEED && r2 == WindBandType.DIR) || (r1 == WindBandType.DIR && r2 == WindBandType.SPEED)) {
            RenderedImage speed = new ImageWorker(twoBands).retainBands(new int[]{ r1 == WindBandType.SPEED ? 0 : 1 }).getRenderedImage();
            RenderedImage dir   = new ImageWorker(twoBands).retainBands(new int[]{ r1 == WindBandType.DIR   ? 0 : 1 }).getRenderedImage();

            DirConvention conv = PngWindConstants.DIR_CONVENTION;
            DirUnit unit = PngWindConstants.DIR_UNIT;

            RenderedImage uv = speedDirToUv(speed, dir, conv, unit);
            return new PngWindTransformResult(uv, Kind.SPEED_DIR, PngWindConstants.U, PngWindConstants.V);
        }

        LOGGER.warning("PNG-WIND: unable to classify bands [" + band1Name + ", " + band2Name + "], got types [" + r1 + ", " + r2 + "], assuming they are U/V in that order");
        return new PngWindTransformResult(twoBands, defaultKind, PngWindConstants.U, PngWindConstants.V);
    }

    private enum WindBandType { U, V, SPEED, DIR, UNKNOWN }

    private WindBandType classify(String n) {
        // exact-ish checks
        if (isU(n)) return WindBandType.U;
        if (isV(n)) return WindBandType.V;
        if (isSpeed(n)) return WindBandType.SPEED;
        if (isDir(n)) return WindBandType.DIR;
        return WindBandType.UNKNOWN;
    }

    private boolean isSpeed(String n) {
        if (SPEED_EXACT.contains(n)) return true;
        return n.contains("spd") || n.contains("wspd") || n.contains("wind_spd") || n.contains("wind_speed");
    }

    private boolean isDir(String n) {
        if (DIR_EXACT.contains(n)) return true;
        return n.contains("dir") || n.contains("wdir") || n.contains("wind_dir") || n.contains("wind_direction");
    }

    private boolean isU(String n) {
        // Keep these conservative to avoid “u” in “humidity” etc.
        if (n.equals("u") || n.equals("uwnd") || n.equals("ugrd")) return true;
        return n.contains("eastward_wind") || n.contains("u_component") || n.contains("_u_");
    }

    private boolean isV(String n) {
        if (n.equals("v") || n.equals("vwnd") || n.equals("vgrd")) return true;
        return n.contains("northward_wind") || n.contains("v_component") || n.contains("_v_");
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("[^a-z0-9]+", "_");
        t = t.replaceAll("_+", "_");
        if (t.startsWith("_")) t = t.substring(1);
        if (t.endsWith("_")) t = t.substring(0, t.length() - 1);
        return t;
    }

    /**
     * Convert speed + direction to a 2-band image [U, V].
     *
     * Convention:
     * - FROM: meteorological (direction wind comes FROM, clockwise from North)
     * - TO: direction wind blows TO
     *
     * Units:
     * - DEG or RAD
     *
     * You’ll implement this with JAI/JAI-ext algebra ops (preferred) or a tiny custom OpImage.
     */
    private RenderedImage speedDirToUv(RenderedImage speed, RenderedImage dir, DirConvention conv, DirUnit unit) {
        // Pseudocode math (theta in radians)
        // theta = (unit==DEG) ? dir * PI/180 : dir
        // if conv==FROM:
        //   u = -speed * sin(theta)
        //   v = -speed * cos(theta)
        // else (TO):
        //   u =  speed * sin(theta)
        //   v =  speed * cos(theta)

        // IMPORTANT: keep the output as float (or double) because you’ll later rescale to byte.
        // Also: propagate NoData (mask later already checks U/V nodata).

        return null;
    }
}