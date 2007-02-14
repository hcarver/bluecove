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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;

public class APIDeclarationsTester extends TestCase {

	protected boolean verbose = false;

	protected boolean reportOnly = true;

	protected boolean reportConstructors = true;
	
	protected boolean reportExtra = true;
	
	private int classDiffCount = 0;
	
	public APIDeclarationsTester() {

	}

	public APIDeclarationsTester(String name) {
		super(name);
	}

	
	protected ClassLoader getClassLoader(List jarURLList) throws Exception {
		ClassWorld world = new ClassWorld();
		ClassRealm containerRealm = world.newRealm("container");
		for (Iterator i = jarURLList.iterator(); i.hasNext();) {
			containerRealm.addConstituent((URL) i.next());
		}
		return containerRealm.getClassLoader();
	}

	protected ClassPool getClassPool(List jarURLList) throws Exception {
		ClassPool refClassPool = new ClassPool();
		for (Iterator i = jarURLList.iterator(); i.hasNext();) {
			refClassPool.appendClassPath(((URL) i.next()).getFile());
		}
		return refClassPool;
	}

	protected void verifyClassList(List names, ClassLoader refLoader, ClassLoader implLoader, ClassPool refClassPool)
			throws Exception {
		for (Iterator i = names.iterator(); i.hasNext();) {
			verifyClass(refLoader, implLoader, (String) i.next(), refClassPool);
		}
	}

	protected void verifyClass(ClassLoader refLoader, ClassLoader implLoader, String className, ClassPool refClassPool)
			throws Exception {
		classDiffCount = 0;
		if (verbose) {
			System.out.println("Testing class " + className);
		}

		Class refClass = refLoader.loadClass(className);
		Class implClass;
		try {
			implClass = implLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			reportFail(className + " not implemented");
			return;
		}

		String classResourceName = className.replace('.', '/') + ".class";
		URL implURL = implLoader.getResource(classResourceName);
		URL refURL = refLoader.getResource(classResourceName);

		if (verbose) {
			System.out.println("Found ref class " + refURL);
			System.out.println("Found impl class " + implURL);
		}

		reportTrue(className + " Reference class not in jar", refURL.toExternalForm().startsWith("jar"));

		reportFalse(className + " Implementation and Reference classes are mixed-up", refURL.sameFile(implURL));

		compareClasses(refClass, implClass, className, refClassPool);
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

	private void compareClasses(Class refClass, Class implClass, String className, ClassPool refClassPool)
			throws Exception {

		reportEquals(className + " isInterface", refClass.isInterface(), implClass.isInterface());
		reportEquals(className + " getModifiers", refClass.getModifiers(), implClass.getModifiers());

		Class[] refInterfaces = refClass.getInterfaces();
		Class[] implInterfaces = implClass.getInterfaces();

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
		Constructor[] refConstructors = refClass.getDeclaredConstructors();
		Constructor[] implConstructors = implClass.getDeclaredConstructors();
		compareConstructors(refConstructors, implConstructors, className);

		// Methods
		Method[] refMethods = refClass.getDeclaredMethods();
		Method[] implMethods = implClass.getDeclaredMethods();
		compareMethods(refMethods, implMethods, className);

		// all accessible public fields
		Field[] refFields = refClass.getFields();
		Field[] implFields = implClass.getFields();
		compareFields(refFields, implFields, className, refClass, implClass, refClassPool);
	}

	private void compareInterfaces(Class[] refInterfaces, Class[] implInterfacess, String className) throws Exception {
		List implNames = new Vector();
		for (int i = 0; i < implInterfacess.length; i++) {
			implNames.add(implInterfacess[i].getName());
		}
		for (int i = 0; i < refInterfaces.length; i++) {
			String interfaceName = refInterfaces[i].getName();
			reportTrue(className + "Interface " + interfaceName, implNames.contains(interfaceName));
		}
	}

	private Map buildNameMap(Member[] members, String className) throws Exception {
		Map namesMap = new Hashtable();
		for (int i = 0; i < members.length; i++) {
			if (ignoreMember(members[i])) {
				//System.out.println("ignore " + members[i].getName());
				continue;
			}
			String name = getName4Map(members[i]);
			if (namesMap.containsKey(name)) {
				Member exists = (Member)namesMap.get(name);
				if (exists.getDeclaringClass().getName().equals(className)) {
					continue;
				}
				//throw new Error("duplicate member name " + name + " " + members[i].getName()+ " = " + ((Member)namesMap.get(name)).getName());
			}
			namesMap.put(name, members[i]);
		}
		return namesMap;
	}

	private boolean ignoreMember(Member member) {
		if (Modifier.isPublic(member.getModifiers())) {
			return false;
		} else if (Modifier.isProtected(member.getModifiers())) {
			return false;
		} else {
			return true;
		}
	}

	private int getModifiers(Member member) {
		int mod = member.getModifiers();
		if (Modifier.isNative(mod)) {
			mod = mod - Modifier.NATIVE;
		}
		return mod;
	}

	private void compareConstructors(Constructor[] refConstructors, Constructor[] implConstructors, String className)
			throws Exception {
		Map implNames = buildNameMap(implConstructors, className);
		int compared = 0;
		for (int i = 0; i < refConstructors.length; i++) {
			if (ignoreMember(refConstructors[i])) {
				continue;
			}
			compareConstructor(refConstructors[i], (Constructor) implNames.get(getName4Map(refConstructors[i])),
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

	private void compareConstructor(Constructor refConstructor, Constructor implConstructor, String className)
			throws Exception {
		String name = refConstructor.getName();
		reportNotNull(className + " Constructor " + name + " is Missing", implConstructor);
		if (implConstructor == null) {
			return;
		}
		reportEquals(className + ". Constructor " + name + " getModifiers", Modifier
				.toString(getModifiers(refConstructor)), Modifier.toString(getModifiers(implConstructor)));
	}

	private void compareMember(Member refMember, Member implMember, String className) throws Exception {
		String name = refMember.getName();
		reportNotNull(className + "." + name + " is Missing", implMember);
		if (implMember == null) {
			return;
		}
		reportEquals(className + "." + name + " getModifiers", Modifier.toString(getModifiers(refMember)), Modifier
				.toString(getModifiers(implMember)));
	}

	private String getName4Map(Member member) {
		StringBuffer name = new StringBuffer();
		name.append(member.getName());
		if ((member instanceof Method) || (member instanceof Constructor)) {
			// Overloaded Methods should have different names
			Class[] param;
			if (member instanceof Method) {
				param = ((Method) member).getParameterTypes();
			} else if (member instanceof Constructor) {
				param = ((Constructor) member).getParameterTypes();
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

	private void compareMethods(Method[] refMethods, Method[] implMethods, String className) throws Exception {
		Map implNames = buildNameMap(implMethods, className);
		int compared = 0;
		for (int i = 0; i < refMethods.length; i++) {
			if (ignoreMember(refMethods[i])) {
				continue;
			}
			compareMethod(refMethods[i], (Method) implNames.get(getName4Map(refMethods[i])), className);
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

	private void compareMethod(Method refMethod, Method implMethod, String className) throws Exception {
		compareMember(refMethod, implMethod, className);
		if (implMethod == null) {
			return;
		}
		String name = refMethod.getName();
		reportEquals(className + "." + name + " getReturnType", refMethod.getReturnType().getName(), implMethod
				.getReturnType().getName());
	}

	private void compareFields(Field[] refFields, Field[] implFields, String className, Class refClass,
			Class implClass, ClassPool refClassPool) throws Exception {
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
			Field impl = (Field) implNames.get(name);
			if ((impl == null) && (implNamesTested.containsKey(name))) {
				continue;
			}
			compareField(refFields[i], impl, className, refClass, implClass, refClassPool);
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
	
	private void compareField(Field refField, Field implField, String className, Class refClass, Class implClass,
			ClassPool refClassPool) throws Exception {
		String name = refField.getName();
		compareMember(refField, implField, className);
		if (implField == null) {
			return;
		}
		reportEquals(className + "." + name + " getType", refField.getType().getName(), implField.getType().getName());
		if ((Modifier.isFinal(refField.getModifiers())) && (Modifier.isStatic(refField.getModifiers()))) {
			// Compare value
			CtClass klass = refClassPool.get(className);
			CtField field = klass.getField(name);
			Object refConstValue = field.getConstantValue();
			Object implConstValue = implField.get(implClass);
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
			String implValue = null;
			if (refField.getType().getName().equals("int")) {
				implValue = String.valueOf(implField.getInt(implClass));
			} else if (refField.getType().getName().equals("byte")) {
				implValue = String.valueOf(implField.getByte(implClass));
			} else if (refField.getType().getName().equals("long")) {
				implValue = String.valueOf(implField.getLong(implClass));
			} else if (refField.getType().getName().equals("java.lang.String")) {
				implValue = implField.get(implClass).toString();
			} else {
				System.out.println("Not implemented comparison for " + refField.getType().getName() + " of "
						+ className + "." + name);
			}
			reportEquals(className + "." + name + " value ", value, implValue);

			// //java.lang.UnsatisfiedLinkError: isNetworkMonitorActive
			// //at
            // javax.microedition.io.Connector.isNetworkMonitorActive(Native
            // Method)
			// if (refField.getType().getName().equals("int")) {
			// reportEquals(className + "." + name + " value ",
			// refField.getInt(refClass),
			// implField.getInt(implClass));
			// } else {
			// System.out.println("Not implemented comparison for " +
            // refField.getType().getName() + " of " + className + "." + name);
			// }
		} else {
			System.out.println("ignore comparison for " + className + "." + name);
		}
	}
}
