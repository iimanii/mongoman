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

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import java.util.HashMap;

/**
 *
 * @author ahmed
 * @param <T>
 */
public class Cursor<T extends Base> {

    MongoCursor<Document> cursor;
    Datastore datastore;
    Class<? extends Base> clazz;
    boolean loadNested;
    
    // Store the last returned document to emulate the curr() behavior
    private T curr;
    private int numSeen;
        
    protected Cursor(MongoCursor<Document> cursor, Class<? extends Base> clazz, Datastore datastore, boolean loadNested) {
        this.cursor = cursor;
        this.clazz = clazz;
        this.datastore = datastore;
        this.loadNested = loadNested;
        this.numSeen = 0;
    }

    /* Returns the element the cursor is at */
    public T curr() {
        return curr;
    }

    /* Checks if there is another object available */
    public boolean hasNext() {
        return cursor.hasNext();
    }

    /* Non-blocking check for tailable cursors to see if another object is available */
    public T tryNext() {
        Document next = cursor.tryNext();
        
        if(next != null) {
            curr = createInstance(clazz, next);
            numSeen++;
            return curr;
        }
        
        return null;
    }

    /* Returns the object the cursor is at and moves the cursor ahead by one .. throws exception if next object doesnt exist */
    public T next() {
        Document next = cursor.next();        
        curr = createInstance(clazz, next);
        numSeen++;
        return curr;
    }

    /* Returns the number of objects through which the cursor has iterated */
    public int numSeen() {
        return numSeen;
    }

    /* MongoCursor does not have one() method, handle accordingly */
    public T one() {
        if(cursor.hasNext()) 
            return next();
        
        return null;
    }

    public MongoCursor<Document> getMongoCursor() {
        return cursor;
    }

    private T createInstance(Class<? extends Base> clazz, Document data) {
        if(data == null)
            return null;

        T instance = T.createInstance(clazz, data);
        if(loadNested)
            instance.loadNested(datastore, new HashMap<>());

        return instance;
    }
}
