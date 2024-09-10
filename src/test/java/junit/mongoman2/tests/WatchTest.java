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
    public void testWatchInsertOperations() {
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT);

        // Insert multiple objects
        TestClass testObj1 = new TestClass("watch_insert_001");
        testObj1.intValue = 100;
        testObj1.save();

        TestClass testObj2 = new TestClass("watch_insert_002");
        testObj2.intValue = 200;
        testObj2.save();

        // Use hasNext() and next()
        Assert.assertTrue(watch.hasNext());  // Should return true for the first insert
        TestClass result1 = watch.next();
        Assert.assertEquals("watch_insert_001", result1.uniqueId);  // Verify first object

        Assert.assertTrue(watch.hasNext());  // Should return true for the second insert
        TestClass result2 = watch.next();
        Assert.assertEquals("watch_insert_002", result2.uniqueId);  // Verify second object

        Assert.assertNull(watch.tryNext());  // No more objects should be available
    }

    @Test
    public void testWatchUpdateReplaceOperations() {
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.UPDATE_REPLACE);

        // Insert an object (should be ignored in this mode)
        TestClass testObj = new TestClass("watch_update_replace_001");
        testObj.intValue = 100;
        testObj.save();

        // Ensure the insert is ignored
        Assert.assertNull(watch.tryNext());  // Should return null because INSERT is not watched in this mode

        // Update operation
        testObj.intValue = 200;
        testObj.save();  // Simulate update

        // Replace operation
        testObj.intValue = 300;
        testObj.replace();  // Use replace instead of save for replace operation

        // Check if the update is captured
        TestClass result1 = watch.tryNext();
        Assert.assertNotNull(result1);  // Ensure we got the update event
        Assert.assertEquals(200, result1.intValue);  // Verify update operation

        // Check if the replace is captured
        TestClass result2 = watch.tryNext();
        Assert.assertNotNull(result2);  // Ensure we got the replace event
        Assert.assertEquals(300, result2.intValue);  // Verify replace operation

        // No more operations should be available
        Assert.assertNull(watch.tryNext());  // Ensure no more events are available
    }

    @Test
    public void testWatchInsertUpdateReplaceOperations() {
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);

        // Step 1: Insert an object
        TestClass testObj1 = new TestClass("watch_insert_update_replace_001");
        testObj1.intValue = 100;
        testObj1.save();  // This insert should trigger the watch

        // Step 2: Insert another object
        TestClass testObj2 = new TestClass("watch_insert_update_replace_002");
        testObj2.intValue = 200;
        testObj2.save();  // This insert should also trigger the watch

        // Step 3: Update the first object
        testObj1.intValue = 150;
        testObj1.save();  // Simulate update, should trigger the watch

        // Step 4: Replace the second object
        TestClass replacementObj = new TestClass("watch_insert_update_replace_002");
        replacementObj.intValue = 250;
        replacementObj.replace();  // Simulate replace, should trigger the watch

        // Use hasNext() and next() to process the changes
        Assert.assertTrue(watch.hasNext());  // Should return true for the first insert
        TestClass result1 = watch.next();
        Assert.assertEquals(testObj1.uniqueId, result1.uniqueId);  // Verify first insert
        Assert.assertEquals(100, result1.intValue);

        Assert.assertTrue(watch.hasNext());  // Should return true for the second insert
        TestClass result2 = watch.next();
        Assert.assertEquals(testObj2.uniqueId, result2.uniqueId);  // Verify second insert
        Assert.assertEquals(200, result2.intValue);

        Assert.assertTrue(watch.hasNext());  // Should return true for the update
        TestClass result3 = watch.next();
        Assert.assertEquals(testObj1.uniqueId, result3.uniqueId);  // Verify update
        Assert.assertEquals(150, result3.intValue);  // Updated value should be 150

        Assert.assertTrue(watch.hasNext());  // Should return true for the replace
        TestClass result4 = watch.next();
        Assert.assertEquals(replacementObj.uniqueId, result4.uniqueId);  // Verify replace
        Assert.assertEquals(250, result4.intValue);  // Replaced value should be 250

        // No more operations should be available
        Assert.assertNull(watch.tryNext());
    }

    @Test
    public void testWatchDeleteOperationsAreIgnored() {
        // Step 1: Initialize a Watch for TestClass that listens for INSERT, UPDATE, and REPLACE operations
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);

        // Step 2: Insert an object (should be detected)
        TestClass testObj1 = new TestClass("watch_delete_ignore_001");
        testObj1.intValue = 100;
        testObj1.save();  // Simulate an insert operation

        // Step 3: Update the object (should be detected)
        testObj1.intValue = 200;
        testObj1.save();  // Simulate an update operation

        // Step 4: Delete the object (should be ignored)
        testObj1.delete();  // Simulate a delete operation (should not be detected)

        // Step 5: Insert another object (should be detected)
        TestClass testObj2 = new TestClass("watch_delete_ignore_002");
        testObj2.intValue = 300;
        testObj2.save();  // Simulate an insert operation

        // Step 6: Use hasNext() and next() to verify that only INSERT and UPDATE/REPLACE operations are detected

        // Check for the first insert
        Assert.assertTrue(watch.hasNext());
        TestClass result1 = watch.next();
        Assert.assertEquals("watch_delete_ignore_001", result1.uniqueId);
        Assert.assertEquals(100, result1.intValue);

        // Check for the update operation
        Assert.assertTrue(watch.hasNext());
        TestClass result2 = watch.next();
        Assert.assertEquals("watch_delete_ignore_001", result2.uniqueId);
        Assert.assertEquals(200, result2.intValue);

        // Ensure that the delete operation was ignored
        // Check for the second insert
        Assert.assertTrue(watch.hasNext());
        TestClass result3 = watch.next();
        Assert.assertEquals("watch_delete_ignore_002", result3.uniqueId);
        Assert.assertEquals(300, result3.intValue);

        // No further changes should be available
        Assert.assertNull(watch.tryNext());
    }

    @Test
    public void testWatchInvalidate() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);
        
        // Step 2: Drop the collection, triggering an invalidate event
        Datastore.getDefaultService().getCollection(Base.getKind(TestClass.class)).drop();
        
        // Step 3: Verify that the watch detects the invalidate operation
        Assert.assertFalse(watch.hasNext());  // The watch should no longer detect any changes
        
        System.out.println("Watch Test Passed: Detected invalidate operation.");
    }
    
    @Test
    public void testNextWithoutData() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);

        try {
            // Step 2: Call next() without having any data to retrieve
            watch.next();
            Assert.fail("Expected MongomanException to be thrown when next() is called without data.");
        } catch (MongomanException e) {
            // test succeeds
        }
    }
    
    @Test
    public void testWatchOperationsOrder() {
        // Step 1: Create a Watch object for TestClass
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);

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
    public void testWatchClose() {
        // Step 1: Initialize a Watch object
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT_UPDATE_REPLACE);

        // Step 2: Simulate an INSERT operation
        TestClass testObj1 = new TestClass("watch_test_001");
        testObj1.intValue = 100;
        testObj1.save();

        // Step 3: Verify watch works before closing
        Assert.assertTrue(watch.hasNext());
        TestClass result = watch.next();
        Assert.assertEquals("watch_test_001", result.uniqueId);

        // Step 4: Close the watch and ensure no further operations are captured
        watch.close();
        
        Assert.assertNull(watch.tryNext());
    }
    
    @Test
    public void testWatchInsertModeWithUnmatchingOperations() {
        // Step 1: Initialize a Watch with INSERT mode
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.INSERT);

        // Step 2: Simulate INSERT operations (these should be captured)
        TestClass testObj1 = new TestClass("watch_insert_001");
        testObj1.intValue = 100;
        testObj1.save();

        TestClass testObj2 = new TestClass("watch_insert_002");
        testObj2.intValue = 200;
        testObj2.save();

        // Step 3: Simulate UPDATE and REPLACE operations (these should not be captured)
        testObj1.intValue = 150;
        testObj1.save();  // Update

        testObj2.intValue = 250;
        testObj2.save();  // Update

        TestClass testObj3 = new TestClass("watch_insert_003");
        testObj3.intValue = 300;
        testObj3.save();  // Insert operation

        testObj3.stringValue = "Replaced";
        testObj3.save();  // Replace operation

        // Step 4: Simulate DELETE operation (this should not be captured)
        testObj1.delete();

        // Step 5: Verify only INSERT operations are captured
        Assert.assertTrue(watch.hasNext());
        TestClass result = watch.next();
        Assert.assertEquals("watch_insert_001", result.uniqueId);  // Insert 1

        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_insert_002", result.uniqueId);  // Insert 2

        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_insert_003", result.uniqueId);  // Insert 3

        // No more changes should be captured
        Assert.assertNull(watch.tryNext());
    }

    @Test
    public void testWatchUpdateReplaceModeWithUnmatchingOperations() {
        // Step 1: Initialize a Watch with UPDATE_REPLACE mode
        Watch<TestClass> watch = new Watch<>(TestClass.class, Watch.WatchMode.UPDATE_REPLACE);

        // Step 2: Simulate INSERT operations (these should not be captured)
        TestClass testObj1 = new TestClass("watch_update_001");
        testObj1.intValue = 100;
        testObj1.save();

        TestClass testObj2 = new TestClass("watch_update_002");
        testObj2.intValue = 200;
        testObj2.save();

        // Step 3: Simulate UPDATE and REPLACE operations (these should be captured)
        testObj1.intValue = 150;
        testObj1.save();  // Update

        testObj2.intValue = 250;
        testObj2.save();  // Update

        TestClass testObj3 = new TestClass("watch_update_003");
        testObj3.intValue = 300;
        testObj3.save();  // Insert (not captured)

        testObj3.stringValue = "Replaced";
        testObj3.save();  // Replace operation (captured)

        // Step 4: Simulate DELETE operation (this should not be captured)
        testObj1.delete();

        // Step 5: Verify only UPDATE and REPLACE operations are captured
        Assert.assertTrue(watch.hasNext());
        TestClass result = watch.next();
        Assert.assertEquals("watch_update_001", result.uniqueId);  // Update 1

        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_update_002", result.uniqueId);  // Update 2

        Assert.assertTrue(watch.hasNext());
        result = watch.next();
        Assert.assertEquals("watch_update_003", result.uniqueId);  // Replace 1

        // No more changes should be captured
        Assert.assertNull(watch.tryNext());
    }
}

