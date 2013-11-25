/*
 * Copyright (C) 2012 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * A very simple class to compile java code dynamically and load it as a class.
 */
public class ClassCompiler {

    /**
     * Compile the given java class file and load it as a Class
     * @param name The full class name (must match with the source code)
     * @param code The source code of the class
     * @return The Class instance, if everything went well, null otherwise
     */
    public static Class<?> compile(String name, String code) {
        // Get the java compiler
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

        // We need to override the file manager, to redirect saving the class to memory
        MyFileManager fileManager = new MyFileManager(jc.getStandardFileManager(null, null, null));

        // Need to catch errors, so we don't spam  the output
        Listener diagnosticListener = new Listener();

        // For now redirect this to System.err. Maybe we should silence this one as well?
        Writer writer = new PrintWriter(System.err);

        // Add the source cöass
        Vector<JavaFileObject> compilationUnits = new Vector<JavaFileObject>();
        compilationUnits.add(new SourceFile(name, code));

        // Execute the compiler
        CompilationTask task = jc.getTask(writer, fileManager, diagnosticListener, null, null, compilationUnits);
        if (!task.call()) return null;

        MyClassLoader cl = new MyClassLoader(fileManager, ClassCompiler.class.getClassLoader());
        try {
            return cl.loadClass(name);
        } catch (ClassNotFoundException e) {
            // This should never happen
            return null;
        }
    }

    /**
     * This class will hold the source code
     */
    static class SourceFile extends SimpleJavaFileObject {

        final String mFile;

        public SourceFile(String name, String source) {
            super(URI.create("inmem:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            mFile = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return mFile;
        }
    }

    /**
     * This class will hold the generated class files
     */
    static class ClassFile extends SimpleJavaFileObject {

        ByteArrayOutputStream mData = new ByteArrayOutputStream();

        public ClassFile(String name) {
            super(URI.create("inmem:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.SOURCE);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            mData.reset();
            return mData;
        }

        public byte[] getBytes() {
            return mData.toByteArray();
        }

    }

    /**
     * We need a custom class loader to load the compiled class files from memory
     */
    static class MyClassLoader extends ClassLoader {

        private MyFileManager mFm;

        public MyClassLoader(MyFileManager fm, ClassLoader parent) {
            super(parent);
            mFm = fm;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // Is this one of our files?
            ClassFile ret = mFm.getFile(name);
            if (ret != null) {
                byte[] data = ret.getBytes();
                return defineClass(name, data, 0, data.length);
            }

            // If not, fall back to parent ClassLoader
            return super.findClass(name);
        }

    }

    /**
     * We also need a custom JavaFileManager to redirect the output into memory.
     * We really don't want to save data to disk: it's slower and it raises problems
     * like "where can we write and read temporary files?".
     * With this solution, we save the files in memory, and thus we can read them
     * and find them faster.
     */
    static class MyFileManager implements JavaFileManager {

        /** Almost all the method calls are delegated to the system file manager */
        private StandardJavaFileManager mFm;

        /** We need to store the generated files */
        private HashMap<String, ClassFile> mOutput = new HashMap<String, ClassFile>();

        public MyFileManager(StandardJavaFileManager fm) {
            mFm = fm;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
                FileObject sibling) throws IOException {
            if (location.isOutputLocation() && kind == Kind.CLASS) {
                ClassFile ret = new ClassFile(className);
                mOutput.put(className, ret);
                return ret;
            }
            return mFm.getJavaFileForOutput(location, className, kind, sibling);
        }


        public ClassFile getFile(String className) {
            return mOutput.get(className);
        }

        @Override
        public int isSupportedOption(String option) {
            return mFm.isSupportedOption(option);
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return mFm.getClassLoader(location);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName,
                Set<Kind> kinds, boolean recurse) throws IOException {
            return mFm.list(location, packageName, kinds, recurse);
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            return mFm.inferBinaryName(location, file);
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return mFm.isSameFile(a, b);
        }

        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            return mFm.handleOption(current, remaining);
        }

        @Override
        public boolean hasLocation(Location location) {
            return mFm.hasLocation(location);
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind)
                throws IOException {
            return mFm.getJavaFileForInput(location, className, kind);
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName)
                throws IOException {
            return mFm.getFileForInput(location, packageName, relativeName);
        }

        @Override
        public FileObject getFileForOutput(Location location, String packageName,
                String relativeName, FileObject sibling) throws IOException {
            return mFm.getFileForOutput(location, packageName, relativeName, sibling);
        }

        @Override
        public void flush() throws IOException {
            mFm.flush();
        }

        @Override
        public void close() throws IOException {
            mFm.close();
        }

    }

    static class Listener implements DiagnosticListener<JavaFileObject>  {

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            // NOP - later we could report the errors
        }

    }

}
