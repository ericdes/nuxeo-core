/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;

/**
 * Simple interface useful to wrap a sequence of calls for performing an import.
 * This could be handy to quickly define an importer and sent it as parameter so
 * the method will be callback.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public interface DocumentsImporter {

    DocumentTranslationMap importDocs(InputStream sourceInputStream)
            throws ImportDocumentException, ClientException, IOException;
}
