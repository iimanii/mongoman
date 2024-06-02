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
package org.mongoman;

/**
 *
 * @author ahmed
 */
public class ExportMode {
    public static final ExportMode JSON = new ExportMode(true, true, false);
    public static final ExportMode JSON_WITH_NULL = new ExportMode(true, false, false);
    public static final ExportMode JSON_FULL_INNER = new ExportMode(true, true, true);
    public static final ExportMode JSON_FULL_INNER_WITH_NULL = new ExportMode(true, false, true);
    
    final boolean json;
    final boolean ignore_null;
    final boolean export_inner;
    
    protected ExportMode(boolean json, boolean ignore_null, boolean export_inner) {
        this.json = json;
        this.ignore_null = ignore_null;
        this.export_inner = export_inner;
    }
}
