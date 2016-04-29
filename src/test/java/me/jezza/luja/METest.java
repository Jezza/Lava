// $Header: //info.ravenbrook.com/project/jili/version/1.1/test/mnj/lua/METest.java#1 $
// Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
// All rights reserved.
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject
// to the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
// CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package me.jezza.luja;
// For j2meunit see http://j2meunit.sourceforge.net/

/**
 * Compendium of all Jill's tests that run in the J2ME environment.
 */
public class METest extends JiliTestCase {

	public static void main(String[] args) {
		new METest().run();
	}

	public Test suite() {
		TestSuite suite = new TestSuite();

		suite.addTest(new ObjectAllTest().suite());
		suite.addTest(new LoaderTest().suite());
		suite.addTest(new LuaTest().suite());
		suite.addTest(new VMTest().suite());
		suite.addTest(new BaseLibTest().suite());
		suite.addTest(new StringLibTest().suite());
		suite.addTest(new SyntaxTest().suite());
		suite.addTest(new MetaTest().suite());
		suite.addTest(new OSLibTest().suite());
		suite.addTest(new TableLibTest().suite());
		suite.addTest(new CoroTest().suite());
		suite.addTest(new PackageLibTest().suite());
		suite.addTest(new MathLibTest().suite());

		return suite;
	}
}
