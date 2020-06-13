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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.types.ObjectId;

/**
 *
 * @author Ahmed
 * Mar 29, 2016
 *
 */

public abstract class Base {
    /* collection name */
    private final String kind;
    
    /* Options */
    private Options options;
    
    /* Key */
    private Key key;
    
    /* mongo ObjectId */
    private ObjectId _id;

    /* underlying db entity */
    private DBObject loaded;
    
    public Base() {
        this(Options.getDefaultOptions());
    }

    public Base(Options options) {
        this.kind = ClassMap.getKind(this.getClass());
        this.options = new Options(options);
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
    protected DBObject toDBObject() {
        BasicDBObject data = new BasicDBObject();
        
        if(_id != null)
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
                
                if(options.ignoreNull && value == null)
                    continue;
                
                data.append(name, convertFieldToDB(value));
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
        
        return data;
    }
    
    /**
     * Uses key to checks if object exists in db
     * @return true if item is stored in db
     */
    public boolean exists() {
        return exists(Datastore.getDefaultService());
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
        return load(Datastore.getDefaultService());
    }
    
    /**
     * loads item from default datastore 
     * @param loadNested if true will also load all nested Base fields
     * @return true on success
     */
    public boolean load(boolean loadNested) {
        return load(Datastore.getDefaultService(), loadNested);
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
        DBObject data = store.get(getKey());
        
        if(data == null)
            return false;
        
        fromDBObject(data);
        
        return loadNested ? loadNested(store) : true;
    }
    
    protected boolean loadNested(Datastore store) {
        boolean result = true;
        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            try {
                Object value = field.get(this);
                
                if(value == null)
                    continue;
            
                if(value instanceof Base) {
                    result &= ((Base) value).load();
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        result &= b.load();
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            result &= b.load();
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
        return save(Datastore.getDefaultService(), false);
    }

    /**
     * saves the entity to default datastore 
     * @param saveNested if true all Base fields will also get saved in their collections
     * @return true if the item is new
     */
    public boolean save(boolean saveNested) {
        return save(Datastore.getDefaultService(), saveNested);
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
        return save(Datastore.getDefaultService(), false, concern);
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
        DBObject e = toDBObject();
        if(saveNested)
            saveNested(store);
        
        boolean isNew = store.put(kind, e, concern);
        
        if(isNew)
           _id = store.getObjectId(getKey());
        
        return isNew;
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
                    ((Base) value).save(true);
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        b.save(true);
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            b.save(true);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
    }
    
    /* deletes entity from datastore and memcache */
    public boolean delete() {
        return delete(Datastore.getDefaultService());
    }
    
    public boolean delete(boolean nested) {
        return delete(Datastore.getDefaultService(), nested);
        
    }
    
    /* deletes entity from datastore */
    public boolean delete(Datastore store) {
        return delete(store, false);
    }

    /* deletes entity from datastore and memcache */
    public boolean delete(Datastore store, boolean nested) {
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
                    ((Base) value).delete(true);
                } else if (value instanceof Base[]) {
                    for(Base b : (Base[]) value)
                        b.delete(true);
                } else if (value instanceof Collection) {
                    Collection l = (Collection) value;

                    if (l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                        for(Base b : (Collection<Base>) l)
                            b.delete(true);
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

        System.out.println("equals: " + object.getClass());

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
        
        return clazz.isPrimitive() ? convertPrimitiveType(value, clazz) : value;
    }
    
    private Collection convertDBToCollectionField(BasicDBList list, Field field) {
        Object[] data = list.toArray();
        Class<?> clazz = field.getType();
        Class<?> base = getGenericBaseType(field.getGenericType());
        
        if(List.class.isAssignableFrom(clazz)) {
            if(base == null)
                return list;

            List<Base> result = new ArrayList<>();

            for(Object o : data)
                if(o != null)
                    result.add(createInstance((Class<? extends Base>) base, (DBObject) o));

            return result;
        } else if(Set.class.isAssignableFrom(clazz)) {
            if(base == null)
                return new HashSet<>(list);

            Set<Base> result = new HashSet<>();

            for(Object o : data)
                if(o != null)
                    result.add(createInstance((Class<? extends Base>) base, (DBObject) o));

            return result;            
        }
        
        return list;
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
        if (genericType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType)genericType).getActualTypeArguments();
            
            if(types.length == 0)
                return null;
            
            Type type0 = types[0];
            
            if(type0 instanceof Class && Base.class.isAssignableFrom((Class<?>)types[0]))
                return (Class<?>)type0;
        }
        
        return null;
    }
    
    /* Helper functions for saving */
    private Object convertFieldToDB(Object value) {
        if(value == null)
            return null;
        
        if(value instanceof Base)
            return convertBaseToDB((Base) value);
        
        if (value instanceof Base[]) {
            BasicDBList list = new BasicDBList();
            for(Base b : (Base[])value)
                list.add(convertBaseToDB((Base) b));
            
            return list;
        }
        
        if (value instanceof Collection) {
            Collection l = (Collection)value; 

            if(l.size() > 0 && Base.class.isAssignableFrom(l.iterator().next().getClass())) {
                BasicDBList list = new BasicDBList();
                for(Base b : (Collection<Base>)l)
                    list.add(convertBaseToDB(b));
                
                return list;
            }
        }
        
        return value;
    }
    
    private DBObject convertBaseToDB(Base obj) {
        if(obj == null)
            return null;
        
        return options.fullSave ? obj.toDBObject() : obj.getKey().data;
    }

}
