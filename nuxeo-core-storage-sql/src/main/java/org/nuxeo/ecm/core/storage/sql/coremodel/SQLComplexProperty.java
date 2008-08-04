/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.PropertyContainer;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLComplexProperty} gives access to a wrapped SQL-level {@link Node}
 * . This is used for documents and for complex properties.
 *
 * @author Florent Guillaume
 */
public class SQLComplexProperty implements Property, PropertyContainer {

    private static final Log log = LogFactory.getLog(SQLComplexProperty.class);

    protected final Node node;

    protected final ComplexType type;

    protected final SQLSession session;

    /**
     * Creates a {@link SQLComplexProperty} to wrap a {@link Node}.
     */
    public SQLComplexProperty(Node node, ComplexType type, SQLSession session) {
        this.node = node;
        this.type = type;
        this.session = session;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return node.getName();
    }

    public Type getType() {
        return type;
    }

    public boolean isNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Object getValue() throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        Collection<Property> properties = getProperties();
        for (Property property : properties) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) value;
        if (map == null) {
            // XXX should delete the node?
            // throw new RuntimeException("null");
            return;
        }
        for (Entry<String, Object> entry : map.entrySet()) {
            Property property = getProperty(entry.getKey());
            property.setValue(entry.getValue());
        }
    }

    /*
     * ----- Property & PropertyContainer -----
     */

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) throws DocumentException {
        return session.makeProperty(node, type, name);
    }

    public Collection<Property> getProperties() throws DocumentException {
        Collection<Field> fields = type.getFields();
        List<Property> properties = new ArrayList<Property>(fields.size());
        for (Field field : fields) {
            String name = field.getName().getPrefixedName();
            properties.add(session.makeProperty(node, type, name));
        }
        return properties;
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return getProperties().iterator();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.PropertyContainer -------------------
     * (used for SQLDocument, SQLComplexProperty itself doesn't need it)
     */

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public List<String> getDirtyFields() {
        throw new UnsupportedOperationException();
    }

    public Object getPropertyValue(String name) throws DocumentException {
        // when called from AbstractSession.getDataModelFields,
        // we may get an unprefixed name...
        try {
            return getProperty(name).getValue();
        } catch (NoSuchPropertyException e) {
            // XXX we do this because when reading prefetched values,
            // only DocumentException is expected
            // (see DocumentModelFactory.createDocumentModel)
            throw new DocumentException(e);
        }
    }

    public String getString(String name) throws DocumentException {
        return (String) getProperty(name).getValue();
    }

    public boolean getBoolean(String name) throws DocumentException {
        Boolean value = (Boolean) getProperty(name).getValue();
        return value == null ? false : value.booleanValue();
    }

    public long getLong(String name) throws DocumentException {
        Long value = (Long) getProperty(name).getValue();
        return value == null ? 0L : value.longValue();
    }

    public double getDouble(String name) throws DocumentException {
        Double value = (Double) getProperty(name).getValue();
        return value == null ? 0D : value.doubleValue();
    }

    public Calendar getDate(String name) throws DocumentException {
        return (Calendar) getProperty(name).getValue();
    }

    public Blob getContent(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        // TODO check constraints
        try {
            getProperty(name).setValue(value);
            // TODO mark dirty fields
        } catch (DocumentException e) {
            // we log a debugging message here as it is a point where the
            // property name is known
            log.error("Error setting property: " + name + " value: " + value);
            throw e;
        }
    }

    public void setString(String name, String value) throws DocumentException {
        setPropertyValue(name, value);
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
        setPropertyValue(name, Boolean.valueOf(value));
    }

    public void setLong(String name, long value) throws DocumentException {
        setPropertyValue(name, Long.valueOf(value));
    }

    public void setDouble(String name, double value) throws DocumentException {
        setPropertyValue(name, Double.valueOf(value));
    }

    public void setDate(String name, Calendar value) throws DocumentException {
        setPropertyValue(name, value);
    }

    public void setContent(String name, Blob value) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void removeProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

}