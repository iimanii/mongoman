/*
 * The MIT License
 *
 * Copyright 2020 ahmed.
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

/**
 *
 * @author ahmed
 */
public class Options {
    
    /** 
     * if true, all fields for nested objects will be included when saving this objects
     * otherwise only keys for nested objects will be included
     */
    public boolean fullSave;
    
    /**
     * does store null fields into database when saving
     */
    public boolean ignoreNull;
    
    /**
     * enable this to remove extra/obsolete properties found in database object 
     *  and are not defined in the class 
     */
    public boolean ignoreUnknownProperties;

    public Options() {
        this.fullSave = false;
        this.ignoreNull = false;
        this.ignoreUnknownProperties = false;
    }
    
    protected Options(Options copy) {
        this.fullSave = copy.fullSave;
        this.ignoreNull = copy.ignoreNull;
        this.ignoreUnknownProperties = copy.ignoreUnknownProperties;
    }
    
    /* Static stuff */
    private static Options defaultOptions = new Options();
    
    public static synchronized void setDefaultOptions(Options options) {
        defaultOptions = options;
    }
    
    public static Options getDefaultOptions() {
        return defaultOptions;
    }
}
