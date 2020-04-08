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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.bson.types.ObjectId;

/**
 *
 * @author Ahmed
 * Mar 29, 2016
 *
 */

public abstract class Base {
    /* kind */
    private final String kind;
    
    /* Key */
    private Key key;
    
    /* if true, all fields for nested objects will be included when saving this objects
       otherwise only keys for nested objects will be included
    */
    private final boolean fullSave;
    
    /* enable this to remove extra/obsolete properties found in database object 
       and are not defined in the class */
    protected final boolean ignoreUnknownProperties;
    
    /* whether or not data was loaded from db */
    private ObjectId _id;

    /* underlying db entity */
    private DBObject loaded;
    
    public Base(String collectionName) {
        this(collectionName, false, false);
    }

    public Base(String collectionName, boolean full) {
        this(collectionName, full, false);
    }

    public Base(String collectionName, boolean full, boolean ignore) {
        Kind.register(collectionName, this.getClass());
        this.kind = collectionName;
        this.fullSave = full;
        this.ignoreUnknownProperties = ignore;
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
            throw new MongomanException("All subclasses of Base Class must implement a constructor that takes no arguments");
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
            throw new MongomanException("All subclasses of Base Class must implement a constructor that takes no arguments");
        } catch(IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
            throw new MongomanException(ex);
        }
    }
    
    protected static DBObject getUniqueIndexes(Class<? extends Base> clazz) {
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
            
            data.append(name, 1);
        }
        
        return data;
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
                    field.set(this, convertValueToType(value, field.getType()));
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

                if(value instanceof Base)
                    if(fullSave)
                        data.append(name, ((Base) value).toDBObject());
                    else
                        data.append(name, ((Base) value).getKey().toDBObject());                        
                else
                    data.append(field.getName(), value);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new MongomanException(ex);
            }
        }
        
        return data;
    }
    
    /**
     * loads item default datastore 
     * @param store
     * @return true on success
     */
    public boolean load() {
        return load(Datastore.getDefaultService());
    }
    
    /**
     * loads item datastore 
     * @param store
     * @return true on success
     */
    public boolean load(Datastore store) {
        DBObject data = store.get(getKey());
        
        if(data == null)
            return false;
        
        fromDBObject(data);
        
        return loadNested(store);
    }
    
    protected boolean loadNested(Datastore store) {
        boolean result = true;
        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            if(Base.class.isAssignableFrom(field.getType())) {
                try {
                    result &= ((Base) field.get(this)).load();
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new MongomanException(ex);
                }
            }
        }
        
        return result;
    }
    
    /**
     * saves the entity to default datastore 
     * @return 
     */
    public boolean save() {
        return save(Datastore.getDefaultService());
    }
    
    /**
     * saves the entity to datastore 
     * @param store
     * @return 
     */
    public boolean save(Datastore store) {
        DBObject e = toDBObject();
        return store.put(kind, e) & saveNested(store);
    }
    

    protected boolean saveNested(Datastore store) {
        boolean result = true;
        
        /* Get all public fields of the class */
        Field[] fields = this.getClass().getFields();
        for(Field field : fields) {
            if(Base.class.isAssignableFrom(field.getType())) {
                try {
                    result &= ((Base) field.get(this)).save();
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new MongomanException(ex);
                }
            }
        }
        
        return result;
    }
    
    /* deletes entity from datastore and memcache */
    public boolean delete() {
        return delete(Datastore.getDefaultService());
    }
    
    /* deletes entity from datastore and memcache */
    public boolean delete(Datastore store) {
        return store.delete(getKey());
    }
    
    /**
     * TODO: mark item as loaded on loading, make sure key only loads do not count
     * @return 
     */
    public boolean isLoaded() {
        return _id != null;
    }
    
    private Object convertValueToType(Object value, Class<?> clazz) {
        if(value == null)
            return null;
        
        if(Base.class.isAssignableFrom(clazz))
           return createInstance((Class<? extends Base>)clazz, (DBObject) value);
        
        if(value instanceof BasicDBList) {
            if(!clazz.isArray())
                return value;
            
            Object[] data = ((BasicDBList)value).toArray();
            
            Object array = Array.newInstance(clazz.getComponentType(), data.length);
            
            if(!clazz.getComponentType().isPrimitive())
                System.arraycopy(data, 0, (Object[]) array, 0, data.length);
            else
                copyPrimitiveArray(data, array);
            return array;
        }
        
        return clazz.isPrimitive() ? convertPrimitiveType(value, clazz) : value;
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
}
