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

import org.mongoman2.annotations.Options;
import org.mongoman2.annotations.Kind;
import java.util.HashMap;


/**
 *
 * @author ahmed
 */
class ClassMap {
    protected static class classVariables {
        String kind;
        boolean shallow;
        boolean ignoreNull;
        boolean ignoreUnknownProperties;
    }
    
    private final static HashMap<String, Class<? extends Base>> KIND_MAP  = new HashMap<>();
    private final static HashMap<Class<? extends Base>, classVariables> CLASS_MAP = new HashMap<>();

    private static classVariables extract(Class<? extends Base> clazz) {
        Kind kind = clazz.getDeclaredAnnotation(Kind.class);
        
        if(kind == null)
            throw new MongomanException(clazz + " does not have @Kind annotation");
        
        classVariables variables = new classVariables();
        variables.kind = kind.value();
        variables.shallow = kind.shallow();
        
        Options options = clazz.getDeclaredAnnotation(Options.class);
        if(options != null) {
            variables.ignoreNull = options.ignoreNull();
            variables.ignoreUnknownProperties = options.ignoreUnknownProperties();
        }
        
        return variables;
    }
    
    private static synchronized void register(classVariables variables, Class<? extends Base> clazz) {
        String name = variables.kind;
        
        if(name == null || name.length() == 0)
            throw new MongomanException("Collection name cannot be null or empty for " + clazz);
        
        Class<? extends Base> clazz0 = KIND_MAP.get(name);

        if(clazz0 == null) {
            KIND_MAP.put(name, clazz);
            CLASS_MAP.put(clazz, variables);
        } else if (clazz0 != clazz)
            throw new MongomanException("Trying to register " + name + " to " + clazz + 
                                        ". Collection already associated with " + clazz0);
    }
    
    protected static final Class<? extends Base> getClass(String kind) {
        return KIND_MAP.get(kind);
    }
    
    protected static final String getKind(Class<? extends Base> clazz) {
        classVariables variables = CLASS_MAP.get(clazz);
        
        /* try to instetiate an object to register kind */
        if(variables == null) {
           variables = extract(clazz);
           register(variables, clazz);
        }
        
        return variables.kind;
    }
    
    protected static final classVariables getVariables(Class<? extends Base> clazz) {
        classVariables variables = CLASS_MAP.get(clazz);
        
        /* try to instetiate an object to register kind */
        if(variables == null) {
           variables = extract(clazz);
           register(variables, clazz);
        }
        
        return variables;
    }
}
