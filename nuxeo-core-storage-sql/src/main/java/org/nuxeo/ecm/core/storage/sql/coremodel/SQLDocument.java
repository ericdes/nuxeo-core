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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.PropertyContainer;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author Florent Guillaume
 */
public class SQLDocument extends SQLComplexProperty implements Document {

    private static final Log log = LogFactory.getLog(SQLDocument.class);

    protected SQLDocument(Node node, ComplexType type, SQLSession session,
            boolean readonly) {
        super(node, type, session, readonly);
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Document -----
     */

    // public String getName(); from SQLComplexProperty
    //
    @Override
    public DocumentType getType() {
        return (DocumentType) type;
    }

    public Session getSession() {
        return session;
    }

    public boolean isFolder() {
        return ((DocumentType) type).isFolder();
    }

    public String getUUID() {
        return getHierarchyNode().getId().toString();
    }

    public Document getParent() throws DocumentException {
        return session.getParent(getHierarchyNode());
    }

    public String getPath() throws DocumentException {
        return session.getPath(getHierarchyNode());
    }

    public Calendar getLastModified() {
        throw new UnsupportedOperationException("unused");
    }

    public boolean isProxy() {
        return false;
    }

    public Repository getRepository() {
        return session.getRepository();
    }

    public void remove() throws DocumentException {
        session.remove(getHierarchyNode());
    }

    public void save() throws DocumentException {
        session.save();
    }

    /**
     * Checks if the document is "dirty", which means that a change on it was
     * made since the last time a snapshot of it was done (for publishing).
     */
    public boolean isDirty() throws DocumentException {
        return getBoolean(Model.MISC_DIRTY_PROP);
    }

    /**
     * Marks the document "dirty", which means that a change on it was made
     * since the last time a snapshot of it was done (for publishing).
     */
    public void setDirty(boolean value) throws DocumentException {
        setBoolean(Model.MISC_DIRTY_PROP, value);
    }

    public void readDocumentPart(DocumentPart dp) throws Exception {
        readPropertyContainer((ComplexProperty) dp, this);
    }

    protected static void readPropertyContainer(
            ComplexProperty complexProperty, PropertyContainer propertyContainer)
            throws DocumentException, PropertyException {
        for (Property property : complexProperty) {
            readOneProperty(property,
                    propertyContainer.getPropertyValue(property.getName()));
        }
    }

    protected static void readOneProperty(Property property, Object value)
            throws PropertyException, DocumentException {
        if (property.isContainer()) {
            if (property.isList()) {
                for (Object v : (List<?>) value) {
                    readPropertyContainer((ComplexProperty) property.add(),
                            (PropertyContainer) v);
                }
            } else {
                property.init((Serializable) value);
                // readPropertyContainer((ComplexProperty) property,
                // (PropertyContainer) value);
            }
        } else {
            property.init((Serializable) value);
        }
    }

    public void writeDocumentPart(DocumentPart dp) throws Exception {
        for (Property property : dp) {
            setPropertyValue(property.getName(), property.getValue());
        }
        dp.clearDirtyFlags();
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
        return;
        // TODO XXX
        // throw new UnsupportedOperationException();
    }

    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        if (type == Boolean.class) {
            return (T) Boolean.FALSE;
        }
        return null;
        // TODO XXX
        // throw new UnsupportedOperationException();
    }

    /*
     * ----- LifeCycle -----
     */

    public String getLifeCyclePolicy() throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        return service.getLifeCyclePolicy(this);
    }

    public String getCurrentLifeCycleState() throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        return service.getCurrentLifeCycleState(this);
    }

    public boolean followTransition(String transition)
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
        return true;
    }

    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        LifeCycle lifeCycle = service.getLifeCycleFor(this);
        if (lifeCycle == null) {
            return Collections.emptyList();
        }
        return lifeCycle.getAllowedStateTransitionsFrom(service.getCurrentLifeCycleState(this));
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Lockable -----
     */

    // TODO: optimize this since it is used in permission checks
    public boolean isLocked() throws DocumentException {
        return getLock() != null;
    }

    public String getLock() throws DocumentException {
        return null;
        // throw new UnsupportedOperationException();
    }

    public void setLock(String key) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public String unlock() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.versioning.VersionableDocument -----
     */

    public boolean isVersion() {
        return false;
    }

    public Document getSourceDocument() throws DocumentException {
        return this;
    }

    public void checkIn(String label) throws DocumentException {
        checkIn(label, null);
    }

    public void checkIn(String label, String description)
            throws DocumentException {
        session.checkIn(getHierarchyNode(), label, description);
    }

    public void checkOut() throws DocumentException {
        session.checkOut(getHierarchyNode());
    }

    public boolean isCheckedOut() throws DocumentException {
        return !getBoolean(Model.MAIN_CHECKED_IN_PROP);
    }

    public void restore(String label) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public List<String> getVersionsIds() throws DocumentException {
        Collection<DocumentVersion> versions = session.getVersions(getHierarchyNode());
        List<String> ids = new ArrayList<String>(versions.size());
        for (DocumentVersion version : versions) {
            ids.add(version.getUUID());
        }
        return ids;
    }

    public Document getVersion(String label) throws DocumentException {
        return session.getVersionByLabel(getHierarchyNode(), label);
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        return new SQLDocumentVersionIterator(
                session.getVersions(getHierarchyNode()));
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        return session.getLastVersion(getHierarchyNode());
    }

    public boolean hasVersions() throws DocumentException {
        log.error("hasVersions unimplemented, returning false");
        return false;
        // XXX TODO
        // throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.DocumentContainer -----
     */

    public Document resolvePath(String path) throws DocumentException {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (path.length() == 0) {
            return this;
        }
        // this API doesn't take absolute paths
        if (path.startsWith("/")) {
            // TODO log warning
            path = path.substring(1);
        }
        return session.resolvePath(getHierarchyNode(), path);
    }

    public Document getChild(String name) throws DocumentException {
        return session.getChild(getHierarchyNode(), name);
    }

    public Iterator<Document> getChildren() throws DocumentException {
        return getChildren(0);
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        List<Document> children = session.getChildren(getHierarchyNode());
        if (start < 0) {
            throw new IllegalArgumentException(String.valueOf(start));
        }
        if (start >= children.size()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        return new SQLDocumentListIterator(children.subList(start,
                children.size()));
    }

    public List<String> getChildrenIds() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        // not optimized as this method doesn't seem to be used
        List<Document> children = session.getChildren(getHierarchyNode());
        List<String> ids = new ArrayList<String>(children.size());
        for (Document child : children) {
            ids.add(child.getUUID());
        }
        return ids;
    }

    public boolean hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(getHierarchyNode(), name);
    }

    public boolean hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(getHierarchyNode());
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        // TODO pos
        return session.addChild(getHierarchyNode(), name, null, typeName);
    }

    public void orderBefore(String src, String dest) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void removeChild(String name) throws DocumentException {
        if (!isFolder()) {
            return; // ignore non folder documents XXX urgh
        }
        Document doc = getChild(name);
        doc.remove();
    }

    /*
     * ----- PropertyContainer inherited from SQLComplexProperty -----
     */

    /*
     * ----- internal for SQLSecurityManager -----
     */

    protected org.nuxeo.ecm.core.model.Property getACLProperty()
            throws DocumentException {
        return session.makeACLProperty(getHierarchyNode());
    }

    /*
     * ----- toString/equals/hashcode -----
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName() + ',' + getUUID() +
                ')';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SQLDocument) {
            return equals((SQLDocument) other);
        }
        return false;
    }

    private boolean equals(SQLDocument other) {
        return getHierarchyNode().getId() == other.getHierarchyNode().getId();
    }

    @Override
    public int hashCode() {
        return getHierarchyNode().getId().hashCode();
    }

}

class SQLDocumentListIterator implements DocumentIterator {

    private final int size;

    private final Iterator<Document> iterator;

    public SQLDocumentListIterator(List<Document> list) {
        size = list.size();
        iterator = list.iterator();
    }

    public long getSize() {
        return size;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Document next() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}

class SQLDocumentVersionIterator implements DocumentVersionIterator {

    private final Iterator<DocumentVersion> iterator;

    public SQLDocumentVersionIterator(Collection<DocumentVersion> list) {
        iterator = list.iterator();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public DocumentVersion next() {
        return iterator.next();
    }

    public DocumentVersion nextDocumentVersion() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
