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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;

public class UserVisiblePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String permission;

    protected final String denyPermission;

    protected final String id;

    public UserVisiblePermission(String id, String perm, String denyPerm) {
        permission = perm;
        denyPermission = denyPerm;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserVisiblePermission other = (UserVisiblePermission) o;

        if (denyPermission != null ? !denyPermission.equals(other.denyPermission) : other.denyPermission != null) {
            return false;
        }
        if (id != null ? !id.equals(other.id) : other.id != null) {
            return false;
        }
        if (permission != null ? !permission.equals(other.permission) : other.permission != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = permission != null ? permission.hashCode() : 0;
        result = 31 * result + (denyPermission != null ? denyPermission.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (denyPermission != null) {
            return String.format("UserVisiblePermission %s [%s (deny %s)]", id, permission, denyPermission);
        } else {
            return String.format("UserVisiblePermission %s [%s]", id, permission);
        }
    }

    public String getPermission() {
        return permission;
    }

    public String getDenyPermission() {
        return denyPermission;
    }

    public String getId() {
        return id;
    }

}
