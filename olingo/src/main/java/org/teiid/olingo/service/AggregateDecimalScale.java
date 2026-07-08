/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.olingo.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.EdmType;

/**
 * Olingo 5 tightened {@code Edm.Decimal} validation: a decimal property whose
 * {@code Scale} facet is unspecified (null) is treated as {@code Scale=0} and
 * rejects any value with a fractional part. The dynamic result type Olingo
 * builds for an {@code $apply=aggregate(x with sum/average as y)} transformation
 * types the alias as {@code Edm.Decimal} with a null scale, so serializing a
 * non-integral sum/average now fails with a 400 where Olingo 4 accepted it.
 * <p>
 * This wraps the aggregation result type so those null-scale decimal properties
 * report a variable scale ({@link Integer#MAX_VALUE}), which Olingo's serializer
 * accepts for any number of fractional digits, restoring the prior behavior. The
 * wrapper is applied only to the transient {@code $apply} type, so ordinary
 * entity types with explicit decimal facets are unaffected.
 */
final class AggregateDecimalScale {

    private static final String EDM_DECIMAL = "Edm.Decimal"; //$NON-NLS-1$

    private AggregateDecimalScale() {
    }

    static EdmStructuredType wrap(EdmStructuredType type) {
        if (type == null || Proxy.isProxyClass(type.getClass())) {
            return type;
        }
        // Preserve the delegate's own interfaces (an $apply result is an
        // EdmStructuredType that is deliberately NOT an EdmEntityType, which the
        // response building branches on) - only the Scale facet is adjusted.
        return (EdmStructuredType) Proxy.newProxyInstance(
                AggregateDecimalScale.class.getClassLoader(),
                collectInterfaces(type),
                new StructuredTypeHandler(type));
    }

    private static Class<?>[] collectInterfaces(EdmStructuredType type) {
        java.util.LinkedHashSet<Class<?>> interfaces = new java.util.LinkedHashSet<>();
        for (Class<?> c = type.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (EdmStructuredType.class.isAssignableFrom(i)) {
                    interfaces.add(i);
                }
            }
        }
        if (interfaces.isEmpty()) {
            interfaces.add(EdmStructuredType.class);
        }
        return interfaces.toArray(new Class<?>[0]);
    }

    private static boolean isNullScaleDecimal(EdmProperty p) {
        if (p == null || p.getScale() != null) {
            return false;
        }
        EdmType t = p.getType();
        return t != null && EDM_DECIMAL.equals(t.getFullQualifiedName().toString());
    }

    private static EdmProperty wrapProperty(final EdmProperty p) {
        return (EdmProperty) Proxy.newProxyInstance(
                AggregateDecimalScale.class.getClassLoader(),
                new Class<?>[] { EdmProperty.class },
                (proxy, method, args) -> {
                    if ("getScale".equals(method.getName()) //$NON-NLS-1$
                            && (args == null || args.length == 0)) {
                        return Integer.MAX_VALUE;
                    }
                    return method.invoke(p, args);
                });
    }

    private static final class StructuredTypeHandler implements InvocationHandler {
        private final EdmStructuredType delegate;

        StructuredTypeHandler(EdmStructuredType delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = method.invoke(delegate, args);
            String name = method.getName();
            if (result instanceof EdmProperty
                    && ("getStructuralProperty".equals(name) || "getProperty".equals(name))) { //$NON-NLS-1$ //$NON-NLS-2$
                EdmProperty p = (EdmProperty) result;
                if (isNullScaleDecimal(p)) {
                    return wrapProperty(p);
                }
            }
            return result;
        }
    }
}
