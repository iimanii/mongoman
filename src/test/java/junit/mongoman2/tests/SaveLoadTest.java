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
package junit.mongoman2.tests;

import com.mongodb.MongoWriteException;
import java.util.*;
import org.junit.*;

import junit.mongoman2.Helper;
import junit.mongoman2.db.*;
import org.mongoman2.MongomanException;

/**
 *
 * @author ahmed
 */
public class SaveLoadTest extends BaseTest {
    private static final double DOUBLE_COMPARISON_DELTA = 0.0000001;

    @Test
    public void testBaseExists() {
        // Step 1: Create and save a new TestClass object
        TestClass testObj = new TestClass("exists_test_001");
        testObj.intValue = 100;
        testObj.stringValue = "Exists Test";
        Assert.assertTrue(testObj.save());  // Save the object

        // Step 2: Verify that the object exists in the datastore
        Assert.assertTrue(testObj.exists());

        // Step 3: Create a new TestClass object with a non-existent uniqueId
        TestClass nonExistentObj = new TestClass("non_existent_001");

        // Step 4: Verify that the non-existent object does not exist in the datastore
        Assert.assertFalse(nonExistentObj.exists());

        System.out.println("Test passed: Base.exists() works as expected.");
    }

    @Test
    public void saveAndLoad_AllFieldsPopulated() {
        // Create a fully populated TestClass object
        TestClass testObj = new TestClass("unique_001");

        /* Primitive types */
        testObj.intValue = 100;
        testObj.longValue = 1000L;
        testObj.doubleValue = 100.50;
        testObj.booleanValue = true;

        /* Arrays of primitive types */
        testObj.intArray = new int[] {1, 2, 3};
        testObj.longArray = new long[] {100L, 200L, 300L};
        testObj.doubleArray = new double[] {10.5, 20.5, 30.5};
        testObj.booleanArray = new boolean[] {true, false, true};

        /* Wrapper types */
        testObj.integerValue = 200;
        testObj.longObject = 2000L;
        testObj.doubleObject = 200.75;
        testObj.booleanObject = Boolean.FALSE;
        
        /* Initialize wrapper type arrays */
        testObj.integerObjectArray = new Integer[] {100, 200, 300};
        testObj.longObjectArray = new Long[] {1000L, 2000L, 3000L};
        testObj.doubleObjectArray = new Double[] {10.1, 20.2, 30.3};
        testObj.booleanObjectArray = new Boolean[] {true, false, true};

        /* String and Date */
        testObj.stringValue = "Test String";
        testObj.dateValue = new Date();

        /* Enum */
        testObj.enumValue = TestClass.TestEnum.VALUE1;
        testObj.enumArray = new TestClass.TestEnum[]{TestClass.TestEnum.VALUE1, TestClass.TestEnum.VALUE2};

        /* Collections: List and Set */
        testObj.intList = Arrays.asList(10, 20, 30);
        testObj.stringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        testObj.enumSet = new HashSet<>(Arrays.asList(TestClass.TestEnum.VALUE1, TestClass.TestEnum.VALUE2));

        /* Maps */
        testObj.stringDoubleMap = new HashMap<>();
        testObj.stringDoubleMap.put("key1", 10.1);
        testObj.stringDoubleMap.put("key2", 20.2);

        testObj.enumKeyedMap = new HashMap<>();
        testObj.enumKeyedMap.put(TestClass.TestEnum.VALUE1, "EnumValue1");
        testObj.enumKeyedMap.put(TestClass.TestEnum.VALUE2, "EnumValue2");

        /* Nested Objects */
        testObj.shallowObjectMap = new HashMap<>();
        testObj.shallowObjectMap.put("shallow1", Helper.initShallowClass(1));

        testObj.nestedObjectMap = new HashMap<>();
        testObj.nestedObjectMap.put("nested1", Helper.initNestedClass("Nested1"));
        testObj.nestedObjectMap.put("nested2", Helper.initNestedClass("Nested2"));

        /* Arrays of Base class */
        testObj.nestedObjectArray = new NestedClass[] {
            Helper.initNestedClass("ArrayNested1"),
            Helper.initNestedClass("ArrayNested2")
        };

        /* Set of NestedClass objects */
        testObj.nestedObjectSet = new HashSet<>(Arrays.asList(
            Helper.initNestedClass("SetNested1"),
            Helper.initNestedClass("SetNested2")
        ));

        /* Nested object with @FullSave */
        testObj.fullySavedNestedObject = Helper.initNestedClass("FullySavedNested");

        /* Shallow object with @FullSave */
        testObj.fullySavedShallowObject = Helper.initShallowClass(2);

        /* Referenced object */
        testObj.referencedObject = Helper.initNestedClass("ReferencedNested");

        // Save the object
        Assert.assertTrue(testObj.save());

        // Load the object back and verify all fields
        TestClass loadedObj = new TestClass("unique_001");
        Assert.assertTrue(loadedObj.load());

        /* Compare final key field */
        Assert.assertEquals(testObj.uniqueId, loadedObj.uniqueId);

        /* Verify primitive types */
        Assert.assertEquals(testObj.intValue, loadedObj.intValue);
        Assert.assertEquals(testObj.longValue, loadedObj.longValue);
        Assert.assertEquals(testObj.doubleValue, loadedObj.doubleValue, DOUBLE_COMPARISON_DELTA); // Use small delta for double comparison
        Assert.assertEquals(testObj.booleanValue, loadedObj.booleanValue);

        /* Compare arrays of primitive types */
        Assert.assertArrayEquals(testObj.intArray, loadedObj.intArray);
        Assert.assertArrayEquals(testObj.longArray, loadedObj.longArray);
        Assert.assertArrayEquals(testObj.doubleArray, loadedObj.doubleArray, DOUBLE_COMPARISON_DELTA); /* Use small delta for double array comparison */
        Assert.assertArrayEquals(testObj.booleanArray, loadedObj.booleanArray);

        /* Compare wrapper types */
        Assert.assertEquals(testObj.integerValue, loadedObj.integerValue);
        Assert.assertEquals(testObj.longObject, loadedObj.longObject);
        Assert.assertEquals(testObj.doubleObject, loadedObj.doubleObject, DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(testObj.booleanObject, loadedObj.booleanObject);

        /* Verify wrapper type arrays */
        Assert.assertArrayEquals(testObj.integerObjectArray, loadedObj.integerObjectArray);
        Assert.assertArrayEquals(testObj.longObjectArray, loadedObj.longObjectArray);
        Assert.assertArrayEquals(testObj.doubleObjectArray, loadedObj.doubleObjectArray);
        Assert.assertArrayEquals(testObj.booleanObjectArray, loadedObj.booleanObjectArray);
        
        /* Compare String and Date */
        Assert.assertEquals(testObj.stringValue, loadedObj.stringValue);
        Assert.assertEquals(testObj.dateValue, loadedObj.dateValue);

        /* Compare enum */
        Assert.assertEquals(testObj.enumValue, loadedObj.enumValue);

        /* Compare collections: List and Set */
        Assert.assertEquals(testObj.intList, loadedObj.intList);        /* Compare List of integers */
        Assert.assertEquals(testObj.stringSet, loadedObj.stringSet);    /* Compare Set of strings */
        Assert.assertEquals(testObj.enumSet, loadedObj.enumSet);        /* Compare Set of enums */

        /* Compare Maps: String to Double */
        Assert.assertEquals(testObj.stringDoubleMap, loadedObj.stringDoubleMap); /* Compare Map<String, Double> */

        /* Compare Maps: Enum to String */
        Assert.assertEquals(testObj.enumKeyedMap, loadedObj.enumKeyedMap); /* Compare Map<TestEnum, String> */

        /* Compare Maps: String to ShallowClass with @FullSave */
        Assert.assertEquals(testObj.shallowObjectMap.keySet(), loadedObj.shallowObjectMap.keySet()); /* Ensure both maps have the same keys */
        for (String key : testObj.shallowObjectMap.keySet()) {
            Assert.assertTrue(testObj.shallowObjectMap.get(key).compareTo(loadedObj.shallowObjectMap.get(key))); /* Use compareTo for @FullSave ShallowClass objects */
        }
        
        /* Compare Maps: String to NestedClass (non-@FullSave) */
        Assert.assertEquals(testObj.nestedObjectMap, loadedObj.nestedObjectMap); /* This will compare the keys of the objects in the map */
        
        /* Compare Arrays of Base class (non-@FullSave) */
        Assert.assertArrayEquals(testObj.nestedObjectArray, loadedObj.nestedObjectArray); /* This will compare both the length and the keys using equals() */
        
        /* Compare Set of NestedClass objects (non-@FullSave) */
        Assert.assertEquals(testObj.nestedObjectSet, loadedObj.nestedObjectSet);        /* This will compare the keys of the objects */
        
        /* Compare fully saved nested object */
        Assert.assertTrue(testObj.fullySavedNestedObject.compareTo(loadedObj.fullySavedNestedObject)); /* Use compareTo for @FullSave NestedClass objects */
        
        /* Compare fully saved shallow object */
        Assert.assertTrue(testObj.fullySavedShallowObject.compareTo(loadedObj.fullySavedShallowObject)); /* Use compareTo for @FullSave ShallowClass objects */

        /* Compare referenced object (only keys) */
        Assert.assertEquals(testObj.referencedObject, loadedObj.referencedObject); /* This compares the keys */
        
        System.out.println("Test passed: All fields populated and saved/loaded correctly.");
    }

    @Test
    public void saveAndLoad_NullFields() {
        // Create a TestClass object with default values (nulls and default primitives)
        TestClass testObj = new TestClass("unique_null_002");

        // Save the object with null fields
        Assert.assertTrue(testObj.save());

        // Load the object back and verify null handling
        TestClass loadedObj = new TestClass("unique_null_002");
        Assert.assertTrue(loadedObj.load());

        /* Verify primitive types */
        Assert.assertEquals(0, loadedObj.intValue);
        Assert.assertEquals(0L, loadedObj.longValue);
        Assert.assertEquals(0.0, loadedObj.doubleValue, DOUBLE_COMPARISON_DELTA);
        Assert.assertFalse(loadedObj.booleanValue);

        /* Verify arrays of primitive types are null */
        Assert.assertNull(loadedObj.intArray);
        Assert.assertNull(loadedObj.longArray);
        Assert.assertNull(loadedObj.doubleArray);
        Assert.assertNull(loadedObj.booleanArray);

        /* Verify wrapper types are null */
        Assert.assertNull(loadedObj.integerValue);
        Assert.assertNull(loadedObj.longObject);
        Assert.assertNull(loadedObj.doubleObject);
        Assert.assertNull(loadedObj.booleanObject);
        
        /* Verify wrapper arrays are null */
        Assert.assertNull(loadedObj.integerObjectArray);
        Assert.assertNull(loadedObj.longObjectArray);
        Assert.assertNull(loadedObj.doubleObjectArray);
        Assert.assertNull(loadedObj.booleanObjectArray);
    
        /* Verify String and Date are null */
        Assert.assertNull(loadedObj.stringValue);
        Assert.assertNull(loadedObj.dateValue);

        /* Verify enum is null */
        Assert.assertNull(loadedObj.enumValue);

        /* Verify collections are null */
        Assert.assertNull(loadedObj.intList);
        Assert.assertNull(loadedObj.stringSet);
        Assert.assertNull(loadedObj.enumSet);

        /* Verify maps are null */
        Assert.assertNull(loadedObj.stringDoubleMap);
        Assert.assertNull(loadedObj.enumKeyedMap);

        /* Verify nested objects are null */
        Assert.assertNull(loadedObj.shallowObjectMap);
        Assert.assertNull(loadedObj.nestedObjectMap);

        /* Verify arrays of Base class are null */
        Assert.assertNull(loadedObj.nestedObjectArray);

        /* Verify set of NestedClass is null */
        Assert.assertNull(loadedObj.nestedObjectSet);

        /* Verify fully saved nested object is null */
        Assert.assertNull(loadedObj.fullySavedNestedObject);

        /* Verify fully saved shallow object is null */
        Assert.assertNull(loadedObj.fullySavedShallowObject);

        /* Verify referenced object is null */
        Assert.assertNull(loadedObj.referencedObject);
        
        System.out.println("Test passed: Null fields handled correctly.");
    }
    
    @Test
    public void saveAndLoad_EmptyCollections() {
        // Create a TestClass object with empty collections
        TestClass testObj = new TestClass("unique_empty_003");

        /* Initialize empty collections */
        testObj.intList = new ArrayList<>();
        testObj.stringSet = new HashSet<>();
        testObj.enumSet = new HashSet<>();
        testObj.stringDoubleMap = new HashMap<>();
        testObj.enumKeyedMap = new HashMap<>();
        testObj.shallowObjectMap = new HashMap<>();
        testObj.nestedObjectMap = new HashMap<>();
        testObj.nestedObjectSet = new HashSet<>();
        testObj.nestedObjectArray = new NestedClass[] {};

        // Save the object
        Assert.assertTrue(testObj.save());

        // Load the object back and verify empty collections
        TestClass loadedObj = new TestClass("unique_empty_003");
        Assert.assertTrue(loadedObj.load());

        /* Verify that the collections are empty */
        Assert.assertNotNull(loadedObj.intList);
        Assert.assertTrue(loadedObj.intList.isEmpty());

        Assert.assertNotNull(loadedObj.stringSet);
        Assert.assertTrue(loadedObj.stringSet.isEmpty());

        Assert.assertNotNull(loadedObj.enumSet);
        Assert.assertTrue(loadedObj.enumSet.isEmpty());

        Assert.assertNotNull(loadedObj.stringDoubleMap);
        Assert.assertTrue(loadedObj.stringDoubleMap.isEmpty());

        Assert.assertNotNull(loadedObj.enumKeyedMap);
        Assert.assertTrue(loadedObj.enumKeyedMap.isEmpty());

        Assert.assertNotNull(loadedObj.shallowObjectMap);
        Assert.assertTrue(loadedObj.shallowObjectMap.isEmpty());

        Assert.assertNotNull(loadedObj.nestedObjectMap);
        Assert.assertTrue(loadedObj.nestedObjectMap.isEmpty());

        Assert.assertNotNull(loadedObj.nestedObjectSet);
        Assert.assertTrue(loadedObj.nestedObjectSet.isEmpty());

        Assert.assertNotNull(loadedObj.nestedObjectArray);
        Assert.assertEquals(0, loadedObj.nestedObjectArray.length);
        
        System.out.println("Test passed: Empty collections saved and loaded correctly.");
    }
    
    @Test
    public void saveAndLoad_EmptyArrays() {
        // Create a TestClass object with empty arrays
        TestClass testObj = new TestClass("unique_empty_arrays_001");

        /* Initialize empty arrays for primitive types */
        testObj.intArray = new int[] {};
        testObj.longArray = new long[] {};
        testObj.doubleArray = new double[] {};
        testObj.booleanArray = new boolean[] {};

        /* Initialize empty arrays for wrapper types */
        testObj.integerObjectArray = new Integer[] {};
        testObj.longObjectArray = new Long[] {};
        testObj.doubleObjectArray = new Double[] {};
        testObj.booleanObjectArray = new Boolean[] {};

        // Save the object
        Assert.assertTrue(testObj.save());

        // Load the object back and verify empty arrays
        TestClass loadedObj = new TestClass("unique_empty_arrays_001");
        Assert.assertTrue(loadedObj.load());

        /* Verify empty arrays for primitive types */
        Assert.assertNotNull(loadedObj.intArray);
        Assert.assertEquals(0, loadedObj.intArray.length);

        Assert.assertNotNull(loadedObj.longArray);
        Assert.assertEquals(0, loadedObj.longArray.length);

        Assert.assertNotNull(loadedObj.doubleArray);
        Assert.assertEquals(0, loadedObj.doubleArray.length);

        Assert.assertNotNull(loadedObj.booleanArray);
        Assert.assertEquals(0, loadedObj.booleanArray.length);

        /* Verify empty arrays for wrapper types */
        Assert.assertNotNull(loadedObj.integerObjectArray);
        Assert.assertEquals(0, loadedObj.integerObjectArray.length);

        Assert.assertNotNull(loadedObj.longObjectArray);
        Assert.assertEquals(0, loadedObj.longObjectArray.length);

        Assert.assertNotNull(loadedObj.doubleObjectArray);
        Assert.assertEquals(0, loadedObj.doubleObjectArray.length);

        Assert.assertNotNull(loadedObj.booleanObjectArray);
        Assert.assertEquals(0, loadedObj.booleanObjectArray.length);
        
        System.out.println("Test passed: Empty arrays saved and loaded correctly.");
    }
    
    @Test
    public void saveAndLoad_NullValuesInCollectionsAndMaps() {
        /* Create a test object with null values in collections and maps */
        TestClass savedObj = new TestClass("unique_007");

        /* Collections with null values */
        savedObj.intList = Arrays.asList(1, null, 3);
        savedObj.stringSet = new HashSet<>(Arrays.asList("A", null, "C"));

        /* Maps with null values */
        savedObj.stringDoubleMap = new HashMap<>();
        savedObj.stringDoubleMap.put("key1", 1.1);
        savedObj.stringDoubleMap.put("key2", null);  /* Null value in map */

        savedObj.enumKeyedMap = new HashMap<>();
        savedObj.enumKeyedMap.put(TestClass.TestEnum.VALUE1, "EnumValue1");
        savedObj.enumKeyedMap.put(TestClass.TestEnum.VALUE2, null);  /* Null value in map */

        /* Nested object map with null values */
        savedObj.nestedObjectMap = new HashMap<>();
        savedObj.nestedObjectMap.put("nested1", Helper.initNestedClass("Nested1"));
        savedObj.nestedObjectMap.put("nested2", null);  /* Null nested object */

        /* Save the object */
        Assert.assertTrue(savedObj.save());

        /* Load the object */
        TestClass loadedObj = new TestClass("unique_007");
        Assert.assertTrue(loadedObj.load());

        /* Verify collections with null values */
        Assert.assertEquals(savedObj.intList, loadedObj.intList);
        Assert.assertEquals(savedObj.stringSet, loadedObj.stringSet);

        /* Verify maps with null values */
        Assert.assertEquals(savedObj.stringDoubleMap, loadedObj.stringDoubleMap);
        Assert.assertEquals(savedObj.enumKeyedMap, loadedObj.enumKeyedMap);

        /* Verify nested object map with null values */
        Assert.assertEquals(savedObj.nestedObjectMap, loadedObj.nestedObjectMap); /* Direct comparison for keys */

        System.out.println("Test passed for handling null values in collections and maps.");
    }

    @Test
    public void saveAndLoad_ModifyFieldAndPersist() {
        /* Create a new TestClass object */
        TestClass testObj = new TestClass("unique_modify_001");

        /* Initialize some fields */
        testObj.intValue = 100;
        testObj.stringValue = "Original String";

        /* Save the object */
        Assert.assertTrue(testObj.save());

        /* Modify a field */
        testObj.intValue = 200;
        testObj.stringValue = "Modified String";

        /* Save the updated object */
        Assert.assertFalse(testObj.save());

        /* Load the object again to verify update persistence */
        TestClass modifiedObj = new TestClass("unique_modify_001");
        Assert.assertTrue(modifiedObj.load());

        /* Compare the updated saved and loaded objects */
        Assert.assertEquals(testObj.intValue, modifiedObj.intValue);
        Assert.assertEquals(testObj.stringValue, modifiedObj.stringValue);
        
        System.out.println("Test passed: Modified field persisted correctly.");
    }

    @Test
    public void saveAndLoad_UpdateNestedObjectsAndVerify() {
        /* Create a new TestClass object */
        TestClass testObj = new TestClass("unique_nested_001");

        /* Initialize nested objects */
        testObj.fullySavedNestedObject = Helper.initNestedClass("OriginalNestedField");
        testObj.fullySavedNestedObject.nestedInt = 100;

        /* Save the object */
        Assert.assertTrue(testObj.save());

        /* Modify nested objects */
        testObj.fullySavedNestedObject.nestedInt = 300;

        /* Save the updated object */
        Assert.assertFalse(testObj.save());

        /* Load the object again to verify update persistence */
        TestClass loadedObj = new TestClass("unique_nested_001");
        Assert.assertTrue(loadedObj.load());

        /* Compare the updated nested objects */
        Assert.assertEquals(testObj.fullySavedNestedObject.nestedInt, loadedObj.fullySavedNestedObject.nestedInt); // FullSave comparison
        
        System.out.println("Test passed: Nested objects updated and verified successfully.");
    }
    
    @Test
    public void saveAndLoad_ensureShallowObjectsCannotBeSavedOrLoaded() {
        // Attempt to create and save a shallow object independently
        ShallowClass shallowObj = Helper.initShallowClass(1);

        try {
            shallowObj.save(); // This should throw an exception
            Assert.fail("Expected MongomanException when saving shallow object independently.");
        } catch (MongomanException e) {
            // Test passes if the exception is caught
            System.out.println("Test passed: Shallow objects cannot be saved independently.");
        }

        // Now, try to load the shallow object independently
        ShallowClass shallowLoadObj = new ShallowClass(1); // Assuming shallow objects have a unique identifier

        try {
            shallowLoadObj.load(); // This should throw an exception
            Assert.fail("Expected MongomanException when loading shallow object independently.");
        } catch (MongomanException e) {
            // Test passes if the exception is caught
            System.out.println("Test passed: Shallow objects cannot be loaded independently.");
        }
    }
    
    @Test
    public void saveAndLoad_replace() {
        // Step 1: Create and save a TestClass object
        TestClass originalObj = new TestClass("replace_test_001");
        originalObj.intValue = 100;
        originalObj.stringValue = "Original String";
        originalObj.save();  // Save the original object

        // Step 2: Verify that the object is saved correctly
        TestClass loadedObj = new TestClass("replace_test_001");
        Assert.assertTrue(loadedObj.load());  // Load the object from the database
        Assert.assertEquals(100, loadedObj.intValue);
        Assert.assertEquals("Original String", loadedObj.stringValue);

        // Step 3: Create a new TestClass object with the same unique ID but different fields
        TestClass replacementObj = new TestClass("replace_test_001");
        replacementObj.intValue = 200;
        replacementObj.stringValue = "Replaced String";

        // Step 4: Replace the original object with the new one .. should return false there is no new object created
        Assert.assertTrue(replacementObj.replace());

        // Step 5: Verify that the original object has been replaced
        loadedObj = new TestClass("replace_test_001");
        Assert.assertTrue(loadedObj.load());
        Assert.assertEquals(200, loadedObj.intValue);  // New value
        Assert.assertEquals("Replaced String", loadedObj.stringValue);  // New value

        // Step 6: Verify that the uniqueId (key) has not changed
        Assert.assertEquals("replace_test_001", loadedObj.uniqueId);  // Key remains the same
    }

    @Test
    public void testSaveVsReplaceWithSameKey() {
        // Step 1: Create and save an original TestClass object
        TestClass originalObj = new TestClass("save_replace_test_001");
        originalObj.intValue = 100;
        originalObj.stringValue = "Original String";
        originalObj.save();  // Save the original object

        // Step 2: Verify that the object is saved correctly
        TestClass loadedObj = new TestClass("save_replace_test_001");
        Assert.assertTrue(loadedObj.load());  // Load the object from the database
        Assert.assertEquals(100, loadedObj.intValue);
        Assert.assertEquals("Original String", loadedObj.stringValue);

        // Step 3: Create a new object with the same key but different field values
        TestClass newObj = new TestClass("save_replace_test_001");
        newObj.intValue = 200;
        newObj.stringValue = "New String";

        // Step 4: Attempt to save the new object using save(), expect an exception due to duplicate key
        try {
            newObj.save();
            Assert.fail("Expected an exception due to saving an object with a duplicate key.");
        } catch (MongoWriteException e) {
            // Exception caught, test passes
            System.out.println("Caught expected exception: " + e.getMessage());
        }

        // Step 5: Load the object again and verify it was not changed
        loadedObj = new TestClass("save_replace_test_001");
        Assert.assertTrue(loadedObj.load());
        Assert.assertEquals(100, loadedObj.intValue);  // Original value should remain
        Assert.assertEquals("Original String", loadedObj.stringValue);  // Original value should remain

        // Step 6: Use replace() to overwrite the original object  .. should return false there is no new object created
        Assert.assertTrue(newObj.replace());

        // Step 7: Load the object again and verify it has been replaced
        loadedObj = new TestClass("save_replace_test_001");
        Assert.assertTrue(loadedObj.load());
        Assert.assertEquals(200, loadedObj.intValue);  // New value should be present
        Assert.assertEquals("New String", loadedObj.stringValue);  // New value should be present
    }

}
