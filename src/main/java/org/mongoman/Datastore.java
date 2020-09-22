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
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.types.ObjectId;

/**
 *
 * @author ahmed
 */
public class Datastore {
    
    private final DB db;
    public final String name;
    private final MongoClient mongoClient;
    
    private final HashMap<String, DBCollection> Collections;
    
    private final static String KEY_INDEX_NAME = "__key_";
    private final static String UNIQUE_INDEX_PREFIX   = "__unique_";
    private final static String REGULAR_INDEX_PREFIX  = "__regular_";
    
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
     
    
    protected ObjectId getObjectId(Key key) {
        DBObject obj = getCollection(key.kind).find(key.filterData, _ID_PROJECTION)
                                      .batchSize(1)
                                      .limit(1)
                                      .next();
        
        if(obj != null)
            return (ObjectId) obj.get("_id");
        
        return null;
    }
    
    protected boolean put(String kind, DBObject data, WriteConcern concern) {
        WriteResult r;

        if(concern == null)
            r = getCollection(kind).save(data);
        else
            r = getCollection(kind).save(data, concern);
        
        return !r.isUpdateOfExisting();
    }
    
    /* delete item using its key */
    protected boolean delete(Key key) {
        if(key.isEmpty())
           throw new MongomanException("Trying to delete item using empty key");
        
        WriteResult r = getCollection(key.kind).remove(key.filterData);
        
        return r.getN() > 0;
    }
    
    /* delete item using its objectid */
    protected boolean delete(String kind, ObjectId id) {
        DBObject obj = new BasicDBObject("_id", id);
        
        WriteResult r = getCollection(kind).remove(obj);
        
        return r.getN() > 0;
    }
    
    /* Static Functions */
    private static Datastore DEFAULT_SERVICE;
    
    public static Datastore getDefaultService() {
        return DEFAULT_SERVICE;
    }
    
    protected static Datastore fetchDefaultService() {
        if(DEFAULT_SERVICE == null)
            throw new MongomanException("Default Service not initialized");
        
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
     * Remove debugging prints
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
        
        /* setup key index */
        DBObject currentKeyIndex = null;
        
        DBObject keyIndex = Base.getKeyFields(ClassMap.getClass(name));
        
        /* match either name or content */
        for(DBObject index : currentIndexes) {
            if(((String)index.get("name")).equals(KEY_INDEX_NAME)) {
                currentKeyIndex = index;
                break;
            }
            
            if(keyIndex.equals(index.get("key"))) {
                currentKeyIndex = index;
                break;
            }
        }
        
        if(currentKeyIndex != null) {
            if((!keyIndex.equals(currentKeyIndex.get("key")) || !((String)currentKeyIndex.get("name")).equals(KEY_INDEX_NAME))) {
                System.out.println("Dropping key index");
                Collection.dropIndex((String)currentKeyIndex.get("name"));
                currentKeyIndex = null;
            }
        }

        if(currentKeyIndex == null) {
            if(keyIndex.keySet().size() > 0) {
                System.out.println("Setting key index");
                Collection.createIndex(keyIndex, KEY_INDEX_NAME, true);
            }
        }
        
        /* setup unique index */
        HashSet<String> set = new HashSet<>();
        
        for(DBObject index : currentIndexes) {
            String n = (String) index.get("name");
            if(n.startsWith(UNIQUE_INDEX_PREFIX) || n.startsWith(REGULAR_INDEX_PREFIX))
                set.add(n);
        }

        Map<String, Boolean> indexFields = Base.getIndexFields(ClassMap.getClass(name));
        
        for(Map.Entry<String, Boolean> e : indexFields.entrySet()) {
            String fieldName = e.getKey();
            boolean isUnique = e.getValue();
            String indexName = isUnique ? UNIQUE_INDEX_PREFIX : REGULAR_INDEX_PREFIX;
            indexName += fieldName;
            
            if(!set.contains(indexName)) {
                System.out.println("Adding index: " + indexName);
                DBObject o = new BasicDBObject(fieldName, 1);
                Collection.createIndex(o, indexName, isUnique);
            }
            
            set.remove(indexName);
        }
        
        for(String s : set) {
            System.out.println("Removing: " + s);
            Collection.dropIndex(s);
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
        db.getCollection(name).drop();
    }
    
    public void dropCollection(Class<? extends Base> clazz) {
        dropCollection(ClassMap.getKind(clazz));
    }
    
    public MongoClient getMongoClient() {
        return mongoClient;
    }
}
