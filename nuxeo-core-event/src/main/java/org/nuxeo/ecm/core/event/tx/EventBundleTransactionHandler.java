/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.event.tx;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_ROLLEDBACK;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to encapsulate Transaction management
 *
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class EventBundleTransactionHandler {

    private static final Log log = LogFactory.getLog(EventBundleTransactionHandler.class);

    private static final String UT_NAME = "UserTransaction";

    private static final String UT_NAME_ALT = "java:comp/UserTransaction";

    private static final String TM_NAME = "TransactionManager";

    private static final String TM_NAME_ALT = "java:/TransactionManager";

    protected UserTransaction tx;

    protected boolean disabled;

    public void beginNewTransaction() {
        beginNewTransaction(null);
    }
    public void beginNewTransaction(Integer transactionTimeout) {
        if (disabled) {
            return;
        }
        if (tx != null) {
            throw new UnsupportedOperationException(
                    "There is already an uncommited transaction running");
        }
        tx = createUT(transactionTimeout);
        if (tx == null) {
            log.debug("No TransactionManager");
            disabled = true;
            return;
        }
        try {
            if (tx.getStatus() == STATUS_COMMITTED) {
                log.error("Transaction is already commited, try to begin anyway");
            }
            tx.begin();
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
        }
    }

    protected UserTransaction createUT(Integer transactionTimeout) {
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (Exception e) {
            disabled = true;
            return null;
        }

        UserTransaction ut = null;
        try {
            ut = (UserTransaction) context.lookup(UT_NAME);
        } catch (NamingException ne) {
            try {
                ut = (UserTransaction) context.lookup(UT_NAME_ALT);
            } catch (NamingException ne2) {
                disabled = true;
            }
        }

        if (transactionTimeout!=null) {
            try {
                ut.setTransactionTimeout(transactionTimeout);
            } catch (SystemException e) {
                log.error("Error while setting transaction timeout to " + transactionTimeout, e);
            }
        }
        return ut;
    }

    protected Transaction createTxFromTM() {
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (Exception e) {
            disabled = true;
            return null;
        }

        TransactionManager tm = null;
        try {
            tm = (TransactionManager) context.lookup(TM_NAME);
        } catch (NamingException ne) {
            try {
                tm = (TransactionManager) context.lookup(TM_NAME_ALT);
            } catch (NamingException ne2) {
            }
        }
        if (tm == null) {
            disabled = true;
            return null;
        }

        try {
            return tm.getTransaction();
        } catch (SystemException e) {
            disabled = true;
            return null;
        }
    }

    protected boolean isUTTransactionActive() {
        try {
            return tx.getStatus() == STATUS_ACTIVE;
        } catch (SystemException e) {
            log.error("Error while getting tx status", e);
            return false;
        }
    }

    private boolean isUTTransactionMarkedRollback() {
        try {
            int status = tx.getStatus();
            return status == STATUS_MARKED_ROLLBACK
                    || status == STATUS_ROLLEDBACK;
        } catch (SystemException e) {
            log.error("Error while getting tx status", e);
            return false;
        }
    }

    public void rollbackTransaction() {
        if (disabled || tx == null) {
            return;
        }
        try {
            if (!isUTTransactionMarkedRollback()) {
                tx.setRollbackOnly();
            }
            commitOrRollbackTransaction();
        } catch (Exception e) {
            log.error("Error while marking tx for rollback", e);
        } finally {
            tx = null;
        }
    }

    public void commitOrRollbackTransaction() {
        if (disabled || tx == null) {
            return;
        }
        try {
            if (isUTTransactionActive()) {
                try {
                    tx.commit();
                } catch (Exception e) {
                    log.error("Error during commit", e);
                }
            } else {
                try {
                    log.debug("Rolling back transaction");
                    tx.rollback();
                } catch (Exception e) {
                    // if the transaction was already rolledback due to
                    // transaction timeout, we must still call tx.rollback but
                    // this causes a spurious error message, so log at debug
                    // level
                    log.debug("Error during rollback", e);
                }
            }
        } finally {
            tx = null;
        }
    }

}
