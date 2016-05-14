package me.jezza.lava;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public class TestSuite implements OldTest {

	private List<OldTest> cases = new ArrayList<>();

	public void addTest(OldTest testCase) {
		cases.add(testCase);
	}

	@Override
	public void runTest() {
		for (OldTest test : cases) {
			test.runTest();
		}
	}

	@Override
	public List<OldTest> cases() {
		return cases;
	}
}
