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
package junit.mongoman2.db;

import java.util.*;

import org.mongoman2.*;
import org.mongoman2.annotations.*;

/**
 *
 * @author ahmed
 */

@Kind("test_class")
public class TestClass extends Base {

    /* Final key field (inherently unique) */
    public final String uniqueId;

    /* Primitive types */
    public int intValue;
    public long longValue;
    public double doubleValue;
    public boolean booleanValue;

    /* Arrays of primitive types */
    public int[] intArray;
    public long[] longArray;
    public double[] doubleArray;
    public boolean[] booleanArray;

    /* Wrapper types */
    public Integer integerValue;
    public Long longObject;
    public Double doubleObject;
    public Boolean booleanObject;
    
    /* Wrapper types arrays */
    public Integer[] integerObjectArray;
    public Long[] longObjectArray;
    public Double[] doubleObjectArray;
    public Boolean[] booleanObjectArray;

    /* String and Date */
    public String stringValue;
    public Date dateValue;

    /* Enum */
    public static enum TestEnum { VALUE1, VALUE2, VALUE3 }
    public TestEnum enumValue;

    /* Collections: List and Set */
    public List<Integer> intList;
    public Set<String> stringSet;
    public Set<TestEnum> enumSet;

    /* Maps */
    public Map<String, Double> stringDoubleMap;
    public Map<TestEnum, String> enumKeyedMap;
    @FullSave
    public Map<String, ShallowClass> shallowObjectMap;
    public Map<String, NestedClass> nestedObjectMap;

    /* Arrays of Base class */
    public NestedClass[] nestedObjectArray;

    /* Set of NestedClass objects */
    public Set<NestedClass> nestedObjectSet;

    /* Nested object with @FullSave (fully saved with the parent) */
    @FullSave
    public NestedClass fullySavedNestedObject;

    /* Shallow object (fully saved with the parent) */
    @FullSave
    public ShallowClass fullySavedShallowObject;

    /* Referenced object (loaded by reference) */
    @Reference
    public NestedClass referencedObject;

    /* Default constructor (required) */
    public TestClass() {
        this(null);
    }

    /* Constructor with uniqueId (for testing) */
    public TestClass(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}