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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Helper functions for loading */
class TypeInfo {
    final Class<?> clazz;
    final Type genericType;

    public TypeInfo(Type type) {
        if (type instanceof Class<?>) {
            this.clazz = (Class<?>) type;
            this.genericType = type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            this.clazz = (Class<?>) parameterizedType.getRawType();
            this.genericType = parameterizedType;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public TypeInfo(Field field) {
        this.clazz = field.getType();
        this.genericType = field.getGenericType();
    }

    public TypeInfo getGenericArgument(int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
            
            if (index >= 0 && index < args.length)
                return new TypeInfo(args[index]);
            else
                throw new IllegalArgumentException("Index " + index + " out of bounds for type arguments");
        }
            
        throw new IllegalArgumentException("Type " + clazz.getName() + " does not have generic arguments");
    }

    public Class<?> getComponentType() {
        return clazz.getComponentType();
    }

    public boolean isEnum() {
        return clazz.isEnum();
    }

    public boolean isMap() {
        return Map.class.isAssignableFrom(clazz);
    }

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(clazz);
    }

    public boolean isList() {
        return List.class.isAssignableFrom(clazz);
    }

    public boolean isSet() {
        return Set.class.isAssignableFrom(clazz);
    }

    public boolean isArray() {
        return clazz.isArray();
    }

    public boolean isBase() {
        return Base.class.isAssignableFrom(clazz);
    }

    public boolean isPrimitive() {
        return clazz.isPrimitive();
    }

    public Class<Enum> getEnumType() {
        return (Class<Enum>) clazz;
    }

    public Class<? extends Base> getBaseType() {
        return (Class<? extends Base>) clazz;
    }
}
