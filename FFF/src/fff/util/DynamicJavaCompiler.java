/**
 * FastFlameFractals (FFF)
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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fff.util;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class DynamicJavaCompiler {
    public static <T> T compile(Class<T> classInterface, String className, String javaCode, DiagnosticListener<JavaFileObject> dl) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MyJavaFileManager fileManager = new MyJavaFileManager(
                compiler.getStandardFileManager(null, null, null));
        Iterable<? extends JavaFileObject> compilationUnits =
            Arrays.asList(new JavaSourceFromString(className, javaCode));
        compiler.getTask(null, fileManager, dl, null, null, compilationUnits).call();
        MyClassLoader classLoader = new MyClassLoader(className, fileManager.getBytesFor(className));
        try {
            return classInterface.cast(classLoader.loadClass(className).newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    static class MyJavaFileManager extends ForwardingJavaFileManager {
        private final Map<String, ByteArrayOutputStream> buffers = new LinkedHashMap();
        MyJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            if (location == StandardLocation.CLASS_OUTPUT && buffers.containsKey(className) && kind == Kind.CLASS) {
                final byte[] bytes = buffers.get(className).toByteArray();
                return new SimpleJavaFileObject(URI.create(className), kind) {
                    public InputStream openInputStream() {
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
            return fileManager.getJavaFileForInput(location, className, kind);
        }
        public JavaFileObject getJavaFileForOutput(Location location, final String className, Kind kind, FileObject sibling) throws IOException {
            return new SimpleJavaFileObject(URI.create(className), kind) {
                public OutputStream openOutputStream() {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    buffers.put(className, baos);
                    return baos;
                }
            };
        }
        public byte[] getBytesFor(String className) {
            ByteArrayOutputStream byteArrayOutputStream = buffers.get(className);
            if (byteArrayOutputStream == null)
                return null;
            return byteArrayOutputStream.toByteArray();
        }
    }
    
    static class JavaSourceFromString extends SimpleJavaFileObject {
        private final String code;
        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    
    static class MyClassLoader extends ClassLoader{
        final String className;
        final byte[] classData;
        public MyClassLoader(String className, byte[] classData) {
            this.className = className;
            this.classData = classData;
        }
        public Class loadClass(String name) throws ClassNotFoundException {
            if(!className.equals(name))
                return super.loadClass(name);
            return defineClass(className, classData, 0, classData.length);
        }
    }
}
