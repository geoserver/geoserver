/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.media.jai.RenderedImageList;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;

/**
 * The Frame Visitor contains the logic to produce frame images.<br>
 * The "visit" method initializes the runnables and the animatorExecutor service, while the
 * "produce" method runs the tasks and generated the frames images.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class FrameCatalogVisitor {

    /** total number of available frames for this visitor */
    int framesNumber;

    /** the list of runnables to be executed */
    List<Future<RenderedImage>> tasks;

    /**
     * Adds a new visitor to the runnables list and initializes the animatorExecutor service is not
     * yet initialied.
     */
    public void visit(
            final GetMapRequest request,
            WebMapService wms,
            WMS wmsConfiguration,
            String aparam,
            String avalue) {
        if (this.tasks == null) {
            this.tasks = new LinkedList<Future<RenderedImage>>();
        }

        FrameLoader loader = new FrameLoader(request, wms, wmsConfiguration, aparam, avalue);

        final FutureTask<RenderedImage> task = new FutureTask<RenderedImage>(loader);
        this.tasks.add(task);
        this.framesNumber++;

        // run the loading in this thread
        wmsConfiguration.getAnimatorExecutorService().execute(task);
    }

    /** Invokes the Executor service and produces the frames images. */
    public RenderedImageList produce(WMS wmsConfiguration) throws IOException {
        List<RenderedImage> images = new ArrayList<RenderedImage>();

        long gifAnimatedSize = 0;

        for (Future<RenderedImage> future : tasks) {
            RenderedImage image = null;
            try {
                image = future.get();
            } catch (InterruptedException e) {
                dispose();
                throw new IOException(e);
            } catch (ExecutionException e) {
                dispose();
                throw new IOException(e);
            }

            if (image == null) {
                continue;
            }

            // collect the images
            gifAnimatedSize += getImageSizeInBytes(image);
            if (wmsConfiguration.getMaxRenderingSize() != null
                    && gifAnimatedSize >= wmsConfiguration.getMaxRenderingSize()) {
                dispose();
                throw new IOException("Max rendering size exceed!");
            }

            images.add(image);
        }

        if (images == null || images.size() == 0) {
            dispose();
            throw new IOException("Empty list of frames.");
        }

        dispose();
        return new RenderedImageList(images);
    }

    private long getImageSizeInBytes(RenderedImage image) {
        int tileWidth = image.getTileWidth();
        int tileLength = image.getNumXTiles();
        int numBands = image.getSampleModel().getNumBands();
        int[] sampleSize = image.getSampleModel().getSampleSize();

        return (long) Math.ceil(2 * tileWidth * tileLength * numBands * (sampleSize[0] / 8.0));
    }

    /** Suddenly stops the Executor service and clear instantiated visitors. */
    private void dispose() {
        this.framesNumber = 0;

        if (this.tasks != null) this.tasks.clear();

        this.tasks = null;
    }
}

/**
 * FrameLoader Callable task.
 *
 * @author Alessio
 */
class FrameLoader implements Callable<RenderedImage> {

    /** The default output format for each frame if not specified in the request */
    private static final String GIF_FORMAT = "image/gif";

    private GetMapRequest request;

    private WebMapService wms;

    private WMS wmsConfiguration;

    private String aparam;

    private String avalue;

    /** Default constructor. */
    public FrameLoader(
            GetMapRequest request,
            WebMapService wms,
            WMS wmsConfiguration,
            String aparam,
            String avalue) {
        this.request = request;
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;
        this.aparam = aparam;
        this.avalue = avalue.replaceAll("\\\\,", ",");
    }

    @Override
    public RenderedImage call() throws Exception {
        org.geoserver.wms.WebMap wmsResponse;

        // Making a shallow copy of the original request and replacing param's values
        GetMapRequest frameRequest = replaceRequestParams(this.request, this.aparam, this.avalue);

        // set rest of the wms defaults
        frameRequest = DefaultWebMapService.autoSetMissingProperties(frameRequest);

        // Setup Frame OUTputFormat
        String outFormat = frameRequest.getFormat();

        // the capabilities of this produce are actually linked to the map response that is going to
        // be used, this class just generates a rendered image
        final Collection<RenderedImageMapResponse> responses =
                this.wmsConfiguration.getAvailableMapResponses();
        for (RenderedImageMapResponse response : responses) {
            if (response.getOutputFormats().contains(outFormat)) {
                MapProducerCapabilities cap = response.getCapabilities(outFormat);
                if (cap != null && cap.getFramesMimeType() != null) {
                    frameRequest.setFormat(cap.getFramesMimeType());
                } else {
                    frameRequest.setFormat(GIF_FORMAT);
                }
            }
        }

        wmsResponse = this.wms.getMap(frameRequest);

        return ((RenderedImageMap) wmsResponse).getImage();
    }

    /** Replacing WMS Request parameter's value */
    private static GetMapRequest replaceRequestParams(
            GetMapRequest theRequest, String param, String value) throws Exception {
        // look for the GetMapRequest reader
        GetMapKvpRequestReader kvpRequestReader =
                (GetMapKvpRequestReader) Dispatcher.findKvpRequestReader(GetMapRequest.class);
        // clone the original request object using the reflection
        GetMapRequest request = (GetMapRequest) BeanUtils.cloneBean(theRequest);

        // looking for composite parameters like env:color or viewparams:param ...
        Map<String, String> rawKvp =
                new CaseInsensitiveMap(new HashMap<String, String>(theRequest.getRawKvp()));
        if (param.contains(":")) {
            // going to replace composite param values for each frame in the KVP map
            String compositeParamKey = param.split(":")[0].toUpperCase();
            String simpleParamKey = param.split(":")[1].toUpperCase();
            List<String> kvps = null;
            if (rawKvp.get(compositeParamKey) != null) {
                kvps = KvpUtils.escapedTokens(rawKvp.get(compositeParamKey), ';');
                // purge old value
                Iterator<String> it = kvps.iterator();
                while (it.hasNext()) {
                    String k = it.next().toUpperCase();
                    if (k.toUpperCase().startsWith(simpleParamKey)) {
                        it.remove();
                    }
                }
            } else {
                kvps = new ArrayList<String>();
            }
            // insert the right one
            kvps.add(simpleParamKey + ":" + value);
            // merge back to the composite value
            rawKvp.remove(compositeParamKey);
            rawKvp.put(compositeParamKey, mergeParams(kvps));
        } else {
            // just a simple plain request parameter... replacing it on the KVP map
            // purge old value
            if (rawKvp.containsKey(param)) {
                rawKvp.remove(param);
            }
            // insert the frame one
            rawKvp.put(param, value);
        }

        // setting up the right RAW-KVP map for the single frame request
        request.setRawKvp(rawKvp);

        // building the request KVP map using the reflection
        HashMap<String, String> kvp = new HashMap<String, String>(rawKvp);
        KvpUtils.parse(kvp);

        // finally building the request
        request = kvpRequestReader.read(new GetMapRequest(), kvp, rawKvp);

        // add the param value for text decorations to use
        request.getEnv().put("avalue", value);

        return request;
    }

    private static String mergeParams(List<String> kvps) {
        StringBuilder sb = new StringBuilder();

        for (String k : kvps) {
            sb.append(k).append(";");
        }
        sb.deleteCharAt(sb.lastIndexOf(";"));
        return sb.toString();
    }
}
