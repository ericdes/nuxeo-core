/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.security.Principal;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Default implementation
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventContextImpl extends AbstractEventContext {

    private static final long serialVersionUID = 1L;

    protected transient CoreSession session;
    protected transient Principal principal;

    /**
     * Constructor to be used by derived classes
     */
    protected EventContextImpl() {
    }

    public EventContextImpl(Object ... args) {
        this(null, null, args);
    }

    public EventContextImpl(CoreSession session, Principal principal, Object ... args) {
        super (args);
        this.session = session;
        this.principal = principal;
    }


    public CoreSession getCoreSession() {
        return session;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public void setPrincipal(NuxeoPrincipal principal) {
        this.principal = principal;
    }

}