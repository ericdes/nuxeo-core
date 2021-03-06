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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.sql.CollectionFragment.CollectionMaker;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.IdGenPolicy;
import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * The {@link Model} is the link between high-level types and SQL-level objects
 * (entity tables, collections). It defines all policies relating to the choice
 * of structure (what schema are grouped together in for optimization) and names
 * in the SQL database (table names, column names), and to what entity names
 * (type name, field name) they correspond.
 * <p>
 * A Nuxeo schema or type is mapped to a SQL-level table. Several types can be
 * aggregated in the same table. In theory, a type could even be split into
 * different tables.
 *
 * @author Florent Guillaume
 */
public class Model {

    private static final Log log = LogFactory.getLog(Model.class);

    public static final String ROOT_TYPE = "Root";

    public static final String REPOINFO_TABLE_NAME = "repositories";

    public static final String REPOINFO_REPONAME_KEY = "name";

    public static final String MAIN_KEY = "id";

    public final String hierTableName;

    public final String mainTableName;

    public static final String CLUSTER_NODES_TABLE_NAME = "cluster_nodes";

    public static final String CLUSTER_NODES_NODEID_KEY = "nodeid";

    public static final String CLUSTER_NODES_CREATED_KEY = "created";

    public static final String CLUSTER_INVALS_TABLE_NAME = "cluster_invals";

    public static final String CLUSTER_INVALS_NODEID_KEY = "nodeid";

    public static final String CLUSTER_INVALS_ID_KEY = "id";

    public static final String CLUSTER_INVALS_FRAGMENTS_KEY = "fragments";

    public static final String CLUSTER_INVALS_KIND_KEY = "kind";

    public static final String MAIN_TABLE_NAME = "types";

    public static final String MAIN_PRIMARY_TYPE_PROP = "ecm:primaryType";

    public static final String MAIN_PRIMARY_TYPE_KEY = "primarytype";

    public static final String MAIN_BASE_VERSION_PROP = "ecm:baseVersion";

    public static final String MAIN_BASE_VERSION_KEY = "baseversionid";

    public static final String MAIN_CHECKED_IN_PROP = "ecm:isCheckedIn";

    public static final String MAIN_CHECKED_IN_KEY = "ischeckedin";

    public static final String MAIN_MAJOR_VERSION_PROP = "ecm:majorVersion";

    public static final String MAIN_MAJOR_VERSION_KEY = "majorversion";

    public static final String MAIN_MINOR_VERSION_PROP = "ecm:minorVersion";

    public static final String MAIN_MINOR_VERSION_KEY = "minorversion";

    public static final String UID_SCHEMA_NAME = "uid";

    public static final String UID_MAJOR_VERSION_KEY = "major_version";

    public static final String UID_MINOR_VERSION_KEY = "minor_version";

    public static final String HIER_TABLE_NAME = "hierarchy";

    public static final String HIER_PARENT_KEY = "parentid";

    public static final String HIER_CHILD_NAME_KEY = "name";

    public static final String HIER_CHILD_POS_KEY = "pos";

    public static final String HIER_CHILD_ISPROPERTY_KEY = "isproperty";

    public static final String COLL_TABLE_POS_KEY = "pos";

    public static final String COLL_TABLE_VALUE_KEY = "item";

    public static final String MISC_TABLE_NAME = "misc";

    public static final String MISC_LIFECYCLE_POLICY_PROP = "ecm:lifeCyclePolicy";

    public static final String MISC_LIFECYCLE_POLICY_KEY = "lifecyclepolicy";

    public static final String MISC_LIFECYCLE_STATE_PROP = "ecm:lifeCycleState";

    public static final String MISC_LIFECYCLE_STATE_KEY = "lifecyclestate";

    public static final String MISC_DIRTY_PROP = "ecm:dirty";

    public static final String MISC_DIRTY_KEY = "dirty";

    public static final String MISC_WF_IN_PROGRESS_PROP = "ecm:wfInProgress";

    public static final String MISC_WF_IN_PROGRESS_KEY = "wfinprogress";

    public static final String MISC_WF_INC_OPTION_PROP = "ecm:wfIncOption";

    public static final String MISC_WF_INC_OPTION_KEY = "wfincoption";

    public static final String ACL_TABLE_NAME = "acls";

    public static final String ACL_PROP = "ecm:acl";

    public static final String ACL_POS_KEY = "pos";

    public static final String ACL_NAME_KEY = "name";

    public static final String ACL_GRANT_KEY = "grant";

    public static final String ACL_PERMISSION_KEY = "permission";

    public static final String ACL_USER_KEY = "user";

    public static final String ACL_GROUP_KEY = "group";

    public static final String VERSION_TABLE_NAME = "versions";

    public static final String VERSION_VERSIONABLE_PROP = "ecm:versionableId";

    public static final String VERSION_VERSIONABLE_KEY = "versionableid";

    public static final String VERSION_CREATED_PROP = "ecm:versionCreated";

    public static final String VERSION_CREATED_KEY = "created";

    public static final String VERSION_LABEL_PROP = "ecm:versionLabel";

    public static final String VERSION_LABEL_KEY = "label";

    public static final String VERSION_DESCRIPTION_PROP = "ecm:versionDescription";

    public static final String VERSION_DESCRIPTION_KEY = "description";

    public static final String PROXY_TYPE = "ecm:proxy";

    public static final String PROXY_TABLE_NAME = "proxies";

    public static final String PROXY_TARGET_PROP = "ecm:proxyTargetId";

    public static final String PROXY_TARGET_KEY = "targetid";

    public static final String PROXY_VERSIONABLE_PROP = "ecm:proxyVersionableId";

    public static final String PROXY_VERSIONABLE_KEY = "versionableid";

    public static final String LOCK_TABLE_NAME = "locks";

    public static final String LOCK_PROP = "ecm:lock";

    public static final String LOCK_KEY = "lock";

    public static final String FULLTEXT_TABLE_NAME = "fulltext";

    public static final String FULLTEXT_FULLTEXT_PROP = "ecm:fulltext";

    public static final String FULLTEXT_FULLTEXT_KEY = "fulltext";

    public static final String FULLTEXT_SIMPLETEXT_PROP = "ecm:simpleText";

    public static final String FULLTEXT_SIMPLETEXT_KEY = "simpletext";

    public static final String FULLTEXT_BINARYTEXT_PROP = "ecm:binaryText";

    public static final String FULLTEXT_BINARYTEXT_KEY = "binarytext";

    /** Special (non-schema-based) simple fragments present in all types. */
    public static final String[] COMMON_SIMPLE_FRAGMENTS = { MISC_TABLE_NAME,
            FULLTEXT_TABLE_NAME };

    /** Special (non-schema-based) collection fragments present in all types. */
    public static final String[] COMMON_COLLECTION_FRAGMENTS = { ACL_TABLE_NAME };

    public static class PropertyInfo {

        public final PropertyType propertyType;

        public final String fragmentName;

        public final String fragmentKey;

        public final boolean readonly;

        public final boolean fulltext;

        public PropertyInfo(PropertyType propertyType, String fragmentName,
                String fragmentKey, boolean readonly) {
            this.propertyType = propertyType;
            this.fragmentName = fragmentName;
            this.fragmentKey = fragmentKey;
            this.readonly = readonly;
            // TODO use some config to decide this
            fulltext = (propertyType.equals(PropertyType.STRING)
                    || propertyType.equals(PropertyType.BINARY) || propertyType.equals(PropertyType.ARRAY_STRING))
                    && (fragmentKey == null || !fragmentKey.equals(MAIN_KEY))
                    && !fragmentName.equals(HIER_TABLE_NAME)
                    && !fragmentName.equals(MAIN_TABLE_NAME)
                    && !fragmentName.equals(VERSION_TABLE_NAME)
                    && !fragmentName.equals(PROXY_TABLE_NAME)
                    && !fragmentName.equals(FULLTEXT_TABLE_NAME)
                    && !fragmentName.equals(LOCK_TABLE_NAME)
                    && !fragmentName.equals(UID_SCHEMA_NAME)
                    && !fragmentName.equals(MISC_TABLE_NAME);
        }

        @Override
        public String toString() {
            return "PropertyInfo(" + fragmentName + ", " + fragmentKey + ", "
                    + propertyType + (readonly ? ", RO" : "")
                    + (fulltext ? ", FT" : "") + ')';
        }
    }

    private final BinaryManager binaryManager;

    protected final RepositoryDescriptor repositoryDescriptor;

    /** The id generation policy. */
    public final IdGenPolicy idGenPolicy;

    /** Is the hierarchy table separate from the main table. */
    protected final boolean separateMainTable;

    private final AtomicLong temporaryIdCounter;

    /** Shared high-level properties that don't come from the schema manager. */
    private final Map<String, Type> specialPropertyTypes;

    /** Per-schema/type info about properties. */
    private final HashMap<String, Map<String, PropertyInfo>> schemaPropertyInfos;

    /** Shared properties. */
    private final Map<String, PropertyInfo> sharedPropertyInfos;

    /** Merged properties (all schemas together + shared). */
    private final Map<String, PropertyInfo> mergedPropertyInfos;

    private final Map<String, Map<String, PropertyInfo>> fulltextStringPropertyInfos;

    private final Map<String, Map<String, PropertyInfo>> fulltextBinaryPropertyInfos;

    /** Per-table info about properties. */
    private final Map<String, Map<String, PropertyType>> fragmentsKeys;

    /** Maps collection table names to their type. */
    private final Map<String, PropertyType> collectionTables;

    /** The factories to build collection fragments. */
    private final Map<String, CollectionMaker> collectionMakers;

    /** Column ordering for collections. */
    private final Map<String, String> collectionOrderBy;

    /**
     * The fragment for each schema, or {@code null} if the schema doesn't have
     * a fragment.
     */
    private final Map<String, String> schemaFragment;

    /** Maps document type or schema to simple fragments. */
    protected final Map<String, Set<String>> typeSimpleFragments;

    /** Maps schema to collection fragments. */
    protected final Map<String, Set<String>> typeCollectionFragments;

    /** Maps schema to simple+collection fragments. */
    protected final Map<String, Set<String>> typeFragments;

    /** Map of doc types to facets, for search. */
    protected final Map<String, Set<String>> documentTypesFacets;

    /** Map of doc type to its supertype, for search. */
    protected final Map<String, String> documentSuperTypes;

    /** Map of doc type to its subtypes (including itself), for search. */
    protected final Map<String, Set<String>> documentSubTypes;

    public Model(RepositoryImpl repository, SchemaManager schemaManager) {
        binaryManager = repository.getBinaryManager();
        repositoryDescriptor = repository.getRepositoryDescriptor();
        idGenPolicy = repositoryDescriptor.idGenPolicy;
        separateMainTable = repositoryDescriptor.separateMainTable;
        temporaryIdCounter = new AtomicLong(0);
        hierTableName = HIER_TABLE_NAME;
        mainTableName = separateMainTable ? MAIN_TABLE_NAME : HIER_TABLE_NAME;

        schemaPropertyInfos = new HashMap<String, Map<String, PropertyInfo>>();
        sharedPropertyInfos = new HashMap<String, PropertyInfo>();
        mergedPropertyInfos = new HashMap<String, PropertyInfo>();
        fulltextStringPropertyInfos = new HashMap<String, Map<String, PropertyInfo>>();
        fulltextBinaryPropertyInfos = new HashMap<String, Map<String, PropertyInfo>>();
        fragmentsKeys = new HashMap<String, Map<String, PropertyType>>();

        collectionTables = new HashMap<String, PropertyType>();
        collectionOrderBy = new HashMap<String, String>();
        collectionMakers = new HashMap<String, CollectionMaker>();

        schemaFragment = new HashMap<String, String>();
        typeFragments = new HashMap<String, Set<String>>();
        typeSimpleFragments = new HashMap<String, Set<String>>();
        typeCollectionFragments = new HashMap<String, Set<String>>();

        documentTypesFacets = new HashMap<String, Set<String>>();
        documentSuperTypes = new HashMap<String, String>();
        documentSubTypes = new HashMap<String, Set<String>>();

        specialPropertyTypes = new HashMap<String, Type>();

        initMainModel();
        initVersionsModel();
        initProxiesModel();
        initLocksModel();
        initAclModel();
        initMiscModel();
        initFullTextModel();
        initModels(schemaManager);

        inferFulltextInfo();
    }

    /**
     * Gets the repository descriptor used for this model.
     *
     * @return the repository descriptor
     */
    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    /**
     * Gets a binary given its digest.
     *
     * @param digest the digest
     * @return the binary for this digest, or {@code null} if unavailable
     *         (error)
     */
    public Binary getBinary(String digest) {
        return binaryManager.getBinary(digest);
    }

    /**
     * Computes a new unique id.
     * <p>
     * If actual ids are computed by the database, this will be a temporary id,
     * otherwise the final one.
     *
     * @return a new id, which may be temporary
     */
    public Serializable generateNewId() {
        switch (idGenPolicy) {
        case APP_UUID:
            return UUID.randomUUID().toString();
            // return "UUID_" + temporaryIdCounter.incrementAndGet();
        case DB_IDENTITY:
            return "T" + temporaryIdCounter.incrementAndGet();
        default:
            throw new AssertionError(idGenPolicy);
        }
    }

    /**
     * Fixup an id that has been turned into a string for high-level Nuxeo APIs.
     *
     * @param id the id to fixup
     * @return the fixed up id
     */
    public Serializable unHackStringId(String id) {
        switch (idGenPolicy) {
        case APP_UUID:
            return id;
        case DB_IDENTITY:
            if (id.startsWith("T")) {
                return id;
            }
            /*
             * Document ids coming from higher level have been turned into
             * strings (by SQLDocument.getUUID) but are really longs for the
             * backend.
             */
            return Long.valueOf(id);
        default:
            throw new AssertionError(idGenPolicy);
        }
    }

    /**
     * Records info about one property, in a schema-based structure and in a
     * table-based structure.
     * <p>
     * If {@literal schemaName} is {@code null}, then the property applies to
     * all types (system properties).
     */
    private void addPropertyInfo(String schemaName, String propertyName,
            PropertyType propertyType, String fragmentName, String fragmentKey,
            boolean readonly, Type type) {
        // per-type
        Map<String, PropertyInfo> propertyInfos;
        if (schemaName == null) {
            propertyInfos = sharedPropertyInfos;
        } else {
            propertyInfos = schemaPropertyInfos.get(schemaName);
            if (propertyInfos == null) {
                propertyInfos = new HashMap<String, PropertyInfo>();
                schemaPropertyInfos.put(schemaName, propertyInfos);
            }
        }
        PropertyInfo propertyInfo = new PropertyInfo(propertyType,
                fragmentName, fragmentKey, readonly);
        propertyInfos.put(propertyName, propertyInfo);

        // per-table
        if (fragmentKey != null) {
            Map<String, PropertyType> fragmentKeys = fragmentsKeys.get(fragmentName);
            if (fragmentKeys == null) {
                fragmentKeys = new LinkedHashMap<String, PropertyType>();
                fragmentsKeys.put(fragmentName, fragmentKeys);
            }
            fragmentKeys.put(fragmentKey, propertyType);
        }

        // system properties
        if (type != null) {
            specialPropertyTypes.put(propertyName, type);
        }

        // merged properties
        PropertyInfo previous = mergedPropertyInfos.get(propertyName);
        if (previous == null) {
            mergedPropertyInfos.put(propertyName, propertyInfo);
        } else {
            log.info(String.format(
                    "Schemas '%s' and '%s' both have a property '%s', "
                            + "unqualified reference in queries will use schema '%1$s'",
                    previous.fragmentName, fragmentName, propertyName));
        }
        // compatibility for use of schema name as prefix
        if (!propertyName.contains(":")) {
            // allow schema name as prefix
            propertyName = schemaName + ':' + propertyName;
            previous = mergedPropertyInfos.get(propertyName);
            if (previous == null) {
                mergedPropertyInfos.put(propertyName, propertyInfo);
            }
        }
    }

    /**
     * Infers type property information from all its schemas.
     */
    private void inferTypePropertyInfos(String typeName, String[] schemaNames) {
        Map<String, PropertyInfo> propertyInfos;
        propertyInfos = schemaPropertyInfos.get(typeName);
        if (propertyInfos == null) {
            propertyInfos = new HashMap<String, PropertyInfo>();
            schemaPropertyInfos.put(typeName, propertyInfos);
        }
        for (String schemaName : schemaNames) {
            Map<String, PropertyInfo> infos = schemaPropertyInfos.get(schemaName);
            if (infos == null) {
                // schema with no properties (complex list)
                continue;
            }
            for (Entry<String, PropertyInfo> info : infos.entrySet()) {
                propertyInfos.put(info.getKey(), info.getValue());
            }
        }
    }

    /**
     * Infers fulltext info for all schemas.
     */
    private void inferFulltextInfo() {
        for (Entry<String, Map<String, PropertyInfo>> entry : schemaPropertyInfos.entrySet()) {
            Map<String, PropertyInfo> stringPropertyInfos = new HashMap<String, PropertyInfo>();
            Map<String, PropertyInfo> binaryPropertyInfos = new HashMap<String, PropertyInfo>();
            for (Entry<String, PropertyInfo> e : entry.getValue().entrySet()) {
                String name = e.getKey();
                PropertyInfo info = e.getValue();
                if (!info.fulltext) {
                    continue;
                }
                if (info.propertyType == PropertyType.STRING
                        || info.propertyType == PropertyType.ARRAY_STRING) {
                    stringPropertyInfos.put(name, info);
                } else if (info.propertyType == PropertyType.BINARY) {
                    binaryPropertyInfos.put(name, info);
                }
            }
            String schemaName = entry.getKey();
            fulltextStringPropertyInfos.put(schemaName, stringPropertyInfos);
            fulltextBinaryPropertyInfos.put(schemaName, binaryPropertyInfos);
        }
    }

    public PropertyInfo getPropertyInfo(String schemaName, String propertyName) {
        Map<String, PropertyInfo> propertyInfos = schemaPropertyInfos.get(schemaName);
        if (propertyInfos == null) {
            // no such schema
            return null;
        }
        PropertyInfo propertyInfo = propertyInfos.get(propertyName);
        return propertyInfo != null ? propertyInfo
                : sharedPropertyInfos.get(propertyName);
    }

    public Map<String, PropertyInfo> getPropertyInfos(String typeName) {
        return schemaPropertyInfos.get(typeName);
    }

    public PropertyInfo getPropertyInfo(String propertyName) {
        return mergedPropertyInfos.get(propertyName);
    }

    public Map<String, PropertyInfo> getFulltextStringPropertyInfos(
            String schemaName) {
        return fulltextStringPropertyInfos.get(schemaName);
    }

    public Map<String, PropertyInfo> getFulltextBinaryPropertyInfos(
            String schemaName) {
        return fulltextBinaryPropertyInfos.get(schemaName);
    }

    /**
     * Finds out if a field is to be indexed as fulltext.
     *
     * @param fragmentName
     * @param fragmentKey the key or {@code null} for a collection
     * @return {@link PropertyType#STRING} or {@link PropertyType#BINARY} if
     *         this field is to be indexed as fulltext
     */
    public PropertyType getFulltextFieldType(String fragmentName,
            String fragmentKey) {
        if (fragmentKey == null) {
            PropertyType type = collectionTables.get(fragmentName);
            if (type == PropertyType.ARRAY_STRING
                    || type == PropertyType.ARRAY_BINARY) {
                return type.getArrayBaseType();
            }
            return null;
        } else {
            Map<String, PropertyInfo> infos = schemaPropertyInfos.get(fragmentName);
            if (infos == null) {
                return null;
            }
            PropertyInfo info = infos.get(fragmentKey);
            if (info != null && info.fulltext) {
                return info.propertyType;
            }
            return null;
        }
    }

    private void addCollectionFragmentInfos(String fragmentName,
            PropertyType propertyType, CollectionMaker maker, String orderBy,
            Map<String, PropertyType> fragmentKeys) {
        collectionTables.put(fragmentName, propertyType);
        collectionMakers.put(fragmentName, maker);
        collectionOrderBy.put(fragmentName, orderBy);
        // set all keys types
        Map<String, PropertyType> old = fragmentsKeys.get(fragmentName);
        if (old == null) {
            fragmentsKeys.put(fragmentName, fragmentKeys);
        } else {
            old.putAll(fragmentKeys);
        }
    }

    public Type getSpecialPropertyType(String propertyName) {
        return specialPropertyTypes.get(propertyName);
    }

    public PropertyType getCollectionFragmentType(String fragmentName) {
        return collectionTables.get(fragmentName);
    }

    public boolean isCollectionFragment(String fragmentName) {
        return collectionTables.containsKey(fragmentName);
    }

    public String getCollectionOrderBy(String fragmentName) {
        return collectionOrderBy.get(fragmentName);
    }

    public Set<String> getFragmentNames() {
        return fragmentsKeys.keySet();
    }

    public Map<String, PropertyType> getFragmentKeysType(String fragmentName) {
        return fragmentsKeys.get(fragmentName);
    }

    /**
     * Create a collection fragment according to the factories registered.
     */
    public Serializable[] newCollectionArray(Serializable id, ResultSet rs,
            List<Column> columns, Context context) throws SQLException {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeArray(id, rs, columns, context, this);
    }

    public CollectionFragment newCollectionFragment(Serializable id,
            Serializable[] array, Context context) {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeCollection(id, array, context);
    }

    public CollectionFragment newEmptyCollectionFragment(Serializable id,
            Context context) {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeEmpty(id, context, this);
    }

    protected void addTypeSimpleFragment(String typeName, String fragmentName) {
        Set<String> fragments = typeSimpleFragments.get(typeName);
        if (fragments == null) {
            fragments = new HashSet<String>();
            typeSimpleFragments.put(typeName, fragments);
        }
        // fragmentName may be null, to just create the entry
        if (fragmentName != null) {
            fragments.add(fragmentName);
        }
        addTypeFragment(typeName, fragmentName);
    }

    protected void addTypeCollectionFragment(String typeName,
            String fragmentName) {
        Set<String> fragments = typeCollectionFragments.get(typeName);
        if (fragments == null) {
            fragments = new HashSet<String>();
            typeCollectionFragments.put(typeName, fragments);
        }
        fragments.add(fragmentName);
        addTypeFragment(typeName, fragmentName);
    }

    protected void addTypeFragment(String typeName, String fragmentName) {
        Set<String> fragments = typeFragments.get(typeName);
        if (fragments == null) {
            fragments = new HashSet<String>();
            typeFragments.put(typeName, fragments);
        }
        // fragmentName may be null, to just create the entry
        if (fragmentName != null) {
            fragments.add(fragmentName);
        }
    }

    public Set<String> getTypeSimpleFragments(String typeName) {
        return typeSimpleFragments.get(typeName);
    }

    public Set<String> getTypeFragments(String typeName) {
        return typeFragments.get(typeName);
    }

    public boolean isType(String typeName) {
        return typeFragments.containsKey(typeName);
    }

    public boolean isDocumentType(String typeName) {
        return documentTypesFacets.containsKey(typeName);
    }

    public String getDocumentSuperType(String typeName) {
        return documentSuperTypes.get(typeName);
    }

    public Set<String> getDocumentSubTypes(String typeName) {
        return documentSubTypes.get(typeName);
    }

    public Set<String> getDocumentTypeFacets(String typeName) {
        Set<String> facets = documentTypesFacets.get(typeName);
        return facets == null ? Collections.<String> emptySet() : facets;
    }

    /**
     * Given a map of id to types, returns a map of fragment names to ids.
     */
    public Map<String, Set<Serializable>> getPerFragmentIds(
            Map<Serializable, String> idType) {
        Map<String, Set<Serializable>> allFragmentIds = new HashMap<String, Set<Serializable>>();
        for (Entry<Serializable, String> e : idType.entrySet()) {
            Serializable id = e.getKey();
            String type = e.getValue();
            for (String fragmentName : getTypeFragments(type)) {
                Set<Serializable> fragmentIds = allFragmentIds.get(fragmentName);
                if (fragmentIds == null) {
                    fragmentIds = new HashSet<Serializable>();
                    allFragmentIds.put(fragmentName, fragmentIds);
                }
                fragmentIds.add(id);
            }
        }
        return allFragmentIds;
    }

    private PropertyType mainIdType() {
        switch (idGenPolicy) {
        case APP_UUID:
            return PropertyType.STRING;
        case DB_IDENTITY:
            return PropertyType.LONG;
        }
        throw new AssertionError(idGenPolicy);
    }

    /**
     * Creates all the models.
     */
    private void initModels(SchemaManager schemaManager) {
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            String typeName = documentType.getName();
            addTypeSimpleFragment(typeName, null); // create entry
            for (Schema schema : documentType.getSchemas()) {
                String fragmentName = initTypeModel(schema);
                addTypeSimpleFragment(typeName, fragmentName); // may be null
                // collection fragments too for this schema
                Set<String> cols = typeCollectionFragments.get(schema.getName());
                if (cols != null) {
                    for (String colFrag : cols) {
                        addTypeCollectionFragment(typeName, colFrag);
                    }
                }
            }
            inferTypePropertyInfos(typeName, documentType.getSchemaNames());
            for (String fragmentName : COMMON_SIMPLE_FRAGMENTS) {
                addTypeSimpleFragment(typeName, fragmentName);
            }
            for (String fragmentName : COMMON_COLLECTION_FRAGMENTS) {
                addTypeCollectionFragment(typeName, fragmentName);
            }
            log.debug("Fragments for " + typeName + ": "
                    + getTypeFragments(typeName));

            // record doc type and facets, super type, sub types
            documentTypesFacets.put(typeName, new HashSet<String>(
                    documentType.getFacets()));
            Type superType = documentType.getSuperType();
            if (superType != null) {
                String superTypeName = superType.getName();
                documentSuperTypes.put(typeName, superTypeName);
            }
        }
        // compute subtypes for all types
        for (String type : documentTypesFacets.keySet()) {
            String superType = type;
            do {
                Set<String> subTypes = documentSubTypes.get(superType);
                if (subTypes == null) {
                    subTypes = new HashSet<String>();
                    documentSubTypes.put(superType, subTypes);
                }
                subTypes.add(type);
                superType = documentSuperTypes.get(superType);
            } while (superType != null);
        }
    }

    /**
     * Special model for the main table (the one containing the primary type
     * information).
     * <p>
     * If the main table is not separate from the hierarchy table, then it's
     * will not really be instantiated by itself but merged into the hierarchy
     * table.
     */
    private void initMainModel() {
        addPropertyInfo(null, MAIN_PRIMARY_TYPE_PROP, PropertyType.STRING,
                mainTableName, MAIN_PRIMARY_TYPE_KEY, true, null);
        addPropertyInfo(null, MAIN_CHECKED_IN_PROP, PropertyType.BOOLEAN,
                mainTableName, MAIN_CHECKED_IN_KEY, false, BooleanType.INSTANCE);
        addPropertyInfo(null, MAIN_BASE_VERSION_PROP, mainIdType(),
                mainTableName, MAIN_BASE_VERSION_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, MAIN_MAJOR_VERSION_PROP, PropertyType.LONG,
                mainTableName, MAIN_MAJOR_VERSION_KEY, false, LongType.INSTANCE);
        addPropertyInfo(null, MAIN_MINOR_VERSION_PROP, PropertyType.LONG,
                mainTableName, MAIN_MINOR_VERSION_KEY, false, LongType.INSTANCE);
    }

    /**
     * Special model for the "misc" table (lifecycle, dirty.).
     */
    private void initMiscModel() {
        addPropertyInfo(null, MISC_LIFECYCLE_POLICY_PROP, PropertyType.STRING,
                MISC_TABLE_NAME, MISC_LIFECYCLE_POLICY_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, MISC_LIFECYCLE_STATE_PROP, PropertyType.STRING,
                MISC_TABLE_NAME, MISC_LIFECYCLE_STATE_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, MISC_DIRTY_PROP, PropertyType.BOOLEAN,
                MISC_TABLE_NAME, MISC_DIRTY_KEY, false, BooleanType.INSTANCE);
        addPropertyInfo(null, MISC_WF_IN_PROGRESS_PROP, PropertyType.BOOLEAN,
                MISC_TABLE_NAME, MISC_WF_IN_PROGRESS_KEY, false,
                BooleanType.INSTANCE);
        addPropertyInfo(null, MISC_WF_INC_OPTION_PROP, PropertyType.STRING,
                MISC_TABLE_NAME, MISC_WF_INC_OPTION_KEY, false,
                StringType.INSTANCE);
    }

    /**
     * Special model for the versions table.
     */
    private void initVersionsModel() {
        addPropertyInfo(null, VERSION_VERSIONABLE_PROP, mainIdType(),
                VERSION_TABLE_NAME, VERSION_VERSIONABLE_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, VERSION_CREATED_PROP, PropertyType.DATETIME,
                VERSION_TABLE_NAME, VERSION_CREATED_KEY, false,
                DateType.INSTANCE);
        addPropertyInfo(null, VERSION_LABEL_PROP, PropertyType.STRING,
                VERSION_TABLE_NAME, VERSION_LABEL_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, VERSION_DESCRIPTION_PROP, PropertyType.STRING,
                VERSION_TABLE_NAME, VERSION_DESCRIPTION_KEY, false,
                StringType.INSTANCE);
    }

    /**
     * Special model for the proxies table.
     */
    private void initProxiesModel() {
        addPropertyInfo(PROXY_TYPE, PROXY_TARGET_PROP, mainIdType(),
                PROXY_TABLE_NAME, PROXY_TARGET_KEY, false, null);
        addPropertyInfo(PROXY_TYPE, PROXY_VERSIONABLE_PROP, mainIdType(),
                PROXY_TABLE_NAME, PROXY_VERSIONABLE_KEY, false, null);
        addTypeSimpleFragment(PROXY_TYPE, PROXY_TABLE_NAME);
    }

    /**
     * Special model for the locks table.
     */
    private void initLocksModel() {
        addPropertyInfo(null, LOCK_PROP, PropertyType.STRING, LOCK_TABLE_NAME,
                LOCK_KEY, false, StringType.INSTANCE);
    }

    /**
     * Special model for the fulltext table.
     */
    private void initFullTextModel() {
        addPropertyInfo(null, FULLTEXT_FULLTEXT_PROP, PropertyType.STRING,
                FULLTEXT_TABLE_NAME, FULLTEXT_FULLTEXT_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, FULLTEXT_SIMPLETEXT_PROP, PropertyType.STRING,
                FULLTEXT_TABLE_NAME, FULLTEXT_SIMPLETEXT_KEY, false,
                StringType.INSTANCE);
        addPropertyInfo(null, FULLTEXT_BINARYTEXT_PROP, PropertyType.STRING,
                FULLTEXT_TABLE_NAME, FULLTEXT_BINARYTEXT_KEY, false,
                StringType.INSTANCE);
    }

    /**
     * Special collection-like model for the ACL table.
     */
    private void initAclModel() {
        Map<String, PropertyType> fragmentKeys = new LinkedHashMap<String, PropertyType>();
        fragmentKeys.put(ACL_POS_KEY, PropertyType.LONG);
        fragmentKeys.put(ACL_NAME_KEY, PropertyType.STRING);
        fragmentKeys.put(ACL_GRANT_KEY, PropertyType.BOOLEAN);
        fragmentKeys.put(ACL_PERMISSION_KEY, PropertyType.STRING);
        fragmentKeys.put(ACL_USER_KEY, PropertyType.STRING);
        fragmentKeys.put(ACL_GROUP_KEY, PropertyType.STRING);
        String fragmentName = ACL_TABLE_NAME;
        addCollectionFragmentInfos(fragmentName, PropertyType.COLL_ACL,
                ACLsFragment.MAKER, ACL_POS_KEY, fragmentKeys);
        addPropertyInfo(null, ACL_PROP, PropertyType.COLL_ACL, fragmentName,
                null, false, null);
    }

    /**
     * Creates the model for one schema or complex type.
     *
     * @return the fragment table name for this type, or {@code null} if this
     *         type doesn't directly hold data
     */
    private String initTypeModel(ComplexType complexType) {
        String typeName = complexType.getName();
        if (schemaFragment.containsKey(typeName)) {
            return schemaFragment.get(typeName); // may be null
        }

        log.debug("Making model for type " + typeName);

        /** Initialized if this type has a table associated. */
        String thisFragmentName = null;
        for (Field field : complexType.getFields()) {
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                /*
                 * Complex type.
                 */
                ComplexType fieldComplexType = (ComplexType) fieldType;
                String subTypeName = fieldComplexType.getName();
                String subFragmentName = initTypeModel(fieldComplexType);
                addTypeSimpleFragment(subTypeName, subFragmentName);
            } else {
                String propertyName = field.getName().getPrefixedName();
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (listFieldType.isSimpleType()) {
                        /*
                         * Array: use a collection table.
                         */
                        String fragmentName = collectionFragmentName(propertyName);
                        PropertyType propertyType = PropertyType.fromFieldType(
                                listFieldType, true);
                        addPropertyInfo(typeName, propertyName, propertyType,
                                fragmentName, null, false, null);

                        Map<String, PropertyType> fragmentKeys = new LinkedHashMap<String, PropertyType>();
                        fragmentKeys.put(COLL_TABLE_POS_KEY, PropertyType.LONG); // TODO
                        // INT
                        fragmentKeys.put(COLL_TABLE_VALUE_KEY,
                                propertyType.getArrayBaseType());
                        addCollectionFragmentInfos(fragmentName, propertyType,
                                ArrayFragment.MAKER, COLL_TABLE_POS_KEY,
                                fragmentKeys);

                        addTypeCollectionFragment(typeName, fragmentName);
                    } else {
                        /*
                         * Complex list.
                         */
                        String subFragmentName = initTypeModel((ComplexType) listFieldType);
                        addTypeSimpleFragment(listFieldType.getName(),
                                subFragmentName);
                    }
                } else {
                    /*
                     * Primitive type.
                     */
                    String fragmentName = typeFragmentName(complexType);
                    PropertyType propertyType = PropertyType.fromFieldType(
                            fieldType, false);
                    String fragmentKey = field.getName().getLocalName();
                    if (fragmentName.equals(UID_SCHEMA_NAME)
                            && (fragmentKey.equals(UID_MAJOR_VERSION_KEY) || fragmentKey.equals(UID_MINOR_VERSION_KEY))) {
                        // HACK special-case the "uid" schema, put major/minor
                        // in the hierarchy table
                        fragmentKey = fragmentKey.equals(UID_MAJOR_VERSION_KEY) ? MAIN_MAJOR_VERSION_KEY
                                : MAIN_MINOR_VERSION_KEY;
                        addPropertyInfo(typeName, propertyName, propertyType,
                                mainTableName, fragmentKey, false, null);
                    } else {
                        addPropertyInfo(typeName, propertyName, propertyType,
                                fragmentName, fragmentKey, false, null);
                        // note that this type has a fragment
                        thisFragmentName = fragmentName;
                    }
                }
            }
        }

        schemaFragment.put(typeName, thisFragmentName); // may be null
        return thisFragmentName;
    }

    private String typeFragmentName(ComplexType type) {
        return type.getName();
    }

    private String collectionFragmentName(String propertyName) {
        return propertyName;
    }
}
