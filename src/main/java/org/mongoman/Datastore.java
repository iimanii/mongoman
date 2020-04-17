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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ahmed
 */
public class Datastore {
    
    public final String name;
    private final MongoClient mongoClient;
    private final DB db;
    
    private final HashMap<String, DBCollection> Collections;
    
    private final static String UNIQUE_KEY_INDEX_NAME = "_key_";
    
    private final static DBObject _ID_PROJECTION = new BasicDBObject("_id", 1);
    
    public Datastore(MongoClient mongoClient, String dbname) {
        this.name = dbname;
        this.mongoClient = mongoClient;
        this.db = mongoClient.getDB(name);
        this.Collections = new HashMap<>();
    }
    
    /* get item */
    protected DBObject get(Key key) {
        return getCollection(key.kind).findOne(key.filterData);
    }
    
    protected boolean exists(Key key) {
        return getCollection(key.kind).find(key.filterData, _ID_PROJECTION)
                                      .batchSize(1)
                                      .limit(1)
                                      .hasNext();
    }
    
    /* save item .. return true if item is new */
    protected boolean put(String kind, DBObject data) {
        WriteResult r = getCollection(kind).save(data);
        
        return !r.isUpdateOfExisting();
    }
    
    /* delete item using its key */
    protected boolean delete(Key key) {
        WriteResult r = getCollection(key.kind).remove(key.filterData);
        
        return r.getN() > 0;
    }
    
    /* Static Functions */
    private static Datastore DEFAULT_SERVICE;
    
    public static Datastore getDefaultService() {
        return DEFAULT_SERVICE;
    }
    
    public static synchronized void setDefaultService(Datastore store) {
        DEFAULT_SERVICE = store;
    }
    
    
    private DBCollection getCollection(String name) {
        if(!Collections.containsKey(name))
            initCollection(name);
        
        return Collections.get(name);
    }
    
    /** TODO: 
     * Make sure indexes are set properly
     * - remove unwanted indexes
     */
    private synchronized void initCollection(String name) {
        if(Collections.containsKey(name))
            return;
        
        DBCollection Collection;
        
        if(!db.collectionExists(name))
            Collection = db.createCollection(name, null);
        else
            Collection = db.getCollection(name);
        
        List<DBObject> currentIndexes = Collection.getIndexInfo();
        
        DBObject currentKeyIndex = null;
        
        DBObject keyIndex = Base.getKeyFields(Kind.getClass(name));
        
        for(DBObject index : currentIndexes) {
            if(((String)index.get("name")).equals(UNIQUE_KEY_INDEX_NAME)) {
                currentKeyIndex = index;
                break;
            }
            
            if(keyIndex.equals(index.get("key"))) {
                currentKeyIndex = index;
                break;
            }
        }
        
        if(currentKeyIndex != null) {
            if((!keyIndex.equals(currentKeyIndex.get("key")) || !((String)currentKeyIndex.get("name")).equals(UNIQUE_KEY_INDEX_NAME))) {
                System.out.println("Dropping unique index");
                Collection.dropIndex((String)currentKeyIndex.get("name"));
                currentKeyIndex = null;
            }
        }

        if(currentKeyIndex == null) {
            System.out.println("Setting unique index");
            Collection.createIndex(keyIndex, UNIQUE_KEY_INDEX_NAME, true);
        }
        
        Collections.put(name, Collection);
    }

    protected DBCursor query(Query query, int skip, int batch, int limit) {
        return getCollection(query.getKind()).find(query.getFilter(), query.getProjection())
                                             .sort(query.getSort())
                                             .skip(skip)
                                             .batchSize(batch)
                                             .limit(limit);
    }
    
    public Set<String> getCollections() {
        return db.getCollectionNames();
    }
    
    public void dropCollection(String name) {
        getCollection(name).drop();
    }
}
