/*
 * The MIT License
 *
 * Copyright 2024 ahmed.
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

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bson.BsonDocument;
import org.bson.Document;

/**
 *
 * @author ahmed
 * @param <T>
 */
public class Watch<T extends Base> {
    public final Class<? extends Base> clazz;
    private final String kind;
    private final WatchMode mode;
    
    private ChangeStreamIterable<Document> stream;
    private MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
    
    private Datastore datastore;
    private ChangeStreamDocument<Document> lastChange;
    private boolean invalidate = false;

    public static enum WatchMode {
        INSERT(OperationType.INSERT),
        UPDATE_REPLACE(OperationType.UPDATE, OperationType.REPLACE),
        INSERT_UPDATE_REPLACE(OperationType.INSERT, OperationType.UPDATE, OperationType.REPLACE);
        
        private final Set<OperationType> allowed;

        private WatchMode(OperationType... allowed) {
            this.allowed = new HashSet<>(Arrays.asList(allowed));
        }
        
        protected boolean isAllowed(OperationType type) {
            return this.allowed.contains(type);
        }
    }
    
    public Watch(Class<? extends Base> clazz, WatchMode mode) {
        this(clazz, mode, Datastore.getDefaultService());
    }
    
    public Watch(Class<? extends Base> clazz, WatchMode mode, Datastore datastore) {
        this.clazz = clazz;
        this.kind = ClassMap.getKind(clazz);
        this.mode = mode;
        
        this.datastore = datastore;
        this.stream = datastore.getCollection(kind).watch();
        this.cursor = this.stream.cursor();
    }    
    
    public synchronized boolean hasNext() {
        if(invalidate)
            return false;
        
        if(lastChange != null)
            return true;
        
        while(cursor.hasNext()) {
            lastChange = cursor.next();
            
            OperationType type = lastChange.getOperationType();

            if(mode.isAllowed(type)) {
                return true;
            } else if(type == OperationType.INVALIDATE) {
                invalidate = true;
                cursor.close();
                return false;
            }
        }
        
        return false;
    }
    
    public synchronized T tryNext() {
        lastChange = peekNext();
        
        T item = buildItem();
        
        if(item != null) {
            lastChange = null;
            return item;
        }
        
        return null;
    }

    public synchronized T next() {
        lastChange = peekNext();
        
        T item = buildItem();
        
        if(item != null) {
            lastChange = null;
            return item;
        }
        
        throw new MongomanException("no new items exist");
    }
    
    public void close() {
        if(!invalidate) {
            cursor.close();
            invalidate = true;
        }
    }
    
    @SuppressWarnings("null")
    private ChangeStreamDocument<Document> peekNext() {
        if(invalidate)
            return null;
        
        if(lastChange != null)
            return lastChange;
        
        ChangeStreamDocument<Document> change;
        
        while((change = cursor.tryNext()) != null) {
            OperationType type = change.getOperationType();
            
            if(mode.isAllowed(type)) {
                return change;
            } else if(type == OperationType.INVALIDATE) {
                invalidate = true;
                cursor.close();
                return null;
            }
        }
        
        return null;
    }
    
    private T buildItem() {
        if(lastChange == null)
            return null;
        
        Document lastdocument = lastChange.getFullDocument();
            
        if(lastdocument == null) {
            BsonDocument key = lastChange.getDocumentKey();

            if(key != null)
                lastdocument = datastore.get(kind, key.getObjectId("_id").getValue());
        }

        return Base.createInstance(clazz, lastdocument);
    }
}
