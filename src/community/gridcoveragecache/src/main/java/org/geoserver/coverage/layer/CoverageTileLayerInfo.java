/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.layer;

import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;

import javax.imageio.ImageWriteParam;
import javax.media.jai.Interpolation;

import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geotools.coverage.grid.io.OverviewPolicy;

import com.sun.media.jai.util.InterpAverage;

public interface CoverageTileLayerInfo extends GeoServerTileLayerInfo {

    public enum SeedingPolicy {
        DIRECT, RECURSIVE;
    }

    public enum InterpolationType {
        NEAREST {
            @Override
            public Interpolation getInterpolationObject() {
                return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            }
        }, BILINEAR {
            @Override
            public Interpolation getInterpolationObject() {
                return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            }
        }, BICUBIC {
            @Override
            public Interpolation getInterpolationObject() {
                return Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            }
        }, BICUBIC2 {
            @Override
            public Interpolation getInterpolationObject() {
                return Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            }
        }, AVERAGE {
            @Override
            public Interpolation getInterpolationObject() {
                return new InterpAverage(DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_SIZE);
            }
        };

        public static final int DEFAULT_BLOCK_SIZE = 3;

        public abstract Interpolation getInterpolationObject();
    }

    public enum TiffCompression {
        NONE {
            @Override
            public ImageWriteParam getCompressionParams() {
                return null;
            }
        },
        LZW {
            @Override
            public ImageWriteParam getCompressionParams() {
                TIFFImageWriteParam writeParam = new TIFFImageWriteParam(null);
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType("LZW");
                return writeParam;
            }
        },
        DEFLATE {
            @Override
            public ImageWriteParam getCompressionParams() {
                TIFFImageWriteParam writeParam = new TIFFImageWriteParam(null);
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType("Deflate");
                return writeParam;
            }

        },
        JPEG {
            @Override
            public ImageWriteParam getCompressionParams() {
                TIFFImageWriteParam writeParam = new TIFFImageWriteParam(null);
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType("JPEG");
                return writeParam;
            }

        };

        public abstract ImageWriteParam getCompressionParams();
    }

    public void setInterpolationType(InterpolationType interpolation);

    public InterpolationType getInterpolationType();

    public void setSeedingPolicy(SeedingPolicy seedingPolicy);

    public SeedingPolicy getSeedingPolicy();

    public void setTiffCompression(TiffCompression tiffCompression);

    public TiffCompression getTiffCompression();

    public void setOverviewPolicy(OverviewPolicy overviewPolicy);

    public OverviewPolicy getOverviewPolicy();
}
