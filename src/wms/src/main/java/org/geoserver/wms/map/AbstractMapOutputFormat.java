/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.wms.decoration.MapDecorationLayout.FF;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WatermarkInfo;
import org.geoserver.wms.decoration.MapDecoration;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.decoration.MetatiledMapDecorationLayout;
import org.geoserver.wms.decoration.WatermarkDecoration;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.springframework.util.Assert;

/**
 * Base class for formats that do actually draw a map
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Gabriel Roldan
 */
public abstract class AbstractMapOutputFormat implements GetMapOutputFormat {
    /** A logger for this class. */
    public static final Logger LOGGER = Logging.getLogger(GetMapOutputFormat.class);

    private final String mime;

    private final Set<String> outputFormatNames;
    protected WMS wms;

    protected AbstractMapOutputFormat(final String mime) {
        this(mime, new String[] {mime});
    }

    protected AbstractMapOutputFormat(final String mime, final String... outputFormats) {
        this(
                mime,
                outputFormats == null
                        ? Collections.emptySet()
                        : new HashSet<>(Arrays.asList(outputFormats)));
    }

    protected AbstractMapOutputFormat(final String mime, Set<String> outputFormats) {
        Assert.notNull(mime, "mime");
        this.mime = mime;
        if (outputFormats == null) {
            outputFormats = Collections.emptySet();
        }

        Set<String> formats = caseInsensitiveOutputFormats(outputFormats);
        formats.add(mime);
        this.outputFormatNames = Collections.unmodifiableSet(formats);
    }

    private static Set<String> caseInsensitiveOutputFormats(Set<String> outputFormats) {
        Set<String> caseInsensitiveFormats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveFormats.addAll(outputFormats);
        return caseInsensitiveFormats;
    }

    protected AbstractMapOutputFormat() {
        this(null, (String[]) null);
    }

    /** @see GetMapOutputFormat#getMimeType() */
    @Override
    public String getMimeType() {
        return mime;
    }

    /** @see GetMapOutputFormat#getOutputFormatNames() */
    @Override
    public Set<String> getOutputFormatNames() {
        return outputFormatNames;
    }

    protected MapDecorationLayout findDecorationLayout(GetMapRequest request, final boolean tiled) {
        if (wms == null) {
            throw new IllegalAccessError();
        }
        String layoutName = null;
        if (request.getFormatOptions() != null) {
            layoutName = (String) request.getFormatOptions().get("layout");
        }

        MapDecorationLayout layout = null;
        if (layoutName != null && !layoutName.trim().isEmpty()) {
            try {
                GeoServerResourceLoader loader = wms.getCatalog().getResourceLoader();
                Resource layouts = loader.get("layouts");
                if (layouts.getType() == Type.DIRECTORY) {
                    Resource layoutConfig = layouts.get(layoutName + ".xml");

                    if (layoutConfig.getType() == Type.RESOURCE) {
                        layout = MapDecorationLayout.fromFile(layoutConfig, tiled);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown layout requested: " + layoutName);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No layouts directory defined");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to load layout: " + layoutName, e);
            }

            if (layout == null) {
                throw new ServiceException("Could not find decoration layout named: " + layoutName);
            }
        }

        if (layout == null) {
            layout = tiled ? new MetatiledMapDecorationLayout() : new MapDecorationLayout();
        }

        MapDecorationLayout.Block watermark = getWatermark(wms.getServiceInfo());
        if (watermark != null) {
            layout.addBlock(watermark);
        }

        return layout;
    }

    public static MapDecorationLayout.Block getWatermark(WMSInfo wms) {
        WatermarkInfo watermark = (wms == null ? null : wms.getWatermark());
        if (watermark != null && watermark.isEnabled()) {
            Map<String, Expression> options = new HashMap<>();
            options.put("url", FF.literal(watermark.getURL()));
            options.put("opacity", FF.literal((255f - watermark.getTransparency()) / 2.55f));

            MapDecoration d = new WatermarkDecoration();
            try {
                d.loadOptions(options);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't construct watermark from configuration", e);
                throw new ServiceException(e);
            }

            MapDecorationLayout.Block.Position p = null;

            switch (watermark.getPosition()) {
                case TOP_LEFT:
                    p = MapDecorationLayout.Block.Position.UL;
                    break;
                case TOP_CENTER:
                    p = MapDecorationLayout.Block.Position.UC;
                    break;
                case TOP_RIGHT:
                    p = MapDecorationLayout.Block.Position.UR;
                    break;
                case MID_LEFT:
                    p = MapDecorationLayout.Block.Position.CL;
                    break;
                case MID_CENTER:
                    p = MapDecorationLayout.Block.Position.CC;
                    break;
                case MID_RIGHT:
                    p = MapDecorationLayout.Block.Position.CR;
                    break;
                case BOT_LEFT:
                    p = MapDecorationLayout.Block.Position.LL;
                    break;
                case BOT_CENTER:
                    p = MapDecorationLayout.Block.Position.LC;
                    break;
                case BOT_RIGHT:
                    p = MapDecorationLayout.Block.Position.LR;
                    break;
                default:
                    throw new ServiceException(
                            "Unknown WatermarkInfo.Position value.  Something is seriously wrong.");
            }

            return new MapDecorationLayout.Block(d, p, null, new Point(0, 0));
        }

        return null;
    }
}
