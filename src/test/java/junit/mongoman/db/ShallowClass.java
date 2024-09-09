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
package junit.mongoman.db;

import java.util.*;

import org.mongoman.*;

/**
 *
 * @author ahmed
 */

@Kind(value = "shallow_class", shallow = true)
public class ShallowClass extends Base {

    public final long shallowKey;
    
    public String shallowField;
    public int shallowInt;
    public List<Integer> shallowIntList;
    public Set<String> shallowStringSet;
    public Map<String, Double> shallowStringDoubleMap;

    public ShallowClass() {
        this(0);
    }

    public ShallowClass(long key) {
        this.shallowKey = key;
    }

    /* Compare all fields */
    public boolean compareTo(ShallowClass other) {
        if (other == null) return false;
        if (!Objects.equals(shallowField, other.shallowField)) return false;
        if (shallowInt != other.shallowInt) return false;
        if (!Objects.equals(shallowIntList, other.shallowIntList)) return false;
        if (!Objects.equals(shallowStringSet, other.shallowStringSet)) return false;
        if (!Objects.equals(shallowStringDoubleMap, other.shallowStringDoubleMap)) return false;
        return true;
    }

    /* Compare only keys */
    public boolean compareKeys(ShallowClass other) {
        if (other == null) return false;
        return this.shallowKey == other.shallowKey;
    }
}
