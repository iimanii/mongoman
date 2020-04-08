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

import java.util.HashMap;

/**
 *
 * @author ahmed
 */
public class Kind {
    
    private final static HashMap<String, Class<? extends Base>> KIND_MAP  = new HashMap<>();
    private final static HashMap<Class<? extends Base>, String> CLASS_MAP = new HashMap<>();

    protected static void register(String name, Class<? extends Base> clazz) {
        if(name == null)
            throw new MongomanException("Kind cannot be null");
        
        if(clazz == null)
            throw new MongomanException("Class cannot be null");
        
        Class<? extends Base> clazz0 = KIND_MAP.get(name);

        if(clazz0 == null) {
            KIND_MAP.put(name, clazz);
            CLASS_MAP.put(clazz, name);
        } else if (clazz0 != clazz)
            throw new MongomanException("Trying to register " + name + " to class " + clazz + 
                                        ". Collection already associated with class " + clazz0);
    }
    
    protected static final Class<? extends Base> getClass(String kind) {
        return KIND_MAP.get(kind);
    }
    
    protected static final String getKind(Class<? extends Base> clazz) {
        String kind = CLASS_MAP.get(clazz);
        
        /* try to instetiate an object to register kind */
        if(kind == null) {
            Base.createInstance(clazz);
            kind = CLASS_MAP.get(clazz);
        }
        
        return kind;
    }
}
