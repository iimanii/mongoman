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
package junit.mongoman;

import java.util.*;

import junit.mongoman.db.*;

/**
 *
 * @author ahmed
 */
public class Helper {

    /* Helper function to initialize ShallowClass */
    public static ShallowClass initShallowClass(long key) {
        ShallowClass shallowObj = new ShallowClass(key);
        shallowObj.shallowField = "shallowField1";
        shallowObj.shallowInt = 1;
        shallowObj.shallowIntList = Arrays.asList(1, 2, 3);
        shallowObj.shallowStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        shallowObj.shallowStringDoubleMap = new HashMap<>();
        shallowObj.shallowStringDoubleMap.put("key1", 1.1);
        shallowObj.shallowStringDoubleMap.put("key2", 2.2);
        return shallowObj;
    }

    /* Helper function to initialize DeepNestedClass */
    public static DeepNestedClass initDeepNestedClass(String deepNestedStringKey, int deepNestedIntKey) {
        DeepNestedClass deepNestedObj = new DeepNestedClass(deepNestedStringKey, deepNestedIntKey);
        deepNestedObj.deepNestedIntList = Arrays.asList(10, 20, 30);
        deepNestedObj.deepNestedStringSet = new HashSet<>(Arrays.asList("X", "Y", "Z"));
        deepNestedObj.deepNestedStringDoubleMap = new HashMap<>();
        deepNestedObj.deepNestedStringDoubleMap.put("key1", 100.1);
        deepNestedObj.deepNestedStringDoubleMap.put("key2", 200.2);
        return deepNestedObj;
    }

    /* Helper function to initialize NestedClass */
    public static NestedClass initNestedClass(String nestedKey) {
        NestedClass nestedObj = new NestedClass(nestedKey);
        nestedObj.nestedInt = 1;
        nestedObj.nestedIntList = Arrays.asList(100, 200, 300);
        nestedObj.nestedStringSet = new HashSet<>(Arrays.asList("L", "M", "N"));
        nestedObj.nestedStringDoubleMap = new HashMap<>();
        nestedObj.nestedStringDoubleMap.put("key1", 1000.1);
        nestedObj.nestedStringDoubleMap.put("key2", 2000.2);

        /* Initialize fully saved and referenced deep-nested objects */
        nestedObj.fullySavedDeepNestedObject = initDeepNestedClass("DeepFullySaved", 400);
        nestedObj.referencedDeepNestedObject = initDeepNestedClass("DeepReferenced", 500);

        return nestedObj;
    }
}