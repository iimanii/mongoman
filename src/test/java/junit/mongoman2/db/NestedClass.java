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
@Kind("nested_class")
public class NestedClass extends Base {
    public final String nestedKey;
    
    public int nestedInt;
    public List<Integer> nestedIntList;
    public Set<String> nestedStringSet;
    public Map<String, Double> nestedStringDoubleMap;
    @FullSave
    public DeepNestedClass fullySavedDeepNestedObject;
    @Reference
    public DeepNestedClass referencedDeepNestedObject;

    public NestedClass() {
        this(null);
    }

    public NestedClass(String nestedField) {
        this.nestedKey = nestedField;
    }
    
    /* Compare all fields */
    public boolean compareTo(NestedClass other) {
        if (other == null) return false;
        if (!Objects.equals(nestedKey, other.nestedKey)) return false;
        if (nestedInt != other.nestedInt) return false;
        if (!Objects.equals(nestedIntList, other.nestedIntList)) return false;
        if (!Objects.equals(nestedStringSet, other.nestedStringSet)) return false;
        if (!Objects.equals(nestedStringDoubleMap, other.nestedStringDoubleMap)) return false;
        if (fullySavedDeepNestedObject != null) {
            if (!fullySavedDeepNestedObject.compareTo(other.fullySavedDeepNestedObject)) return false;
        } else if (other.fullySavedDeepNestedObject != null) {
            return false;
        }
        return true;
    }

    /* Compare only keys */
    public boolean compareKeys(NestedClass other) {
        if (other == null) return false;
        return Objects.equals(this.nestedKey, other.nestedKey);
    }
}

