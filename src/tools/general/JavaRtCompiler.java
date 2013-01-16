/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008,2009 Karel Mittig
 *-----------------------------------------------------------------------------
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  Please refer to the LICENSE.txt file that comes along with this source file
 *  or to http://www.gnu.org/licenses/gpl.txt for a full version of the license.
 *
 *-----------------------------------------------------------------------------
 * For any comment, question, suggestion, bugfix or code contribution please
 * contact Karel Mittig : karel [dot] mittig [at] gmail [dot] com
 * Created  :   20 July 2009
 *---------------------------------------------------------------------------*/
package tools.general;

//////////////////////////////////////////
//IMPORTS
import java.io.*;
import java.util.*;
import javax.tools.*;
import java.net.*;
//////////////////////////////////////////

/**
 * Class used to dynamically compile and load Java classes
 * Let's go with some magic in Java
 * @author Karel
 */
public class JavaRtCompiler {
	static JavaCompiler compiler;
    static DiagnosticCollector<JavaFileObject> diagnostics;
    static StandardJavaFileManager fileManager;
	static String workingDirectory = "serverscripts";
	/**Default directory in which to store compiled files*/
	public final static String COMPILER_DIRECTORY = "./.tmp/";
	
//	<------------------------------------------------------------------------->
	/**
	 * Compile class from file
	 * @param path Path to access to class file
	 * @param classname The class name
	 * @param packageName The package defined for the class
	 * @return compiled class
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Class fileCompile(String path, String classname, String packageName) throws Exception {
		try{
			compiler = ToolProvider.getSystemJavaCompiler();
		    diagnostics = new DiagnosticCollector<JavaFileObject>();
		    fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		    File outputdir = new File(workingDirectory);
		    if (!outputdir.exists())outputdir.mkdir();
		    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputdir));
	
		    //fileManager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(new File("serverscripts")));
		    Iterable<? extends JavaFileObject> compilationUnits = fileManager
		        .getJavaFileObjectsFromStrings(Arrays.asList(path+File.separatorChar+classname+".java"));
		    JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null,
		        null, compilationUnits);
		    boolean success = task.call();
		    fileManager.close();
		    
		    if (!success){
		    	throw new Exception(diagnostics.getDiagnostics().toString());
		    } 
		    File file = new File(path);
		    Class<?> javaClass=null;
			//Class<?> params[] = {};
			//Constructor<JavaGsScript> constructor=null;
			//JavaGsScript script = null;

	        // Convert File to a URL
	        URL url = file.toURI().toURL();          // file:/c:/myclasses/
	        URL[] urls = new URL[]{url};
	    
	        // Create a new class loader with the directory
	        ClassLoader cl = new URLClassLoader(urls);
	    
	        // Load in the class; MyClass.class should be located in the directory 
	        // file:/c:/myclasses/com/mycompany
	        javaClass = cl.loadClass(packageName+"."+classname);
	        return javaClass;
	        //constructor = javaService.getConstructor(params);
			//script = constructor.newInstance();
	        //return success;
		} catch (Exception e){
			e.printStackTrace();
			throw e;
		}
		//return false;
	  }
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Compile class from memory
	 * @param scriptcontent The class content to compile
	 * @param classname The class Name to use
	 * @param packageName The class package to set
	 * @param path Path in which to store compiled file
	 * @return compiled Class 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Class memoryCompile(String scriptcontent, String classname, String packageName,String path) throws Exception {
		try{
			compiler = ToolProvider.getSystemJavaCompiler();
		    diagnostics = new DiagnosticCollector<JavaFileObject>();
		    fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		    File outputdir = new File(COMPILER_DIRECTORY);
		    if (!outputdir.exists())outputdir.mkdir();
		    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputdir));
			
		    StringWriter writer = new StringWriter();
		    PrintWriter out = new PrintWriter(writer);
		    out.print(scriptcontent);
		    out.close();
		    JavaFileObject jfo = new JavaSourceFromString(classname, writer.toString());
		    
		    Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(jfo);
		    JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null,null, compilationUnits);
		    boolean success = task.call();
		    fileManager.close();
		    
		    if (!success){
		    	throw new Exception(diagnostics.getDiagnostics().toString());
		    } 
		    File file = new File(path);
		    Class<?> javaClass=null;

		    // Convert File to a URL
	        URL url = file.toURI().toURL();          // file:/c:/myclasses/
	        URL[] urls = new URL[]{url};
	    
	        // Create a new class loader with the directory
	        ClassLoader cl = new URLClassLoader(urls);
	    
	        // Load in the class; MyClass.class should be located in
	        // the directory file:/c:/myclasses/com/mycompany
	        javaClass = cl.loadClass(packageName+"."+classname);
	        return javaClass;
		} catch (Exception e){
			throw e;
		}
		//return false;
	  }
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Construct SimpleJavaFileObject from a string
	 */
	static class JavaSourceFromString extends SimpleJavaFileObject {
		  final String code;
		  JavaSourceFromString(String name, String code) {
		    super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
		    this.code = code;
		  }

		  @Override
		  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		    return code;
		  }
		}
//	<------------------------------------------------------------------------->
	
	
}
