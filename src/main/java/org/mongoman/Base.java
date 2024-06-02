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
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;

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
    private DBObject loaded;

    public Base() {
        ClassMap.classVariables v = ClassMap.getVariables(this.getClass());
        this.kind = v.kind;
        this.shallow = v.shallow;
        this.ignoreNull = v.ignoreNull;
        this.ignoreUnknownProperties = v.ignoreUnknownProperties;
        this.dbExportMode = new ExportMode(false, this.ignoreNull, false);
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
    protected static <T extends Base> T createInstance(Class<? extends Base> clazz, DBObject data) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            T item = (T)constructor.newInstance();
            item.fromDBObject(data);
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
    
    protected static BasicDBObject getKeyFields(Class<? extends Base> clazz) {
        BasicDBObject data = new BasicDBObject();
        
        /* Get all public fields of the class */
        Field[] fields = clazz.getFields();
        
        for(Field field : fields) {
            /* Must be final */
            if(!Modifier.isFinal(field.getModifiers()))
                continue;
            
            /* must not be static */
            if(Modifier.isStatic(field.getModifiers()))
                continue;
            
            String name = field.getName();
            Class<?> field_class = field.getType();
            
            if(Base.class.isAssignableFrom(field_class)) {
                BasicDBObject inner = (BasicDBObject) getKeyFields((Class<? extends Base>) field_class);
                for(String n : inner.keySet()) {
                    data.append(name + "." + n, 1);
                }
            } else
                data.append(name, 1);
        }
        
        return data;
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
    protected void fromDBObject(DBObject data) {
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            String name = field.getName();
            
            if(data.containsField(name)) {
                if(Modifier.isFinal(field.getModifiers()))
                    field.setAccessible(true);

                Object value = data.get(name);
                
                try {
                    field.set(this, convertDBToField(value, field));
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new MongomanException(ex);
                }
            }
        }
        
        _id = (ObjectId) data.get("_id");
        loaded = data;
    }
    
    /* creates dbobject from item */
    protected BasicDBObject toDBObject(ExportMode mode) {
        BasicDBObject data = new BasicDBObject();
        
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
                
                data.append(name, convertFieldToDB(value, mode.export_inner || field.isAnnotationPresent(FullSave.class), mode));
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
    
    public String toJSON(ExportMode mode) {
        BasicDBObject data = toDBObject(mode);
        
        JsonWriterSettings settings = DEFAULT_JSONWRITER_SETTINGS;
        
        return data.toJson(settings);
    }
    
    /**
     * Uses key to checks if object exists in db
     * @return true if item is stored in db
     */
    public boolean exists() {
        return exists(Datastore.fetchDefaultService());
    }
        
    /**
     * Uses key to checks if object exists in db
     * @param store
     * @return true if item is stored in db
     */
    public boolean exists(Datastore store) {
        return store.exists(getKey());
    }
    
    /**
     * loads item default datastore 
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
     * loads item datastore 
     * @param store
     * @return true on success
     */
    public boolean load(Datastore store) {
        return load(store, false);
    }
    
    /**
     * loads item datastore 
     * @param store
     * @param loadNested if true will also load all nested Base fields
     * @return true on success
     */
    public boolean load(Datastore store, boolean loadNested) {
//        System.out.println(this.getKey() + " " + loadNested);
        return load(store, loadNested, new HashMap<>());
    }
    
    private boolean load(Datastore store, boolean loadNested, Map<Key, DBObject> loaded) {
        if(shallow)
           throw new MongomanException("Shallow objects cannot be loaded: " + this.getClass().getName());
        
        Key k = getKey();
        
        if(!loaded.containsKey(k))
            loaded.put(getKey(), store.get(k));

        DBObject data = loaded.get(k);
        
        if(data == null)
            return false;
        
        fromDBObject(data);
        
        return loadNested ? loadNested(store, loaded) : true;
    }
    
    protected boolean loadNested(Datastore store, Map<Key, DBObject> loaded) {
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
     * @param saveNested if true all Base fields will also get saved in their collections
     * @return true if the item is new
     */
    public boolean save(boolean saveNested) {
        return save(Datastore.fetchDefaultService(), saveNested);
    }
    
    /**
     * saves the entity to default datastore 
     * @param store
     * @return true if the item is new
     */
    public boolean save(Datastore store) {
        return save(store, false);
    }
    
    /**
     * saves the entity to datastore 
     * @param store
     * @param saveNested if true all Base fields will also get saved in their collections
     * @return true if the item is new
     */
    public boolean save(Datastore store, boolean saveNested) {
        return save(store, saveNested, null);
    }

        
    /**
     * saves the entity to default datastore using specified write concern
     * @param concern
     * @return true if the item is new 
     */
    public boolean save(WriteConcern concern) {
        return save(Datastore.fetchDefaultService(), false, concern);
    }
    
    /**
     * saves the entity to specified datastore using specified write concern
     * @param store
     * @param concern
     * @return true if the item is new 
     */
    public boolean save(Datastore store, WriteConcern concern) {
        return save(store, false, concern);
    }
    
    protected boolean save(Datastore store, boolean saveNested, WriteConcern concern) {
        if(shallow)
           throw new MongomanException("Shallow objects cannot be saved: " + this.getClass().getName());
        
        DBObject e = toDBObject(this.dbExportMode);
        
        boolean isNew = store.save(kind, e, concern);
        
        if(saveNested)
            saveNested(store);
        
        if(isNew)
           _id = store.getObjectId(getKey());
        
        return isNew;
    }
    
    public static void saveAll(List<? extends Base> list) {
        saveAll(Datastore.fetchDefaultService(), list);
    }
    
    public static void saveAll(Datastore store, List<? extends Base> list) {
        Map<String, List<DBObject>> map = new HashMap<>();
        
        for(Base b : list) {
            String kind = b.kind;
            if(!map.containsKey(kind))
                map.put(kind, new ArrayList<>());
            
            map.get(kind).add(b.toDBObject(b.dbExportMode));
        }
        
        for(Map.Entry<String, List<DBObject>> e : map.entrySet())
            store.saveMany(e.getKey(), e.getValue());
    }
    
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
    
    /* deletes entity from datastore and memcache */
    public boolean delete() {
        return delete(Datastore.fetchDefaultService());
    }
    
    public boolean delete(boolean nested) {
        return delete(Datastore.fetchDefaultService(), nested);
        
    }
    
    /* deletes entity from datastore */
    public boolean delete(Datastore store) {
        return delete(store, false);
    }

    /* deletes entity from datastore and memcache */
    public boolean delete(Datastore store, boolean nested) {
        if(shallow)
           throw new MongomanException("Shallow objects cannot be deleted: " + this.getClass().getName());
        
        if(nested)
            deleteNested(store);
        
        if(_id != null)
            return store.delete(kind, _id);
        
        return store.delete(getKey());
    }
    
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
     * @return 
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
    
    /* Helper functions for loading */
    private Object convertDBToField(Object value, Field field) {
        if(value == null)
            return null;
        
        Class<?> clazz = field.getType();
        
        if(Enum.class.isAssignableFrom(clazz))
            return Enum.valueOf((Class<Enum>)clazz, value.toString());
        
        if(Base.class.isAssignableFrom(clazz))
           return createInstance((Class<? extends Base>)clazz, (DBObject) value);
        
        if(value instanceof BasicDBList) {
            BasicDBList list = (BasicDBList)value;

            if(Collection.class.isAssignableFrom(clazz))
                return convertDBToCollectionField(list, field);
            
            if(clazz.isArray())
                return convertDBToArrayField(list, clazz);
            
            return value;
        }
        
        if(value instanceof BasicDBObject) {
            BasicDBObject map = (BasicDBObject)value;

            if(Map.class.isAssignableFrom(clazz))
                return convertDBToMapField(map, field);
            
            return value;            
        }
        
        return clazz.isPrimitive() ? convertPrimitiveType(value, clazz) : value;
    }
    
    private Map convertDBToMapField(BasicDBObject map, Field field) {
        Class<?> ckey = (Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
        Class<?> cval = getGenericBaseType(field.getGenericType(), 1);
                
        boolean isKeyEnum = Enum.class.isAssignableFrom(ckey);
        boolean isValBase = cval != null;
        
        if(!isKeyEnum && !isValBase)
            return map;
        
        Map<Object, Object> result = new LinkedHashMap<>();
        
        for(Map.Entry<String, Object> e : map.entrySet()) {
            Object o = e.getValue();
            if(o != null) {
                Object mkey = isKeyEnum ? Enum.valueOf((Class<Enum>)ckey, e.getKey()) : e.getKey();
                Object mval = isValBase ? createInstance((Class<? extends Base>) cval, (DBObject) o) : o;
                result.put(mkey, mval);
            }
        }

        return result;
    }
    
    private Collection convertDBToCollectionField(BasicDBList list, Field field) {
        Object[] data = list.toArray();
        Class<?> clazz = field.getType();
        Class<?> base = getGenericBaseType(field.getGenericType());
        Class<Enum> enumm = getGenericEnumType(field.getGenericType());
        
        Collection<?> result;
        
        if (List.class.isAssignableFrom(clazz))
            result = new ArrayList<>();
        else if (Set.class.isAssignableFrom(clazz))
            result = new HashSet<>();
        else
            return list;
        
        for (Object o : data) {
            if (o != null) {
                if (enumm != null)
                    ((Collection<Enum>) result).add(Enum.valueOf(enumm, o.toString()));
                else if (base != null)
                    ((Collection<Base>) result).add(createInstance((Class<? extends Base>) base, (DBObject) o));
                else
                    ((Collection<Object>) result).add(o);
            }
        }

        return result;
    }
    
    private Object convertDBToArrayField(BasicDBList list, Class<?> clazz) {
        Object[] data = list.toArray();
        Object array = Array.newInstance(clazz.getComponentType(), data.length);
        Class<?> component = clazz.getComponentType();

        if(!component.isPrimitive()) {
            if(Base.class.isAssignableFrom(component)) {
                Base[] a = (Base[]) array;
                for(int i=0; i < data.length; i++)
                    if(data[i] != null)
                        a[i] = createInstance((Class<? extends Base>)clazz.getComponentType(), (DBObject) data[i]);
            } else
                System.arraycopy(data, 0, (Object[]) array, 0, data.length);
        } else
            copyPrimitiveArray(data, array);

        return array;
    }
    
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

    private Class<?> getGenericBaseType(Type genericType) {
        return getGenericBaseType(genericType, 0);
    }
    
    private Class<?> getGenericBaseType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType)genericType).getActualTypeArguments();
            
            if(types.length == 0)
                return null;
            
            Type type0 = types[index];
            
            if(type0 instanceof Class && Base.class.isAssignableFrom((Class<?>)types[index]))
                return (Class<?>)type0;
        }
        
        return null;
    }
    
    private Class<Enum> getGenericEnumType(Type genericType) {
        return getGenericEnumType(genericType, 0);
    }
    
    private Class<Enum> getGenericEnumType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType)genericType).getActualTypeArguments();
            
            if(types.length == 0)
                return null;
            
            Type type0 = types[index];
            
            if(type0 instanceof Class && Enum.class.isAssignableFrom((Class<?>)types[index]))
                return (Class<Enum>)type0;
        }
        
        return null;
    }
    
    /* Helper functions for saving */
    private Object convertFieldToDB(Object value, boolean fullsave, ExportMode mode) {
        if(value == null)
            return null;
        
        if(value instanceof Enum)
            return ((Enum)value).name();
        
        if(value instanceof Base)
            return convertBaseToDB((Base) value, fullsave, mode);
        
        if (value instanceof Base[]) {
            BasicDBList list = new BasicDBList();
            for(Base b : (Base[])value)
                list.add(convertBaseToDB((Base) b, fullsave, mode));
            
            return list;
        }
        
        if (value instanceof Collection) {
            Collection l = (Collection)value; 

            if(l.size() > 0) {
                Object first = l.iterator().next();
                
                if(Base.class.isAssignableFrom(first.getClass())) {
                    BasicDBList list = new BasicDBList();
                    for(Base b : (Collection<Base>)l)
                        list.add(convertBaseToDB(b, fullsave, mode));

                    return list;
                }
                
                if(first instanceof Enum) {
                    BasicDBList list = new BasicDBList();
                    for(Enum e : (Collection<Enum>)l)
                        list.add(e.name());

                    return list;                    
                }
            }
        }
        
        if (value instanceof Map) {
            Map<Object, Object> m = (Map)value; 
            
            if(m.size() > 0) {                
                Class<?> ckey = m.keySet().iterator().next().getClass();
                Class<?> cval = m.values().iterator().next().getClass();
                
                boolean isKeyEnum = Enum.class.isAssignableFrom(ckey);
                boolean isValBase = Base.class.isAssignableFrom(cval);
                
                if(isKeyEnum || isValBase) {
                    BasicDBObject map = new BasicDBObject();
                    
                    for(Map.Entry<Object, Object> e : m.entrySet()) {
                        String key = isKeyEnum ? e.getKey().toString() : (String) e.getKey();
                        Object val = isValBase ? convertBaseToDB((Base)e.getValue(), fullsave, mode) : e.getValue();
                        map.put(key, val);
                    }
    
                    return map;
                }
            }
        }
        
        return value;
    }
    
    private DBObject convertBaseToDB(Base obj, boolean fullsave, ExportMode mode) {
        if(obj == null)
            return null;
        
        return fullsave || obj.shallow ? obj.toDBObject(mode) : obj.getKey().data;
    }
}
