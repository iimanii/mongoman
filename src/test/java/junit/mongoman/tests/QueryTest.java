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

import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.mongoman.Helper;
import junit.mongoman.db.NestedClass;
import org.junit.*;

import junit.mongoman.db.TestClass;
import org.mongoman.Cursor;
import org.mongoman.MongomanException;
import org.mongoman.Query;

/**
 *
 * @author ahmed
 */
public class QueryTest extends BaseTest {
    
    @Before
    public void cleanUp() {
        datastore.getCollection(TestClass.getKind(TestClass.class)).remove(new BasicDBObject());
        datastore.getCollection(NestedClass.getKind(NestedClass.class)).remove(new BasicDBObject());
    }
    
    @Test
    public void testCreateFilterWithNonExistingFields() {
        Query<TestClass> query = new Query<>(TestClass.class);

        /* Test case for a non-existing field in TestClass */
        try {
            query.createFilter("nonExistentField", Query.FilterOperator.EQUAL, "value");
            Assert.fail("Expected MongomanException for a non-existent field in TestClass.");
        } catch (MongomanException e) {
            // Test passed
        }

        /* Test case for a non-existing field in a nested object */
        try {
            query.createFilter("nestedObject.nonExistentField", Query.FilterOperator.EQUAL, "value");
            Assert.fail("Expected MongomanException for a non-existent field in a nested object.");
        } catch (MongomanException e) {
            // Test passed
        }

        /* Test case for a non-existing field in a nested collection object */
        try {
            query.createFilter("nestedObjectSet.nonExistentField", Query.FilterOperator.EQUAL, "value");
            Assert.fail("Expected MongomanException for a non-existent field in a nested collection object.");
        } catch (MongomanException e) {
            // Test passed
        }

        /* Test case for a non-existing field in a nested array object */
        try {
            query.createFilter("nestedObjectArray.nonExistentField", Query.FilterOperator.EQUAL, "value");
            Assert.fail("Expected MongomanException for a non-existent field in a nested array object.");
        } catch (MongomanException e) {
            // Test passed
        }

        /* Test case for a non-existing field in an enum-keyed map */
        try {
            query.createFilter("enumKeyedMap.INVALID_ENUM_KEY", Query.FilterOperator.EQUAL, "value");
            Assert.fail("Expected MongomanException for a non-existent field in an enum-keyed map.");
        } catch (MongomanException e) {
            // Test passed
        }
    }
        
    @Test
    public void testCreateFilterForReferenceObjects() {
        Query<TestClass> query = new Query<>(TestClass.class);

        /* Test case for a valid key field in a referenced object */
        try {
            Query.Filter filter = query.createFilter("referencedObject.nestedKey", Query.FilterOperator.EQUAL, "NestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'referencedObject.nestedKey' in a referenced object.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in a referenced object.");
        }

        /* Test case for a valid non-key field in a referenced object */
        try {
            query.createFilter("referencedObject.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.fail("Expected MongomanException for a non-key field in a referenced object.");
        } catch (MongomanException e) {
            System.out.println("Test passed: Caught MongomanException for a non-key field in a referenced object.");
        }

        /* Test case for a valid key field in an array of non-fully saved objects */
        try {
            Query.Filter filter = query.createFilter("nestedObjectArray.nestedKey", Query.FilterOperator.EQUAL, "ArrayNestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'nestedObjectArray.nestedKey' in an array of non-fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in an array of non-fully saved objects.");
        }

        /* Test case for a valid non-key field in an array of non-fully saved objects */
        try {
            query.createFilter("nestedObjectArray.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.fail("Expected MongomanException for a non-key field in an array of non-fully saved objects.");
        } catch (MongomanException e) {
            System.out.println("Test passed: Caught MongomanException for a non-key field in an array of non-fully saved objects.");
        }

        /* Test case for a valid key field in a collection (Set) of non-fully saved objects */
        try {
            Query.Filter filter = query.createFilter("nestedObjectSet.nestedKey", Query.FilterOperator.EQUAL, "SetNestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'nestedObjectSet.nestedKey' in a Set of non-fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in a Set of non-fully saved objects.");
        }

        /* Test case for a valid non-key field in a collection (Set) of non-fully saved objects */
        try {
            query.createFilter("nestedObjectSet.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.fail("Expected MongomanException for a non-key field in a Set of non-fully saved objects.");
        } catch (MongomanException e) {
            System.out.println("Test passed: Caught MongomanException for a non-key field in a Set of non-fully saved objects.");
        }
    }

    @Test
    public void testCreateFilterForFullySavedObjects() {
        Query<TestClass> query = new Query<>(TestClass.class);

        /* Test case for a key field in a fully saved object */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObject.nestedKey", Query.FilterOperator.EQUAL, "NestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'fullySavedNestedObject.nestedKey' in a fully saved object.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in a fully saved object.");
        }

        /* Test case for a non-key field in a fully saved object */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObject.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid non-key field 'fullySavedNestedObject.nestedInt' in a fully saved object.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid non-key field in a fully saved object.");
        }

        /* Test case for a nested field in a fully saved deep nested object */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObject.fullySavedDeepNestedObject.deepNestedStringKey", Query.FilterOperator.EQUAL, "DeepKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid field in a deep fully saved object.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid field in a deep fully saved object.");
        }

        /* Test case for a non-key field in a deep fully saved object */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObject.fullySavedDeepNestedObject.deepNestedIntList", Query.FilterOperator.EQUAL, Arrays.asList(1, 2, 3));
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid non-key field in a deep fully saved object.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid non-key field in a deep fully saved object.");
        }
        
        /* Test case for a key field in an array of fully saved objects */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectArray.nestedKey", Query.FilterOperator.EQUAL, "ArrayNestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'nestedObjectArray.nestedKey' in an array of fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in an array of fully saved objects.");
        }

        /* Test case for a non-key field in an array of fully saved objects */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectArray.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid non-key field 'nestedObjectArray.nestedInt' in an array of fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid non-key field in an array of fully saved objects.");
        }

        /* Test case for a key field in a Set of fully saved objects */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectSet.nestedKey", Query.FilterOperator.EQUAL, "SetNestedKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid key field 'nestedObjectSet.nestedKey' in a Set of fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid key field in a Set of fully saved objects.");
        }

        /* Test case for a non-key field in a Set of fully saved objects */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectSet.nestedInt", Query.FilterOperator.EQUAL, 100);
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid non-key field 'nestedObjectSet.nestedInt' in a Set of fully saved objects.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid non-key field in a Set of fully saved objects.");
        }

        /* Test case for a key field in a fully saved deep nested object within an array */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectArray.fullySavedDeepNestedObject.deepNestedStringKey", Query.FilterOperator.EQUAL, "DeepKey_001");
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid field in a deep fully saved object within an array.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid field in a deep fully saved object within an array.");
        }

        /* Test case for a non-key field in a fully saved deep nested object within a Set */
        try {
            Query.Filter filter = query.createFilter("fullySavedNestedObjectArray.fullySavedDeepNestedObject.deepNestedIntList", Query.FilterOperator.EQUAL, Arrays.asList(1, 2, 3));
            Assert.assertNotNull(filter);
            System.out.println("Test passed: Valid non-key field in a deep fully saved object within a Set.");
        } catch (MongomanException e) {
            Assert.fail("This should not throw an exception for a valid non-key field in a deep fully saved object within a Set.");
        }
    }

    @Test
    public void testExecutingQueryWithNonKeyFieldFilter() {
        // Create a new object to save in the datastore
        TestClass testObj = new TestClass("test_query_nonkey_001");
        testObj.intValue = 123;  // Non-key field
        testObj.stringValue = "Some String";  // Just another field to distinguish the object

        // Save the object in the datastore
        testObj.save();

        // Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Set a filter to find objects where intValue equals 123 (a non-key field)
        try {
            Query.Filter filter = query.createFilter("intValue", Query.FilterOperator.EQUAL, 123);
            query.setFilter(filter);

            // Execute the query (this runs the filter against the datastore)
            Cursor<TestClass> cursor = query.execute(datastore);

            // Verify that the cursor has the expected result
            Assert.assertTrue(cursor.hasNext());  // Check if at least one result is returned
            TestClass result = cursor.next();     // Get the first result
            Assert.assertEquals(123, result.intValue);  // Verify the non-key field matches the filter
            Assert.assertEquals("Some String", result.stringValue);  // Double-check another field for distinction

            System.out.println("Test passed: Query executed and non-key field filter applied correctly.");
        } catch (MongomanException e) {
            Assert.fail("Setting the filter should not throw an exception.");
        }
    }
    
    @Test
    public void testExecutingQueryWithSortingAndVerifyingOrder() {
        // Create and save multiple objects with different intValue to test sorting
        TestClass testObj1 = new TestClass("test_query_sort_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Object 1";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_query_sort_002");
        testObj2.intValue = 30;
        testObj2.stringValue = "Object 2";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_query_sort_003");
        testObj3.intValue = 20;
        testObj3.stringValue = "Object 3";
        testObj3.save();

        // Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Add sorting on the intValue field in ascending order
        query.addSort("intValue", Query.SortDirection.ASC);

        // Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Verify that the results are sorted in ascending order based on intValue
        Assert.assertTrue(cursor.hasNext());
        TestClass result1 = cursor.next();
        Assert.assertEquals(10, result1.intValue);
        Assert.assertEquals("Object 1", result1.stringValue);

        Assert.assertTrue(cursor.hasNext());
        TestClass result2 = cursor.next();
        Assert.assertEquals(20, result2.intValue);
        Assert.assertEquals("Object 3", result2.stringValue);

        Assert.assertTrue(cursor.hasNext());
        TestClass result3 = cursor.next();
        Assert.assertEquals(30, result3.intValue);
        Assert.assertEquals("Object 2", result3.stringValue);

        System.out.println("Test passed: Query executed with sorting and returned results in correct order.");
    }
    
    @Test
    public void testExecutingQueryWithProjectionAndCheckingRetrievedFields() {
        // Create and save an object with multiple fields set
        TestClass testObj = new TestClass("test_query_projection_001");
        testObj.intValue = 456;
        testObj.stringValue = "Projection Test String";
        testObj.enumValue = TestClass.TestEnum.VALUE1;
        testObj.save();

        // Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Set a filter to find the specific object by its unique ID (key field)
        Query.Filter filter = query.createFilter("uniqueId", Query.FilterOperator.EQUAL, "test_query_projection_001");
        query.setFilter(filter);

        // Add a projection to include only the 'stringValue' and 'enumValue' fields
        query.addProjection("stringValue");
        query.addProjection("enumValue");

        // Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Verify that the results contain only the projected fields
        Assert.assertTrue(cursor.hasNext());
        TestClass result = cursor.next();

        // Check that the 'stringValue' and 'enumValue' are correctly retrieved
        Assert.assertEquals("Projection Test String", result.stringValue);  // Should be retrieved
        Assert.assertEquals(TestClass.TestEnum.VALUE1, result.enumValue);  // Should be retrieved

        // Check that other non-projected fields are left as null
        Assert.assertNull(result.integerValue);  // Non-projected wrapper type should be null
        Assert.assertNull(result.longObject);    // Non-projected wrapper type should be null

        System.out.println("Test passed: Query executed with projection, and only the projected fields were retrieved.");
    }

    @Test
    public void testExecutingQueryWithIgnoredFields() {
        // Create and save an object with multiple fields set
        TestClass testObj = new TestClass("test_query_ignore_001");
        testObj.intValue = 789;
        testObj.stringValue = "Ignored Field Test String";
        testObj.enumValue = TestClass.TestEnum.VALUE2;
        testObj.save();

        // Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Set a filter to find the specific object by its unique ID (key field)
        Query.Filter filter = query.createFilter("uniqueId", Query.FilterOperator.EQUAL, "test_query_ignore_001");
        query.setFilter(filter);

        // Add ignored fields so that 'stringValue' and 'enumValue' are not included in the results
        query.ignoreField("stringValue");
        query.ignoreField("enumValue");

        // Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Verify that the results do not contain the ignored fields
        Assert.assertTrue(cursor.hasNext());
        TestClass result = cursor.next();

        // Check that the ignored fields are null
        Assert.assertNull(result.stringValue);  // Ignored field should be null
        Assert.assertNull(result.enumValue);    // Ignored field should be null

        // Ensure that other fields are still populated correctly
        Assert.assertEquals(789, result.intValue);  // Non-ignored field should be populated

        System.out.println("Test passed: Query executed with ignored fields, and they were correctly excluded from the result.");
    }
    
    @Test
    public void testCannotMixProjectionAndIgnoreFields() {
        // Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Add a projection field first
        query.addProjection("stringValue");

        // Now attempt to ignore a field, which should trigger the exception
        try {
            query.ignoreField("intValue");
            Assert.fail("Expected MongomanException due to mixing projection and ignore.");
        } catch (MongomanException e) {
            System.out.println("Test passed: MongomanException thrown for mixing projection and ignore.");
        }

        // Create a new query for testing the reverse case
        Query<TestClass> query2 = new Query<>(TestClass.class);

        // Add an ignore field first
        query2.ignoreField("enumValue");

        // Now attempt to add a projection, which should also trigger the exception
        try {
            query2.addProjection("dateValue");
            Assert.fail("Expected MongomanException due to mixing ignore and projection.");
        } catch (MongomanException e) {
            System.out.println("Test passed: MongomanException thrown for mixing ignore and projection.");
        }
    }
    
    @Test
    public void testQueryWithFiltersSortingProjection() {
        // Step 1: Create and save multiple objects to test the combination of filters, sorting, and projection
        TestClass testObj1 = new TestClass("combo_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Object 1";
        testObj1.save();

        TestClass testObj2 = new TestClass("combo_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "Object 2";
        testObj2.save();

        TestClass testObj3 = new TestClass("combo_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "Object 3";
        testObj3.enumValue = TestClass.TestEnum.VALUE1;
        testObj3.save();

        // Step 2: Create a Query object for TestClass and apply a filter
        Query<TestClass> query = new Query<>(TestClass.class);

        // Filter: Retrieve objects where intValue is greater than or equal to 20
        query.setFilter(query.createFilter("intValue", Query.FilterOperator.GREATER_THAN_OR_EQUAL, 20));

        // Step 3: Add sorting on the intValue field in descending order
        query.addSort("intValue", Query.SortDirection.DESC);

        // Step 4: Add projection to only retrieve specific fields (intValue and stringValue)
        query.addProjection("intValue");
        query.addProjection("stringValue");

        // Step 5: Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Step 6: Verify the results - should return objects with intValue >= 20, sorted in descending order
        Assert.assertTrue(cursor.hasNext());
        TestClass result1 = cursor.next();
        Assert.assertEquals(30, result1.intValue);  // Verify intValue
        Assert.assertEquals("Object 3", result1.stringValue);  // Verify stringValue
        Assert.assertNull(result1.enumValue);  // Ensure non-projected fields (like enumValue) are not present

        Assert.assertTrue(cursor.hasNext());
        TestClass result2 = cursor.next();
        Assert.assertEquals(20, result2.intValue);  // Verify intValue
        Assert.assertEquals("Object 2", result2.stringValue);  // Verify stringValue
        Assert.assertNull(result2.enumValue);  // Ensure non-projected fields (like enumValue) are not present

        // Step 7: Ensure no more results are returned
        Assert.assertFalse(cursor.hasNext());

        System.out.println("Test passed: Query executed with a combination of filters, sorting, and projection.");
    }

    @Test
    public void testQueryWithNestedFieldFilter() {
        // Step 1: Create and save an object with a nested field
        NestedClass nestedObj1 = Helper.initNestedClass("nestedKey_001");
        nestedObj1.nestedInt = 100;
        nestedObj1.save();

        NestedClass nestedObj2 = Helper.initNestedClass("nestedKey_002");
        nestedObj2.nestedInt = 200;
        nestedObj2.save();

        TestClass testObj1 = new TestClass("nested_001");
        testObj1.fullySavedNestedObject = nestedObj1;  // Set the nested object
        testObj1.save();

        TestClass testObj2 = new TestClass("nested_002");
        testObj2.fullySavedNestedObject = nestedObj2;  // Set another nested object
        testObj2.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Set a filter on the nested field (fullySavedNestedObject.nestedInt)
        query.setFilter(query.createFilter("fullySavedNestedObject.nestedInt", Query.FilterOperator.EQUAL, 200));

        // Step 4: Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Step 5: Verify that the correct object is returned
        Assert.assertTrue(cursor.hasNext());
        TestClass result = cursor.next();
        Assert.assertEquals("nested_002", result.uniqueId);  // Verify the correct object is returned
        Assert.assertNotNull(result.fullySavedNestedObject);
        Assert.assertEquals(200, result.fullySavedNestedObject.nestedInt);  // Verify the nested field matches the filter

        // Step 6: Ensure no more results are returned
        Assert.assertFalse(cursor.hasNext());

        System.out.println("Test passed: Query executed with filter on nested field.");
    }
    
    @Test
    public void testQueryWithNestedFieldProjection() {
        // Step 1: Create and save an object with a nested field
        NestedClass nestedObj1 = Helper.initNestedClass("nestedKey_001");
        nestedObj1.nestedInt = 100;
        nestedObj1.nestedStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
        nestedObj1.save();

        TestClass testObj1 = new TestClass("nested_proj_001");
        testObj1.fullySavedNestedObject = nestedObj1;  // Set the nested object
        testObj1.intValue = 123;
        testObj1.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Add a projection for a nested field (fullySavedNestedObject.nestedStringSet)
        query.addProjection("fullySavedNestedObject.nestedStringSet");

        // Step 4: Set a filter to fetch the exact object
        query.setFilter(query.createFilter("uniqueId", Query.FilterOperator.EQUAL, "nested_proj_001"));

        // Step 5: Execute the query and fetch the results
        Cursor<TestClass> cursor = query.execute(datastore);

        // Step 6: Verify that the correct object is returned and only the projected field is included
        Assert.assertTrue(cursor.hasNext());
        TestClass result = cursor.next();

        // Ensure the nested field `nestedStringSet` is included
        Assert.assertNotNull(result.fullySavedNestedObject);
        Assert.assertNotNull(result.fullySavedNestedObject.nestedStringSet);
        Assert.assertTrue(result.fullySavedNestedObject.nestedStringSet.contains("A"));
        Assert.assertTrue(result.fullySavedNestedObject.nestedStringSet.contains("B"));
        Assert.assertTrue(result.fullySavedNestedObject.nestedStringSet.contains("C"));

        // Verify other fields of the nested object are not loaded (choose non-primitive fields)
        Assert.assertNull(result.fullySavedNestedObject.fullySavedDeepNestedObject);  // Should not be included since it wasn't projected

        // Step 7: Ensure no more results are returned
        Assert.assertFalse(cursor.hasNext());

        System.out.println("Test passed: Query executed with projection on nested field.");
    }
    
    @Test
    public void testKeysOnlyQueryProjection() {
        // Step 1: Create and save multiple objects with different field values
        TestClass testObj1 = new TestClass("keys_only_001");
        testObj1.intValue = 100;
        testObj1.stringValue = "Object 1";
        testObj1.enumValue = TestClass.TestEnum.VALUE1;
        testObj1.save();

        TestClass testObj2 = new TestClass("keys_only_002");
        testObj2.intValue = 200;
        testObj2.stringValue = "Object 2";
        testObj2.enumValue = TestClass.TestEnum.VALUE2;
        testObj2.save();

        // Step 2: Create a Query object and set it to keys-only mode
        Query<TestClass> query = new Query<>(TestClass.class);
        query.setKeysOnly();  // This ensures that only the key fields are projected

        // Step 3: Execute the query and collect the results
        Cursor<TestClass> cursor = query.execute(datastore);

        Set<TestClass> actualResults = new HashSet<>();
        while (cursor.hasNext()) {
            TestClass result = cursor.next();
            actualResults.add(result);

            // Step 4: Verify that only key fields are populated (other fields should be null or default)
            Assert.assertNotNull(result.uniqueId);  // Key field should be populated

            // Non-key fields should not be populated
            Assert.assertEquals(0, result.intValue);  // Non-key primitive field should have default value
            Assert.assertNull(result.stringValue);  // Non-key String field should be null
            Assert.assertNull(result.enumValue);    // Non-key Enum field should be null
        }

        // Step 5: Ensure that the expected objects were retrieved (based only on keys)
        Set<TestClass> expectedObjects = new HashSet<>(Arrays.asList(testObj1, testObj2));
        Assert.assertEquals(expectedObjects, actualResults);

        System.out.println("Test passed: Keys-only query projection verified successfully.");
    }

    @Test
    public void testQueryWithoutFilterRetrievesAllResults() {
        // Step 1: Create and save multiple objects in the collection
        TestClass testObj1 = new TestClass("unique_no_filter_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Test Object 1";
        testObj1.save();

        TestClass testObj2 = new TestClass("unique_no_filter_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "Test Object 2";
        testObj2.save();

        TestClass testObj3 = new TestClass("unique_no_filter_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "Test Object 3";
        testObj3.save();

        // Step 2: Create a Query object for TestClass with no filter
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Execute the query and collect the results
        Cursor<TestClass> cursor = query.execute(datastore);

        Set<TestClass> retrievedObjects = new HashSet<>();
        while (cursor.hasNext()) {
            retrievedObjects.add(cursor.next());
        }

        // Step 4: Create a set of expected objects
        Set<TestClass> expectedObjects = new HashSet<>(Arrays.asList(
            testObj1, testObj2, testObj3
        ));

        // Step 5: Compare the retrieved objects with the original ones
        Assert.assertEquals(expectedObjects, retrievedObjects);

        System.out.println("Test passed: Query without filter retrieved all results.");
    }
    
    @Test
    public void testQueryCountAndSizeWithLimitAndOffset() {
        // Step 1: Create and save five objects in the collection
        TestClass testObj1 = new TestClass("unique_count_size_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Test Object 1";
        testObj1.save();

        TestClass testObj2 = new TestClass("unique_count_size_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "Test Object 2";
        testObj2.save();

        TestClass testObj3 = new TestClass("unique_count_size_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "Test Object 3";
        testObj3.save();

        TestClass testObj4 = new TestClass("unique_count_size_004");
        testObj4.intValue = 40;
        testObj4.stringValue = "Test Object 4";
        testObj4.save();

        TestClass testObj5 = new TestClass("unique_count_size_005");
        testObj5.intValue = 50;
        testObj5.stringValue = "Test Object 5";
        testObj5.save();

        // Step 2: Create a Query object for TestClass with no filter and set limit and skip (offset)
        Query<TestClass> query = new Query<>(TestClass.class)
            .setLimit(3)  // Limit the result to 3 objects
            .setSkip(1);  // Skip the first object

        // Step 3: Execute the query and verify count and size
        Cursor<TestClass> cursor = query.execute(datastore);

        // Verify that the count matches the total number of objects (irrespective of limit/skip)
        long count = cursor.count();  // Assuming count() returns total count in the datastore
        Assert.assertEquals(5, count);  // Total should be 5

        // Verify that the size (after applying limit and skip) matches the expected number
        int size = cursor.size();  // Assuming size() takes the limit and skip into account
        Assert.assertEquals(3, size);  // Should return 3 objects after skipping 1 and limiting to 3

        // Step 4: Verify the actual objects returned by the query
        Set<TestClass> expectedResults = new HashSet<>(Arrays.asList(testObj2, testObj3, testObj4));
        Set<TestClass> actualResults = new HashSet<>();

        while (cursor.hasNext()) {
            actualResults.add(cursor.next());
        }

        // Check that the actual results match the expected ones
        Assert.assertEquals(expectedResults, actualResults);

        System.out.println("Test passed: Query count and size with limit and offset are correct.");
    }
    
    @Test
    public void testCursorNumSeen() {
        // Step 1: Create and save multiple objects for testing cursor iteration
        TestClass testObj1 = new TestClass("test_cursor_seen_001");
        testObj1.stringValue = "SeenTest_One";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_cursor_seen_002");
        testObj2.stringValue = "SeenTest_Two";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_cursor_seen_003");
        testObj3.stringValue = "SeenTest_Three";
        testObj3.save();

        // Step 2: Create a Query object for TestClass with no filters (fetch all objects)
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Execute the query and obtain the cursor
        Cursor<TestClass> cursor = query.execute(datastore);

        // Step 4: Iterate through the cursor and verify numSeen at each step
        int expectedSeen = 0;
        while (cursor.hasNext()) {
            cursor.next();
            expectedSeen++;
            // Verify that numSeen matches the expected count after each iteration
            Assert.assertEquals(expectedSeen, cursor.numSeen());
        }

        // Step 5: Verify the total number of objects seen matches the expected total
        Assert.assertEquals(3, expectedSeen);

        System.out.println("Test passed: Cursor correctly counted the number of seen objects at each step.");
    }
    
    @Test
    public void testCursorCurr() {
        // Step 1: Create and save multiple objects
        TestClass testObj1 = new TestClass("cursor_curr_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Object 1";
        testObj1.save();

        TestClass testObj2 = new TestClass("cursor_curr_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "Object 2";
        testObj2.save();

        TestClass testObj3 = new TestClass("cursor_curr_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "Object 3";
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Execute the query and retrieve the cursor
        Cursor<TestClass> cursor = query.execute(datastore);

        // Step 4: Move the cursor and verify `curr()` at each step
        Assert.assertTrue(cursor.hasNext());
        TestClass first = cursor.next(); // Move to the first object
        Assert.assertEquals(first, cursor.curr());  // Verify curr() is the same as next()

        Assert.assertTrue(cursor.hasNext());
        TestClass second = cursor.next(); // Move to the second object
        Assert.assertEquals(second, cursor.curr());  // Verify curr() is the same as next()

        Assert.assertTrue(cursor.hasNext());
        TestClass third = cursor.next(); // Move to the third object
        Assert.assertEquals(third, cursor.curr());  // Verify curr() is the same as next()

        Assert.assertFalse(cursor.hasNext()); // No more objects

        System.out.println("Test passed: Cursor.curr() works as expected.");
    }

    @Test
    public void testQueryOneWithSorting() {
        // Step 1: Create and save multiple objects to test the `one()` method
        TestClass testObj1 = new TestClass("test_one_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "FirstMatch";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_one_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "SecondMatch";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_one_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "ThirdMatch";
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Set a filter to match objects with intValue greater than or equal to 20
        query.setFilter(query.createFilter("intValue", Query.FilterOperator.GREATER_THAN_OR_EQUAL, 20));

        // Step 4: Add sorting on intValue to ensure the order (ascending)
        query.addSort("intValue", Query.SortDirection.ASC);

        // Step 5: Retrieve the first matching document using `one()`
        TestClass firstMatch = query.execute(datastore).one();

        // Step 6: Verify that the first matching document is returned based on sorting
        Assert.assertNotNull(firstMatch);
        Assert.assertEquals(testObj2.uniqueId, firstMatch.uniqueId);  // Ensure that the first sorted document (testObj2) is returned
        Assert.assertEquals(20, firstMatch.intValue);
        Assert.assertEquals("SecondMatch", firstMatch.stringValue);

        System.out.println("Test passed: `one()` returned the correct first matching document based on sorting.");
    }

    
    @Test
    public void testComplexCompoundFilterUsingAndOrOperators() {
        // Step 1: Create and save multiple objects to test complex compound filtering
        TestClass testObj1 = new TestClass("complex_001");
        testObj1.intValue = 10;
        testObj1.stringValue = "Object 1";
        testObj1.enumValue = TestClass.TestEnum.VALUE1;
        testObj1.save();

        TestClass testObj2 = new TestClass("complex_002");
        testObj2.intValue = 20;
        testObj2.stringValue = "Object 2";
        testObj2.enumValue = TestClass.TestEnum.VALUE2;
        testObj2.save();

        TestClass testObj3 = new TestClass("complex_003");
        testObj3.intValue = 30;
        testObj3.stringValue = "Object 3";
        testObj3.enumValue = TestClass.TestEnum.VALUE1;
        testObj3.save();

        TestClass testObj4 = new TestClass("complex_004");
        testObj4.intValue = 40;
        testObj4.stringValue = "Object 4";
        testObj4.enumValue = TestClass.TestEnum.VALUE2;
        testObj4.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Create complex compound filters:
        // (intValue >= 20 AND stringValue = 'Object 2') OR (enumValue = VALUE1 AND intValue < 30)
        Query.Filter filter1 = query.createFilter("intValue", Query.FilterOperator.GREATER_THAN_OR_EQUAL, 20);  // intValue >= 20
        Query.Filter filter2 = query.createFilter("stringValue", Query.FilterOperator.EQUAL, "Object 2");       // stringValue = 'Object 2'
        Query.Filter andFilter1 = new Query.Filter(Query.FilterOperator.AND, filter1, filter2);                 // AND condition

        Query.Filter filter3 = query.createFilter("enumValue", Query.FilterOperator.EQUAL, TestClass.TestEnum.VALUE1);  // enumValue = VALUE1
        Query.Filter filter4 = query.createFilter("intValue", Query.FilterOperator.LESS_THAN, 30);                     // intValue < 30
        Query.Filter andFilter2 = new Query.Filter(Query.FilterOperator.AND, filter3, filter4);                        // AND condition

        // Combine the two AND filters using OR
        Query.Filter compoundFilter = new Query.Filter(Query.FilterOperator.OR, andFilter1, andFilter2);  // OR condition

        // Step 4: Use the helper function to execute the query and assert results
        Set<TestClass> expectedResults = new HashSet<>(Arrays.asList(testObj1, testObj2)); // These should match the filter
        assertQueryWithFilter(query, compoundFilter, expectedResults);

        System.out.println("Test passed: Complex compound filter using AND/OR operators executed successfully.");
    }
    
    @Test
    public void testEqualAndNotEqualOperators() {
        // Step 1: Create and save multiple objects with different field values for testing EQUAL and NOT_EQUAL
        TestClass testObj1 = new TestClass("test_equal_001");
        testObj1.intValue = 100;
        testObj1.stringValue = "Match";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_equal_002");
        testObj2.intValue = 200;
        testObj2.stringValue = "Mismatch";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_equal_003");
        testObj3.intValue = 300;
        testObj3.stringValue = "Match";
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> equalQuery = new Query<>(TestClass.class);

        // Step 3: Add a filter using the EQUAL operator on stringValue
        Query.Filter equalFilter = equalQuery.createFilter("stringValue", Query.FilterOperator.EQUAL, "Match");
        Set<TestClass> expectedEqualResults = new HashSet<>(Arrays.asList(testObj1, testObj3));

        // Step 4: Use the helper function to assert EQUAL results
        assertQueryWithFilter(equalQuery, equalFilter, expectedEqualResults);

	// Step 5: Create another Query object for NOT_EQUAL operator
	Query<TestClass> queryNotEqual = new Query<>(TestClass.class);
        
        // Step 6: Add a filter using the NOT_EQUAL operator on intValue
        Query.Filter notEqualFilter = queryNotEqual.createFilter("intValue", Query.FilterOperator.NOT_EQUAL, 300);
        Set<TestClass> expectedNotEqualResults = new HashSet<>(Arrays.asList(testObj1, testObj2));

        // Step 7: Use the helper function to assert NOT_EQUAL results
        assertQueryWithFilter(queryNotEqual, notEqualFilter, expectedNotEqualResults);

        System.out.println("Test passed: EQUAL and NOT_EQUAL operators worked as expected.");
    }
    
    @Test
    public void testComparisonOperators() {
        // Step 1: Create and save four objects with different intValue values
        TestClass testObj1 = new TestClass("test_comparison_001");
        testObj1.intValue = 100;
        testObj1.save();

        TestClass testObj2 = new TestClass("test_comparison_002");
        testObj2.intValue = 200;
        testObj2.save();

        TestClass testObj3 = new TestClass("test_comparison_003");
        testObj3.intValue = 300;
        testObj3.save();

        TestClass testObj4 = new TestClass("test_comparison_004");
        testObj4.intValue = 400;
        testObj4.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Test GREATER_THAN: intValue > 200
        Query.Filter greaterThanFilter = query.createFilter("intValue", Query.FilterOperator.GREATER_THAN, 200);
        Set<TestClass> expectedGT = new HashSet<>(Arrays.asList(testObj3, testObj4));
        assertQueryWithFilter(query, greaterThanFilter, expectedGT);

        // Test GREATER_THAN_OR_EQUAL: intValue >= 200
        Query.Filter greaterThanOrEqualFilter = query.createFilter("intValue", Query.FilterOperator.GREATER_THAN_OR_EQUAL, 200);
        Set<TestClass> expectedGTE = new HashSet<>(Arrays.asList(testObj2, testObj3, testObj4));
        assertQueryWithFilter(query, greaterThanOrEqualFilter, expectedGTE);

        // Test LESS_THAN: intValue < 300
        Query.Filter lessThanFilter = query.createFilter("intValue", Query.FilterOperator.LESS_THAN, 300);
        Set<TestClass> expectedLT = new HashSet<>(Arrays.asList(testObj1, testObj2));
        assertQueryWithFilter(query, lessThanFilter, expectedLT);

        // Test LESS_THAN_OR_EQUAL: intValue <= 300
        Query.Filter lessThanOrEqualFilter = query.createFilter("intValue", Query.FilterOperator.LESS_THAN_OR_EQUAL, 300);
        Set<TestClass> expectedLTE = new HashSet<>(Arrays.asList(testObj1, testObj2, testObj3));
        assertQueryWithFilter(query, lessThanOrEqualFilter, expectedLTE);

        System.out.println("Test passed: Comparison operators GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, and LESS_THAN_OR_EQUAL worked as expected.");
    }
    
    @Test
    public void testInAndNotInOperators() {
        // Step 1: Create and save multiple objects with different intValue and stringValue for testing IN and NOT_IN
        TestClass testObj1 = new TestClass("test_in_001");
        testObj1.intValue = 100;
        testObj1.stringValue = "Object A";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_in_002");
        testObj2.intValue = 200;
        testObj2.stringValue = "Object B";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_in_003");
        testObj3.intValue = 300;
        testObj3.stringValue = "Object C";
        testObj3.save();

        TestClass testObj4 = new TestClass("test_in_004");
        testObj4.intValue = 400;
        testObj4.stringValue = "Object D";
        testObj4.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // Step 3: Create a filter using the IN operator on stringValue
        Query.Filter inFilter = query.createFilter("stringValue", Query.FilterOperator.IN, Arrays.asList("Object A", "Object C", "Object D"));
        Set<TestClass> expectedInResults = new HashSet<>(Arrays.asList(testObj1, testObj3, testObj4));

        // Step 4: Use the helper function to assert IN operator results
        assertQueryWithFilter(query, inFilter, expectedInResults);

        // Step 5: Create a filter using the NOT_IN operator on intValue
        Query.Filter notInFilter = query.createFilter("intValue", Query.FilterOperator.NOT_IN, Arrays.asList(100, 400));
        Set<TestClass> expectedNotInResults = new HashSet<>(Arrays.asList(testObj2, testObj3));

        // Step 6: Use the helper function to assert NOT_IN operator results
        assertQueryWithFilter(query, notInFilter, expectedNotInResults);

        System.out.println("Test passed: IN and NOT_IN operators worked as expected.");
    }
    
    @Test
    public void testInAndNotInOperatorsForPrimitiveArrayAndEnumSet() {
        // Step 1: Create and save multiple objects with intArray and enumSet fields
        TestClass testObj1 = new TestClass("test_in_array_enum_001");
        testObj1.intArray = new int[] {1, 2, 3};
        testObj1.enumSet = new HashSet<>(Arrays.asList(TestClass.TestEnum.VALUE1, TestClass.TestEnum.VALUE2));
        testObj1.save();

        TestClass testObj2 = new TestClass("test_in_array_enum_002");
        testObj2.intArray = new int[] {4, 5, 6};
        testObj2.enumSet = new HashSet<>(Arrays.asList(TestClass.TestEnum.VALUE2, TestClass.TestEnum.VALUE3));
        testObj2.save();

        TestClass testObj3 = new TestClass("test_in_array_enum_003");
        testObj3.intArray = new int[] {7, 8, 9};
        testObj3.enumSet = new HashSet<>(Arrays.asList(TestClass.TestEnum.VALUE1, TestClass.TestEnum.VALUE3));
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> query = new Query<>(TestClass.class);

        // ===== Test IN operator for intArray =====
        // Step 3: Create a filter using the IN operator on intArray
        // We want to find objects where intArray contains 1 or 5
        Query.Filter inIntArrayFilter = query.createFilter("intArray", Query.FilterOperator.IN, new int[] {1, 5});
        Set<TestClass> expectedIntArrayInResults = new HashSet<>(Arrays.asList(testObj1, testObj2));

        // Step 4: Use the helper function to assert IN operator results for intArray
        assertQueryWithFilter(query, inIntArrayFilter, expectedIntArrayInResults);

        // ===== Test NOT_IN operator for intArray =====
        // Step 5: Create a filter using the NOT_IN operator on intArray
        // We want to find objects where intArray does not contain 2 or 6
        Query.Filter notInIntArrayFilter = query.createFilter("intArray", Query.FilterOperator.NOT_IN, new int[] {2, 6});
        Set<TestClass> expectedIntArrayNotInResults = new HashSet<>(Arrays.asList(testObj3));

        // Step 6: Use the helper function to assert NOT_IN operator results for intArray
        assertQueryWithFilter(query, notInIntArrayFilter, expectedIntArrayNotInResults);

        // ===== Test IN operator for enumSet =====
        // Step 7: Create a filter using the IN operator on enumSet
        // We want to find objects where enumSet contains VALUE1 or VALUE3
        Query.Filter inEnumSetFilter = query.createFilter("enumSet", Query.FilterOperator.IN, new TestClass.TestEnum[] {TestClass.TestEnum.VALUE1, TestClass.TestEnum.VALUE3});
        Set<TestClass> expectedEnumSetInResults = new HashSet<>(Arrays.asList(testObj1, testObj2, testObj3));

        // Step 8: Use the helper function to assert IN operator results for enumSet
        assertQueryWithFilter(query, inEnumSetFilter, expectedEnumSetInResults);

        // ===== Test NOT_IN operator for enumSet =====
        // Step 9: Create a filter using the NOT_IN operator on enumSet
        // We want to find objects where enumSet does not contain VALUE2
        Query.Filter notInEnumSetFilter = query.createFilter("enumSet", Query.FilterOperator.NOT_IN, new TestClass.TestEnum[] {TestClass.TestEnum.VALUE2});
        Set<TestClass> expectedEnumSetNotInResults = new HashSet<>(Arrays.asList(testObj3));

        // Step 10: Use the helper function to assert NOT_IN operator results for enumSet
        assertQueryWithFilter(query, notInEnumSetFilter, expectedEnumSetNotInResults);

        System.out.println("Test passed: IN and NOT_IN operators for primitive arrays and enum sets worked as expected.");
    }
    
    @Test
    public void testNorOperator() {
        // Step 1: Create and save multiple objects with different field values for testing NOR
        TestClass testObj1 = new TestClass("test_nor_001");
        testObj1.intValue = 100;
        testObj1.stringValue = "Match";
        testObj1.enumValue = TestClass.TestEnum.VALUE1;
        testObj1.save();

        TestClass testObj2 = new TestClass("test_nor_002");
        testObj2.intValue = 200;
        testObj2.stringValue = "Mismatch";
        testObj2.enumValue = TestClass.TestEnum.VALUE2;
        testObj2.save();

        TestClass testObj3 = new TestClass("test_nor_003");
        testObj3.intValue = 300;
        testObj3.stringValue = "Mismatch";
        testObj3.enumValue = TestClass.TestEnum.VALUE1;
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> queryNor = new Query<>(TestClass.class);

        // Step 3: Add a filter using the NOR operator for stringValue = "Match" and intValue = 300
        Query.Filter norFilter = new Query.Filter(Query.FilterOperator.NOR,
            queryNor.createFilter("stringValue", Query.FilterOperator.EQUAL, "Match"),
            queryNor.createFilter("intValue", Query.FilterOperator.EQUAL, 300));

        // Step 4: Execute the query and collect results for NOR
        Set<TestClass> expectedNorResults = new HashSet<>(Collections.singletonList(testObj2));  // Only testObj2 should match
        assertQueryWithFilter(queryNor, norFilter, expectedNorResults);

        System.out.println("Test passed: NOR operator worked as expected.");
    }

    @Test
    public void testRegexOperator() {
        // Step 1: Create and save multiple objects with different string values for testing REGEX
        TestClass testObj1 = new TestClass("test_regex_001");
        testObj1.stringValue = "RegexTest_One";
        testObj1.save();

        TestClass testObj2 = new TestClass("test_regex_002");
        testObj2.stringValue = "RegexTest_Two";
        testObj2.save();

        TestClass testObj3 = new TestClass("test_regex_003");
        testObj3.stringValue = "SomeOtherTest";
        testObj3.save();

        // Step 2: Create a Query object for TestClass
        Query<TestClass> queryRegex = new Query<>(TestClass.class);

        // Step 3: Add a filter using the REGEX operator to match strings that start with "RegexTest"
        Query.Filter regexFilter = queryRegex.createFilter("stringValue", Query.FilterOperator.REGEX, "^RegexTest");

        // Step 4: Expected results should include only testObj1 and testObj2
        Set<TestClass> expectedRegexResults = new HashSet<>(Arrays.asList(testObj1, testObj2));

        // Step 5: Use assertFilter to verify the results
        assertQueryWithFilter(queryRegex, regexFilter, expectedRegexResults);

        System.out.println("Test passed: REGEX operator worked as expected.");
    }
    
    /* helper functions */
    public void assertQueryWithFilter(Query<TestClass> query, Query.Filter filter, Set<TestClass> expectedResults) {
        // Set the filter in the query
        query.setFilter(filter);

        // Execute the query and collect the results
        Cursor<TestClass> cursor = query.execute(datastore);
        Set<TestClass> actualResults = new HashSet<>();
        while (cursor.hasNext()) {
            actualResults.add(cursor.next());
        }

        // Assert that the actual results match the expected results
        Assert.assertEquals(expectedResults, actualResults);
    }

}
