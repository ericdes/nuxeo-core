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

package org.nuxeo.ecm.core.storage.sql;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import javax.resource.cci.Connection;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 *
 * @author Florent Guillaume
 */
public interface Session extends Connection {

    /**
     * Checks if the session is live (not closed).
     *
     * @return {@code true} if the session is live
     */
    boolean isLive();

    /**
     * Gets the session repository name.
     *
     * @return the repository name
     */
    String getRepositoryName() throws StorageException;

    /**
     * Gets the {@link Model} associated to this session.
     *
     * @return the model
     * @throws StorageException
     */
    Model getModel() throws StorageException;

    /**
     * Saves the modifications to persistent storage.
     * <p>
     * Modifications will be actually written only upon transaction commit.
     *
     * @throws StorageException
     */
    void save() throws StorageException;

    /**
     * Gets the root node of the repository.
     *
     * @return the root node
     */
    Node getRootNode() throws StorageException;

    /**
     * Gets a node given its id.
     *
     * @param id the id
     * @return the node, or {@code null} if not found
     * @throws StorageException
     */
    Node getNodeById(Serializable id) throws StorageException;

    /**
     * Gets a node given its absolute path, or given an existing node and a
     * relative path.
     *
     * @param path the path
     * @param node the node (ignored for absolute paths)
     *
     * @return the node, or {@code null} if not found
     * @throws StorageException
     */
    Node getNodeByPath(String path, Node node) throws StorageException;

    /**
     * Gets the parent of a node.
     * <p>
     * The root has a {@code null} parent.
     *
     * @param node the node
     * @return the parent node, or {@code null} for the root's parent
     * @throws StorageException
     */
    Node getParentNode(Node node) throws StorageException;

    /**
     * Gets the absolute path of a node.
     *
     * @param node the node
     * @return the path
     * @throws StorageException
     */
    String getPath(Node node) throws StorageException;

    /**
     * Checks if a child node with the given name exists.
     * <p>
     * There are two kinds of children, the regular children documents and the
     * complex properties. The {@code boolean} {@value complexProp} allows a
     * choice between those.
     *
     * @param parent the parent node
     * @param name the child name
     * @param complexProp whether to check complex properties or regular
     *            children
     * @return {@code true} if a child node with that name exists
     * @throws StorageException
     */
    boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException;

    /**
     * Gets a child node given its parent and name.
     *
     * @param parent the parent node
     * @param name the child name
     * @param complexProp whether to check complex properties or regular
     *            children
     * @return the child node, or {@code null} is not found
     * @throws StorageException
     */
    Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException;

    /**
     * Checks it a node has children.
     *
     * @param parent the parent node
     * @param complexProp whether to check complex properties or regular
     *            children
     * @return {@code true} if the parent has children
     * @throws StorageException
     */
    boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException;

    /**
     * Gets the children of a node.
     *
     * @param parent the parent node
     * @param name the children name to get (for lists of complex properties),
     *            or {@code null} for all
     * @param complexProp whether to check complex properties or regular
     *            children
     * @return the collection of children
     * @throws StorageException
     */
    List<Node> getChildren(Node parent, String name, boolean complexProp)
            throws StorageException;

    /**
     * Creates a new child node.
     *
     * @param parent the parent to which the child is added
     * @param name the child name
     * @param pos the child position, or {@code null}
     * @param typeName the child type
     * @param complexProp whether this is a complex property ({@code true}) or a
     *            regular child ({@code false})
     * @return the new node
     * @throws StorageException
     */
    Node addChildNode(Node parent, String name, Long pos, String typeName,
            boolean complexProp) throws StorageException;

    /**
     * Creates a new child node with given id (used for import).
     *
     * @param id the id
     * @param parent the parent to which the child is added
     * @param name the child name
     * @param pos the child position, or {@code null}
     * @param typeName the child type
     * @param complexProp whether this is a complex property ({@code true}) or a
     *            regular child ({@code false})
     * @return the new node
     * @throws StorageException
     */
    Node addChildNode(Serializable id, Node parent, String name, Long pos,
            String typeName, boolean complexProp) throws StorageException;

    /**
     * Creates a proxy for a version node.
     *
     * @param targetId the target id
     * @param versionableId the versionable id
     * @param parent the parent to which the proxy is added
     * @param name the proxy name
     * @param pos the proxy position
     * @return the new proxy node
     * @throws StorageException
     */
    Node addProxy(Serializable targetId, Serializable versionableId,
            Node parent, String name, Long pos) throws StorageException;

    /**
     * Removes a node from the storage.
     *
     * @param node the node to remove
     * @throws StorageException
     */
    void removeNode(Node node) throws StorageException;

    /**
     * Moves a node to a new location with a new name.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param source the node to move
     * @param parent the new parent to which the node is moved
     * @param name the new node name
     * @return the moved node
     * @throws StorageException
     */
    Node move(Node source, Node parent, String name) throws StorageException;

    /**
     * Copies a node to a new location with a new name.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param source the node to copy
     * @param parent the new parent to which the node is copied
     * @param name the new node name
     * @return the copied node
     * @throws StorageException
     */
    Node copy(Node source, Node parent, String name) throws StorageException;

    /**
     * Checks in a checked-out node: creates a new version with a copy of its
     * information.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param node the node to check in
     * @param label the label for the version
     * @param description the description for the version
     * @return the created version
     * @throws StorageException
     */
    Node checkIn(Node node, String label, String description)
            throws StorageException;

    /**
     * Checks out a checked-in node.
     *
     * @param node the node to check out
     * @throws StorageException
     */
    void checkOut(Node node) throws StorageException;

    /**
     * Restores a node to a given version.
     * <p>
     * The restored node is checked in.
     *
     * @param node the node to restore
     * @param label the version label
     * @throws StorageException
     */
    void restoreByLabel(Node node, String label) throws StorageException;

    /**
     * Gets a version given its versionable id and label.
     *
     * @param versionableId the versionable id
     * @param label the label
     * @return the version node, or {@code null} if not found
     * @throws StorageException
     */
    Node getVersionByLabel(Serializable versionableId, String label)
            throws StorageException;

    /**
     * Gets all the versions for a given versionable node.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param node the versionable node
     * @return the list of versions
     * @throws StorageException
     */
    List<Node> getVersions(Node node) throws StorageException;

    /**
     * Gets the last version for a given versionable node.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param node the versionable node
     * @return the last version, or {@code null} if no versions exist
     * @throws StorageException
     */
    Node getLastVersion(Node node) throws StorageException;

    /**
     * Creates a binary value given an input stream.
     *
     * @param in the input stream
     * @return the binary value
     * @throws StorageException
     */
    Binary getBinary(InputStream in) throws StorageException;

    /**
     * Finds the proxies for a document. If the parent is not null, the search
     * will be limited to its direct children.
     * <p>
     * If the document is a version, then only proxies to that version will be
     * looked up.
     * <p>
     * If the document is a proxy, then all similar proxies (pointing to any
     * version of the same versionable) are retrieved.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param document the document
     * @param parent the parent, or {@code null}
     * @return the list of proxies
     * @throws StorageException
     */
    List<Node> getProxies(Node document, Node parent) throws StorageException;

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query as a parsed tree
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, also count the total size without
     *            offset/limit
     * @return the resulting list with total size included
     */
    PartialList<Serializable> query(SQLQuery query, QueryFilter queryFilter,
            boolean countTotal) throws StorageException;

}
