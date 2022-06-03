/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * @test
 * @bug 8273154
 * @summary Provide a JavadocTester method for non-overlapping, unordered output matching
 * @library /tools/lib/ ../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestJavadocTester
 */

import javadoc.tester.JavadocTester;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import toolbox.ToolBox;

/**
 * Tests basic mechanisms in the {@code JavadocTester} class.
 *
 * It is not a direct test of the javadoc tool or the output generated by the
 * Standard Doclet, although both are indirectly used as part of this test.
 *
 * The test works by exercising the {@code JavadocTester} API with a series of
 * positive and negative tests.  The {@code passed} and {@code failed} methods
 * are overridden to record the messages reported by the underlying instance, so
 * that the messages can subsequently be verified. Also, {@code printSummary}
 * is overridden to suppress the default action to throw {@code Error} when
 * tests have failed.
 */
public class TestJavadocTester extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestJavadocTester tester = new TestJavadocTester();
        tester.setup().runTests();
    }

    private final List<String> messages = new ArrayList<>();
    private int testErrors = 0;

    /**
     * Overrides the default implementation of {@code passed} to record the argument.
     * {@inheritDoc}
     *
     * @param message a short description of the outcome
     */
    @Override
    public void passed(String message) {
        super.passed(message);
        messages.add("Passed: " + message);
    }

    /**
     * Overrides the default implementation of {@code failed} to record the argument.
     * {@inheritDoc}
     *
     * @param message a short description of the outcome
     */
    @Override
    public void failed(String message) {
        super.failed(message);
        messages.add("FAILED: " + message);
    }

    /**
     * Overrides the default implementation of {@code printSummary} to suppress
     * the error thrown as a result of errors reported by {@code JavadocTester}.
     * Instead, an error is thrown if any errors are found by the tests in this class.
     */
    @Override
    public void printSummary() {
        try {
            super.printSummary();
        } catch (Error e) {
            if (e.getClass() != Error.class) {
                throw e;
            }
            report("Suppressed: " + e);
        }

        if (testErrors > 0) {
            report(testErrors + " errors found");
            throw new Error(testErrors + " errors found");
        }
    }

    /**
     * Checks the content of messages reported by the {@code passed} and {@code failed}
     * methods in {@code JavadocTester}.  The messages are saved by the local overloads
     * of those methods in this class.
     *
     * Because some of the messages are <em>very</em> long, it is enough to pass in
     * initial substrings of the expected messages.
     *
     * Note that messages reported by {@code JavadocTester} use filenames as given
     * to the various {@code check...} calls. By convention, these always use {@code /}
     * as the file separator, and not the platform file separator.
     *
     * @param expect initial substrings of expected messages
     */
    void checkMessages(String... expect) {
        for (String e : expect) {
            Optional<String> match = messages.stream()
                    .filter(m -> m.startsWith(e))
                    .findFirst();
            if (match.isPresent()) {
                report("found '" + e + "'");
            } else {
                report("ERROR: no message found for '" + e + "'");
                testErrors++;
            }
        }
    }

    /**
     * Reports a message, preceded by {@code >>> }.
     *
     * It is helpful/important to distinguish the messages written as a side-effect
     * of the underlying tests from the messages used to report the outcome of the
     * tests that verify those messages.  Instead of interposing to mark the messages
     * written as a side effect of the underlying tests, we leave those messages
     * unchanged, and instead, mark the messages reporting whether those messages
     * are as expected or not.
     *
     * @param message the message to be reported.
     */
    private void report(String message) {
        message.lines().forEachOrdered(l -> out.println(">>> " + l));
    }

    //-------------------------------------------------

    private final ToolBox tb = new ToolBox();

    TestJavadocTester setup() throws IOException {
        Path src = Path.of("src");
        tb.writeJavaFiles(src, """
                package p;
                /**
                 * First sentence abc.
                 * Second sentence.
                 * abc123
                 * def456
                 * ghi789
                 * abc123
                 * def456
                 * ghi789
                 */
                public class C {
                    private C() { }
                    /** m3 comment. */
                    public void m3() { }
                    /** m2 comment. */
                    public void m2() { }
                    /** m1 comment. */
                    public void m1() { }
                }
                """);

        javadoc("-d", "out",
                "-sourcepath", src.toString(),
                "-noindex", "-nohelp",
                "p");
        return this;
    }

    @Test
    public void testSimpleStringCheck() {
        messages.clear();
        new OutputChecker("p/C.html")
                .check("Second sentence",
                        "abc123",
                        "def456");
        messages.forEach(this::report);
        checkMessages(
                """
                    Passed: out/p/C.html: following text found:
                    Second sentence""",
                """
                    Passed: out/p/C.html: following text found:
                    abc123""",
                """
                    Passed: out/p/C.html: following text found:
                    def456""");
    }

    @Test
    public void testSimpleNegativeStringCheck_expected() {
        messages.clear();
        new OutputChecker("p/C.html")
                .setExpectFound(false)
                .check("Third sentence.");
        checkMessages(
                """
                    Passed: out/p/C.html: following text not found:
                    Third sentence""");
    }

    @Test
    public void testSimpleNegativeStringCheck_unexpected() {
        messages.clear();
        new OutputChecker("p/C.html")
                .check("Third sentence.");
        checkMessages(
                """
                    FAILED: out/p/C.html: following text not found:
                    Third sentence""");
    }

    @Test
    public void testSimpleRegexCheck() {
        messages.clear();
        new OutputChecker("p/C.html")
                .check(Pattern.compile("S.cond s.nt.nc."),
                        Pattern.compile("[abc]{3}[123]{3}"),
                        Pattern.compile("d.f4.6"));
        checkMessages(
                """
                    Passed: out/p/C.html: following pattern found:
                    S.cond s.nt.nc.""",
                """
                    Passed: out/p/C.html: following pattern found:
                    [abc]{3}[123]{3}""",
                """
                    Passed: out/p/C.html: following pattern found:
                    d.f4.6""");
    }

    @Test
    public void testOrdered() {
        messages.clear();
        // methods are listed alphabetically in the Summary table,
        // but in source-code order in the Details section.
        new OutputChecker("p/C.html")
                .check("<h2>Method Summary</h2>",
                        "<a href=\"#m1()\" class=\"member-name-link\">m1</a>",
                        "<a href=\"#m2()\" class=\"member-name-link\">m2</a>",
                        "<a href=\"#m3()\" class=\"member-name-link\">m3</a>")
                .check("<h2>Method Details</h2>",
                        "<section class=\"detail\" id=\"m3()\">\n",
                        "<section class=\"detail\" id=\"m2()\">\n",
                        "<section class=\"detail\" id=\"m1()\">\n");

        checkMessages(
                """
                    Passed: out/p/C.html: following text found:
                    <h2>Method Summary</h2>""",
                """
                    Passed: out/p/C.html: following text found:
                    <a href="#m1()" class="member-name-link">m1</a>""",
                """
                    Passed: out/p/C.html: following text found:
                    <a href="#m2()" class="member-name-link">m2</a>""",
                """
                    Passed: out/p/C.html: following text found:
                    <a href="#m3()" class="member-name-link">m3</a>""",
                """
                    Passed: out/p/C.html: following text found:
                    <h2>Method Details</h2>""",
                """
                    Passed: out/p/C.html: following text found:
                    <section class="detail" id="m3()">""",
                """
                    Passed: out/p/C.html: following text found:
                    <section class="detail" id="m2()">""",
                """
                    Passed: out/p/C.html: following text found:
                    <section class="detail" id="m1()">"""
        );
    }

    @Test
    public void testUnordered_expected() {
        messages.clear();
        new OutputChecker("p/C.html")
                .setExpectOrdered(false)
                .check("Second sentence",
                        "First sentence");
        checkMessages(
                """
                    Passed: out/p/C.html: following text found:
                    Second sentence""",
                """
                    Passed: out/p/C.html: following text found:
                    First sentence""");
    }

    @Test
    public void testUnordered_unexpected() {
        messages.clear();
        new OutputChecker("p/C.html")
                .check("Second sentence",
                        "First sentence");
        checkMessages(
                """
                    Passed: out/p/C.html: following text found:
                    Second sentence""",
                """
                    FAILED: out/p/C.html: following text was found on line""");
    }

    @Test
    public void testComplete_Ordered() {
        messages.clear();
        // In the following calls, the strings are specified in the expected order.
        // File separators are made platform-specific by calling 'fix'.
        // Newlines are handled automatically by the 'check' method.
        new OutputChecker(Output.OUT)
                .check("Loading source files for package p...\n",
                        "Constructing Javadoc information...\n",
                        fix("Creating destination directory: \"out/\"\n"))
                .check(Pattern.compile("Standard Doclet .*\\R"))
                .check("Building tree for all the packages and classes...\n",
                        fix("Generating out/p/C.html...\n"),
                        fix("Generating out/p/package-summary.html...\n"),
                        fix("Generating out/p/package-tree.html...\n"),
                        fix("Generating out/overview-tree.html...\n"),
                        fix("Generating out/index.html...\n"))
                .checkComplete();
        checkMessages("Passed: All output matched");
    }

    @Test
    public void testComplete_Unordered() {
        messages.clear();
        // In the following calls, the strings are deliberately specified out of the expected order.
        // File separators are made platform-specific by calling 'fix'.
        // Newlines are handled automatically by the 'check' method.
        new OutputChecker(Output.OUT)
                .setExpectOrdered(false)
                .check("Loading source files for package p...\n",
                        "Constructing Javadoc information...\n",
                        "Building tree for all the packages and classes...\n")
                .check(fix("Creating destination directory: \"out/\"\n",
                        "Generating out/index.html...\n",
                        "Generating out/overview-tree.html...\n",
                        "Generating out/p/package-tree.html...\n",
                        "Generating out/p/package-summary.html...\n",
                        "Generating out/p/C.html...\n"))
                .check(Pattern.compile("Standard Doclet .*\\R"))
                .checkComplete();
        checkMessages("Passed: All output matched");
    }

    @Test
    public void testEmpty() {
        messages.clear();
        new OutputChecker(Output.STDERR)
                .checkEmpty();
        checkMessages("Passed: STDERR is empty, as expected");
    }

    @Test
    public void testBadFile() {
        messages.clear();
        new OutputChecker("does-not-exist.html")
                .check("abcdef",
                        "very long string ".repeat(10))
                .check(Pattern.quote("abcdef"),
                        Pattern.quote("very long string".repeat(10)));
        checkMessages("FAILED: File not found: does-not-exist.html");
    }

    @Test
    public void testAnyOf() {
        messages.clear();
        new OutputChecker("p/C.html")
                .checkAnyOf("m1()", "m2()", "m3()")    // expect all found
                .checkAnyOf("m1()", "m2()", "M3()")    // expect some found
                .checkAnyOf("M1()", "M2()", "M3()");   // expect none found
        checkMessages("Passed: 3 matches found",
                "Passed: 2 matches found",
                "FAILED: no match found for any text");
    }

    @Test
    public void testUnique() {
        messages.clear();
        new OutputChecker("p/C.html")
                .setExpectOrdered(false)
                .checkUnique("id=\"m1()\"", "id=\"m2()\"", "id=\"m3()\"")   // expect unique
                .checkUnique("m1()", "m2()", "m3()");                       // expect not unique
        checkMessages("Passed: out/p/C.html: id=\"m1()\" is unique",
                "Passed: out/p/C.html: id=\"m2()\" is unique",
                "Passed: out/p/C.html: id=\"m3()\" is unique",
                "FAILED: out/p/C.html: m1() is not unique",
                "FAILED: out/p/C.html: m2() is not unique",
                "FAILED: out/p/C.html: m3() is not unique");
    }

    /**
     * {@return a string with {@code /} replaced by the platform file separator}
     *
     * @param item the string
     */
    private String fix(String item) {
        return item.replace("/", FS);
    }

    /**
     * {@return an array of strings with {@code /} replaced by the platform file separator}
     *
     * @param items the strings
     */
    private String[] fix(String... items) {
        return Stream.of(items)
                .map(this::fix)
                .toArray(String[]::new);
    }
}
