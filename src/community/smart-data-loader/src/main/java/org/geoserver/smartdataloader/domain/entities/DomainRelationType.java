/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain.entities;

/**
 * Cardinality of a relation of the domain model, the direction goes from the cotnaining entity to
 * the destination entity.
 */
public enum DomainRelationType {
    ONEONE,
    ONEMANY,
    MANYONE,
    MANYMANY
}
