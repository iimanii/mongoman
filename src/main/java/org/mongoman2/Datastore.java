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

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.*;

/**
 *
 * @author ahmed
 */
public class Datastore {
    
    private final MongoDatabase db;
    public final String name;
    private final MongoClient mongoClient;

    private final HashMap<String, MongoCollection<Document>> collections;

    private final static String KEY_INDEX_NAME = "__key_";
    private final static String UNIQUE_INDEX_PREFIX = "__unique_";
    private final static String REGULAR_INDEX_PREFIX = "__regular_";

    private final static Document _ID_PROJECTION = new Document("_id", 1);

    public Datastore(MongoClient mongoClient, String dbname) {
        this.name = dbname;
        this.mongoClient = mongoClient;
        this.db = mongoClient.getDatabase(name);
        this.collections = new HashMap<>();
    }

    /* get item */
    protected Document get(Key key) {
        return getCollection(key.kind).find(key.filterData).limit(1).first();
    }
    protected Document get(String kind, ObjectId id) {
        return getCollection(kind).find(new Document("_id", id)).limit(1).first();
    }
    
    protected boolean exists(Key key) {
        return getCollection(key.kind)
                .find(key.filterData)
                .projection(_ID_PROJECTION)
                .limit(1)
                .iterator()
                .hasNext();
    }

    protected ObjectId getObjectId(Key key) {
        Document doc = getCollection(key.kind)
                .find(key.filterData)
                .projection(_ID_PROJECTION)
                .limit(1)
                .first();

        return (doc != null) ? doc.getObjectId("_id") : null;
    }

    protected boolean save(String kind, Key key, Document data, WriteConcern concern) {
        MongoCollection<Document> collection = getCollection(kind).withWriteConcern(concern != null ? concern : WriteConcern.ACKNOWLEDGED);
        
        UpdateOptions options = new UpdateOptions().upsert(true);
        UpdateResult result = collection.updateOne(key.filterData, new Document("$set", data), options);
        
        return result.getMatchedCount() == 0;
    }

    protected void saveMany(String kind, List<Document> data) {
        getCollection(kind).insertMany(data);
    }

    /* delete item using its key */
    protected boolean delete(Key key) {
        if(key.isEmpty())
            throw new MongomanException("Trying to delete item using empty key");

        return getCollection(key.kind).deleteOne(key.filterData).getDeletedCount() > 0;
    }

    /* delete item using its objectid */
    protected boolean delete(String kind, ObjectId id) {
        Document obj = new Document("_id", id);
        return getCollection(kind).deleteOne(obj).getDeletedCount() > 0;
    }

    public MongoCollection<Document> getCollection(String name) {
        if(!collections.containsKey(name))
            initCollection(name);

        return collections.get(name);
    }

    public Set<String> getCollections() {
        return new HashSet<>(db.listCollectionNames().into(new ArrayList<>()));
    }

    public void dropCollection(String name) {
        getCollection(name).drop();
    }

    public void dropCollection(Class<? extends Base> clazz) {
        dropCollection(ClassMap.getKind(clazz));
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void shutdown() {
        mongoClient.close();
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

    /**
     * TODO: Remove debugging prints
     */
    private synchronized void initCollection(String name) {
        if(collections.containsKey(name))
            return;

        MongoCollection<Document> collection;

        if(!db.listCollectionNames().into(new ArrayList<>()).contains(name))
             db.createCollection(name);

        collection = db.getCollection(name);
        ListIndexesIterable<Document> currentIndexes = collection.listIndexes();

        /* setup key index */
        Document currentKeyIndex = null;
        Document keyIndex = Base.getKeyFields(ClassMap.getClass(name));

        for (Document index : currentIndexes) {
            /* first try by index name */
            if(index.getString("name").equals(KEY_INDEX_NAME)) {
                currentKeyIndex = index;
                break;
            }
            
            /* try by value */
            if(keyIndex.equals(index.get("key"))) {
                currentKeyIndex = index;
                break;                
            }
        }

        if(currentKeyIndex != null) {
            if(!keyIndex.equals(currentKeyIndex.get("key")) || !currentKeyIndex.getString("name").equals(KEY_INDEX_NAME)) {
                System.out.println("Dropping key index");
                collection.dropIndex(currentKeyIndex.getString("name"));
                currentKeyIndex = null;
            }
        }

        if(currentKeyIndex == null && !keyIndex.isEmpty()) {
            System.out.println("Setting key index");
            collection.createIndex(keyIndex, new IndexOptions().name(KEY_INDEX_NAME).unique(true));
        }

        /* setup unique and regular indexes */
        HashSet<String> existingIndexes = new HashSet<>();
        
        for (Document index : currentIndexes) {
            String indexName = index.getString("name");
            if(indexName.startsWith(UNIQUE_INDEX_PREFIX) || indexName.startsWith(REGULAR_INDEX_PREFIX)) {
                existingIndexes.add(indexName);
            }
        }

        Map<String, Boolean> indexFields = Base.getIndexFields(ClassMap.getClass(name));
        
        for (Map.Entry<String, Boolean> entry : indexFields.entrySet()) {
            String fieldName = entry.getKey();
            boolean isUnique = entry.getValue();
            String indexName = (isUnique ? UNIQUE_INDEX_PREFIX : REGULAR_INDEX_PREFIX) + fieldName;

            if(!existingIndexes.contains(indexName)) {
                System.out.println("Adding index: " + indexName);
                Document index = new Document(fieldName, 1);
                collection.createIndex(index, new IndexOptions().name(indexName).unique(isUnique));
            }

            existingIndexes.remove(indexName);
        }

        for (String indexName : existingIndexes) {
            System.out.println("Removing index: " + indexName);
            collection.dropIndex(indexName);
        }

        collections.put(name, collection);
    }

}
