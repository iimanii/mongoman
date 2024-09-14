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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.Document;
import org.mongoman2.annotations.FullSave;

/**
 *
 * @author ahmed
 */
public class Filter {
    
    final Query.FilterOperator op;
    private String property;
    private Object value;
    private Filter[] filters;
    private Document cached;
    private String options;

    Filter(String property, Query.FilterOperator op, Object value) {
        this.op = op;
        this.property = property;

        if(value instanceof Base) {
            this.value = ((Base) value).getKey().filterData;
        } else if(value instanceof Enum) {
            this.value = ((Enum) value).name();  // Single enum to string
        } else if (value instanceof Collection) {
            this.value = handleCollection((Collection) value);  // Handle collections (including enums)
        } else if (value.getClass().isArray()) {
            this.value = handleArray(value);  // Handle arrays (including enums)
        } else {
            this.value = value;  // Primitive types or other objects
        }
    }
    
    Filter(Query.FilterOperator op, Filter... filters) {
        this.op = op;
        this.filters = filters;
    }

    public Filter options(String value) {
        this.options = value;
        cached = null;
        return this;
    }

    @Override
    public String toString() {
        return toDocument().toJson();
    }

    private Object handleCollection(Collection<?> collection) {
        if (collection.isEmpty())
            return collection;

        Object first = collection.iterator().next();
        if (first instanceof Enum) {
            List<String> enumNames = new ArrayList<>();
            for (Enum e : (Collection<Enum>) collection) {
                enumNames.add(e.name());  // Convert each enum to string
            }
            return enumNames;
        }

        return collection;  // Non-enum collections can be returned as-is
    }

    private Object handleArray(Object array) {
        if (array instanceof Enum[]) {
            List<String> enumNames = new ArrayList<>();
            for (Enum e : (Enum[]) array) {
                enumNames.add(e.name());  // Convert each enum to string
            }
            return enumNames;
        } else {
            int length = Array.getLength(array);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(array, i));
            }
            return list;
        }
    }
    
    protected Document toDocument() {
        if (cached != null)
            return new Document(cached);

        cached = new Document();
        
        if (property != null) {
            Document comp = new Document();
            comp.put(op.code, value);
            
            if (options != null)
                comp.put("$options", options);
            
            cached.put(property, comp);
        } else {
            List<Document> arr = new ArrayList<>();
            
            for (Filter f : filters) {
                arr.add(f.toDocument());
            }
            cached.put(op.code, arr);
        }
        
        return new Document(cached);
    }

    protected void validateFieldPath(Class<?> currentClass) {
        Filter.validateFieldPath(property, currentClass);
    }
    
    protected static void validateFieldPath(String property, Class<?> currentClass) {
        String[] parts = property.split("\\."); // Split the field path by dot for nested fields
        boolean isFullSaved = true; // Top-level class (TestClass) fields are fully saved by default
        
        /* Loop through each part of the nested path */
        for (String part : parts) {
            /* Get the field in the current class, this will throw NoSuchFieldException if the field doesn't exist */
                Field currentField;
                try {
                    currentField = currentClass.getField(part);
                } catch (NoSuchFieldException | SecurityException ex) {
                    throw new MongomanException(ex);
                }
            
            /* If the class is not fully saved and the field is not a key field, throw an exception */
            if (!isFullSaved && !Base.isKeyField(currentField))
                throw new MongomanException("Field '" + part + "' is invalid because the object is not fully saved.");

            /* Determine the type of the current field to move to the next class level */
            TypeInfo typeInfo = new TypeInfo(currentField);
            if (typeInfo.isCollection()) {
                currentClass = typeInfo.getGenericArgument(0).clazz; // Get the element type for collections (List/Set)
            } else if (typeInfo.isMap()) {
                currentClass = typeInfo.getGenericArgument(1).clazz; // Get the value type for Map<K, V>
            } else if (typeInfo.isArray()) {
                currentClass = typeInfo.getComponentType(); // Get the component type for arrays
            } else {
                currentClass = currentField.getType(); // Move to the next level for non-collection, non-map fields
            }
            
            /* Update the fully saved status for the next level */
            isFullSaved = currentField.isAnnotationPresent(FullSave.class); // Check if this field is marked with @FullSave
        }
    }
}
