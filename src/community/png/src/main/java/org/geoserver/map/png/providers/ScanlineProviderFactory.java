package org.geoserver.map.png.providers;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

public class ScanlineProviderFactory {

    public static ScanlineProvider getProvider(RenderedImage image) {
        ColorModel cm = image.getColorModel();
        SampleModel sm = image.getSampleModel();

        Raster raster;
        if (image instanceof BufferedImage) {
            raster = ((BufferedImage) image).getRaster();
            // in case the raster has a parent, this is likely a subimage, we have to force
            // a copy of the raster to get a data buffer we can scroll over without issues
            if(raster.getParent() != null) {
                raster = image.getData(new Rectangle(0, 0, raster.getWidth(), raster.getHeight()));
            }
        } else {
            // TODO: we could build a tile oriented reader that fetches tiles in parallel here
            raster = image.getData();
        }

        // grab the right scanline extractor based on image features
        if (cm instanceof ComponentColorModel && sm.getDataType() == DataBuffer.TYPE_BYTE) {
            if (sm.getNumBands() == 3 || sm.getNumBands() == 4) {
                return new RasterByteABGRProvider(raster, cm.hasAlpha());
            } else if (sm.getNumBands() == 2 && cm.hasAlpha()) {
                return new RasterByteSingleBandProvider(raster, 8, 2 * raster.getWidth());
            } else if (sm.getNumBands() == 1) {
                if (sm.getDataType() == DataBuffer.TYPE_BYTE) {
                    if (sm instanceof MultiPixelPackedSampleModel) {
                        if (cm.getPixelSize() == 8) {
                            return new RasterByteSingleBandProvider(raster, 8, raster.getWidth());
                        } else if (cm.getPixelSize() == 4) {
                            int scanlineLength = (raster.getWidth() + 1) / 2;
                            return new RasterByteSingleBandProvider(raster, 4, scanlineLength);
                        } else if (cm.getPixelSize() == 2) {
                            int scanlineLength = (raster.getWidth() + 2) / 4;
                            return new RasterByteSingleBandProvider(raster, 2, scanlineLength);
                        } else if (cm.getPixelSize() == 1) {
                            int scanlineLength = (raster.getWidth() + 4) / 8;
                            return new RasterByteSingleBandProvider(raster, 1, scanlineLength);
                        }
                    } else {
                        if (cm.getPixelSize() == 8) {
                            return new RasterByteSingleBandProvider(raster, 8, raster.getWidth());
                        } else if (cm.getPixelSize() == 4) {
                            int scanlineLength = (raster.getWidth() + 1) / 2;
                            return new RasterByteRepackSingleBandProvider(raster, 4,
                                    scanlineLength);
                        } else if (cm.getPixelSize() == 2) {
                            int scanlineLength = (raster.getWidth() + 2) / 4;
                            return new RasterByteRepackSingleBandProvider(raster, 2,
                                    scanlineLength);
                        } else if (cm.getPixelSize() == 1) {
                            int scanlineLength = (raster.getWidth() + 4) / 8;
                            return new RasterByteRepackSingleBandProvider(raster, 1,
                                    scanlineLength);
                        }
                    }
                }
            }
        } else if (cm instanceof ComponentColorModel && sm.getDataType() == DataBuffer.TYPE_USHORT) {
            if (sm.getNumBands() == 3 || sm.getNumBands() == 4) {
                return new RasterShortABGRProvider(raster, cm.hasAlpha());
            } else if (sm.getNumBands() == 2 && cm.hasAlpha()) {
                return new RasterShortGrayAlphaProvider(raster);
            } else if (sm.getNumBands() == 1) {
                return new RasterShortSingleBandProvider(raster);
            }
        } else if (cm instanceof DirectColorModel && sm.getDataType() == DataBuffer.TYPE_INT) {
            if (sm.getNumBands() == 3 || sm.getNumBands() == 4) {
                return new RasterIntABGRProvider(raster, cm.hasAlpha());
            }
        } else if (cm instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) cm;
            if (sm.getDataType() == DataBuffer.TYPE_BYTE) {
                if (sm instanceof MultiPixelPackedSampleModel) {
                    if (icm.getPixelSize() == 8) {
                        return new RasterByteSingleBandProvider(raster, 8, raster.getWidth(), icm);
                    } else if (icm.getPixelSize() == 4) {
                        int scanlineLength = (raster.getWidth() + 1) / 2;
                        return new RasterByteSingleBandProvider(raster, 4, scanlineLength, icm);
                    } else if (icm.getPixelSize() == 2) {
                        int scanlineLength = (raster.getWidth() + 2) / 4;
                        return new RasterByteSingleBandProvider(raster, 2, scanlineLength, icm);
                    } else if (icm.getPixelSize() == 1) {
                        int scanlineLength = (raster.getWidth() + 4) / 8;
                        return new RasterByteSingleBandProvider(raster, 1, scanlineLength, icm);
                    }
                } else {
                    if (icm.getPixelSize() == 8) {
                        return new RasterByteSingleBandProvider(raster, 8, raster.getWidth(), icm);
                    } else if (icm.getPixelSize() == 4) {
                        int scanlineLength = (raster.getWidth() + 1) / 2;
                        return new RasterByteRepackSingleBandProvider(raster, 4, scanlineLength,
                                icm);
                    } else if (icm.getPixelSize() == 2) {
                        int scanlineLength = (raster.getWidth() + 2) / 4;
                        return new RasterByteRepackSingleBandProvider(raster, 2, scanlineLength,
                                icm);
                    } else if (icm.getPixelSize() == 1) {
                        int scanlineLength = (raster.getWidth() + 4) / 8;
                        return new RasterByteRepackSingleBandProvider(raster, 1, scanlineLength,
                                icm);
                    }
                }
            } 
        } else if (sm.getDataType() == DataBuffer.TYPE_USHORT) {
            return new RasterShortSingleBandProvider(raster, (IndexColorModel) cm);
        }

        throw new IllegalArgumentException("Unsupported image type: " + image);

    }
}
