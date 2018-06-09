/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.util.List;

class ProductReferences {

    List<ProductReference> products;

    public ProductReferences(List<ProductReference> products) {
        super();
        this.products = products;
    }

    public List<ProductReference> getProducts() {
        return products;
    }
}
