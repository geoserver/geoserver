/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class LayerResource extends BaseResource {

    static Logger LOGGER = Logging.getLogger(LayerResource.class);

    public LayerResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ItemLayerJSONFormat(MediaType.APPLICATION_JSON),
                new ItemLayerJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        ImportTask task = task();
        getResponse().setEntity(getFormatGet().toRepresentation(task.getLayer()));
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        ImportTask task = task();

        LayerInfo layer = (LayerInfo) getFormatPostOrPut().toObject(getRequest().getEntity());
        
        updateLayer(task, layer, importer);
        importer.changed(task);

        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
        getResponse().setEntity(getFormatGet().toRepresentation(task()));
    }
    
    static void updateLayer(ImportTask orig, LayerInfo l, Importer importer) {
        //update the original layer and resource from the new

        ResourceInfo r = l.getResource();
        
        //TODO: this is not thread safe, clone the object before overwriting it
        //save the existing resource, which will be overwritten below,  
        ResourceInfo resource = orig.getLayer().getResource();

        if (r != null) {
            // we support the following resource info properties:
            // (don't just use blindly copy everything)
            if (r.getTitle() != null) {
                resource.setTitle(r.getTitle());
            }
            if (r.getAbstract() != null) {
                resource.setAbstract(r.getAbstract());
            }
            if (r.getDescription() != null) {
                resource.setDescription(r.getDescription());
            }
        }
        
        CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
        l.setResource(resource);
        // @hack workaround OWSUtils bug - trying to copy null collections
        // why these are null in the first place is a different question
        LayerInfoImpl impl = (LayerInfoImpl) orig.getLayer();
        if (impl.getAuthorityURLs() == null) {
            impl.setAuthorityURLs(new ArrayList(1));
        }
        if (impl.getIdentifiers() == null) {
            impl.setIdentifiers(new ArrayList(1));
        }
        // @endhack
        cb.updateLayer(orig.getLayer(), l);

        // validate SRS - an invalid one will destroy capabilities doc and make
        // the layer totally broken in UI
        CoordinateReferenceSystem newRefSystem = null;

        String srs = r != null ? r.getSRS() : null;
        if (srs != null) {
            try {
                newRefSystem = CRS.decode(srs);
            } catch (NoSuchAuthorityCodeException ex) {
                String msg = "Invalid SRS " + srs;
                LOGGER.warning(msg + " in PUT request");
                throw ImportJSONWriter.badRequest(msg);
            } catch (FactoryException ex) {
                throw new RestletException("Error with referencing",Status.SERVER_ERROR_INTERNAL,ex);
            }
            // make this the specified native if none exists
            // useful for csv or other files
            if (resource.getNativeCRS() == null) {
                resource.setNativeCRS(newRefSystem);
            }
            resource.setSRS(srs);
        }

    }

    class ItemLayerJSONFormat extends StreamDataFormat {

        public ItemLayerJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).layer();
        }
    
        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            newWriter(out).layer(task(), true, expand(1));
        }
    }
}
