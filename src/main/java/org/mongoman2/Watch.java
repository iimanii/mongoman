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
    
    private ChangeStreamIterable<Document> stream;
    private MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
    
    private Datastore datastore;
    private ChangeStreamDocument<Document> lastChange;
    private boolean invalidate = false;
    
    public Watch(Class<? extends Base> clazz) {
        this(clazz, Datastore.getDefaultService());
    }
    
    public Watch(Class<? extends Base> clazz, Datastore datastore) {
        this.clazz = clazz;
        this.kind = ClassMap.getKind(clazz);
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
            
//            System.out.println(lastChange.toString());
            
            switch(lastChange.getOperationType()) {
                case INVALIDATE:
                    invalidate = true;
                    cursor.close();
                    return false;
                case DELETE:
                case DROP:
                case DROP_DATABASE:
                case RENAME:
                case OTHER:
                    System.out.println(lastChange.toString());
                    continue;
                case INSERT:
                case REPLACE:
                case UPDATE:
                    return true;
            }
        }
        
        return false;
    }
    
    private boolean tryNext() {
        if(invalidate)
            return false;
        
        if(lastChange != null)
            return true;
        
        while((lastChange = cursor.tryNext()) != null) {
//            System.out.println(lastChange.toString());
            
            switch(lastChange.getOperationType()) {
                case INVALIDATE:
                    invalidate = true;
                    cursor.close();
                    return false;
                case DELETE:
                case DROP:
                case DROP_DATABASE:
                case RENAME:
                case OTHER:
                    continue;
                case INSERT:
                case REPLACE:
                case UPDATE:
                    return true;
            }
        }
        
        return false;
    }

    public synchronized T next() {
        if(lastChange != null || tryNext()) {
            Document lastdocument = lastChange.getFullDocument();
            
            if(lastdocument == null) {
                BsonDocument key = lastChange.getDocumentKey();
                
                if(key != null)
                    lastdocument = datastore.get(kind, key.getObjectId("_id").getValue());
            }
            
            T item = Base.createInstance(clazz, lastdocument);
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
}
