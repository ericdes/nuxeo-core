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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.jms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventPublisherExecutor {

    ExecutorService executor;
    MessagePublisher publisher;

    private static EventPublisherExecutor INSTANCE;

    public static EventPublisherExecutor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EventPublisherExecutor();
            try {
                INSTANCE.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return INSTANCE;
    }

    public void start() throws Exception {
        publisher = CoreEventPublisher.getInstance().createPublisher();
        executor = Executors.newSingleThreadExecutor();
    }

    public void stop() throws Exception {
        executor.shutdown();
        publisher.close();
        executor = null;
        publisher = null;
    }

    public void publish(final Object content) throws JMSException {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    System.out.println("############ serial publishing from thread "
                                    + Thread.currentThread());
                    publisher.publish(content);
                } catch (Exception e) {
                    e.printStackTrace(); // TODO
                }
            }
        });
    }

}
