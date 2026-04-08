/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.imagen.media.jiffleop.JiffleDescriptor;
import org.eclipse.imagen.media.range.Range;
import org.eclipse.imagen.media.range.RangeFactory;
import org.geoserver.pngwind.PngWindConstants.BandType;
import org.geoserver.pngwind.config.PngWindConfig;
import org.geoserver.pngwind.config.PngWindConfig.DirectionConvention;
import org.geoserver.pngwind.config.PngWindConfig.DirectionUnit;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Utility class to transform a 2-band image containing wind data in various forms (U/V, speed/direction) into a
 * standard U/V form. The transformation is based on heuristics applied to the band names, and can handle a variety of
 * common naming conventions. If the bands cannot be classified, the original image is returned as-is, with a warning
 * logged, assuming it is already in U/V form in the order of the bands.
 */
public final class PngWindTransform {

    private static class BandPair {
        BandType b1;

        BandType b2;

        @Override
        public String toString() {
            return "BandPair{" + "b1=" + b1 + ", b2=" + b2 + '}';
        }

        public BandPair(BandType b1, BandType b2) {
            this.b1 = b1;
            this.b2 = b2;
        }

        public static BandPair toKind(Kind kind) {
            return switch (kind) {
                case UV -> new BandPair(BandType.U, BandType.V);
                case VU -> new BandPair(BandType.V, BandType.U);
                case SPEED_DIR -> new BandPair(BandType.SPEED, BandType.DIRECTION);
            };
        }

        public boolean isUnknown() {
            return b1 == BandType.UNKNOWN && b2 == BandType.UNKNOWN;
        }

        public boolean isUV() {
            return (b1 == BandType.U && b2 == BandType.V) || (b1 == BandType.V && b2 == BandType.U);
        }

        public Kind getUVFamilyKind() {
            return b1 == BandType.U ? Kind.UV : Kind.VU;
        }

        public boolean isPolar() {
            return (b1 == BandType.SPEED && b2 == BandType.DIRECTION)
                    || (b1 == BandType.DIRECTION && b2 == BandType.SPEED);
        }
    }

    private final PngWindConfig config;

    public PngWindTransform(PngWindConfig config) {
        this.config = config;
    }

    public boolean isSpeed(String n) {
        return config.getBandMatching().getSpeed().matches(n);
    }

    public boolean isDir(String n) {
        return config.getBandMatching().getDirection().matches(n);
    }

    public boolean isU(String n) {
        return config.getBandMatching().getU().matches(n);
    }

    public boolean isV(String n) {
        return config.getBandMatching().getV().matches(n);
    }

    private static final Logger LOGGER = Logging.getLogger(PngWindTransform.class);

    public enum Kind {
        UV,
        VU,
        SPEED_DIR
    }

    /**
     * Container for the result of the transformation, including the resulting image and the kind of transformation
     * applied (if any).
     */
    public static class PngWindTransformResult {
        private final RenderedImage uv;
        private final Kind kind;

        public PngWindTransformResult(RenderedImage uv, Kind kind) {
            this.uv = uv;
            this.kind = kind;
        }

        public RenderedImage getUv() {
            return uv;
        }

        public Kind getKind() {
            return kind;
        }
    }

    /**
     * Transforms the given 2-band image containing wind data into U/V form if needed, based on heuristics applied to
     * the band names in the provided request context. The heuristics can classify the bands as U/V or speed/direction.
     *
     * @param twoBands
     * @param ctx
     * @return
     */
    public PngWindTransformResult toUV(RenderedImage twoBands, PngWindRequestContext ctx) {
        String n1 = normalize(ctx.getBand1().getName());
        String n2 = normalize(ctx.getBand2().getName());
        Kind defaultKind = Kind.UV;

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Detecting the band types for bands [" + n1 + ", " + n2 + "]");
        }
        BandPair pair = new BandPair(classify(n1), classify(n2));

        // If both bands are unknown, try the heuristic of finding
        // a single position where they differ only by 'u' vs 'v'
        if (pair.isUnknown()) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Fall back on paired u/v bands with similar names heuristic");
            }
            int pos = findUvPosition(n1, n2);
            if (pos >= 0) {
                if (n1.charAt(pos) == 'u' && n2.charAt(pos) == 'v') {
                    pair = BandPair.toKind(Kind.UV);
                } else if (n1.charAt(pos) == 'v' && n2.charAt(pos) == 'u') {
                    pair = BandPair.toKind(Kind.VU);
                }
            }
        }

        // Case A: already U/V (any order)
        if (pair.isUV()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Image is U/V family type. No transformation needed: " + pair);
            }
            // Note that in case of V/U order we keep the original order
            // and just mark it in the kind for downstream to handle
            return new PngWindTransformResult(twoBands, pair.getUVFamilyKind());
        }

        // Case B: speed/direction (any order)
        if (pair.isPolar()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Image is SPEED/DIR type. Proceeding to U/V transformation");
            }
            RenderedImage uv = transformToUV(twoBands, ctx, pair.b1);
            return new PngWindTransformResult(uv, Kind.SPEED_DIR);
        }

        LOGGER.warning("PNG-WIND: unable to classify bands [" + n1 + ", " + n2 + "], got types [" + pair.b1 + ", "
                + pair.b2 + "], assuming they are U/V in that order");
        return new PngWindTransformResult(twoBands, defaultKind);
    }

    private RenderedImage transformToUV(RenderedImage twoBands, PngWindRequestContext ctx, BandType r1) {
        boolean isSpeedFirst = r1 == BandType.SPEED;
        // Get Nodata
        Double b1Nodata = ctx.getBand1().getNodata();
        Double b2Nodata = ctx.getBand2().getNodata();
        Double speedNodata = isSpeedFirst ? b1Nodata : b2Nodata;
        Double dirNodata = isSpeedFirst ? b2Nodata : b1Nodata;

        RenderedImage speed = new ImageWorker(twoBands)
                .retainBands(new int[] {isSpeedFirst ? 0 : 1})
                .getRenderedImage();
        RenderedImage dir = new ImageWorker(twoBands)
                .retainBands(new int[] {isSpeedFirst ? 1 : 0})
                .getRenderedImage();
        String dirUom = isSpeedFirst ? ctx.getBand2().getUom() : ctx.getBand1().getUom();
        DirectionUnit dirUnit =
                dirUom != null && dirUom.toLowerCase(Locale.ROOT).contains("deg")
                        ? DirectionUnit.DEG
                        : config.getDirectionUnit();
        return polarToUV(speed, dir, config.getDirectionConvention(), dirUnit, speedNodata, dirNodata);
    }

    /**
     * Heuristic to find a single position where the two band names differ only by 'u' vs 'v', to help classify them as
     * U/V when other heuristics fail. i.e. hourly_u10m vs hourly_v10m, etc.
     *
     * @param b1
     * @param b2
     * @return
     */
    private int findUvPosition(String b1, String b2) {
        if (b1.length() != b2.length()) {
            return -1;
        }

        int uvPos = -1;

        for (int i = 0; i < b1.length(); i++) {
            char cb1 = b1.charAt(i);
            char cb2 = b2.charAt(i);

            if (cb1 == cb2) {
                continue;
            }

            if ((cb1 == 'u' && cb2 == 'v') || (cb1 == 'v' && cb2 == 'u')) {
                if (uvPos != -1) {
                    return -1;
                }
                uvPos = i;
            } else {
                return -1;
            }
        }

        return uvPos;
    }

    private BandType classify(String n) {
        // Fast path: direct match with enum names
        String t = n.trim().toUpperCase();
        switch (t) {
            case "U":
                return BandType.U;
            case "V":
                return BandType.V;
            case "SPEED":
                return BandType.SPEED;
            case "DIRECTION":
                return BandType.DIRECTION;
        }

        // Otherwise, apply the configured heuristics
        if (isU(n)) return BandType.U;
        if (isV(n)) return BandType.V;
        if (isSpeed(n)) return BandType.SPEED;
        if (isDir(n)) return BandType.DIRECTION;
        return BandType.UNKNOWN;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("[^a-z0-9]+", "_");
        t = t.replaceAll("_+", "_");
        if (t.startsWith("_")) t = t.substring(1);
        if (t.endsWith("_")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static RenderedImage polarToUV(
            RenderedImage speed,
            RenderedImage dir,
            DirectionConvention dirConvention,
            DirectionUnit dirUnit,
            Double speedNoData,
            Double dirNoData) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Transforming polar representation (speed/dir) to vectorial (u/v)");
        }
        // Sources visible to the script as "spd" and "dir"
        RenderedImage[] sources = {speed, dir};
        String[] sourceNames = {"spd", "dir"};

        String destName = "dest";

        // NOTE: write to dest[0] and dest[1] for multi-band output
        // theta in radians
        // FROM: u = -spd*sin(theta), v = -spd*cos(theta)
        // TO:   u =  spd*sin(theta), v =  spd*cos(theta)
        String thetaExpr = dirUnit == DirectionUnit.DEG ? "(dir * 3.141592653589793) / 180.0" : "dir";

        String sign = dirConvention == DirectionConvention.FROM ? "-" : "";

        String script = "theta = " + thetaExpr + ";\n" + "dest[0] = "
                + sign + "spd * sin(theta);\n" + "dest[1] = "
                + sign + "spd * cos(theta);\n";

        // Optional per-source nodata ranges (same order as sources[])
        Range[] noData = (speedNoData != null && dirNoData != null)
                ? new Range[] {
                    RangeFactory.create(speedNoData, true, speedNoData, true, true),
                    RangeFactory.create(dirNoData, true, dirNoData, true, true)
                }
                : null;

        return JiffleDescriptor.create(
                sources, sourceNames, destName, script, null, DataBuffer.TYPE_FLOAT, 2, null, null, noData, null);
    }
}
