/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.core.convert.extension;

import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;

/**
 * Interface that must be implemented by Converter that depend on an external
 * service.
 * <p>
 * Compared to {@link Converter} interface, it adds support for
 * checking converter availability.
 *
 * @author tiry
 */
public interface ExternalConverter extends Converter {

    /**
     * Checks if the converter is available.
     *
     * @return
     */
    ConverterCheckResult isConverterAvailable();

}
