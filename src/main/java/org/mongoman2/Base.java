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

import org.mongoman2.annotations.FullSave;
import org.mongoman2.annotations.Index;
import org.mongoman2.annotations.Unique;
import org.mongoman2.annotations.Reference;
import com.mongodb.WriteConcern;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import static org.mongoman.Base.isKeyField;

/**
 *
 * @author Ahmed
 * Mar 29, 2016
 *
 */

public abstract class Base implements Serializable {
    /* collection name */
    private final String kind;
    
    /* shallow objects do not get saved */
    private final boolean shallow;

    /* does store null fields into database when saving */
    private final boolean ignoreNull;
    
    /* enable this to remove extra/obsolete properties found in database object and are not defined in the class */
    private final boolean ignoreUnknownProperties;
    
    private final ExportMode dbExportMode;
    
    /* Key */
    private Key key;
    
    /* mongo ObjectId */
    private ObjectId _id;

    /* underlying db entity */
    private Document loaded;

    public Base() {
        ClassMap.classVariables v = ClassMap.getVariables(this.getClass());
        this.kind = v.kind;
        this.shallow = v.shallow;
        this.ignoreNull = v.ignoreNull;
        this.ignoreUnknownProperties = v.ignoreUnknownProperties;
        this.dbExportMode = this.ignoreNull ? ExportMode.DB_IGNORE_NULL : ExportMode.DB;
    }
    
    final public Key getKey() {
        if(key == null)
            try {
                this.key = new Key(this);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new MongomanException(ex);
            }
        
        return key;
    }

    final public String getKind() {
        return kind;
    }
    
    public static String getKind(Class<? extends Base> clazz) {
        return ClassMap.getKind(clazz);
    }
    
    /* creates an instance given a subclass and its data */
    protected static <T extends Base> T createInstance(Class<? extends Base> clazz, Document data) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            T item = (T)constructor.newInstance();
            item.fromDocument(data);
            return item;
        } catch(NoSuchMethodException ex) {
            throw new MongomanException(clazz.getName() + " All subclasses of Base Class must implement a constructor that takes no arguments");
        } catch(IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
            throw new MongomanException(ex);
        }
    }
    
    /* creates a blank instance */
    protected static <T extends Base> T createInstance(Class<? extends Base> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            T item = (T)constructor.newInstance();
            return item;
        } catch(NoSuchMethodException ex) {
            throw new MongomanException(clazz.getName() + " All subclasses of Base Class must implement a constructor that takes no arguments");
        } catch(IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
            throw new MongomanException(ex);
        }
    }
    
    protected static Document getKeyFields(Class<? extends Base> clazz) {
        Document data = new Document();
        
        /* Get all public fields of the class */
        Field[] fields = clazz.getFields();
        
        for(Field field : fields) {
            /* Check if field is key */
            if(!isKeyField(field))
                continue;
            
            String name = field.getName();
            Class<?> field_class = field.getType();
            
            if(Base.class.isAssignableFrom(field_class)) {
                Document inner = getKeyFields((Class<? extends Base>) field_class);
                for(String n : inner.keySet()) {
                    data.append(name + "." + n, 1);
                }
            } else
                data.append(name, 1);
        }
        
        return data;
    }
    
    /* Check if the field is final and non-static */
    public static boolean isKeyField(Field field) {
        return Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers());
    }
    
    /* 
     * returns Map<FieldName, isUniqueIndex>
     */
    protected static Map<String, Boolean> getIndexFields(Class<? extends Base> clazz) {
        Map<String, Boolean> index = new HashMap<>();
        
        /* Get all public fields of the class */
        Field[] fields = clazz.getFields();
        
        for(Field field : fields) {
            /* must not be static */
            if(Modifier.isStatic(field.getModifiers()))
                continue;
            
            if(field.isAnnotationPresent(Unique.class))
                index.put(field.getName(), true);
            else if(field.isAnnotationPresent(Index.class))
                index.put(field.getName(), false);
        }
        
        return index;
    }
    
    /* loads data into the object */
    protected void fromDocument(Document data) {
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            String name = field.getName();
            
            if(data.containsKey(name)) {
                if(Modifier.isFinal(field.getModifiers()))
                    field.setAccessible(true);

                Object value = data.get(name);
                
                try {
                    field.set(this, convertDBToField(value, new TypeInfo(field)));
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new MongomanException(ex);
                }
            }
        }
        
        _id = data.getObjectId("_id");
        loaded = data;
    }
    
    /* creates Document from item */
    protected Document toDocument(ExportMode mode) {
        Document data = new Document();
        
        if(!mode.json && _id != null)
            data.put("_id", _id);
        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        
        for(Field field : fields) {
            /* must not be static */
            if(Modifier.isStatic(field.getModifiers()))
                continue;
            
            String name = field.getName();
            
            try {
                Object value = field.get(this);
                
                if(mode.ignore_null && value == null)
                    continue;
                
                data.append(name, convertFieldToDB(value, field.isAnnotationPresent(FullSave.class), mode));
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
        
        return data;
    }
    
    private static final JsonWriterSettings DEFAULT_JSONWRITER_SETTINGS = 
        JsonWriterSettings.builder()
                .dateTimeConverter((value, writer) -> {
                    writer.writeNumber(Long.toString(value));
                }).objectIdConverter((value, writer) -> {
                    writer.writeNull();
                }).outputMode(JsonMode.RELAXED).build();
    
    public String toJSON(boolean ignore_null) {
        ExportMode mode = ignore_null ? ExportMode.JSON_IGNORE_NULL : ExportMode.JSON;
        
        Document data = toDocument(mode);
        
        JsonWriterSettings settings = DEFAULT_JSONWRITER_SETTINGS;
        
        return data.toJson(settings);
    }
    
    public static String toJSON(Map map, boolean fullsave, boolean ignore_null) {
        ExportMode mode = ignore_null ? ExportMode.JSON_IGNORE_NULL : ExportMode.JSON;
        
        Document data = (Document) convertFieldToDB(map, fullsave, mode);
        
        JsonWriterSettings settings = DEFAULT_JSONWRITER_SETTINGS;
        
        return data.toJson(settings);
    }
    
    public static String toJSON(Collection collection, String name, boolean fullsave, boolean ignore_null) {
        ExportMode mode = ignore_null ? ExportMode.JSON_IGNORE_NULL : ExportMode.JSON;
        
        List<Document> list = (List<Document>) convertFieldToDB(collection, fullsave, mode);
        
        Document data = new Document();
        data.put(name, list);
        
        JsonWriterSettings settings = DEFAULT_JSONWRITER_SETTINGS;
        
        return data.toJson(settings);
    }

    /**
     * Uses key to check if object exists in db
     * @return true if item is stored in db
     */
    public boolean exists() {
        return exists(Datastore.fetchDefaultService());
    }
        
    /**
     * Uses key to check if object exists in db
     * @param store
     * @return true if item is stored in db
     */
    public boolean exists(Datastore store) {
        return store.exists(getKey());
    }
    
        /**
     * loads item from default datastore 
     * @return true on success
     */
    public boolean load() {
        return load(Datastore.fetchDefaultService());
    }

    /**
     * loads item from default datastore 
     * @param loadNested if true will also load all nested Base fields
     * @return true on success
     */
    public boolean load(boolean loadNested) {
        return load(Datastore.fetchDefaultService(), loadNested);
    }

    /**
     * loads item from specified datastore 
     * @param store the datastore to load the object from
     * @return true on success
     */
    public boolean load(Datastore store) {
        return load(store, false);
    }

    /**
     * loads item from specified datastore 
     * @param store the datastore to load the object from
     * @param loadNested if true, all nested Base fields will also get loaded
     * @return true on success
     */
    public boolean load(Datastore store, boolean loadNested) {
        return load(store, loadNested, new HashMap<>());
    }

    private boolean load(Datastore store, boolean loadNested, Map<Key, Document> loaded) {
        if(shallow)
            throw new MongomanException("Shallow objects cannot be loaded: " + this.getClass().getName());
        
        Key k = getKey();
        
        if(!loaded.containsKey(k))
            loaded.put(getKey(), store.get(k));

        Document data = loaded.get(k);
        
        if(data == null)
            return false;
        
        fromDocument(data);
        
        return loadNested ? loadNested(store, loaded) : true;
    }

    protected boolean loadNested(Datastore store, Map<Key, Document> loaded) {
        boolean result = true;

        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            try {
                Object value = field.get(this);

                if(value == null)
                    continue;

                /* dont load by reference object */
                if(field.isAnnotationPresent(Reference.class))
                    continue;

                if(value instanceof Base) {
                    if(!((Base)value).shallow)
                        result &= ((Base) value).load(store, true, loaded);
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        if(!b.shallow)                        
                            result &= b.load(store, true, loaded);
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            if(!b.shallow)
                                result &= b.load(store, true, loaded);
                    }
                } else if (value instanceof Map) {
                    Map m = (Map) value;

                    if(m.size() > 0 && m.keySet().iterator().next() instanceof String &&
                       Base.class.isAssignableFrom(m.values().iterator().next().getClass())) {

                        for(Base b : ((Map<String, Base>)m).values())
                            if(!b.shallow)
                                result &= b.load(store, true, loaded);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }

        return result;
    }
    
    /**
     * saves the entity to default datastore 
     * @return true if the item is new 
     */
    public boolean save() {
        return save(Datastore.fetchDefaultService(), false);
    }

    /**
     * saves the entity to default datastore 
     * @param saveNested if true, all Base fields will also get saved in their collections
     * @return true if the item is new
     */
    public boolean save(boolean saveNested) {
        return save(Datastore.fetchDefaultService(), saveNested);
    }

    /**
     * saves the entity to specified datastore 
     * @param store the datastore to save the object to
     * @return true if the item is new
     */
    public boolean save(Datastore store) {
        return save(store, false);
    }

    /**
     * saves the entity to specified datastore 
     * @param store the datastore to save the object to
     * @param saveNested if true, all Base fields will also get saved in their collections
     * @return true if the item is new
     */
    public boolean save(Datastore store, boolean saveNested) {
        return save(store, saveNested, null);
    }

        
    /**
     * saves the entity to specified datastore using specified write concern
     * @param concern the write concern to use
     * @return true if the item is new 
     */
    public boolean save(WriteConcern concern) {
        return save(Datastore.fetchDefaultService(), false, concern);
    }

    /**
     * saves the entity to specified datastore using specified write concern
     * @param store the datastore to save the object to
     * @param concern the write concern to use
     * @return true if the item is new 
     */
    public boolean save(Datastore store, WriteConcern concern) {
        return save(store, false, concern);
    }

    protected boolean save(Datastore store, boolean saveNested, WriteConcern concern) {
        if(shallow)
           throw new MongomanException("Shallow objects cannot be saved: " + this.getClass().getName());
        
        Document doc = toDocument(this.dbExportMode);
        
        ObjectId id = store.save(kind, getKey(), doc, concern);
        
        if(saveNested)
            saveNested(store);
        
        if(id != null)
           _id = id;
        
        return id != null;
    }
    
    public boolean replace() {
        return replace(Datastore.fetchDefaultService(), null);
    }
    
    protected boolean replace(Datastore store, WriteConcern concern) {
        if(shallow)
           throw new MongomanException("Shallow objects cannot be saved: " + this.getClass().getName());
        
        Document doc = toDocument(this.dbExportMode);
        
        return store.replace(kind, getKey(), doc, concern);
    }
    
    /**
     * Static method to save a list of Base objects
     * @param list the list of Base objects to save
     */
    public static void saveAll(List<? extends Base> list) {
        saveAll(Datastore.fetchDefaultService(), list);
    }

    /**
     * Static method to save a list of Base objects using a specified datastore
     * @param store the datastore to save the objects to
     * @param list the list of Base objects to save
     */
    public static void saveAll(Datastore store, List<? extends Base> list) {
        Map<String, List<Document>> map = new HashMap<>();
        
        for(Base b : list) {
            String kind = b.kind;
            if(!map.containsKey(kind))
                map.put(kind, new ArrayList<>());
            
            map.get(kind).add(b.toDocument(b.dbExportMode));
        }
        
        for(Map.Entry<String, List<Document>> e : map.entrySet())
            store.saveMany(e.getKey(), e.getValue());
    }
    
    /**
     * saves nested objects to the datastore
     * @param store the datastore to save the nested objects to
     */
    protected void saveNested(Datastore store) {        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            try {
                Object value = field.get(this);
                
                if(value == null)
                    continue;
            
                if(value instanceof Base) {
                    if(!((Base) value).shallow)
                        ((Base) value).save(store, true);
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        if(!b.shallow)
                            b.save(store, true);
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            if(!b.shallow)
                                b.save(store, true);
                    }
                } else if (value instanceof Map) {
                    Map m = (Map) value;

                    if(m.size() > 0 && m.keySet().iterator().next() instanceof String &&
                        Base.class.isAssignableFrom(m.values().iterator().next().getClass())) {

                        for(Base b : ((Map<String, Base>)m).values())
                            if(!b.shallow)
                                b.save(store, true);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
    }

    /**
     * deletes the entity from default datastore and memcache
     * @return true if the item was deleted
     */
    public boolean delete() {
        return delete(Datastore.fetchDefaultService());
    }
    
    /**
     * deletes the entity and optionally its nested objects from default datastore
     * @param nested whether to delete nested objects as well
     * @return true if the item was deleted
     */
    public boolean delete(boolean nested) {
        return delete(Datastore.fetchDefaultService(), nested);
    }
    
    /**
     * deletes the entity from the specified datastore
     * @param store the datastore to delete the object from
     * @return true if the item was deleted
     */
    public boolean delete(Datastore store) {
        return delete(store, false);
    }

    /**
     * deletes the entity and optionally its nested objects from the specified datastore
     * @param store the datastore to delete the object from
     * @param nested whether to delete nested objects as well
     * @return true if the item was deleted
     */
    public boolean delete(Datastore store, boolean nested) {
        if(shallow)
            throw new MongomanException("Shallow objects cannot be deleted: " + this.getClass().getName());
        
        if(nested)
            deleteNested(store);
        
        if(_id != null)
            return store.delete(kind, _id);
        
        return store.delete(getKey());
    }

    /**
     * deletes nested objects from the specified datastore
     * @param store the datastore to delete the nested objects from
     */
    protected void deleteNested(Datastore store) {        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            try {
                Object value = field.get(this);
                
                if(value == null)
                    continue;
            
                if(value instanceof Base) {
                    if(!((Base)value).shallow)
                        ((Base) value).delete(store, true);
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        if(!b.shallow)
                            b.delete(store, true);
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            if(!b.shallow)
                                b.delete(store, true);
                    }
                } else if (value instanceof Map) {
                    Map m = (Map) value;

                    if(m.size() > 0 && m.keySet().iterator().next() instanceof String &&
                        Base.class.isAssignableFrom(m.values().iterator().next().getClass())) {

                        for(Base b : ((Map<String, Base>)m).values())
                            if(!b.shallow)
                                b.delete(store, true);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
    }

    /**
     * TODO: mark item as loaded on loading, make sure key only loads do not count
     * Checks if the object is loaded
     * @return true if the object is loaded
     */
    public boolean isLoaded() {
        return _id != null;
    }

    @Override
    public boolean equals(Object object) {
        if(object == this)
            return true;
        
        if (object == null)
            return false;

        if(!(object instanceof Base))
            return false;
        
        Base base = (Base) object;
        
        return getKey().equals(base.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    
    private Object convertDBToField(Object value, TypeInfo type) {
        if(value == null)
            return null;
        
        if(type.isEnum())
            return Enum.valueOf(type.getEnumType(), value.toString());
        
        if(type.isBase())
           return createInstance(type.getBaseType(), (Document) value);
        
        if(type.isCollection())
            return convertDBToCollectionField((List<Object>)value, type);
            
        if(type.isArray() && value instanceof List)
            return convertDBToArrayField((List<Object>)value, type);
        
        if(type.isMap())
            return convertDBToMapField((Document)value, type);
        
        return type.isPrimitive() ? convertPrimitiveType(value, type.clazz) : value;
    }
    
    private Map convertDBToMapField(Document map, TypeInfo type) {
        TypeInfo tkey = type.getGenericArgument(0);
        TypeInfo tval = type.getGenericArgument(1);
        
        Map<Object, Object> result = new LinkedHashMap<>();
        
        for(Map.Entry<String, Object> e : map.entrySet()) {
            Object o = e.getValue();
            Object mkey = tkey.isEnum() ? Enum.valueOf(tkey.getEnumType(), e.getKey()) : e.getKey();
            Object mval = convertDBToField(o, tval);
            result.put(mkey, mval);
        }

        return result;
    }
    
    private Collection convertDBToCollectionField(List<Object> list, TypeInfo type) {        
        Collection<Object> result;
        
        if (type.isList())
            result = new ArrayList<>();
        else if (type.isSet())
            result = new HashSet<>();
        else
            return list;
        
        for (Object o : list) {
            result.add(convertDBToField(o, type.getGenericArgument(0)));
        }

        return result;
    }
    
    private Object convertDBToArrayField(List<Object> list, TypeInfo type) {
        Class<?> componentType = type.getComponentType();
        Object array = Array.newInstance(componentType, list.size());
        
        if(componentType.isPrimitive())
            copyPrimitiveArray(list.toArray(), array);
        else {
            TypeInfo itemType = new TypeInfo(componentType);

            for(int i=0; i < list.size(); i++) {
                Array.set(array, i, convertDBToField(list.get(i), itemType));
            }
        }

        return array;
    }
    
    /* Primitive types */
    private void copyPrimitiveArray(Object[] src, Object dst) {
        Class<?> type = dst.getClass().getComponentType();
        
        if(type == int.class) {
            int[] arr = (int[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).intValue();
        } 
        
        else if(type == short.class) {
            short[] arr = (short[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).shortValue();
        } 
        
        else if(type == long.class) {
            long[] arr = (long[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).longValue();
        }

        else if(type == float.class) {
            float[] arr = (float[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).floatValue();
        } 
        
        else if(type == double.class) {
            double[] arr = (double[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).doubleValue();
        }
                        
        else if(type == byte.class) {
            byte[] arr = (byte[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((Number)src[i]).byteValue();
        }
        
        else if(type == char.class) {
            char[] arr = (char[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = ((String) src[i]).charAt(0);
        } 
        
        else if(type == boolean.class) {
            boolean[] arr = (boolean[]) dst;
            for(int i=0; i < src.length; i++)
                arr[i] = (boolean) src[i];
        } 
    }
    
    private Object convertPrimitiveType(Object value, Class<?> type) {
        if(type == int.class)
            return ((Number)value).intValue();
        
        if(type == short.class)             
            return ((Number)value).shortValue();
        
        if(type == long.class)
            return ((Number)value).longValue();

        if(type == float.class)
            return ((Number)value).floatValue();
        
        if(type == double.class)
            return ((Number)value).doubleValue();            

        if(type == byte.class)
            return ((Number)value).byteValue();

        if(type == char.class)
            return ((String) value).charAt(0);
        
        if(type == boolean.class)
            return (boolean) value;
        
        return value;
    }

    /* Helper functions for saving */
    private static Object convertFieldToDB(Object value, boolean fullsave, ExportMode mode) {        
        if(value == null)
            return null;
        
        if(value instanceof Enum)
            return ((Enum)value).name();
        
        if(value instanceof Base)
            return convertBaseToDB((Base) value, fullsave, mode);
        
        if (value instanceof Base[]) {
            List<Document> list = new ArrayList<>();
            for(Base b : (Base[])value)
                list.add(convertBaseToDB((Base) b, fullsave, mode));
            
            return list;
        }

        /* Handle arrays (primitive and and non Base object arrays) */
        if (value.getClass().isArray())
            return convertArrayToDB(value, fullsave, mode);
        
        if (value instanceof Collection)
            return convertCollectionToDB((Collection)value, fullsave, mode);
        
        if (value instanceof Map)
            return convertMapToDB((Map)value, fullsave, mode);
        
        return value;
    }

    private static List<Object> convertArrayToDB(Object array, boolean fullsave, ExportMode mode) {
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(convertFieldToDB(Array.get(array, i), fullsave, mode));
        }
        return list;
    }
    
    private static List<Object> convertCollectionToDB(Collection<?> collection, boolean fullsave, ExportMode mode) {
        List<Object> list = new ArrayList<>();
        
        for (Object item : collection) {
            list.add(convertFieldToDB(item, fullsave, mode));
        }

        return list;
    }
    
    private static Document convertMapToDB(Map<?, ?> map, boolean fullsave, ExportMode mode) {
        Document dbObject = new Document();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String mKey = key instanceof Enum ? ((Enum) key).name() : key.toString();
            dbObject.put(mKey, convertFieldToDB(value, fullsave, mode));
        }
        
        return dbObject;
    }
    
    private static Document convertBaseToDB(Base obj, boolean fullsave, ExportMode mode) {
        if(obj == null)
            return null;
        
        return fullsave ? obj.toDocument(mode) : obj.getKey().data;
    }
}
