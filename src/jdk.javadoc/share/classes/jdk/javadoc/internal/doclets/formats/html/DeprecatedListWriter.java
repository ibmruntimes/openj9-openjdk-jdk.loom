/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.javadoc.internal.doclets.formats.html;

import com.sun.source.doctree.DeprecatedTree;
import java.util.List;

import javax.lang.model.element.Element;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DeprecatedAPIListBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;

/**
 * Generate File to list all the deprecated classes and class members with the
 * appropriate links.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see java.util.List
 */
public class DeprecatedListWriter extends SummaryListWriter<DeprecatedAPIListBuilder> {

    /**
     * Constructor.
     *
     * @param configuration the configuration for this doclet
     * @param filename the file to be generated
     */

    public DeprecatedListWriter(HtmlConfiguration configuration, DocPath filename) {
        super(configuration, filename, PageMode.DEPRECATED, "deprecated elements",
              configuration.contents.deprecatedAPI, "doclet.Window_Deprecated_List");
    }

    /**
     * Get list of all the deprecated classes and members in all the Packages
     * specified on the command line.
     * Then instantiate DeprecatedListWriter and generate File.
     *
     * @param configuration the current configuration of the doclet.
     * @throws DocFileIOException if there is a problem writing the deprecated list
     */
    public static void generate(HtmlConfiguration configuration) throws DocFileIOException {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.DEPRECATED)) {
            DocPath filename = DocPaths.DEPRECATED_LIST;
            DeprecatedListWriter depr = new DeprecatedListWriter(configuration, filename);
            depr.generateSummaryListFile(configuration.deprecatedAPIListBuilder);
        }
    }

    @Override
    protected void addExtraSection(DeprecatedAPIListBuilder list, Content content) {
        addSummaryAPI(list.getForRemoval(), "forRemoval",
                    "doclet.For_Removal", "doclet.Element", content);
    }

    @Override
    protected void addExtraIndexLink(DeprecatedAPIListBuilder list, Content target) {
        if (!list.getForRemoval().isEmpty()) {
            addIndexLink("forRemoval", "doclet.For_Removal", target);
        }
    }

    protected void addComments(Element e, Content desc) {
        List<? extends DeprecatedTree> tags = utils.getDeprecatedTrees(e);
        if (!tags.isEmpty()) {
            addInlineDeprecatedComment(e, tags.get(0), desc);
        } else {
            desc.add(HtmlTree.EMPTY);
        }
    }

}
