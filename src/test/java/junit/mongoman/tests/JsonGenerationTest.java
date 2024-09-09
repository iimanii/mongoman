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
package junit.mongoman.tests;

import java.util.*;
import org.json.*;
import org.junit.*;

import junit.mongoman.*;
import junit.mongoman.db.*;
import org.mongoman.*;

/**
 *
 * @author ahmed
 */
public class JsonGenerationTest extends BaseTest {
    
    @Test
    public void generateJSON_IncludeNull() {
        /* Create a test object with some fields set to null */
        TestClass testObj = new TestClass("unique_001");
        testObj.intValue = 100;
        testObj.stringValue = null;  /* Null field */
        testObj.enumValue = null;  /* Null enum */
        testObj.nestedObjectMap = new HashMap<>();
        testObj.nestedObjectMap.put("nested1", Helper.initNestedClass("Nested1"));
        testObj.nestedObjectMap.put("nested2", null);  /* Null nested object */

        /* Instantiate fully saved nested object, but set some fields to null */
        testObj.fullySavedNestedObject = Helper.initNestedClass("FullySavedNested");
        testObj.fullySavedNestedObject.nestedIntList = null;  /* Set a collection field to null */
        testObj.fullySavedNestedObject.nestedStringSet = null;  /* Set a set field to null */

        /* Initialize a referenced object and set some of its fields to null */
        testObj.referencedObject = Helper.initNestedClass("ReferencedNested");
        testObj.referencedObject.nestedIntList = null;  /* This should NOT be reflected in the JSON */
        testObj.referencedObject.nestedStringSet = null;  /* This should NOT be reflected in the JSON */

        /* Generate JSON with null fields included */
        String json = testObj.toJSON(false);

        /* Parse JSON and verify that null fields are included */
        JSONObject jsonObject = new JSONObject(json);
        Assert.assertTrue(jsonObject.has("stringValue"));
        Assert.assertTrue(jsonObject.isNull("stringValue"));
        Assert.assertTrue(jsonObject.has("enumValue"));
        Assert.assertTrue(jsonObject.isNull("enumValue"));
        Assert.assertTrue(jsonObject.getJSONObject("nestedObjectMap").has("nested2"));  /* Null nested object in map should be included */
        Assert.assertTrue(jsonObject.getJSONObject("nestedObjectMap").isNull("nested2"));  /* Null nested object should be explicitly null */

        /* Verify that fully saved nested object includes null fields */
        JSONObject fullySavedNestedObject = jsonObject.getJSONObject("fullySavedNestedObject");
        Assert.assertTrue(fullySavedNestedObject.has("nestedIntList"));
        Assert.assertTrue(fullySavedNestedObject.isNull("nestedIntList"));
        Assert.assertTrue(fullySavedNestedObject.has("nestedStringSet"));
        Assert.assertTrue(fullySavedNestedObject.isNull("nestedStringSet"));

        /* Verify that referenced object does NOT include null fields, as it's not fully saved */
        JSONObject referencedObject = jsonObject.getJSONObject("referencedObject");
        Assert.assertFalse(referencedObject.has("nestedIntList"));  /* Referenced object should not have null fields included */
        Assert.assertFalse(referencedObject.has("nestedStringSet"));  /* Referenced object should not have null fields included */
        
        System.out.println("Test passed: JSON generated with null fields included.");
    }
    
    @Test
    public void generateJSON_IgnoreNull() {
        /* Create a test object with some fields set to null */
        TestClass testObj = new TestClass("unique_002");
        testObj.intValue = 100;
        testObj.stringValue = null;  /* Null field */
        testObj.enumValue = null;  /* Null enum */
        testObj.nestedObjectMap = new HashMap<>();
        testObj.nestedObjectMap.put("nested1", Helper.initNestedClass("Nested1"));
        testObj.nestedObjectMap.put("nested2", null);  /* Null nested object */

        /* Instantiate fully saved nested object, but set some fields to null */
        testObj.fullySavedNestedObject = Helper.initNestedClass("FullySavedNested");
        testObj.fullySavedNestedObject.nestedIntList = null;  /* Set a collection field to null */
        testObj.fullySavedNestedObject.nestedStringSet = null;  /* Set a set field to null */

        /* Initialize a referenced object and set some of its fields to null */
        testObj.referencedObject = Helper.initNestedClass("ReferencedNested");
        testObj.referencedObject.nestedIntList = null;  /* This should NOT be reflected in the JSON */
        testObj.referencedObject.nestedStringSet = null;  /* This should NOT be reflected in the JSON */

        /* Generate JSON with null fields ignored */
        String json = testObj.toJSON(true);

        /* Parse JSON and verify that null fields are ignored */
        JSONObject jsonObject = new JSONObject(json);
        Assert.assertFalse(jsonObject.has("stringValue"));  /* Null string should be ignored */
        Assert.assertFalse(jsonObject.has("enumValue"));  /* Null enum should be ignored */

        /* Maps should still contain null values as they are part of the map */
        Assert.assertTrue(jsonObject.getJSONObject("nestedObjectMap").has("nested2"));  /* Null nested object in map should be included */
        Assert.assertTrue(jsonObject.getJSONObject("nestedObjectMap").isNull("nested2"));  /* Null nested object should be explicitly null */
        
        /* Verify that fully saved nested object does NOT include null fields */
        JSONObject fullySavedNestedObject = jsonObject.getJSONObject("fullySavedNestedObject");
        Assert.assertFalse(fullySavedNestedObject.has("nestedIntList"));  /* Null fields should be ignored */
        Assert.assertFalse(fullySavedNestedObject.has("nestedStringSet"));  /* Null fields should be ignored */

        /* Verify that referenced object does NOT include null fields, as it's not fully saved */
        JSONObject referencedObject = jsonObject.getJSONObject("referencedObject");
        Assert.assertFalse(referencedObject.has("nestedIntList"));  /* Referenced object should not have null fields included */
        Assert.assertFalse(referencedObject.has("nestedStringSet"));  /* Referenced object should not have null fields included */

        System.out.println("Test passed: toJSON with ignoreNull");
    }
    
    @Test
    public void generateJSON_Map_No_FullSave() {
        /* Create a map of nested objects (without @FullSave) */
        Map<String, NestedClass> nestedMap = new HashMap<>();
        nestedMap.put("nested1", Helper.initNestedClass("NestedKey1"));
        nestedMap.put("nested2", Helper.initNestedClass("NestedKey2"));

        /* Generate JSON */
        String json = Base.toJSON(nestedMap, false, false);  // No fullsave, no ignore_null

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);

        /* Retrieve and validate the nested objects */
        JSONObject nested1 = jsonObject.getJSONObject("nested1");
        JSONObject nested2 = jsonObject.getJSONObject("nested2");

        /* Ensure only the 'nestedKey' is saved for both objects */
        Assert.assertTrue(nested1.has("nestedKey"));
        Assert.assertEquals("NestedKey1", nested1.getString("nestedKey"));

        Assert.assertTrue(nested2.has("nestedKey"));
        Assert.assertEquals("NestedKey2", nested2.getString("nestedKey"));

        /* Ensure no other fields are present for nested1 */
        Assert.assertFalse(nested1.has("nestedInt"));
        Assert.assertFalse(nested1.has("nestedIntList"));
        Assert.assertFalse(nested1.has("nestedStringSet"));
        Assert.assertFalse(nested1.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested1.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested1.has("referencedDeepNestedObject"));

        /* Ensure no other fields are present for nested2 */
        Assert.assertFalse(nested2.has("nestedInt"));
        Assert.assertFalse(nested2.has("nestedIntList"));
        Assert.assertFalse(nested2.has("nestedStringSet"));
        Assert.assertFalse(nested2.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested2.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested2.has("referencedDeepNestedObject"));

        System.out.println("Test passed: JSON generated for Map without fullsave.");
    }

    @Test
    public void generateJSON_Map_FullSave_IgnoreNull() {
        /* Create a map of nested objects with @FullSave and some null fields */
        Map<String, NestedClass> nestedMap = new HashMap<>();
        NestedClass nested1 = Helper.initNestedClass("NestedKey1");
        NestedClass nested2 = new NestedClass("NestedKey2");  // Empty object with null fields

        nested1.nestedInt = 123;
        nested1.nestedIntList = Arrays.asList(1, 2, 3);
        nested1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nested1.fullySavedDeepNestedObject = Helper.initDeepNestedClass("DeepKey1", 456);
        nested1.referencedDeepNestedObject = Helper.initDeepNestedClass("DeepKey2", 789);
        nested1.nestedStringDoubleMap = null;

        nestedMap.put("nested1", nested1);
        nestedMap.put("nested2", nested2);  // Object with null fields

        /* Generate JSON */
        String json = Base.toJSON(nestedMap, true, true);  // FullSave and ignore null fields

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);

        /* Retrieve and validate nested1 object */
        JSONObject nested1Json = jsonObject.getJSONObject("nested1");
        Assert.assertEquals(nested1.nestedKey, nested1Json.getString("nestedKey"));
        Assert.assertEquals(nested1.nestedInt, nested1Json.getInt("nestedInt"));

        /* Check fully saved deep nested object for nested1 */
        JSONObject fullySavedDeepNestedJson = nested1Json.getJSONObject("fullySavedDeepNestedObject");
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedStringKey, fullySavedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedIntKey, fullySavedDeepNestedJson.getInt("deepNestedIntKey"));

        /* Ensure null fields are not present */
        Assert.assertFalse(nested1Json.has("nestedStringDoubleMap"));
        
        /* Check that referencedDeepNestedObject exists but only contains the key field */
        JSONObject referencedDeepNestedJson = nested1Json.getJSONObject("referencedDeepNestedObject");
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedStringKey, referencedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedIntKey, referencedDeepNestedJson.getInt("deepNestedIntKey"));
        Assert.assertFalse(referencedDeepNestedJson.has("deepNestedIntList"));   // Ensure other fields are not present

        /* Retrieve and validate nested2 object */
        JSONObject nested2Json = jsonObject.getJSONObject("nested2");
        Assert.assertEquals(nested2.nestedKey, nested2Json.getString("nestedKey"));

        /* Ensure no other fields are present for nested2 (since they were null and ignored) */
        Assert.assertFalse(nested2Json.has("nestedIntList"));
        Assert.assertFalse(nested2Json.has("nestedStringSet"));
        Assert.assertFalse(nested2Json.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested2Json.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested2Json.has("referencedDeepNestedObject"));

        System.out.println("Test passed: JSON generated for Map with fullsave and null fields ignored.");
    }

    @Test
    public void generateJSON_Map_FullSave_IncludeNull() {
        /* Create a map of nested objects with some null fields */
        Map<String, NestedClass> nestedMap = new HashMap<>();
        NestedClass nested1 = Helper.initNestedClass("NestedKey1");
        NestedClass nested2 = new NestedClass("NestedKey2");  // Empty object with null fields

        nested1.nestedInt = 123;
        nested1.nestedIntList = Arrays.asList(1, 2, 3);
        nested1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nested1.fullySavedDeepNestedObject = Helper.initDeepNestedClass("DeepKey1", 456);
        nested1.referencedDeepNestedObject = Helper.initDeepNestedClass("DeepKey2", 789);
        nested1.nestedStringDoubleMap = null;  /* This should appear as null in the output */

        nestedMap.put("nested1", nested1);
        nestedMap.put("nested2", nested2);  /* Object with null fields */

        /* Generate JSON */
        String json = Base.toJSON(nestedMap, true, false);  // FullSave and include null fields

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);

        /* Retrieve and validate nested1 object */
        JSONObject nested1Json = jsonObject.getJSONObject("nested1");
        Assert.assertEquals(nested1.nestedKey, nested1Json.getString("nestedKey"));
        Assert.assertEquals(nested1.nestedInt, nested1Json.getInt("nestedInt"));

        /* Check fully saved deep nested object for nested1 */
        JSONObject fullySavedDeepNestedJson = nested1Json.getJSONObject("fullySavedDeepNestedObject");
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedStringKey, fullySavedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedIntKey, fullySavedDeepNestedJson.getInt("deepNestedIntKey"));

        /* Ensure null fields are present */
        Assert.assertTrue(nested1Json.has("nestedStringDoubleMap"));
        Assert.assertTrue(nested1Json.isNull("nestedStringDoubleMap"));

        /* Check that referencedDeepNestedObject exists but only contains the key field */
        JSONObject referencedDeepNestedJson = nested1Json.getJSONObject("referencedDeepNestedObject");
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedStringKey, referencedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedIntKey, referencedDeepNestedJson.getInt("deepNestedIntKey"));
        Assert.assertFalse(referencedDeepNestedJson.has("deepNestedIntList"));   // Ensure other fields are not present

        /* Retrieve and validate nested2 object */
        JSONObject nested2Json = jsonObject.getJSONObject("nested2");
        Assert.assertEquals(nested2.nestedKey, nested2Json.getString("nestedKey"));

        /* Ensure other fields are present for nested2 (even though they were null and should be included) */
        Assert.assertTrue(nested2Json.has("nestedIntList"));
        Assert.assertTrue(nested2Json.isNull("nestedIntList"));
        Assert.assertTrue(nested2Json.has("nestedStringSet"));
        Assert.assertTrue(nested2Json.isNull("nestedStringSet"));
        Assert.assertTrue(nested2Json.has("nestedStringDoubleMap"));
        Assert.assertTrue(nested2Json.isNull("nestedStringDoubleMap"));
        Assert.assertTrue(nested2Json.has("fullySavedDeepNestedObject"));
        Assert.assertTrue(nested2Json.isNull("fullySavedDeepNestedObject"));
        Assert.assertTrue(nested2Json.has("referencedDeepNestedObject"));
        Assert.assertTrue(nested2Json.isNull("referencedDeepNestedObject"));

        System.out.println("Test passed: JSON generated for Map with fullsave and null fields included.");
    }
    
    @Test
    public void generateJSON_Collection_No_FullSave() {
        /* Create a collection of nested objects */
        List<NestedClass> nestedCollection = new ArrayList<>();
        NestedClass nested1 = Helper.initNestedClass("NestedKey1");
        NestedClass nested2 = new NestedClass("NestedKey2");  // Empty object with null fields

        nested1.nestedInt = 123;
        nested1.nestedIntList = Arrays.asList(1, 2, 3);
        nested1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nested1.fullySavedDeepNestedObject = Helper.initDeepNestedClass("DeepKey1", 456);
        nested1.referencedDeepNestedObject = Helper.initDeepNestedClass("DeepKey2", 789);
        nested1.nestedStringDoubleMap = null;

        nestedCollection.add(nested1);
        nestedCollection.add(nested2);  /* Object with null fields */

        /* Generate JSON without fullsave */
        String json = Base.toJSON(nestedCollection, "nestedCollection", false, false);  // Not FullSave and include null fields

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);
        JSONArray nestedArray = jsonObject.getJSONArray("nestedCollection");

        /* Validate nested1 object (without fullsave, only keys should be present for referenced objects) */
        JSONObject nested1Json = nestedArray.getJSONObject(0);
        Assert.assertEquals(nested1.nestedKey, nested1Json.getString("nestedKey"));

        /* Ensure no other fields are present for nested1 */
        Assert.assertFalse(nested1Json.has("nestedInt"));
        Assert.assertFalse(nested1Json.has("nestedIntList"));
        Assert.assertFalse(nested1Json.has("nestedStringSet"));
        Assert.assertFalse(nested1Json.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested1Json.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested1Json.has("referencedDeepNestedObject"));

        /* Validate nested2 object (with null fields, but only key should be present) */
        JSONObject nested2Json = nestedArray.getJSONObject(1);
        Assert.assertEquals(nested2.nestedKey, nested2Json.getString("nestedKey"));

        /* Ensure no other fields are present for nested2 */
        Assert.assertFalse(nested2Json.has("nestedInt"));
        Assert.assertFalse(nested2Json.has("nestedIntList"));
        Assert.assertFalse(nested2Json.has("nestedStringSet"));
        Assert.assertFalse(nested2Json.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested2Json.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested2Json.has("referencedDeepNestedObject"));
        
        System.out.println("Test passed: JSON generated for Collection without fullsave.");
    }
    
    @Test
    public void generateJSON_Collection_FullSave_IgnoreNull() {
        /* Create a collection of deep nested objects */
        List<NestedClass> nestedCollection = new ArrayList<>();
        NestedClass nested1 = Helper.initNestedClass("NestedKey1");
        NestedClass nested2 = new NestedClass("NestedKey2");  /* Empty object with null fields */

        nested1.nestedInt = 123;
        nested1.nestedIntList = Arrays.asList(1, 2, 3);
        nested1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nested1.fullySavedDeepNestedObject = Helper.initDeepNestedClass("DeepKey1", 456);
        nested1.referencedDeepNestedObject = Helper.initDeepNestedClass("DeepKey2", 789);
        nested1.nestedStringDoubleMap = null;

        nestedCollection.add(nested1);
        nestedCollection.add(nested2);  /* Object with null fields */

        /* Generate JSON with fullsave and ignore null fields */
        String json = Base.toJSON(nestedCollection, "nestedCollection", true, true);  // FullSave and ignore null fields

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);
        JSONArray nestedArray = jsonObject.getJSONArray("nestedCollection");

        /* Validate deep nested1 object */
        JSONObject nested1Json = nestedArray.getJSONObject(0);
        Assert.assertEquals(nested1.nestedKey, nested1Json.getString("nestedKey"));
        Assert.assertEquals(nested1.nestedInt, nested1Json.getInt("nestedInt"));

        /* Check fully saved deep nested object for nested1 */
        JSONObject fullySavedDeepNestedJson = nested1Json.getJSONObject("fullySavedDeepNestedObject");
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedStringKey, fullySavedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedIntKey, fullySavedDeepNestedJson.getInt("deepNestedIntKey"));

        /* Ensure null fields are not present */
        Assert.assertFalse(nested1Json.has("nestedStringDoubleMap"));

        /* Check that referencedDeepNestedObject exists but only contains the key field */
        JSONObject referencedDeepNestedJson = nested1Json.getJSONObject("referencedDeepNestedObject");
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedStringKey, referencedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedIntKey, referencedDeepNestedJson.getInt("deepNestedIntKey"));
        Assert.assertFalse(referencedDeepNestedJson.has("deepNestedIntList"));  /* Ensure other fields are not present */

        /* Validate nested2 object */
        JSONObject nested2Json = nestedArray.getJSONObject(1);
        Assert.assertEquals(nested2.nestedKey, nested2Json.getString("nestedKey"));

        /* Ensure no other fields are present for nested2 (since they were null and ignored) */
        Assert.assertFalse(nested2Json.has("nestedIntList"));
        Assert.assertFalse(nested2Json.has("nestedStringSet"));
        Assert.assertFalse(nested2Json.has("nestedStringDoubleMap"));
        Assert.assertFalse(nested2Json.has("fullySavedDeepNestedObject"));
        Assert.assertFalse(nested2Json.has("referencedDeepNestedObject"));

        System.out.println("Test passed: JSON generated for Collection with fullsave and null fields ignored.");
    }
    
    @Test
    public void generateJSON_Collection_FullSave_IncludeNull() {
        /* Create a collection of deep nested objects some null fields */
        List<NestedClass> nestedCollection = new ArrayList<>();
        NestedClass nested1 = Helper.initNestedClass("NestedKey1");
        NestedClass nested2 = new NestedClass("NestedKey2");  /* Empty object with null fields */

        nested1.nestedInt = 123;
        nested1.nestedIntList = Arrays.asList(1, 2, 3);
        nested1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nested1.fullySavedDeepNestedObject = Helper.initDeepNestedClass("DeepKey1", 456);
        nested1.referencedDeepNestedObject = Helper.initDeepNestedClass("DeepKey2", 789);
        nested1.nestedStringDoubleMap = null;

        nestedCollection.add(nested1);
        nestedCollection.add(nested2);  /* Object with null fields */

        /* Generate JSON with fullsave and null fields included */
        String json = Base.toJSON(nestedCollection, "nestedCollection", true, false);  // FullSave and include null fields

        /* Parse JSON */
        JSONObject jsonObject = new JSONObject(json);
        JSONArray nestedArray = jsonObject.getJSONArray("nestedCollection");

        /* Validate nested1 object */
        JSONObject nested1Json = nestedArray.getJSONObject(0);
        Assert.assertEquals(nested1.nestedKey, nested1Json.getString("nestedKey"));
        Assert.assertEquals(nested1.nestedInt, nested1Json.getInt("nestedInt"));

        /* Check fully saved deep nested object for nested1 */
        JSONObject fullySavedDeepNestedJson = nested1Json.getJSONObject("fullySavedDeepNestedObject");
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedStringKey, fullySavedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.fullySavedDeepNestedObject.deepNestedIntKey, fullySavedDeepNestedJson.getInt("deepNestedIntKey"));

        /* Check that null fields are present */
        Assert.assertTrue(nested1Json.has("nestedStringDoubleMap"));  // Null field included
        Assert.assertTrue(nested1Json.isNull("nestedStringDoubleMap"));

        /* Check that referencedDeepNestedObject exists but only contains the key field */
        JSONObject referencedDeepNestedJson = nested1Json.getJSONObject("referencedDeepNestedObject");
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedStringKey, referencedDeepNestedJson.getString("deepNestedStringKey"));
        Assert.assertEquals(nested1.referencedDeepNestedObject.deepNestedIntKey, referencedDeepNestedJson.getInt("deepNestedIntKey"));
        Assert.assertFalse(referencedDeepNestedJson.has("deepNestedIntList"));  /* Ensure other fields are not present */

        /* Validate nested2 object */
        JSONObject nested2Json = nestedArray.getJSONObject(1);
        Assert.assertEquals(nested2.nestedKey, nested2Json.getString("nestedKey"));

        /* Ensure null fields are included for nested2 */
        Assert.assertTrue(nested2Json.has("nestedIntList"));
        Assert.assertTrue(nested2Json.isNull("nestedIntList"));
        Assert.assertTrue(nested2Json.has("nestedStringSet"));
        Assert.assertTrue(nested2Json.isNull("nestedStringSet"));
        Assert.assertTrue(nested2Json.has("nestedStringDoubleMap"));
        Assert.assertTrue(nested2Json.isNull("nestedStringDoubleMap"));
        Assert.assertTrue(nested2Json.has("fullySavedDeepNestedObject"));
        Assert.assertTrue(nested2Json.isNull("fullySavedDeepNestedObject"));
        Assert.assertTrue(nested2Json.has("referencedDeepNestedObject"));
        Assert.assertTrue(nested2Json.isNull("referencedDeepNestedObject"));
        
        System.out.println("Test passed: JSON generated for Collection with fullsave and null fields included.");
    }
    
}
