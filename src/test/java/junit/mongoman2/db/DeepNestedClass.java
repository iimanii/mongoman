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

import org.mongoman2.Base;
import org.mongoman2.annotations.Kind;

/**
 *
 * @author ahmed
 */
@Kind("deep_nested_class")
public class DeepNestedClass extends Base {
    public final String deepNestedStringKey;
    public final int deepNestedIntKey;
    
    public List<Integer> deepNestedIntList;
    public Set<String> deepNestedStringSet;
    public Map<String, Double> deepNestedStringDoubleMap;

    public DeepNestedClass() {
        this(null, 0);
    }

    public DeepNestedClass(String deepNestedStringKey, int deepNestedIntKey) {
        this.deepNestedStringKey = deepNestedStringKey;
        this.deepNestedIntKey = deepNestedIntKey;
    }
    
    /* Compare all fields */
    public boolean compareTo(DeepNestedClass other) {
        if (other == null) return false;
        if (!Objects.equals(deepNestedStringKey, other.deepNestedStringKey)) return false;
        if (deepNestedIntKey != other.deepNestedIntKey) return false;
        if (!Objects.equals(deepNestedIntList, other.deepNestedIntList)) return false;
        if (!Objects.equals(deepNestedStringSet, other.deepNestedStringSet)) return false;
        if (!Objects.equals(deepNestedStringDoubleMap, other.deepNestedStringDoubleMap)) return false;
        return true;
    }

    /* Compare only keys */
    public boolean compareKeys(DeepNestedClass other) {
        if (other == null) return false;
        return Objects.equals(this.deepNestedStringKey, other.deepNestedStringKey) && this.deepNestedIntKey == other.deepNestedIntKey;
    }
}
