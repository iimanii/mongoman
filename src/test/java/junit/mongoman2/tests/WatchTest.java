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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import junit.mongoman2.db.TestClass;
import org.junit.Assert;
import org.junit.Test;
import org.mongoman2.MongomanException;
import org.mongoman2.Base;
import org.mongoman2.Datastore;
import org.mongoman2.Watch;

/**
 *
 * @author ahmed
 */
public class WatchTest extends BaseTest {

    @Test
    public void testWatchInsertOperation() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class);
        
        // Step 2: Insert a new object into the collection
        TestClass testObj = new TestClass("watch_test_001");
        testObj.intValue = 123;
        testObj.save();  // Simulate an insert operation
        
        // Step 3: Verify that the watch detects the insert operation
        Assert.assertTrue(watch.hasNext());  // The watch should detect the insert
        TestClass result = watch.next();     // Fetch the inserted object
        
        // Step 4: Verify that the returned object matches the inserted one
        Assert.assertEquals(testObj.uniqueId, result.uniqueId);
        Assert.assertEquals(testObj.intValue, result.intValue);
        
        System.out.println("Watch Test Passed: Detected insert operation.");
    }

    @Test
    public void testWatchInvalidate() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class);
        
        // Step 2: Drop the collection, triggering an invalidate event
        Datastore.getDefaultService().getCollection(Base.getKind(TestClass.class)).drop();
        
        // Step 3: Verify that the watch detects the invalidate operation
        Assert.assertFalse(watch.hasNext());  // The watch should no longer detect any changes
        
        System.out.println("Watch Test Passed: Detected invalidate operation.");
    }
    
    @Test
    public void testNextWithoutDataThrowsException() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class);

        try {
            // Step 2: Call next() without having any data to retrieve
            watch.next();
            Assert.fail("Expected MongomanException to be thrown when next() is called without data.");
        } catch (MongomanException e) {
            // test succeeds
        }
    }
    
    @Test
    public void testWatchUpdateOperationsOrder() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class);

        // Step 2: Insert multiple objects
        TestClass testObj1 = new TestClass("watch_update_001");
        testObj1.intValue = 100;
        testObj1.save();  // Simulate an insert operation

        TestClass testObj2 = new TestClass("watch_update_002");
        testObj2.intValue = 200;
        testObj2.save();  // Simulate an insert operation

        TestClass testObj3 = new TestClass("watch_update_003");
        testObj3.intValue = 300;
        testObj3.save();  // Simulate an insert operation

        // Step 3: Perform update operations
        testObj1.intValue = 150;
        testObj1.save();  // Update operation for first object

        testObj2.intValue = 250;
        testObj2.save();  // Update operation for second object

        // Step 4: Collect the expected results
        List<String> expectedOrder = Arrays.asList(
            "watch_update_001",  // Insert 1
            "watch_update_002",  // Insert 2
            "watch_update_003",  // Insert 3
            "watch_update_001",  // Update 1
            "watch_update_002"   // Update 2
        );

        // Step 5: Fetch the results step by step
        List<String> actualOrder = new ArrayList<>();
        actualOrder.add(watch.next().uniqueId); // Insert 1
        actualOrder.add(watch.next().uniqueId); // Insert 2
        actualOrder.add(watch.next().uniqueId); // Insert 3
        actualOrder.add(watch.next().uniqueId); // Update 1
        actualOrder.add(watch.next().uniqueId); // Update 2

        // Step 6: Assert the collected order
        Assert.assertEquals(expectedOrder, actualOrder);
    }

    
    @Test
    public void testWatchWithMixedOperations() {
        // Step 1: Initialize a Watch for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class);

        // Step 2: Simulate valid INSERT operations
        TestClass testObj1 = new TestClass("watch_test_001");
        testObj1.intValue = 100;
        testObj1.save();

        TestClass testObj2 = new TestClass("watch_test_002");
        testObj2.intValue = 200;
        testObj2.save();

        // Step 3: Simulate a non-data DELETE operation (which should be skipped)
        testObj1.delete();

        // Step 4: Simulate an UPDATE operation
        testObj2.intValue = 300;
        testObj2.save();

        // First hasNext() should return true, as the first object was inserted (testObj1)
        // Expect the first object (testObj1) with uniqueId "watch_test_001"
        Assert.assertTrue(watch.hasNext());
        TestClass result = watch.next();
        Assert.assertEquals("watch_test_001", result.uniqueId);

        // Second hasNext() should return true, as the second object was inserted (testObj2)
        // Expect the second object (testObj2) with uniqueId "watch_test_002" and updated intValue 300
        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_test_002", result.uniqueId);
        
        // Third hasNext() should return true for the update operation on testObj2
        // Expect the same object (testObj2) but with updated intValue of 300
        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_test_002", result.uniqueId);
        Assert.assertEquals(300, result.intValue);

        watch.close();
        
        // After processing all changes (including the delete), hasNext() should return false
        Assert.assertFalse(watch.hasNext());
    }
}

