/**
 * JavaFlameFractals (JFF)
 * A library for rendering flame fractals asynchronously using Java and OpenCL.
 *
 * Copyright (c) 2015 Jeremiah N. Hankins
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jnhankins.jff.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * {@code RuntimeJavaCompiler} contains static methods which can dynamical
 * compile Java source code into usable {@code Class} objects at runtime.
 * The
 * {@link #compileClass(java.lang.String, java.lang.String, javax.tools.DiagnosticListener, java.util.Locale, java.nio.charset.Charset) compileClass}
 * method takes the {@link Class#getCanonicalName() canonical name} and Java
 * source code of a class to be compiled and returns a {@link Class} object
 * representing the compiled class.
 * <p>
 * The specified class name must be the full
 * {@link Class#getCanonicalName() canonical name} of the class contained in the
 * specified source code, otherwise the the behavior of the {@code compileClass}
 * method is undefined. When the specified class name does not match the class
 * name in the specified source code, {@code compileClass} will typically either
 * throw a {@code ClassNotFoundException} or will generate a compiler diagnostic
 * error such as
 * <pre>{@code /com/jnhankins/util/Bar.java:5: error: class Foo is public, should be declared in a file named Foo.java}.</pre>
 * However, this behavior cannot be relied on because it is determined by the
 * implementation of the Java compiler which is beyond the control of this
 * class.
 * <p>
 * Example usage:
 * <pre>{@code // The source code for the new class
 * String classSource =
 *         "package com.jnhankins.util;\n"
 *       + "import java.lang.Runnable;\n"
 *       + "public class Foo implements Runnable {\n"
 *       + "    public void run() {\n"
 *       + "        System.out.println(\"Hello World!\");\n"
 *       + "    }\n"
 *       + "}";
 * 
 * // The canonical name of the new class
 * String className = "com.jnhankins.util.Foo";
 * 
 * // Compile the class and get the Class object
 * Class clazz = RuntimeJavaCompiler.compileClass(classSource, className);
 * 
 * // Instantiate the class and cast it into a useful type
 * Runnable foo = (Runnable)clazz.newInstance();
 * 
 * // Use the instance
 * foo.run();}</pre>
 * 
 * @author Jeremiah N. Hankins
 */
public class RuntimeJavaCompiler {
    
    /**
     * Compiles the specified source code and returns a new {@code Class} object
     * representing the class contained within the source code. Any error or
     * warning messages produced by the Java compiler will be written to
     * {@code System.out}.
     * <p>
     * If the Java compiler is unable to compile the specified source code (e.g.
     * if the source contains a syntax error) an {@link CompilationException}
     * will be thrown.
     * <p>
     * The specified class name must be the full
     * {@link Class#getCanonicalName() canonical name} of the class contained in
     * the specified, otherwise the behavior of the this methods is undefined.
     * <p>
     * Equivalent to: {@code compileClass(classSource, className, null)} 
     * 
     * @param classSource the Java source code for the class
     * @param className the canonical name of the class
     * @return the resulting {@code Class} object or {@code null} if the
     * provided source code could not be compiled
     * @throws NullPointerException if {@code classSource} or {@code className}
     * is {@code null}
     * @throws ClassNotFoundException might be thrown if {@code className} is
     * not the full canonical name of the class contained in {@code sourceCode}
     * @see #compileClass(java.lang.String, java.lang.String, javax.tools.DiagnosticListener) 
     */
    public static Class compileClass(String classSource, String className) 
            throws CompilationException, ClassNotFoundException {
        return compileClass(classSource, className, null);
    }
    
    /**
     * Compiles the specified source code and returns a new {@code Class} object
     * representing the class contained within the source code. Any error or
     * warning messages produced by the Java compiler will be passed to the
     * specified {@code DiagnosticListener}.
     * <p>
     * If the Java compiler is unable to compile the specified source code (e.g.
     * if the source contains a syntax error) an {@link CompilationException}
     * will be thrown.
     * <p>
     * The specified class name must be the full
     * {@link Class#getCanonicalName() canonical name} of the class contained in
     * the specified, otherwise the behavior of the this methods is undefined.
     * <p>
     * Equivalent to: {@code compileClass(classSource, className, diagnostics, null, null)}
     * 
     * @param classSource the Java source code for the class
     * @param className the canonical name of the class
     * @param diagnostics a diagnostic listener for non-fatal diagnostics; if
     * {@code null} the {@code compiler}'s default method for reporting
     * diagnostics will be used
     * @return the resulting {@code Class} object
     * @throws NullPointerException if {@code classSource} or {@code className}
     * is {@code null}
     * @throws CompilationException if the source code could not be compiled
     * @throws ClassNotFoundException might be thrown if {@code className} is
     * not the full canonical name of the class contained in {@code sourceCode}
     * @see #compileClass(java.lang.String, java.lang.String, javax.tools.DiagnosticListener, java.util.Locale, java.nio.charset.Charset) 
     */
    public static Class compileClass(String classSource, 
            String className, 
            DiagnosticListener<? super JavaFileObject> diagnostics) 
            throws CompilationException, ClassNotFoundException {
        return compileClass(classSource, className, diagnostics, null, null);
    }
    
    /**
     * Compiles the specified source code and returns a new {@code Class} object
     * representing the class contained within the source code. Any error or
     * warning messages produced by the Java compiler will be passed to the
     * specified {@code DiagnosticListener}.
     * <p>
     * If the Java compiler is unable to compile the specified source code (e.g. if
     * the source contains a syntax error) an {@link CompilationException} will
     * be thrown.
     * <p>
     * The specified class name must be the full
     * {@link Class#getCanonicalName() canonical name} of the class contained in
     * the specified, otherwise the behavior of the this methods is undefined.
     * 
     * @param classSource the Java source code for the class
     * @param className the canonical name of the class
     * @param diagnostics a diagnostic listener for non-fatal diagnostics; if
     * {@code null} the {@code compiler}'s default method for reporting
     * diagnostics will be used
     * @param locale the locale to apply when formatting diagnostics; if
     * {@code null} the default locale will be used
     * @param charset the {@link Charset} used by {@code classSource}; if
     * {@code null} the platform default will be used
     * @return the resulting {@code Class} object
     * @throws NullPointerException if {@code classSource} or {@code className}
     * is {@code null}
     * @throws CompilationException if the source code could not be compiled
     * @throws ClassNotFoundException might be thrown if {@code className} is
     * not the full canonical name of the class contained in {@code sourceCode}
     */
    static Class compileClass(String classSource,
            String className,
            DiagnosticListener<? super JavaFileObject> diagnostics, 
            Locale locale, 
            Charset charset) 
            throws CompilationException, ClassNotFoundException {
        // Check for garbage in
        if (classSource == null || className == null)
            throw new NullPointerException();
        
        // Get the Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new RuntimeException("Could not locate JDK compiler.");
        
        // Get the Java compiler's standard file manager
        JavaFileManager standardFileManager = 
            compiler.getStandardFileManager(null, locale, charset);
        
        // Create a map from class name keys to byte buffers values.
        // The buffers will hold the compiled Java bytecode for the classes.
        Map<String, ByteArrayOutputStream> buffers = new HashMap();
        
        // Extend the compiler's standard file manager so that it uses the
        // buffers we've crated in working memeory instead of temporary files
        JavaFileManager fileManager = 
                new ForwardingJavaFileManager(standardFileManager) {
                    
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, 
                    String className, Kind kind, FileObject sibling) 
                    throws IOException {
                // Construct a buffer to the class with the specified name and
                // return a JavaFileObject (for output) which wraps the buffer
                return new SimpleJavaFileObject(URI.create(className), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        buffers.put(className, out);
                        return out;
                    }
                };
            }
            
            @Override
            public JavaFileObject getJavaFileForInput(Location location, 
                    String className, Kind kind) throws IOException {
                // If the location is not CLASS_OUTPUT, the kind is not CLASS,
                // or the buffer map does not contain a buffer for the specified
                // class name, use the default implementation of this method
                if (location != StandardLocation.CLASS_OUTPUT || 
                        kind != Kind.CLASS || !buffers.containsKey(className))
                    return super.getJavaFileForInput(location, className, kind);
                // Otherwise, construct and return a JavaFileObject (for output)
                // which has the same contents as the specified class's buffer
                return new SimpleJavaFileObject(URI.create(className), kind) {
                    @Override
                    public InputStream openInputStream() {
                        byte[] bytes = buffers.get(className).toByteArray();
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
        };
        
        // Create a URI which describes the JavaFileObject we're about to
        // construct as containing the source code for the class we wish to
        // compile as a string
        URI uri = URI.create("string:///"
                + className.replace('.', '/') + Kind.SOURCE.extension);
        
        // Construct a JavaFileObject which wraps the source code of the class
        // we wish to compile
        JavaFileObject sourceFile = new SimpleJavaFileObject(uri, Kind.SOURCE) {
            @Override 
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return classSource;
            }
        };
        
        // Compile the source code
        boolean successful = compiler.getTask(
                null,           // Output writer (System.err)
                fileManager,    // File manager
                diagnostics,    // Diagnostic listener (errors and warning)
                null,           // Compiler options
                null,           // Classes that require annotation processing
                Arrays.asList(sourceFile)) // Compilation units
                .call();
        
        // If the compilation task was not completed with out errors, return
        // null since we cannot procede
        if (!successful)
            throw new CompilationException();
        
        // Construct a class loader for the class we've compiled which can load
        // the from the compile Java bytecode in memeory
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class loadClass(String name) throws ClassNotFoundException {
                if (!className.equals(name))
                    return super.loadClass(name);
                byte[] classData;
                try {
                    classData = buffers.get(className).toByteArray();
                } catch (NullPointerException ex) {
                    throw new ClassNotFoundException();
                }
                return defineClass(className, classData, 0, classData.length);
            }
        };
        
        // Load and return the class
        return classLoader.loadClass(className);
    }
}
