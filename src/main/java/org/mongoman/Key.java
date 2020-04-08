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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author ahmed
 */
public class Key {
    public final String kind;
    private final BasicDBObject data;
    
    protected Key(Base object) throws IllegalArgumentException, IllegalAccessException {
        kind = object.getKind();
        data = new BasicDBObject();
        
        /* Get all public fields of the class */
        Field[] fields = object.getClass().getFields();
        
        for(Field field : fields) {
            /* Must be final */
            if(!Modifier.isFinal(field.getModifiers()))
                continue;
            
            /* must not be static */
            if(Modifier.isStatic(field.getModifiers()))
                continue;
            
            String name = field.getName();

            Object value = field.get(object);
            
            /* in case of a Base class .. only use its key */
            if(value instanceof Base)
                data.append(name, ((Base) value).getKey().data);
            else
                data.append(field.getName(), value);
        }
    }
        
    protected BasicDBObject toDBObject() {
        return data;
    }
    
    @Override
    public String toString() {
        return data.toJson();
    }
}
