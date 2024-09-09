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
package org.mongoman;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mongoman.Base.TypeInfo;

/**
 *
 * @author ahmed
 * @param <T>
 */
public class Query <T extends Base> {
    
    public static class Filter {
        final FilterOperator op;
        private String property;
        private Object value;
        private Filter[] filters;
        private BasicDBObject cached;
        private String options;
        
        public Filter(String property, FilterOperator op, Object value) {
            this.op = op;
            this.property = property;
            
            if(value instanceof Base) {
                this.value = ((Base) value).getKey().filterData;
            } else if(value instanceof Enum) {
                this.value = ((Enum) value).name();  // Single enum to string
            } else if (value instanceof Collection) {
                this.value = handleCollection((Collection) value);  // Handle collections (including enums)
            } else if (value.getClass().isArray()) {
                this.value = handleArray(value);  // Handle arrays (including enums)
            } else {
                this.value = value;  // Primitive types or other objects
            }
        }
        
        public Filter(FilterOperator op, Filter... filters) {
            this.op = op;
            this.filters = filters;
        }
        
        public Filter options(String value) {
            this.options = value;
            cached = null;
            return this;
        }
        
        @Override
        public String toString() {
            return toDBObj().toJson();
        }
        
        private Object handleCollection(Collection<?> collection) {
            if (collection.isEmpty())
                return collection;

            Object first = collection.iterator().next();
            if (first instanceof Enum) {
                List<String> enumNames = new ArrayList<>();
                for (Enum e : (Collection<Enum>) collection) {
                    enumNames.add(e.name());  // Convert each enum to string
                }
                return enumNames;
            }

            return collection;  // Non-enum collections can be returned as-is
        }
        
        private Object handleArray(Object array) {
            if (array instanceof Enum[]) {
                List<String> enumNames = new ArrayList<>();
                for (Enum e : (Enum[]) array) {
                    enumNames.add(e.name());  // Convert each enum to string
                }
                return enumNames;
            }

            return array;  // Non-enum arrays can be returned as-is
        }
        
        protected BasicDBObject toDBObj() {
            if(cached != null)
                return cached;
            
            cached = new BasicDBObject();
            
            if(property != null) {
                BasicDBObject comp = new BasicDBObject();
                comp.put(op.code, value);
                
                if(options != null)
                    comp.put("$options", options);
                
                cached.put(property, comp);
            } else {
                BasicDBList arr = new BasicDBList();

                for(Filter f : filters)
                    arr.add(f.toDBObj());

                cached.put(op.code, arr);
            }            
            
            return cached;
        }
        
        protected void validateFieldPath(Class<?> currentClass) {
            String[] parts = property.split("\\.");  // Split the field path by dot for nested fields
            boolean isFullSaved = true;  // Top-level class (TestClass) fields are fully saved by default

            /* Loop through each part of the nested path */
            for (String part : parts) {
                /* Get the field in the current class, this will throw NoSuchFieldException if the field doesn't exist */
                Field currentField;
                try {
                    currentField = currentClass.getField(part);
                } catch (NoSuchFieldException | SecurityException ex) {
                    throw new MongomanException(ex);
                }

                /* If the class is not fully saved and the field is not a key field, throw an exception */
                if (!isFullSaved && !Base.isKeyField(currentField))
                    throw new MongomanException("Field '" + part + "' is invalid because the object is not fully saved.");

                /* Determine the type of the current field to move to the next class level */
                TypeInfo typeInfo = new TypeInfo(currentField);
                if (typeInfo.isCollection()) {
                    currentClass = typeInfo.getGenericArgument(0).clazz;  // Get the element type for collections (List/Set)
                } else if (typeInfo.isMap()) {
                    currentClass = typeInfo.getGenericArgument(1).clazz;  // Get the value type for Map<K, V>
                } else if (typeInfo.isArray()) {
                    currentClass = typeInfo.getComponentType();  // Get the component type for arrays
                } else {
                    currentClass = currentField.getType();  // Move to the next level for non-collection, non-map fields
                }

                /* Update the fully saved status for the next level */
                isFullSaved = currentField.isAnnotationPresent(FullSave.class);  // Check if this field is marked with @FullSave
            }
        }
    }
    
    public static enum SortDirection {
        ASC(1), DESC(-1);
        
        final int dir;

        private SortDirection(int dir) {
            this.dir = dir;
        }
    }
    
    public static enum FilterOperator {
        EQUAL("$eq"), NOT_EQUAL("$ne"),
        GREATER_THAN("$gt"), GREATER_THAN_OR_EQUAL("$gte"),
        LESS_THAN("$lt"), LESS_THAN_OR_EQUAL("$lte"),
        IN("$in"), NOT_IN("$nin"),
        AND("$and"), OR("$or"), NOR("$nor"),
        REGEX("$regex");
        
        final String code;

        private FilterOperator(String code) {
            this.code = code;
        }
    }

    /* Method to create a new Filter and validate fields immediately */
    public Filter createFilter(String property, FilterOperator operator, Object value) {
        Filter f = new Filter(property, operator, value);
        f.validateFieldPath(clazz);
        return f;
    }    
    
    public final Class<? extends Base> clazz;
    private final String kind;
    private boolean keysOnly;
    private Filter filter;
    private boolean loadNested;
    
    private final LinkedHashMap<String, SortDirection> sort;
    private final HashSet<String> projection;
    private final HashSet<String> ignore;
    
    private BasicDBObject computedProjection;
    private BasicDBObject computedSort;
    
    private int skip;
    private int limit;
    private int batch;
    
    public Query(Class<T> clazz) {
        this.clazz = clazz;
        this.kind = ClassMap.getKind(clazz);
        this.keysOnly = false;
        this.loadNested = false;
        this.sort = new LinkedHashMap<>();
        this.projection = new HashSet<>();
        this.ignore = new HashSet<>();
        this.skip = 0;
        this.limit = 0;
        this.batch = 1000;
    }
    
    public Query setKeysOnly() {
        this.keysOnly = true;
        
        return this;
    }
    
    public Query setFilter(Filter filter) {
        this.filter = filter;
        
        return this;
    }
    
    public Query setSkip(int skip) {
        this.skip = skip;
        return this;
    }
    
    public Query setLimit(int limit) {
        this.limit = limit;
        return this;
    }    
    
    public Query setBatch(int batch) {
        this.batch = batch;
        return this;
    }
    
    public Query setLoadNested(boolean loadNested) {
        this.loadNested = loadNested;
        return this;
    }
    
    public Query addSort(String field, SortDirection dir) {
        sort.put(field, dir);
        
        computedSort = null;
        
        return this;
    }
    
    public Query addProjection(String field) {
        if (!ignore.isEmpty())
            throw new MongomanException("Cannot add projection field when ignore fields are set.");

        projection.add(field);

        computedProjection = null;
        
        return this;
    }
    
    public Query ignoreField(String field) {
        if (!projection.isEmpty())
            throw new MongomanException("Cannot add ignore field when projection fields are set.");

        ignore.add(field);
        
        computedProjection = null;
        
        return this;
    }
    
    public Cursor<T> execute() {
        return execute(Datastore.fetchDefaultService());
    }

    public Cursor<T> execute(Datastore datastore) {
        return new Cursor<>(datastore.query(this, skip, batch, limit), clazz, datastore, loadNested);
    }
    
    public String getKind() {
        return kind;
    }
    
    public BasicDBObject getFilter() {
        if(filter != null)
            return filter.toDBObj();
        
        return new BasicDBObject();
    }
    
    public BasicDBObject getProjection() {
        if(computedProjection != null)
            return computedProjection;
        
        if(keysOnly)
            return Base.getKeyFields(clazz);
        
        computedProjection = new BasicDBObject();
        
        for(String s : projection)
            computedProjection.put(s, 1);
        
        for(String s : ignore)
            computedProjection.put(s, 0);
            
        return computedProjection;
    }
    
    public BasicDBObject getSort() {
        if(computedSort != null)
            return computedSort;
        
        computedSort = new BasicDBObject();

        for(Map.Entry<String, SortDirection> e : sort.entrySet()) {
            computedSort.put(e.getKey(), e.getValue().dir);
        }
        
        return computedSort;
    }
    
    public int getSkip() {
        return skip;
    }
    
    public int getLimit() {
        return limit;
    }    
    
    public int getBatch() {
        return batch;
    }
}
