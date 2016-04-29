package me.jezza.luja;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public class TestSuite implements Test {

	private List<Test> cases = new ArrayList<>();

	public void addTest(Test testCase) {
		cases.add(testCase);
	}

	@Override
	public void runTest() {
		for (Test test : cases) {
			test.runTest();
		}
	}

	@Override
	public List<Test> cases() {
		return cases;
	}
}
