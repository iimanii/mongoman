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

import org.junit.Assert;
import org.junit.Test;
import junit.mongoman.db.*;

/**
 *
 * @author ahmed
 */

public class DeleteTest extends BaseTest {

    @Test
    public void deleteObjectAndVerifyNonExistence() {
        // Create and save a new TestClass object
        TestClass testObj = new TestClass("unique_to_delete");
        testObj.intValue = 123;
        testObj.stringValue = "Test string to delete";
        
        Assert.assertTrue(testObj.save());

        // Verify object exists in the database
        TestClass loadedObj = new TestClass("unique_to_delete");
        Assert.assertTrue(loadedObj.load());

        // Now, delete the object
        Assert.assertTrue(loadedObj.delete());

        // Attempt to load the object again, should return false as it no longer exists
        TestClass deletedObj = new TestClass("unique_to_delete");
        Assert.assertFalse(deletedObj.load());

        System.out.println("Test passed: Object deleted and verified it no longer exists in the database.");
    }
    
    @Test
    public void deleteObjectWithReferenceAndVerifyCleanup() {
        // Step 1: Create and save a referenced object (NestedClass)
        NestedClass referencedObj = new NestedClass("ReferencedNested");
        referencedObj.nestedInt = 500;
        Assert.assertTrue(referencedObj.save());

        // Step 2: Create a TestClass that references the referencedObj
        TestClass testObj = new TestClass("unique_002");
        testObj.referencedObject = referencedObj;  // Reference the object
        Assert.assertTrue(testObj.save());

        // Step 3: Load and verify both objects
        TestClass loadedObj = new TestClass("unique_002");
        Assert.assertTrue(loadedObj.load());
        Assert.assertEquals(testObj.referencedObject, loadedObj.referencedObject);

        // Step 4: Delete the referencing object (TestClass)
        Assert.assertTrue(loadedObj.delete());

        // Step 5: Verify the referenced object still exists
        NestedClass loadedReferencedObj = new NestedClass("ReferencedNested");
        Assert.assertTrue(loadedReferencedObj.load());  // Should still exist

        // Step 6: Optionally check that no reference remains to the deleted object
        TestClass deletedObj = new TestClass("unique_002");
        Assert.assertFalse(deletedObj.load());  // The deleted object should no longer exist
        
        System.out.println("Test passed: Object with reference deleted, referenced object still exists.");
    }
}
