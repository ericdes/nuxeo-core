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
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ArrayMap;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Null;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterDescriptor;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterService;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @version $Revision: 1.0 $
 */
@SuppressWarnings( { "SuppressionAnnotation" })
public class DocumentModelImpl implements DocumentModel, Cloneable {

    public static final String STRICT_LAZY_LOADING_POLICY_KEY = "org.nuxeo.ecm.core.strictlazyloading";

    public static final long F_STORED = 1L;

    public static final long F_DETACHED = 2L;

    // reserved: 4, 8

    public static final long F_VERSION = 16L;

    public static final long F_PROXY = 32L;

    public static final long F_LOCKED = 64L;

    public static final long F_DIRTY = 128L;

    private static final long serialVersionUID = 4473357367146978325L;

    private static final Log log = LogFactory.getLog(DocumentModelImpl.class);

    protected String sid;

    protected DocumentRef ref;

    protected TypeRef<DocumentType> type;

    protected String[] declaredSchemas;

    protected Set<String> declaredFacets;

    protected String id;

    protected Path path;

    protected DataModelMap dataModels;

    protected DocumentRef parentRef;

    protected String lock;

    // acp is not send between client/server
    // it will be loaded lazy first time it is accessed
    // and discarded when object is serialized
    protected transient ACP acp;

    // whether the acp was cached
    protected transient boolean isACPLoaded = false;

    // the adapters registered for this document - only valid on client
    protected transient ArrayMap<Class, Object> adapters;

    // flags : TODO
    // bit 0 - IS_STORED (1 if stored in repo, 0 otherwise)
    // bit 1 - IS_DETACHED (1 after deserialization, 0 otherwise)
    // bit 2 - 3: reserved for future use
    // bit 4: IS_VERSION (true if set)
    // bit 5: IS_PROXY (true if set)
    // bit 6: IS_LOCKED (true if set)
    // bit 7: IS_DIRTY (true if set)
    protected long flags = 0L;

    protected String repositoryName;

    protected String sourceId;

    private ScopedMap contextData;

    @SuppressWarnings( { "CollectionDeclaredAsConcreteClass" })
    protected HashMap<String, Serializable> prefetch;

    private String currentLifeCycleState;

    private String lifeCyclePolicy;

    protected static Boolean strictSessionManagement=null;

    protected DocumentModelImpl() {
    }

    /**
     * Constructor to use a document model client side without referencing a
     * document.
     * <p>
     * It must at least contain the type.
     *
     * @param type String
     */
    public DocumentModelImpl(String type) {
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        dataModels = new DataModelMapImpl();
        contextData = new ScopedMap();
    }

    /**
     * Constructor to use a document model client side without referencing a
     * document.
     * <p>
     * It must at least contain the type.
     *
     * @param sid String
     * @param type String
     */
    public DocumentModelImpl(String sid, String type) {
        this(type);
        this.sid = sid;
    }

    /**
     * Constructor to be used by clients.
     * <p>
     * A client constructed data model must contain at least the path and the
     * type.
     *
     * @param parentPath
     * @param name
     * @param type
     */
    public DocumentModelImpl(String parentPath, String name, String type) {
        this(parentPath, name, type, null);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parent DocumentModel
     * @param name String
     * @param type String
     */
    public DocumentModelImpl(DocumentModel parent, String name, String type) {
        this(parent.getPathAsString(), name, type, null);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parent DocumentModel
     * @param name String
     * @param type String
     * @param data DataModelMap
     */
    public DocumentModelImpl(DocumentModel parent, String name, String type,
            DataModelMap data) {
        this(parent.getPathAsString(), name, type, data);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parentPath
     * @param name
     * @param type
     * @param data allows to initialize a document with initial data
     */
    public DocumentModelImpl(String parentPath, String name, String type,
            DataModelMap data) {
        path = new Path(parentPath + '/' + name);
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        ref = new PathRef(parentPath, name);
        dataModels = data == null ? new DataModelMapImpl() : data;
        contextData = new ScopedMap();
    }

    /**
     * Constructor to be used on server side to create a document model.
     *
     * @param sid
     * @param type
     * @param id
     * @param path
     * @param docRef
     * @param parentRef
     * @param schemas
     * @param facets
     */
    public DocumentModelImpl(String sid, String type, String id, Path path,
            DocumentRef docRef, DocumentRef parentRef, String[] schemas,
            Set<String> facets) {
        this(sid, type, id, path, null, docRef, parentRef, schemas, facets);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param sid String
     * @param type String
     * @param id String
     * @param path Path
     * @param lock String
     * @param docRef DocumentRef
     * @param parentRef DocumentRef
     * @param schemas String[]
     * @param facets
     */
    @Deprecated
    public DocumentModelImpl(String sid, String type, String id, Path path,
            String lock, DocumentRef docRef, DocumentRef parentRef,
            String[] schemas, Set<String> facets) {
        this.sid = sid;
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        this.id = id;
        this.path = path;
        ref = docRef;
        this.parentRef = parentRef;
        declaredSchemas = schemas;
        declaredFacets = facets;
        dataModels = new DataModelMapImpl();
        this.lock = lock;
        contextData = new ScopedMap();
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param sid String
     * @param type String
     * @param id String
     * @param path Path
     * @param lock String
     * @param docRef DocumentRef
     * @param parentRef DocumentRef
     * @param schemas String[]
     * @param facets
     * @param sourceId String
     * @param repositoryName String
     */
    public DocumentModelImpl(String sid, String type, String id, Path path,
            String lock, DocumentRef docRef, DocumentRef parentRef,
            String[] schemas, Set<String> facets, String sourceId,
            String repositoryName) {
        this.sid = sid;
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        this.id = id;
        this.path = path;
        ref = docRef;
        this.parentRef = parentRef;
        declaredSchemas = schemas;
        declaredFacets = facets;
        dataModels = new DataModelMapImpl();
        this.lock = lock;
        contextData = new ScopedMap();
        this.repositoryName = repositoryName;
        this.sourceId = sourceId;
    }

    public DocumentType getDocumentType() {
        return type.get();
    }

    /**
     * Gets the title from the dublincore schema.
     *
     * @return String
     * @throws ClientException
     * @see DocumentModel#getTitle()
     */
    public String getTitle() throws ClientException {
        String title = (String) getProperty("dublincore", "title");
        if (title != null) {
            return title;
        }
        title = getName();
        if (title != null) {
            return title;
        }
        return id;
    }

    public String getSessionId() {
        return sid;
    }

    public DocumentRef getRef() {
        return ref;
    }

    public DocumentRef getParentRef() {
        if (parentRef == null && path != null) {
            Path parentPath = path.removeLastSegments(1);
            parentRef = new PathRef(parentPath.toString());
        }
        return parentRef;
    }

    public CoreSession getCoreSession() {
        if (sid == null) {
            return null;
        }
        return CoreInstance.getInstance().getSession(sid);
    }

    protected boolean useStrictSessionManagement() {
        if (strictSessionManagement==null) {
            strictSessionManagement = Boolean.parseBoolean(Framework.getProperty(STRICT_LAZY_LOADING_POLICY_KEY, "false"));
        }
        return strictSessionManagement;
    }

    protected CoreSession getTempCoreSession() throws ClientException {
        CoreSession tempSession = null;
        if (sid!=null) { // detached docs need a tmp session anyway
             if (useStrictSessionManagement()) {
                 throw new ClientException("Document " + id + " is bound to a closed CoreSession, can not reconnect");
             }
        }
        try {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repo = mgr.getRepository(repositoryName);
            tempSession = repo.open();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        return tempSession;
    }

    /** @deprecated use {@link #getCoreSession} instead. */
    @Deprecated
    public final CoreSession getClient() throws ClientException {
        if (sid == null) {
            throw new UnsupportedOperationException(
                    "Cannot load data models for client defined models");
        }
        CoreSession session = CoreInstance.getInstance().getSession(sid);
        if (session == null && sid != null && repositoryName != null) {
            // session was closed => open a new one
            try {
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                Repository repo = mgr.getRepository(repositoryName);
                session = repo.open();
                // set new session id
                sid = session.getSessionId();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return session;
    }

    /**
     * Detaches the documentImpl from its existing session, so that it can
     * survive beyond the session's closing.
     *
     * @param loadAll if {@code true}, load all data from the session before
     *            detaching
     */
    public void detach(boolean loadAll) throws ClientException {
        if (sid == null) {
            return;
        }
        if (loadAll && type != null) {
            DocumentType dt = type.get();
            if (dt != null) {
                for (String schema : dt.getSchemaNames()) {
                    if (!isSchemaLoaded(schema)) {
                        loadDataModel(schema);
                    }
                }
            }
        }
        // fetch ACP too if possible
        if (ref!=null) {
            getACP();
        }
        sid = null;
    }


    /**
     * Lazily loads the given data model.
     *
     * @param schema
     * @return DataModel
     * @throws ClientException
     */
    protected final DataModel loadDataModel(String schema)
            throws ClientException {
        if (hasSchema(schema)) { // lazy data model
            if (sid == null) {
                DataModel dataModel = new DataModelImpl(schema); // supports non
                // bound docs
                dataModels.put(schema, dataModel);
                return dataModel;
            }
            CoreSession session = getCoreSession();

            DataModel dataModel = null;
            if (ref!=null) {
                if (session!=null) {
                    dataModel = session.getDataModel(ref, schema);
                }
                else {
                    if (useStrictSessionManagement()) {
                        log.warn("DocumentModel " + id + " is bound to a null or closed session : lazy loading is not available");
                    }
                    else {
                        CoreSession tmpSession = getTempCoreSession();
                        try {
                            dataModel = tmpSession.getDataModel(ref, schema);
                        }
                        finally {
                            if (tmpSession!=null) {
                                CoreInstance.getInstance().close(tmpSession);
                            }
                        }
                    }
                }
                dataModels.put(schema, dataModel);
            }
            return dataModel;
        }
        return null;
    }

    public DataModel getDataModel(String schema) throws ClientException {
        DataModel dataModel = dataModels.get(schema);
        if (dataModel == null) {
            dataModel = loadDataModel(schema);
        }
        return dataModel;
    }

    public Collection<DataModel> getDataModelsCollection() {
        return dataModels.values();
    }

    public void addDataModel(DataModel dataModel) {
        dataModels.put(dataModel.getSchema(), dataModel);
    }

    public String[] getDeclaredSchemas() {
        return declaredSchemas;
    }

    public Set<String> getDeclaredFacets() {
        return declaredFacets;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        if (path != null) {
            return path.lastSegment();
        }
        return null;
    }

    public String getPathAsString() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }

    public Map<String, Object> getProperties(String schemaName)
            throws ClientException {
        DataModel dm = getDataModel(schemaName);
        return dm == null ? null : dm.getMap();
    }

    /**
     * Gets property.
     * <p>
     * Get property is also consulting the prefetched properties.
     *
     * @param schemaName String
     * @param name String
     * @return Object
     * @see DocumentModel#getProperty(String, String)
     */
    public Object getProperty(String schemaName, String name)
            throws ClientException {
        DataModel dm = dataModels.get(schemaName);
        if (dm == null) { // no data model loaded
            // try prefetched props
            if (prefetch != null) {
                Object value = prefetch.get(schemaName + '.' + name);
                if (value != null) {
                    return value == Null.VALUE ? null : value;
                }
            }
            dm = getDataModel(schemaName);
        }
        return dm == null ? null : dm.getData(name);
    }

    public void setPathInfo(String parentPath, String name) {
        path = new Path(parentPath + '/' + name);
        ref = new PathRef(parentPath, name);
    }

    public String getLock() {
        return lock;
    }

    public boolean isLocked() {
        return lock != null;
    }

    public void setLock(String key) throws ClientException {
        CoreSession session = getCoreSession();

        if (session!=null) {
            session.setLock(ref, key);
        }
        else {
             CoreSession tmpSession = getTempCoreSession();
             try {
                 tmpSession.setLock(ref, key);
             }
             finally {
                 if (tmpSession!=null) {
                     try {
                         tmpSession.save();
                     }
                     finally {
                         CoreInstance.getInstance().close(tmpSession);
                     }
                 }
             }
        }
        lock = key;
    }

    public void unlock() throws ClientException {
        CoreSession session = getCoreSession();
        if (session!=null) {
            if (session.unlock(ref) != null) {
                lock = null;
            }
        }
        else {
             CoreSession tmpSession = getTempCoreSession();
             try {
                 if (tmpSession.unlock(ref) != null) {
                     lock = null;
                 }
             }
             finally {
                 if (tmpSession!=null) {
                     try {
                         tmpSession.save();
                     }
                     finally {
                         CoreInstance.getInstance().close(tmpSession);
                     }
                 }
             }
        }
    }

    public ACP getACP() throws ClientException {
        if (!isACPLoaded) { // lazy load
            CoreSession session = getCoreSession();
            if (session!=null) {
                acp = session.getACP(ref);
            }
            else {
                CoreSession tmpSession = getTempCoreSession();
                try {
                    acp = tmpSession.getACP(ref);
                }
                finally {
                    if (tmpSession!=null) {
                        CoreInstance.getInstance().close(tmpSession);
                    }
                }
            }
            isACPLoaded = true;
        }
        return acp;
    }

    public void setACP(ACP acp, boolean overwrite) throws ClientException {
        CoreSession session = getCoreSession();
        if (session!=null) {
            session.setACP(ref, acp, overwrite);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                tmpSession.setACP(ref, acp, overwrite);
            }
            finally {
                if (tmpSession!=null) {
                    try {
                        tmpSession.save();
                    }
                    finally {
                        CoreInstance.getInstance().close(tmpSession);
                    }
                }
            }
        }
        isACPLoaded = false;
    }

    public String getType() {
        // TODO there are some DOcumentModel impl like DocumentMessageImpl which
        // use null types and extend this impl which is wrong - fix this -> type
        // must never be null
        return type != null ? type.getName() : null;
    }

    public void setProperties(String schemaName, Map<String, Object> data)
            throws ClientException {
        DataModel dm = getDataModel(schemaName);
        if (dm != null) {
            dm.setMap(data);
        }
    }

    public void setProperty(String schemaName, String name, Object value)
            throws ClientException {
        DataModel dm = getDataModel(schemaName);
        if (dm != null) {
            dm.setData(name, value);
        }
    }

    public boolean hasSchema(String schema) {
        if (type == null) {
            return false;
        }
        DocumentType dt = type.get(); // some tests use dummy types. TODO: fix
        // these tests? (TestDocumentModel)
        return dt == null ? false : dt.hasSchema(schema);
    }

    public boolean hasFacet(String facet) {
        if (declaredFacets != null) {
            return declaredFacets.contains(facet);
        }
        return false;
    }

    public Path getPath() {
        return path;
    }

    public DataModelMap getDataModels() {
        return dataModels;
    }

    public void copyContentInto(DocumentModelImpl other) {
        other.declaredSchemas = declaredSchemas;
        other.declaredFacets = declaredFacets;
        other.dataModels = dataModels;
    }

    public boolean isFolder() {
        return hasFacet("Folderish");
    }

    public boolean isVersionable() {
        return hasFacet("Versionable");
    }

    public boolean isDownloadable() throws ClientException {
        if (hasFacet("Downloadable")) {
            // XXX find a better way to check size that does not depend on the
            // document schema
            Long size = (Long) getProperty("common", "size");
            if (size != null) {
                return size != 0;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> itf) {
        T facet = (T) getAdapters().get(itf);
        if (facet == null) {
            facet = findAdapter(itf);
            if (facet != null) {
                adapters.put(itf, facet);
            }
        }
        return facet;
    }

    /**
     * Lazy initialization for adapters because they don't survive the
     * serialization.
     */
    @SuppressWarnings("unchecked")
    private ArrayMap<Class, Object> getAdapters() {
        if (adapters == null) {
            adapters = new ArrayMap<Class, Object>();
        }

        return adapters;
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        T facet;

        if (!refreshCache) {
            facet = getAdapter(itf);
        } else {
            facet = findAdapter(itf);
        }

        if (facet != null) {
            getAdapters().put(itf, facet);
        }
        return facet;
    }

    @SuppressWarnings("unchecked")
    private <T> T findAdapter(Class<T> itf) {
        DocumentAdapterService svc = (DocumentAdapterService) Framework.getRuntime().getComponent(
                DocumentAdapterService.NAME);
        if (svc != null) {
            DocumentAdapterDescriptor dae = svc.getAdapterDescriptor(itf);
            if (dae != null) {
                String facet = dae.getFacet();
                if (facet == null) {
                    // if no facet is specified, accept the adapter
                    return (T) dae.getFactory().getAdapter(this, itf);
                } else if (hasFacet(facet)) {
                    return (T) dae.getFactory().getAdapter(this, itf);
                } else {
                    // TODO: throw an exception
                    log.error("Document model cannot be adapted to " + itf
                            + " because it has no facet " + facet);
                }
            }
        } else {
            log.warn("DocumentAdapterService not available. Cannot get document model adaptor for "
                    + itf);
        }
        return null;
    }

    public boolean followTransition(String transition) throws ClientException {
        CoreSession session = getCoreSession();
        boolean res = false;
        if (session != null) {
            res = session.followTransition(ref, transition);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                res = tmpSession.followTransition(ref, transition);
            }
            finally {
                if (tmpSession!=null) {
                    try {
                        tmpSession.save();
                    }
                    finally {
                        CoreInstance.getInstance().close(tmpSession);
                    }
                }
            }
        }
        // Invalidate the prefetched value in this case.
        if (res) {
            currentLifeCycleState = null;
        }
        return res;
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        Collection<String> allowedStateTransitions = new ArrayList<String>();
        CoreSession session = getCoreSession();
        if (session != null) {
            allowedStateTransitions = session.getAllowedStateTransitions(ref);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                allowedStateTransitions = tmpSession.getAllowedStateTransitions(ref);
            }
            finally {
                if (tmpSession!=null) {
                    CoreInstance.getInstance().close(tmpSession);
                }
            }
        }
        return allowedStateTransitions;
    }

    public String getCurrentLifeCycleState() throws ClientException {
        if (currentLifeCycleState != null) {
            return currentLifeCycleState;
        }
        // document was just created => not life cycle yet
        if (sid == null) {
            return null;
        }
        // String currentLifeCycleState = null;
        CoreSession session = getCoreSession();
        if (session != null) {
            currentLifeCycleState = session.getCurrentLifeCycleState(ref);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                currentLifeCycleState = tmpSession.getCurrentLifeCycleState(ref);
            }
            finally {
                if (tmpSession!=null) {
                    CoreInstance.getInstance().close(tmpSession);
                }
            }
        }
        return currentLifeCycleState;
    }

    public String getLifeCyclePolicy() throws ClientException {
        if (lifeCyclePolicy != null) {
            return lifeCyclePolicy;
        }
        // String lifeCyclePolicy = null;
        CoreSession session = getCoreSession();
        if (session != null) {
            lifeCyclePolicy = session.getLifeCyclePolicy(ref);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                lifeCyclePolicy = tmpSession.getLifeCyclePolicy(ref);
            }
            finally {
                if (tmpSession!=null) {
                    CoreInstance.getInstance().close(tmpSession);
                }
            }
        }
        return lifeCyclePolicy;
    }

    public boolean isVersion() {
        return (flags & F_VERSION) != 0;
    }

    public boolean isProxy() {
        return (flags & F_PROXY) != 0;
    }

    public void setIsVersion(boolean isVersion) {
        if (isVersion) {
            flags |= F_VERSION;
        } else {
            flags &= ~F_VERSION;
        }
    }

    public void setIsProxy(boolean isProxy) {
        if (isProxy) {
            flags |= F_PROXY;
        } else {
            flags &= ~F_PROXY;
        }
    }

    public ScopedMap getContextData() {
        return contextData;
    }

    public Serializable getContextData(ScopeType scope, String key) {
        return contextData.getScopedValue(scope, key);
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        contextData.putScopedValue(scope, key, value);
    }

    public Serializable getContextData(String key) {
        return contextData.getScopedValue(key);
    }

    public void putContextData(String key, Serializable value) {
        contextData.putScopedValue(key, value);
    }

    public void copyContextData(DocumentModel otherDocument) {
        ScopedMap otherMap = otherDocument.getContextData();
        if (otherMap != null) {
            contextData.putAll(otherMap);
        }
    }

    public void copyContent(DocumentModel sourceDoc) throws ClientException {
        String[] schemas = sourceDoc.getDeclaredSchemas();
        declaredSchemas = schemas == null ? null : schemas.clone();
        Set<String> facets = sourceDoc.getDeclaredFacets();
        declaredFacets = facets == null ? null : new HashSet<String>(facets);

        DataModelMap newDataModels = new DataModelMapImpl();
        for (String key : sourceDoc.getDocumentType().getSchemaNames()) {
            DataModel oldDM = sourceDoc.getDataModel(key);
            DataModel newDM = cloneDataModel(oldDM);
            newDataModels.put(key, newDM);
        }
        dataModels = newDataModels;
    }

    public static Object cloneField(Field field, String key, Object value) {
        // key is unused
        Object clone;
        Type type = field.getType();
        if (type.isSimpleType()) {
            // CLONE TODO
            if (value instanceof Calendar) {
                Calendar newValue = (Calendar) value;
                clone = newValue.clone();
            } else {
                clone = value;
            }
        } else if (type.isListType()) {
            ListType ltype = (ListType) type;
            Field lfield = ltype.getField();
            Type ftype = lfield.getType();
            List<Object> list;
            if (value instanceof Object[]) { // these are stored as arrays
                list = Arrays.asList((Object[]) value);
            } else {
                list = (List<Object>) value;
            }
            if (ftype.isComplexType()) {
                List<Object> clonedList = new ArrayList<Object>(list.size());
                for (Object o : list) {
                    clonedList.add(cloneField(lfield, null, o));
                }
                clone = clonedList;
            } else {
                Class<?> klass = JavaTypes.getClass(ftype);
                if (klass.isPrimitive()) {
                    clone = PrimitiveArrays.toPrimitiveArray(list, klass);
                } else {
                    clone = list.toArray((Object[]) Array.newInstance(klass,
                            list.size()));
                }
            }
        } else { // complex type
            ComplexType ctype = (ComplexType) type;
            if (ctype.getName().equals(TypeConstants.CONTENT)) { // if a blob
                Blob blob = (Blob) value; // TODO
                clone = blob;
            } else { // a map
                Map<String, Object> map = (Map<String, Object>) value;
                Map<String, Object> clonedMap = new HashMap<String, Object>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object v = entry.getValue();
                    String k = entry.getKey();
                    if (v == null) {
                        continue;
                    }
                    clonedMap.put(k, cloneField(ctype.getField(k), k, v));
                }
                clone = clonedMap;
            }
        }
        return clone;
    }

    public static DataModel cloneDataModel(Schema schema, DataModel data) {
        DataModel dm = new DataModelImpl(schema.getName());
        for (Field field : schema.getFields()) {
            String key = field.getName().getLocalName();
            Object value;
            try {
                value = data.getData(key);
            } catch (PropertyException e1) {
                continue;
            }
            if (value == null) {
                continue;
            }
            Object clone = cloneField(field, key, value);
            try {
                dm.setData(key, clone);
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
        }
        return dm;
    }

    public DataModel cloneDataModel(DataModel data) {
        SchemaManager mgr = TypeService.getSchemaManager();
        return cloneDataModel(mgr.getSchema(data.getSchema()), data);
    }

    public String getCacheKey() throws ClientException {
        // UUID - sessionId
        String key = id + '-' + sid + '-' + getPathAsString();
        // :FIXME: Assume a dublin core schema => enough for us right now.
        Calendar timeStamp = (Calendar) getProperty("dublincore", "modified");

        if (timeStamp != null) {
            key += '-' + String.valueOf(timeStamp.getTimeInMillis());
        }
        return key;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getVersionLabel() {
        return (String) contextData.getScopedValue("version.label");
    }

    public boolean isSchemaLoaded(String name) {
        return dataModels.containsKey(name);
    }

    // TODO: id is schema.field and not prefix:field
    public void prefetchProperty(String id, Object value) {
        if (prefetch == null) {
            prefetch = new HashMap<String, Serializable>();
        }
        Serializable sValue = (Serializable) value;
        prefetch.put(id, value == null ? Null.VALUE : sValue);
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        currentLifeCycleState = lifecycle;
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        this.lifeCyclePolicy = lifeCyclePolicy;
    }

    public void setFlags(long flags) {
        this.flags |= flags;
    }

    public void clearFlags(long flags) {
        this.flags &= ~flags;
    }

    public void clearFlags() {
        flags = 0L;
    }

    public long getFlags() {
        return flags;
    }

    public boolean hasFlags(long flags) {
        return (this.flags & flags) == flags;
    }

    @Override
    // need this for tree in RCP clients
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DocumentModelImpl) {
            DocumentModel documentModel = (DocumentModel) obj;
            String id = documentModel.getId();
            if (id != null) {
                return id.equals(this.id);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(DocumentModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" -title: ");
        try {
            buf.append(getProperty("dublincore", "title"));
        } catch (ClientException e) {
            buf.append("ERROR GETTING THE TITLE: " + e);
        }
        buf.append(", sessionId: ");
        buf.append(sid);
        buf.append(", doc id: ");
        buf.append(id);
        buf.append(", name: ");
        buf.append(getName());
        buf.append(", path: ");
        buf.append(path);
        buf.append(", ref: ");
        buf.append(ref);
        buf.append(", parent ref: ");
        buf.append(getParentRef());
        buf.append(", data models: ");
        buf.append(dataModels);
        buf.append(", declaredFacets: ");
        buf.append(declaredFacets);
        buf.append(", declaredSchemas: ");
        buf.append(declaredSchemas);
        buf.append('}');

        return buf.toString();
    }

    public Map<String, Serializable> getPrefetch() {
        return prefetch;
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException {

        CoreSession session = getCoreSession();

        if (session!=null) {
            return session.getDocumentSystemProp(ref, systemProperty, type);
        } else {
            CoreSession tmpSession = getTempCoreSession();
            try {
                return tmpSession.getDocumentSystemProp(ref, systemProperty, type);
            }
            finally {
                if (tmpSession!=null) {
                    CoreInstance.getInstance().close(tmpSession);
                }
            }
        }
    }

    public boolean isLifeCycleLoaded() {
        return currentLifeCycleState != null;
    }

    public DocumentPart getPart(String schema) throws ClientException {
        DataModel dm = getDataModel(schema);
        if (dm != null) {
            return ((DataModelImpl) dm).getDocumentPart();
        }
        return null; // TODO thrown an exception?
    }

    public DocumentPart[] getParts() throws ClientException {
        DocumentType type;
        try {
            type = Framework.getService(SchemaManager.class).getDocumentType(
                    getType());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Collection<Schema> schemas = type.getSchemas();
        int size = schemas.size();
        DocumentPart[] parts = new DocumentPart[size];
        int i = 0;
        for (Schema schema : schemas) {
            DataModel dm = getDataModel(schema.getName());
            parts[i++] = ((DataModelImpl) dm).getDocumentPart();
        }
        return parts;
    }

    public Property getProperty(String xpath) throws ClientException {
        Path path = new Path(xpath);
        if (path.segmentCount() == 0) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String segment = path.segment(0);
        int p = segment.indexOf(':');
        if (p == -1) { // support also other schema paths? like schema.property
            // allow also unprefixed schemas -> make a search for the first
            // matching schema having a property with same name as path segment
            // 0
            DocumentPart[] parts = getParts();
            for (DocumentPart part : parts) {
                if (part.getSchema().hasField(segment)) {
                    return part.resolvePath(path.toString());
                }
            }
            // could not find any matching schema
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String prefix = segment.substring(0, p);
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        Schema schema = mgr.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = mgr.getSchema(prefix);
            if (schema == null) {
                throw new PropertyNotFoundException(xpath,
                        "Could not find registered schema with prefix: "
                                + prefix);
            }
            // workaround for a schema prefix bug -> XPATH lookups in
            // DocumentPart must use prefixed
            // names for schema with prefixes and non prefixed names for the
            // rest o schemas.
            // Until then we used the name as the prefix but we must remove it
            // since it is not a valid prefix:
            // NXP-1913
            String[] segments = path.segments();
            segments[0] = segments[0].substring(p + 1);
            path = Path.createFromSegments(segments);
        }

        DocumentPart part = getPart(schema.getName());
        if (part == null) {
            throw new PropertyNotFoundException(
                    xpath,
                    String.format(
                            "Document '%s' with title '%s' and type '%s' does not have any schema with prefix '%s'",
                            getRef(), getTitle(), getType(), prefix));
        }
        return part.resolvePath(path.toString());
    }

    public Serializable getPropertyValue(String path) throws PropertyException, ClientException {
        return getProperty(path).getValue();
    }

    public void setPropertyValue(String path, Serializable value)
            throws PropertyException, ClientException {
        getProperty(path).setValue(value);
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        DocumentModelImpl dm = (DocumentModelImpl) super.clone();
        // dm.id =id;
        // dm.acp = acp;
        // dm.currentLifeCycleState = currentLifeCycleState;
        // dm.lifeCyclePolicy = lifeCyclePolicy;
        // dm.declaredSchemas = declaredSchemas; // schemas are immutable so we
        // don't clone the array
        // dm.flags = flags;
        // dm.repositoryName = repositoryName;
        // dm.ref = ref;
        // dm.parentRef = parentRef;
        // dm.path = path; // path is immutable
        // dm.isACPLoaded = isACPLoaded;
        // dm.prefetch = dm.prefetch; // prefetch can be shared
        // dm.lock = lock;
        // dm.sourceId =sourceId;
        // dm.sid = sid;
        // dm.type = type;
        dm.declaredFacets = new HashSet<String>(declaredFacets); // facets
        // should be
        // clones too -
        // they are not
        // immutable
        // context data is keeping contextual info so it is reseted
        dm.contextData = new ScopedMap();

        // copy parts
        dm.dataModels = new DataModelMapImpl();
        for (Map.Entry<String, DataModel> entry : dataModels.entrySet()) {
            String key = entry.getKey();
            DataModel data = entry.getValue();
            DataModelImpl newData;
            try {
                newData = new DataModelImpl(key, data.getMap());
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
            dm.dataModels.put(key, newData);
        }
        return dm;
    }

    public void reset() {
        if (dataModels != null) {
            dataModels.clear();
        }
        if (prefetch != null) {
            prefetch.clear();
        }
        isACPLoaded = false;
        acp = null;
        currentLifeCycleState = null;
        lifeCyclePolicy = null;
    }

    public void refresh() throws ClientException {
        refresh(REFRESH_DEFAULT, null);
    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        if ((refreshFlags & REFRESH_ACP_IF_LOADED) != 0 && isACPLoaded) {
            refreshFlags |= REFRESH_ACP;
            // we must not clean the REFRESH_ACP_IF_LOADED flag since it is used
            // below on the client
        }

        if ((refreshFlags & REFRESH_CONTENT_IF_LOADED) != 0) {
            refreshFlags |= REFRESH_CONTENT;
            Collection<String> keys = dataModels.keySet();
            schemas = keys.toArray(new String[keys.size()]);
        }

        Object[] result = getCoreSession().refreshDocument(ref, refreshFlags,
                schemas);

        if ((refreshFlags & REFRESH_PREFETCH) != 0) {
            prefetch = (HashMap<String, Serializable>) result[0];
        }
        if ((refreshFlags & REFRESH_LOCK) != 0) {
            lock = (String) result[1];
        }
        if ((refreshFlags & REFRESH_LIFE_CYCLE) != 0) {
            currentLifeCycleState = (String) result[2];
            lifeCyclePolicy = (String) result[3];
        }
        acp = null;
        isACPLoaded = false;
        if ((refreshFlags & REFRESH_ACP) != 0) {
            acp = (ACP) result[4];
            isACPLoaded = true;
        }
        dataModels.clear();
        if ((refreshFlags & REFRESH_CONTENT) != 0) {
            DocumentPart[] parts = (DocumentPart[]) result[5];
            if (parts != null) {
                for (DocumentPart part : parts) {
                    DataModelImpl dm = new DataModelImpl(part);
                    dataModels.put(dm.getSchema(), dm);
                }
            }
        }
    }

}
