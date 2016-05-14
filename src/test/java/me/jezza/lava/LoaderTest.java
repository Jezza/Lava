package me.jezza.lava;

import java.io.InputStream;

import me.jezza.lava.annotations.SkipSetup;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jezza
 */
public class LoaderTest extends AbstractTest {

	@Override
	protected void populate(Lua L) {
	}

	protected Proto loadProto(String fileName) {
		Class<? extends AbstractTest> _class = getClass();
		InputStream in;
		String s;
		for (String suffix : FILE_SUFFIXES) {
			s = "/" + fileName + suffix;
			in = _class.getResourceAsStream(s);
			if (in != null) {
				System.out.println("Loading: " + fileName + suffix);
				Loader loader = new Loader(in, fileName + suffix);
				try {
					return loader.undump();
				} catch (Exception e) {
					e.printStackTrace();
					throw new AssertionError("Failed to load '" + fileName + '\'');
				}
			}
		}
		throw new AssertionError("Failed to load '" + fileName + '\'');
	}

	/**
	 * Tests LoaderTest0.luc.
	 */
	@Test
	@SkipSetup
	public void testLoader0() {
		Assert.assertNotNull("Failed to load proto from LoaderTest0.luc", loadProto("LoaderTest0"));
	}

	/**
	 * Tests LoaderTest1.luc.
	 */
	@Test
	@SkipSetup
	public void testLoader1() {
		loadProto("LoaderTest1");
	}

	/**
	 * Tests LoaderTest2.luc.
	 */
	@Test
	@SkipSetup
	public void testLoader2() {
		loadProto("LoaderTest2");
	}

	/**
	 * Tests LoaderTest3.luc.
	 */
	@Test
	@SkipSetup
	public void testLoader3() {
		loadProto("LoaderTest3");
	}
}
