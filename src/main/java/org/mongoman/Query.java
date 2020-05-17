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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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
        
        public Filter(String property, FilterOperator op, Object value) {
            this.op = op;
            this.property = property;
            this.value = value;
            
            if(value instanceof Base)
               this.value = ((Base)value).getKey().filterData;
        }
        
        public Filter(FilterOperator op, Filter... filters) {
            this.op = op;
            this.filters = filters;
        }
        
        protected BasicDBObject toDBObj() {
            if(cached != null)
                return cached;
            
            cached = new BasicDBObject();
            
            if(property != null) {
                BasicDBObject comp = new BasicDBObject();
                comp.put(op.code, value);
                
                cached.put(property, comp);
            } else {
                BasicDBList arr = new BasicDBList();

                for(Filter f : filters)
                    arr.add(f.toDBObj());

                cached.put(op.code, arr);
            }
            
            return cached;
        }
        
        @Override
        public String toString() {
            return toDBObj().toJson();
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
        AND("$and"), OR("$or"), NOT("$not"), NOR("$nor");
        
        final String code;

        private FilterOperator(String code) {
            this.code = code;
        }
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
    
    public Query(Class<? extends Base> clazz) {
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
        projection.add(field);

        computedProjection = null;
        
        return this;
    }
    
    public Query ignoreField(String field) {
        ignore.add(field);
        
        computedProjection = null;
        
        return this;
    }
    
    public Cursor<T> execute() {
        return execute(Datastore.getDefaultService());
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
