/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serial;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * A subclass of {@link Image} that never adds random noise to the url to allow the browser to cache the image.
 *
 * @see Image#shouldAddAntiCacheParameter()
 */
public class CachingImage extends Image {

    @Serial
    private static final long serialVersionUID = -5788028224598001515L;

    /**
     * @see Image#Image(String, ResourceReference, ResourceReference...)
     * @param id
     * @param resourceReference
     * @param resourceReferences
     */
    public CachingImage(String id, ResourceReference resourceReference, ResourceReference... resourceReferences) {
        super(id, resourceReference, resourceReferences);
    }

    /**
     * @see Image#Image(String, ResourceReference, PageParameters, ResourceReference...)
     * @param id
     * @param resourceReference
     * @param resourceParameters
     * @param resourceReferences
     */
    public CachingImage(
            String id,
            ResourceReference resourceReference,
            PageParameters resourceParameters,
            ResourceReference... resourceReferences) {
        super(id, resourceReference, resourceParameters, resourceReferences);
    }

    /**
     * @see Image#Image(String, IResource, IResource...)
     * @param id
     * @param imageResource
     * @param imageResources
     */
    public CachingImage(String id, IResource imageResource, IResource... imageResources) {
        super(id, imageResource, imageResources);
    }

    /**
     * @see Image#Image(String, IModel)
     * @param id
     * @param model
     */
    public CachingImage(String id, IModel<?> model) {
        super(id, model);
    }

    /**
     * @see Image#Image(String, String)
     * @param id
     * @param string
     */
    public CachingImage(String id, String string) {
        this(id, new Model<>(string));
    }

    /**
     * @see Image#Image(String)
     * @param id
     */
    public CachingImage(String id) {
        super(id);
    }

    /**
     * Overridden to allow caching.
     *
     * @return always {@code false}
     */
    @Override
    protected boolean shouldAddAntiCacheParameter() {
        return false;
    }
}
