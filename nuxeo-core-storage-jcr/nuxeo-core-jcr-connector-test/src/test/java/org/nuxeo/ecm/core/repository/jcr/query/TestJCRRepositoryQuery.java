/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.repository.jcr.query;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.test.QueryTestCase;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;

/**
 * @author Florent Guillaume
 */
public class TestJCRRepositoryQuery extends QueryTestCase {

    @Override
    public void deployRepository() throws Exception {
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "query-repository-contrib.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CustomVersioningService.xml");
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    public void undeployRepository() throws Exception {
    }

    @Override
    public void testQueryNegativeMultiple() throws Exception {
        // JCR cannot do negative queries on multi-valued properties
        // Not testing dc:contributors
        DocumentModelList dml;
        createDocs();
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType <> 'Versionable'");
        assertEquals(3, dml.size()); // 3 folders
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType = 'Versionable' AND ecm:mixinType <> 'Downloadable'");
        assertEquals(1, dml.size()); // 1 note
    }

    public void testQuerySpecialFieldsWithNegative() throws Exception {
        DocumentModelList dml;
        createDocs();
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType NOT IN ('Folderish')");
        assertEquals(4, dml.size()); // 3 file / 1 note
        dml = session.query("SELECT * FROM Document WHERE ecm:mixinType NOT IN ('Commentable', 'Downloadable')");
        assertEquals(3, dml.size()); // 3 folders
    }

    @Override
    public void testFulltextBlob() {
        // TODO blob indexing configuration
    }

    @Override
    public void testStartsWithNonPath() {
        // not done for JCR
    }

}
