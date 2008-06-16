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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.File;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.glassfish.embed.GFApplication;
import org.glassfish.embed.GlassFish;
import org.w3c.dom.Document;

/**
 * TestCase deploying an embedded GlassFish v3.
 *
 * @author Florent Guillaume
 */
public abstract class GlassFishTestCase extends TestCase {

    private GlassFish glassfish;

    private GFApplication gfApplication;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        GlassFish.setLogLevel(Level.CONFIG);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        String uri = getClass().getResource("/test-domain.xml").toExternalForm();
        Document domainXml = builder.parse(uri);

        glassfish = new GlassFish(domainXml);
        gfApplication = glassfish.deploy(new File("target/classes"));
    }

    @Override
    public void tearDown() throws Exception {
        gfApplication.undeploy();
        glassfish.stop();
        super.tearDown();
    }
}
