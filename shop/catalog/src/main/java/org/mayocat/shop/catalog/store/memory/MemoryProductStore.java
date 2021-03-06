/*
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.shop.catalog.store.memory;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.mayocat.shop.catalog.model.Collection;
import org.mayocat.shop.catalog.model.Product;
import org.mayocat.shop.catalog.store.ProductStore;
import org.mayocat.store.EntityDoesNotExistException;
import org.mayocat.store.InvalidMoveOperation;
import org.mayocat.store.memory.AbstractPositionedEntityMemoryStore;
import org.mayocat.store.memory.DefaultPositionedEntity;
import org.xwiki.component.annotation.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * In-memory implementation of {@link ProductStore}
 *
 * @version $Id$
 */
@Component("memory")
public class MemoryProductStore extends AbstractPositionedEntityMemoryStore<Product, DefaultPositionedEntity<Product>>
        implements ProductStore
{
    private static final Predicate<Product> ON_SHELF = new Predicate<Product>()
    {
        public boolean apply(@Nullable Product input)
        {
            return input.getOnShelf() != null && input.getOnShelf();
        }
    };

    public void moveProduct(String productToMove, String productToMoveRelativeTo,
            RelativePosition relativePosition) throws InvalidMoveOperation
    {
        throw new RuntimeException("Unsupported operation exception");
    }

    public List<Product> findOrphanProducts()
    {
        return FluentIterable.from(all()).filter(new Predicate<Product>()
        {
            @Override public boolean apply(@Nullable Product input)
            {
                return input.getCollections().isLoaded() && input.getCollections().get().isEmpty();
            }
        }).toList();
    }

    public Integer countAllForCollection(Collection collection)
    {
        return findAllForCollection(collection).size();
    }

    public List<Product> findForCollection(Collection collection, Integer number, Integer offset)
    {
        if (number == 0) {
            return FluentIterable.from(findAllForCollection(collection)).skip(offset).toList();
        }
        return FluentIterable.from(findAllForCollection(collection)).skip(offset).limit(number).toList();
    }

    public List<Product> findAllForCollection(final Collection collection)
    {
        return FluentIterable.from(all()).filter(new Predicate<Product>()
        {
            public boolean apply(@Nullable Product input)
            {
                return input.getCollections().isLoaded() && input.getCollections().get().contains(collection);
            }
        }).toList();
    }

    public List<Product> findAllOnShelf(Integer number, Integer offset)
    {
        if (number == 0) {
            return FluentIterable.from(all()).filter(ON_SHELF).skip(offset).toList();
        }
        return FluentIterable.from(all()).filter(ON_SHELF).skip(offset).limit(number)
                .toList();
    }

    public Integer countAllOnShelf()
    {
        return FluentIterable.from(all()).filter(ON_SHELF).size();
    }

    public void updateStock(UUID productId, Integer stockOffset) throws EntityDoesNotExistException
    {
        Product product = this.findById(productId);
        product.setStock(product.getStock() + stockOffset);
    }

    protected DefaultPositionedEntity<Product> createForEntity(Product entity, Integer position)
    {
        return new DefaultPositionedEntity<Product>(entity, position);
    }
}
