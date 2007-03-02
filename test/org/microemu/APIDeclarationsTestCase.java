/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package org.microemu;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class APIDeclarationsTestCase extends TestCase {

	protected boolean verbose = false;

	protected boolean reportOnly = true;

	protected boolean reportConstructors = true;
	
	protected boolean reportExtra = true;
	
	private int classDiffCount = 0;
	
	public APIDeclarationsTestCase() {

	}

	public APIDeclarationsTestCase(String name) {
		super(name);
	}

	
	protected ClassPool createClassPool(List jarURLList) throws Exception {
		ClassPool classPool = new ClassPool();
		for (Iterator i = jarURLList.iterator(); i.hasNext();) {
			classPool.appendClassPath(((URL) i.next()).getFile());
		}
		return classPool;
	}
	
	protected ClassPool createClassPool(String className) throws Exception {
		ClassPool classPool = new ClassPool(true);
		String resource = getClassResourceName(className);
		URL url = this.getClass().getClassLoader().getResource(resource);
		if (url == null) {
			throw new MalformedURLException("Unable to find class " + className + " URL");
		}
		String path = url.toExternalForm();
		path = path.substring(0, path.length() - resource.length() - 1);
		if (path.startsWith("file:")) {
			path = path.substring(5);
		}
		classPool.appendClassPath(path);
		return classPool;
	}
	
	protected void verifyClassList(List names, ClassPool implClassPool, ClassPool refClassPool) throws Exception {
		for (Iterator i = names.iterator(); i.hasNext();) {
			verifyClass((String) i.next(), implClassPool, refClassPool);
		}
	}

	public static String getClassResourceName(String className) {
		return className.replace('.', '/').concat(".class");
	}
	
	protected void verifyClass(String className, ClassPool implClassPool, ClassPool refClassPool) throws Exception {
		classDiffCount = 0;
		if (verbose) {
			System.out.println("Testing class " + className);
		}

		CtClass refClass = null;
		CtClass implClass; 
		
		try {
			refClass = refClassPool.get(className);
		} catch (NotFoundException e) {
			fail("Reference class not found " + className);
		}
		try {
			implClass = implClassPool.get(className);
		} catch (NotFoundException e) {
			reportFail(className + " not implemented");
			return;
		}

		compareClasses(refClass, implClass, className);
//		if ((classDiffCount > 0) && (!verbose)) {
//			System.out.println("Found impl class " + implURL);
//		}
	}

	public void reportFail(String message) {
		if (reportOnly) {
			System.out.println(message);
			classDiffCount++;
		} else {
			fail(message);
		}
	}

	public void reportTrue(String message, boolean condition) {
		try {
			assertTrue(message, condition);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportFalse(String message, boolean condition) {
		try {
			assertFalse(message, condition);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportEquals(String message, boolean expected, boolean actual) {
		try {
			assertEquals(message, expected, actual);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportEquals(String message, int expected, int actual) {
		try {
			assertEquals(message, expected, actual);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportEquals(String message, String expected, String actual) {
		try {
			assertEquals(message, expected, actual);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportNotNull(String message, Object object) {
		try {
			assertNotNull(message, object);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}

	public void reportNull(String message, Object object) {
		try {
			assertNull(message, object);
		} catch (AssertionFailedError e) {
			if (reportOnly) {
				System.out.println(e.getMessage());
				classDiffCount++;
			} else {
				throw e;
			}
		}
	}
	
	public static String getSimpleName(Object obj) {
		// Java 1.5
		// obj.getClass().getSimpleName()
        String simpleName = obj.getClass().getName();
        // strip the package name
        return simpleName.substring(simpleName.lastIndexOf(".") + 1);
    }

	private void compareClasses(CtClass refClass, CtClass implClass, String className)
			throws Exception {

		reportEquals(className + " isInterface", refClass.isInterface(), implClass.isInterface());
		reportEquals(className + " getModifiers", refClass.getModifiers(), implClass.getModifiers());

		CtClass[] refInterfaces = refClass.getInterfaces();
		CtClass[] implInterfaces = implClass.getInterfaces();

		if (verbose) {
			System.out.println("interfaces implemented " + implInterfaces.length);
		}

		reportEquals(className + " interfaces implemented", refInterfaces.length, implInterfaces.length);
		compareInterfaces(refInterfaces, implInterfaces, className);

		if (refClass.getSuperclass() != null) {
			reportEquals(className + "Superclass", refClass.getSuperclass().getName(), implClass.getSuperclass()
					.getName());
		} else {
			reportNull(className + "Superclass", implClass.getSuperclass());
		}

		// Constructors
		CtConstructor[] refConstructors = refClass.getDeclaredConstructors();
		CtConstructor[] implConstructors = implClass.getDeclaredConstructors();
		compareConstructors(refConstructors, implConstructors, className);

		// Methods
		CtMethod[] refMethods = refClass.getDeclaredMethods();
		CtMethod[] implMethods = implClass.getDeclaredMethods();
		compareMethods(refMethods, implMethods, className);

		// all accessible public fields
		CtField[] refFields = refClass.getFields();
		CtField[] implFields = implClass.getFields();
		compareFields(refFields, implFields, className, refClass, implClass);
	}

	private void compareInterfaces(CtClass[] refInterfaces, CtClass[] implInterfacess, String className) throws Exception {
		List implNames = new Vector();
		for (int i = 0; i < implInterfacess.length; i++) {
			implNames.add(implInterfacess[i].getName());
		}
		for (int i = 0; i < refInterfaces.length; i++) {
			String interfaceName = refInterfaces[i].getName();
			reportTrue(className + "Interface " + interfaceName, implNames.contains(interfaceName));
		}
	}

	private Map buildNameMap(CtMember[] members, String className) throws Exception {
		Map namesMap = new Hashtable();
		for (int i = 0; i < members.length; i++) {
			if (ignoreMember(members[i])) {
				//System.out.println("ignore " + members[i].getName());
				continue;
			}
			String name = getName4Map(members[i]);
			if (namesMap.containsKey(name)) {
				CtMember exists = (CtMember)namesMap.get(name);
				if (exists.getDeclaringClass().getName().equals(className)) {
					continue;
				}
				//throw new Error("duplicate member name " + name + " " + members[i].getName()+ " = " + ((Member)namesMap.get(name)).getName());
			}
			namesMap.put(name, members[i]);
		}
		return namesMap;
	}

	private boolean ignoreMember(CtMember member) {
		if (Modifier.isPublic(member.getModifiers())) {
			return false;
		} else if (Modifier.isProtected(member.getModifiers())) {
			return false;
		} else {
			return true;
		}
	}

	private int getModifiers(CtMember member) {
		int mod = member.getModifiers();
		if (Modifier.isNative(mod)) {
			mod = mod - Modifier.NATIVE;
		}
		if (Modifier.isSynchronized(mod)) {
			mod = mod - Modifier.SYNCHRONIZED;
		}
		return mod;
	}

	private void compareConstructors(CtConstructor[] refConstructors, CtConstructor[] implConstructors, String className)
			throws Exception {
		Map implNames = buildNameMap(implConstructors, className);
		int compared = 0;
		for (int i = 0; i < refConstructors.length; i++) {
			if (ignoreMember(refConstructors[i])) {
				continue;
			}
			compareConstructor(refConstructors[i], (CtConstructor) implNames.get(getName4Map(refConstructors[i])),
					className);
			compared++;
			implNames.remove(getName4Map(refConstructors[i]));
		}
		if (!reportConstructors) {
			return;
		}
		reportEquals(className + " number of Constructors ", compared, implNames.size() + compared);
		for (Iterator i = implNames.keySet().iterator(); i.hasNext();) {
			System.out.println("   extra constructor " + i.next());
		}

	}

	private void compareConstructor(CtConstructor refConstructor, CtConstructor implConstructor, String className)
			throws Exception {
		String name = refConstructor.getName();
		reportNotNull(className + " Constructor " + name + " is Missing", implConstructor);
		if (implConstructor == null) {
			return;
		}
		reportEquals(className + ". Constructor " + name + " getModifiers", Modifier
				.toString(getModifiers(refConstructor)), Modifier.toString(getModifiers(implConstructor)));
	}

	private void compareMember(CtMember refMember, CtMember implMember, String className) throws Exception {
		String name = refMember.getName();
		reportNotNull(className + "." + name + " is Missing", implMember);
		if (implMember == null) {
			return;
		}
		reportEquals(className + "." + name + " getModifiers", Modifier.toString(getModifiers(refMember)), Modifier
				.toString(getModifiers(implMember)));
	}

	private String getName4Map(CtMember member) throws NotFoundException {
		StringBuffer name = new StringBuffer();
		name.append(member.getName());
		if ((member instanceof CtMethod) || (member instanceof CtConstructor)) {
			// Overloaded Methods should have different names
			CtClass[] param;
			if (member instanceof CtMethod) {
				param = ((CtMethod) member).getParameterTypes();
			} else if (member instanceof CtConstructor) {
				param = ((CtConstructor) member).getParameterTypes();
			} else {
				throw new Error("intenal test error");
			}
			name.append("(");
			for (int i = 0; i < param.length; i++) {
				if (i != 0) {
					name.append(" ,");
				}
				name.append(param[i].getName());
			}
			name.append(")");
		}
		return name.toString();
	}

	private void compareMethods(CtMethod[] refMethods, CtMethod[] implMethods, String className) throws Exception {
		Map implNames = buildNameMap(implMethods, className);
		int compared = 0;
		for (int i = 0; i < refMethods.length; i++) {
			if (ignoreMember(refMethods[i])) {
				continue;
			}
			compareMethod(refMethods[i], (CtMethod) implNames.get(getName4Map(refMethods[i])), className);
			compared++;
			implNames.remove(getName4Map(refMethods[i]));
		}
		if (!reportExtra) {
			return;
		}
		reportEquals(className + " number of Methods ", compared, implNames.size() + compared);
		for (Iterator i = implNames.keySet().iterator(); i.hasNext();) {
			System.out.println("   extra method " + i.next());
		}
	}

	private void compareMethod(CtMethod refMethod, CtMethod implMethod, String className) throws Exception {
		compareMember(refMethod, implMethod, className);
		if (implMethod == null) {
			return;
		}
		String name = refMethod.getName();
		reportEquals(className + "." + name + " getReturnType", refMethod.getReturnType().getName(), implMethod
				.getReturnType().getName());
	}

	private void compareFields(CtField[] refFields, CtField[] implFields, String className, CtClass refClass, CtClass implClass) throws Exception {
		Map implNames = buildNameMap(implFields, className);
		Map implNamesTested = new Hashtable();
		int compared = 0;
		for (int i = 0; i < refFields.length; i++) {
			if (ignoreMember(refFields[i])) {
				continue;
			}
			String name = getName4Map(refFields[i]);
			if (verbose) {
				System.out.println("compareField " + className + "." + refFields[i].getName());
			}
			CtField impl = (CtField) implNames.get(name);
			if ((impl == null) && (implNamesTested.containsKey(name))) {
				continue;
			}
			compareField(refFields[i], impl, className, refClass, implClass);
			compared++;
			implNamesTested.put(name, impl);
			implNames.remove(name);
		}
		if (!reportExtra) {
			return;
		}
		reportEquals(className + " number of Fields ", compared, implNames.size() + compared);
		for (Iterator i = implNames.keySet().iterator(); i.hasNext();) {
			System.out.println("   extra field " + i.next());
		}
	}
	
	private void compareField(CtField refField, CtField implField, String className, CtClass refClass, CtClass implClass) throws Exception {
		String name = refField.getName();
		compareMember(refField, implField, className);
		if (implField == null) {
			return;
		}
		reportEquals(className + "." + name + " getType", refField.getType().getName(), implField.getType().getName());
		if ((Modifier.isFinal(refField.getModifiers())) && (Modifier.isStatic(refField.getModifiers()))) {
			// Compare value
			Object refConstValue = refField.getConstantValue();
			Object implConstValue = implField.getConstantValue();
			if (refConstValue == null) {
				if (implConstValue != null) {
					if (verbose) {
						System.out.println("Not implemented comparison for " + refField.getType().getName() + " of "
								+ className + "." + name);
					}
				}
				return;
			}

			String value = refConstValue.toString();
			String implValue = implConstValue.toString();
//			if (refField.getType().getName().equals("int")) {
//				implValue = String.valueOf(implField.getInt(implClass));
//			} else if (refField.getType().getName().equals("byte")) {
//				implValue = String.valueOf(implField.getByte(implClass));
//			} else if (refField.getType().getName().equals("long")) {
//				implValue = String.valueOf(implField.getLong(implClass));
//			} else if (refField.getType().getName().equals("java.lang.String")) {
//				implValue = implField.get(implClass).toString();
//			} else {
//				System.out.println("Not implemented comparison for " + refField.getType().getName() + " of "
//						+ className + "." + name + " = " + value);
//			}
			reportEquals(className + "." + name + " value ", value, implValue);
		} else {
			System.out.println("ignore comparison for " + className + "." + name);
		}
	}
}
