package me.jezza.lava;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.jezza.lava.annotations.*;
import me.jezza.lava.annotations.Library.None;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Jezza
 */
public abstract class AbstractTest {
	public static final String[] FILE_SUFFIXES = {"", ".lua", ".luc"};

	@Rule
	public MethodRule method = new MethodRule();

	protected Lua L;

	protected Lua newLua() {
		return new Lua();
	}

	protected Lua createLua() {
		Lua L = newLua();

		Library[] libraries = method.methodAnnotations(Library.class);
		if (libraries == null || libraries.length == 0) {
			populate(L);
		} else {
			try {
				openLibraries(L, libraries);
			} catch (Exception e) {
				Assert.fail("Failed to populate libraries: " + e.getMessage());
			}
		}
		return L;
	}

	@Before
	public final void setup() {
		if (method.methodAnnotation(SkipSetup.class) != null) {
			System.out.println("The method '" + method + "' has bypassed the setup phase.");
			return;
		}
		this.L = createLua();
		Expected expected = method.methodAnnotation(Expected.class);
		if (expected != null) {
			int value = expected.value();
			System.out.println("Auto-running '" + method + "' with " + value + " expected result(s)");
			expected(value);
		}
		Call call = method.methodAnnotation(Call.class);
		if (call != null) {
			int results = call.value();
			System.out.println("Auto-running '" + method + "' with " + results + " expected result(s)");
			call(results);
		}
	}

	protected void populate(Lua L) {
	}

	protected void openLibraries(Lua L, Library[] libraries) throws Exception {
		for (Library library : libraries) {
			if (library.value() == None.class) {
				System.out.println("The method '" + method + "' has removed all of the default class' packages.");
				return;
			}
			try {
				library.value().getDeclaredMethod("open", Lua.class).invoke(null, L);
			} catch (NoSuchMethodException e) {
				Assert.fail("Failed to locate the Library: '" + library.value().getCanonicalName() + "', with an open(Lua) method: " + e.getMessage());
			} catch (InvocationTargetException | IllegalAccessException e) {
				Assert.fail("Failed to invoke Library('" + library.value().getCanonicalName() + "') with open(Lua) method: " + e.getMessage());
			}
		}
		System.out.println("The method '" + method + "' has overridden the class' default packages.");
	}

	protected int doString(String s) {
		return doString(L, s);
	}

	protected int doString(Lua L, String s) {
		System.out.println("Running: [[" + s + "]]");
		return L.doString(s);
	}

	/**
	 * Loads file and leaves LuaFunction on the stack.
	 * Fails the test if there was a problem loading the file.
	 */
	protected void loadFile() {
		loadFile(L, getClass().getSimpleName());
	}

	/**
	 * Loads file and leaves LuaFunction on the stack.
	 * Fails the test if there was a problem loading the file.
	 *
	 * @param fileName filename without '.luc' extension.
	 */
	protected void loadFile(String fileName) {
		loadFile(L, fileName);
	}

	/**
	 * Loads file and leaves LuaFunction on the stack.  Fails the test if
	 * there was a problem loading the file.
	 *
	 * @param L        Lua state in which to load file.
	 * @param fileName filename without '.luc' extension.
	 */
	protected void loadFile(Lua L, String fileName) {
		Class<? extends AbstractTest> _class = getClass();
		InputStream is;
		String s;
		for (String suffix : FILE_SUFFIXES) {
			s = "/" + fileName + suffix;
			is = _class.getResourceAsStream(s);
			if (is != null) {
				System.out.println("Loading: " + fileName + suffix);
				int status = L.load(is, "@" + fileName + suffix);
				Assert.assertTrue("Failed to load '" + fileName + "' successfully", status == 0);
				return;
			}
		}
		Assert.fail("Failed to load '" + fileName + '\'');
	}

	protected void loadFileAndFunction() {
		loadFileAndFunction(L, getClass().getSimpleName(), method.name());
	}

	protected void loadFileAndFunction(String file) {
		loadFileAndFunction(L, file, method.name());
	}

	protected void loadFileAndFunction(String file, String name) {
		loadFileAndFunction(L, file, name);
	}

	protected void loadFileAndFunction(Lua L, String file, String name) {
		loadFile(L, file);
		L.call(0, 0);
		L.push(L.getGlobal(name));
	}

	protected void expected(int n) {
		call(n);
		Lua L = this.L;
		for (int i = 1; i <= n; ++i)
			Assert.assertTrue("Result " + i + " isn't true", Boolean.TRUE.equals(L.value(i)));
	}

	protected void call(int n) {
		call(L, getClass().getSimpleName(), method.name(), n);
	}

	protected void call(String name, int n) {
		call(L, getClass().getSimpleName(), name, n);
	}

	protected void call(String file, String name, int n) {
		call(L, file, name, n);
	}

	protected void call(Lua L, String file, String name, int n) {
		loadFileAndFunction(L, file, name);
		System.out.println("Running: " + name);
		LuaJavaCallback errFunc = errorFunction();
		if (errFunc == null) {
			L.call(0, n);
			return;
		}
		int status = L.pcall(0, n, errFunc);
		if (status != 0)
			System.out.println(L.toString(L.value(-1)));
		Assert.assertTrue(name, status == 0);
	}

	protected LuaJavaCallback errorFunction() {
		return new AddWhere();
	}
}

final class MethodRule extends TestWatcher {
	private String name;
	private String className;
	private String methodName;
	private Method method;

	@Override
	protected void starting(Description d) {
		String className = d.getClassName();
		String methodName = d.getMethodName();
		try {
			method = d.getTestClass().getDeclaredMethod(methodName);
		} catch (NoSuchMethodException e) {
			Assert.fail("Failed to locate originating method: " + className + '.' + methodName);
		}
		Name n = d.getAnnotation(Name.class);
		name = n != null ? n.value() : methodName;
		this.className = className;
		this.methodName = methodName;
	}

	public String name() {
		return name;
	}

	public String methodName() {
		return methodName;
	}

	public String className() {
		return className;
	}

	public <T extends Annotation> T methodAnnotation(Class<T> annotationType) {
		return method.getDeclaredAnnotation(annotationType);
	}

	public <T extends Annotation> T[] methodAnnotations(Class<T> annotationType) {
		return method.getDeclaredAnnotationsByType(annotationType);
	}

	@Override
	public String toString() {
		return className + '.' + methodName + "()";
	}
}

final class AddWhere implements LuaJavaCallback {

	@Override
	public int luaFunction(Lua L) {
		boolean any = false;
		for (int i = 1; i <= 3; ++i) {
			String s = L.where(i);
			if (!s.isEmpty()) {
				if (any)
					s = s + " > ";
				any = true;
				L.insert(s, -1);
				L.concat(2);
			}
		}
		return 1;
	}
}
