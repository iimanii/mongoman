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

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 *
 * @author ahmed
 * @param <T>
 */
public class Cursor <T extends Base> {
    
    DBCursor cursor;
    Datastore datastore;
    Class<? extends Base> clazz;
    
    protected Cursor(DBCursor cursor, Class<? extends Base> clazz, Datastore datastore) {
        this.cursor = cursor;
        this.clazz = clazz;
        this.datastore = datastore;
    }
    
    /* Counts the number of objects matching the query */
    public int count​(){
        return cursor.count();
    }
    
    /* Returns the element the cursor is at */
    public T curr() {
        DBObject current = cursor.curr();
        return createInstance(clazz, current);
    }

    /* Checks if there is another object available */
    public boolean hasNext() {
        return cursor.hasNext();
    }
    
    /* Non blocking check for tailable cursors to see if another object is available */
    public T tryNext() {
        DBObject next = cursor.tryNext();
        return createInstance(clazz, next);
    }
    
    /* Returns the object the cursor is at and moves the cursor ahead by one */
    public T next​() {        
        DBObject current = cursor.next();
        return createInstance(clazz, current);
    }
    
    /* Returns the number of objects through which the cursor has iterated */
    public int numSeen​() {
        return cursor.numSeen();
    }

    /* Returns the first document that matches the query */
    public T one​() {
        DBObject current = cursor.one​();
        return createInstance(clazz, current);
    }
    
    public void remove​() {
        cursor.remove();
    }

    /* Counts the number of objects matching the query this does take limit/skip into consideration */
    public int size​() {
        return cursor.size();
    }

    public DBCursor getDBCursor() {
        return cursor;
    }    
    
    private T createInstance(Class<? extends Base> clazz, DBObject data) {
        if(data == null)
            return null;
        
        T instance = T.createInstance(clazz, data);
        instance.loadNested(datastore);
        
        return instance;
    }
}
