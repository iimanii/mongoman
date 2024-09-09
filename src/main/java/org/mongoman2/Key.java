/*
 * The MIT License
 *
 * Copyright 2018 Ahmed Tarek.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.mongoman2;

import org.bson.Document;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.mongoman2.Query.Filter;

/**
 *
 * @author ahmed
 */
public class Key implements Serializable {
    public final String kind;
    protected final Document data;
    protected final Document filterData;

    private final int hashCode;

    private Filter filter;

    protected Key(Base object) throws IllegalArgumentException, IllegalAccessException {
        kind = object.getKind();
        data = toDocument(object);
        filterData = toFilterDocument(data);

        hashCode = Arrays.hashCode(new int[]{kind.hashCode(), data.hashCode()});
    }

    private Document toDocument(Base object) throws IllegalArgumentException, IllegalAccessException {
        Document result = new Document();

        /* Get all public fields of the class */
        Field[] fields = object.getClass().getFields();

        for(Field field : fields) {
            /* Must be final */
            if(!Modifier.isFinal(field.getModifiers()))
                continue;

            /* must not be static */
            if(Modifier.isStatic(field.getModifiers()))
                continue;

            String name = field.getName();

            Object value = field.get(object);

            /* in case of a Base class .. only use its key */
            if(value instanceof Base)
                result.append(name, ((Base) value).getKey().data);
            else if(value instanceof Enum)
                result.append(name, ((Enum)value).name());
            else
                result.append(name, value);
        }
        return result;
    }

    /**
     * Ensures that nested objects are referenced correctly
     * -> field.subfield = value
     * 
     * @param data
     * @return
     */
    private Document toFilterDocument(Document data) {
        Document result = new Document();

        for(Entry<String, Object> e : data.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();

            if(value instanceof Document) {
                Map<String, Object> inner = toFilterDocument((Document) value);
                for(Entry<String, Object> e0 : inner.entrySet()) {
                    result.put(key + "." + e0.getKey(), e0.getValue());
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    public Filter toFilter() {
        if(filter != null)
            return filter;

        Filter[] filters = new Filter[filterData.size()];
        int i = 0;

        for(Entry<String, Object> e : filterData.entrySet()) {
            filters[i++] = new Filter(e.getKey(), Query.FilterOperator.EQUAL, e.getValue());
        }

        filter = new Filter(Query.FilterOperator.AND, filters);

        return filter;
    }

    @Override
    public String toString() {
        return data.toJson();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null)
            return false;

        if (!(object instanceof Key))
            return false;

        Key key = (Key) object;

        if(!Objects.equals(kind, key.kind))
            return false;

        return data.equals(key.data);
    }

    boolean isEmpty() {
        return filterData.isEmpty();
    }
}
