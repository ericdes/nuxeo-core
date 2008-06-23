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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.PrintWriter;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * The managed connection represents an actual physical connection to the
 * underlying storage. It is created by the {@link ManagedConnectionFactory},
 * and then encapsulated into a {@link Connection} which is then returned to the
 * application (via the {@link ConnectionFactory}).
 * <p>
 * If sharing is allowed, several different {@link Connection}s may be
 * associated to a given {@link ManagedConnection}, although not at the same
 * time.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionImpl implements ManagedConnection,
        ManagedConnectionMetaData {

    private static final Log log = LogFactory.getLog(ManagedConnectionImpl.class);

    private PrintWriter out;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionSpecImpl connectionSpec;

    /**
     * High-level {@link Connection} returned to the application, if an
     * association has been made.
     */
    private ConnectionImpl connection;

    /**
     * The low-level session managed by this connection.
     */
    private final SessionImpl session;

    /**
     * List of listeners set by the application server which we must notify of
     * all activity happening on our {@link Connection}.
     */
    private final ListenerList listeners;

    /**
     * Creates a new physical connection to the underlying storage. Called by
     * the {@link ManagedConnectionFactory} when it needs a new connection.
     *
     * @throws ResourceException
     */
    public ManagedConnectionImpl(
            ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionRequestInfoImpl connectionRequestInfo)
            throws ResourceException {
        out = managedConnectionFactory.getLogWriter();
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionSpec = connectionRequestInfo.connectionSpec;
        listeners = new ListenerList();
        // create the underlying session
        session = managedConnectionFactory.getConnection(connectionSpec);
    }

    /*
     * ----- javax.resource.spi.ManagedConnection -----
     */

    /**
     * Creates a new {@link Connection} handle to this {@link ManagedConnection}
     * .
     */
    public synchronized Connection getConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        assert connectionRequestInfo instanceof ConnectionRequestInfoImpl;
        if (connection != null) {
            throw new ResourceException("Sharing not supported");
        }
        connection = new ConnectionImpl(this);
        return connection;
    }

    /**
     * Cleans up the physical connection, so that it may be reused.
     * <p>
     * Called by the application server before putting back this
     * {@link ManagedConnection} into the application server pool.
     * <p>
     * Later, the application server may call {@link #getConnection} again.
     */
    public void cleanup() {
        connection = null;
    }

    /**
     * Destroys the physical connection.
     * <p>
     * Called by the application server before this {@link ManagedConnection} is
     * destroyed.
     */
    public void destroy() throws ResourceException {
        cleanup();
        session.close();
    }

    /**
     * Called by the application server to change the association of an
     * application-level {@link Connection} handle with a
     * {@link ManagedConnection} instance.
     * <p>
     * Used when connection sharing is in effect.
     */
    public synchronized void associateConnection(Object object)
            throws ResourceException {
        ConnectionImpl connection = (ConnectionImpl) object;
        ManagedConnectionImpl managedConnection = connection.getManagedConnection();
        if (managedConnection != this) {
            // reassociate it with us
            connection.setManagedConnection(this);
            // update ManagedConnection to set who has it
            managedConnection.connection = null;
            this.connection = connection;
        }
    }

    public XAResource getXAResource() {
        return session;
    }

    public LocalTransaction getLocalTransaction() {
        throw new UnsupportedOperationException(
                "Local transactions not supported");
    }

    /**
     * Called by the application server to add a listener who should be notified
     * of all relevant events on this connection.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Called by the application server to remove a listener.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    public ManagedConnectionMetaData getMetaData() {
        return this;
    }

    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionMetaData -----
     */

    public String getEISProductName() {
        return "Nuxeo Core SQL Storage";
    }

    public String getEISProductVersion() {
        return "1.0.0";
    }

    public int getMaxConnections() {
        return Integer.MAX_VALUE; // or lower?
    }

    public String getUserName() throws ResourceException {
        Credentials credentials = connectionSpec.getCredentials();
        if (credentials == null) {
            return ""; // XXX
        }
        return credentials.getUserName();
    }

    /*
     * ----- -----
     */

    /**
     * Called by {@link ManagedConnectionFactoryImpl#matchManagedConnections}.
     */
    protected ManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    /*
     * ----- Event management -----
     */

    private void sendClosedEvent() {
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, null);
    }

    protected void sendTxStartedEvent() {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, null);
    }

    protected void sendTxCommittedEvent() {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, null);
    }

    protected void sendTxRolledbackEvent() {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, null);
    }

    protected void sendErrorEvent(Exception cause) {
        sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED, cause);
    }

    private void sendEvent(int type, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (connection != null) {
            event.setConnectionHandle(connection);
        }
        sendEvent(event);
    }

    /**
     * Notifies the application server, through the
     * {@link ConnectionEventListener}s it has registered with us, of what
     * happens with this connection.
     */
    private void sendEvent(ConnectionEvent event) {
        for (Object object : listeners.getListeners()) {
            ConnectionEventListener listener = (ConnectionEventListener) object;
            switch (event.getId()) {
            case ConnectionEvent.CONNECTION_CLOSED:
                listener.connectionClosed(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                listener.localTransactionStarted(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                listener.localTransactionCommitted(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                listener.localTransactionRolledback(event);
                break;
            case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                listener.connectionErrorOccurred(event);
                break;
            }
        }
    }

    /**
     * ------------------------------------------------------------------
     * delegated methods called from the {@link ConnectionImpl} itself
     * ------------------------------------------------------------------
     */

    /*
     * ----- part of javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        sendClosedEvent();
        connection = null;
    }

    /*
     * ----- Session -----
     */

    public void save() throws StorageException {
        session.save();
    }

    public Node addNode(Node parent, String name, String typeName)
            throws StorageException {
        return session.addNode(parent, name, typeName);
    }

    public Model getModel() {
        return session.getModel();
    }

    public Node getNode(Node parent, String name) throws StorageException {
        return session.getNode(parent, name);
    }

    public Node getRootNode() throws StorageException {
        return session.getRootNode();
    }

    public void removeNode(Node node) throws StorageException {
        session.removeNode(node);
    }

}
