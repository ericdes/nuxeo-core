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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.storage.PartialList;

/**
 * @author Florent Guillaume
 */
public class SQLQueryResult implements QueryResult {

    private static final Log log = LogFactory.getLog(SQLQueryResult.class);

    protected final SQLSession session;

    protected final Iterator<Serializable> it;

    protected final long size;

    protected final long totalSize;

    /** When not null, order documents models by path asc (true) or desc (false) */
    protected final Boolean orderByPath;

    protected final int limit;

    protected final int offset;

    protected Serializable currentId;

    public SQLQueryResult(SQLSession session, PartialList<Serializable> pl,
            Boolean orderByPath, long limit, long offset) {
        this.session = session;
        it = pl.list.iterator();
        size = pl.list.size();
        this.totalSize = pl.totalSize;
        this.orderByPath = orderByPath;
        this.limit = (int) limit;
        this.offset = (int) offset;
    }

    public long count() {
        return size;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public DocumentModelList getDocumentModels() throws QueryException {
        String[] schemas = { "common" };
        List<DocumentModel> list = new ArrayList<DocumentModel>((int) size);
        while (it.hasNext()) {
            currentId = it.next();
            try {
                // TODO skip root
                Document doc = session.getDocumentById(currentId);
                list.add(DocumentModelFactory.createDocumentModel(doc, schemas));
            } catch (DocumentException e) {
                log.error("Could not create document model for doc: "
                        + currentId + ": " + e.getMessage());
            }
        }
        if (orderByPath != null) {
            Collections.sort(list, new PathComparator(
                    orderByPath.booleanValue()));
        }
        if (limit != 0) {
            // do limit/offset by hand
            int size = list.size();
            list.subList(0, offset > size ? size : offset).clear();
            size = list.size();
            if (limit < size) {
                list.subList(limit, size).clear();
            }
        }
        return new DocumentModelListImpl(list, totalSize);
    }

    public static class PathComparator implements Comparator<DocumentModel> {

        private final int sign;

        public PathComparator(boolean asc) {
            this.sign = asc ? 1 : -1;
        }

        public int compare(DocumentModel doc1, DocumentModel doc2) {
            String p1 = doc1.getPathAsString();
            String p2 = doc2.getPathAsString();
            return sign * p1.compareTo(p2);
        }
    }

    public DocumentIterator getDocuments(int start) {
        // initial skip
        for (int i = 0; i < start; i++) {
            if (it.hasNext()) {
                it.next();
            } else {
                return EmptyDocumentIterator.INSTANCE;
            }
        }

        return new DocumentIterator() {
            public Document next() {
                currentId = it.next();
                try {
                    return session.getDocumentById(currentId);
                } catch (DocumentException e) {
                    log.error(e.getMessage());
                    return next();
                }
            }

            public boolean hasNext() {
                return it.hasNext();
            }

            public long getSize() {
                return size;
            }

            public void remove() {
            }
        };
    }

    public boolean next() {
        if (!it.hasNext()) {
            return false;
        }
        currentId = it.next();
        return true;
    }

    public Object getObject() throws QueryException {
        throw new UnsupportedOperationException();
    }

    public long row() {
        throw new UnsupportedOperationException();
    }

    public boolean getBoolean(int i) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public boolean getBoolean(String column) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public double getDouble(int i, double defaultValue) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public double getDouble(String column, double defaultValue)
            throws QueryException {
        throw new UnsupportedOperationException();
    }

    public long getLong(int i, long defaultValue) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public long getLong(String column, long defaultValue) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public Object getObject(String column) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public String getString(int i) throws QueryException {
        throw new UnsupportedOperationException();
    }

    public String getString(String column) throws QueryException {
        throw new UnsupportedOperationException();
    }

}
