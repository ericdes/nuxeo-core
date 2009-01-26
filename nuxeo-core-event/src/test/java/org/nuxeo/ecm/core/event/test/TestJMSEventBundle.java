package org.nuxeo.ecm.core.event.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.jms.JMSEventBundle;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestJMSEventBundle extends NXRuntimeTestCase {


    protected CoreSession fakeCoreSession = new FakeCoreSession();
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    protected EventBundle createTestEventBundle() {
        EventBundle bundle = new EventBundleImpl();

        EventContext ctx1 = new EventContextImpl(fakeCoreSession, new SimplePrincipal("toto"));
        EventContext ctx2 = new EventContextImpl(fakeCoreSession, new SimplePrincipal("titi"));

        DocumentRef parentRef = new IdRef("01");
        DocumentRef docRef = new IdRef("02");
        String[] schemas = {"file","dublincore"};
        DocumentModel srcDoc = new DocumentModelImpl("sid0", "File", "02", new Path("/"), docRef, parentRef, schemas, null);
        DocumentRef destinationRef = new IdRef("03");

        EventContext ctx3 = new DocumentEventContext(null,new SimplePrincipal("tata"),srcDoc, destinationRef);


        bundle.push(ctx1.event("EVT1"));
        bundle.push(ctx2.event("EVT2"));
        bundle.push(ctx3.event("EVT3"));

        return bundle;
    }

    public final static Object serialize(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(obj);
        out.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        return in.readObject();
    }

    public void testBundleSerialization() throws Exception {

        EventBundle srcEventBundle = createTestEventBundle();

        JMSEventBundle srcJmsEventBundle = new JMSEventBundle(srcEventBundle);

        JMSEventBundle dstJmsEventBundle2 = (JMSEventBundle)serialize(srcJmsEventBundle);

        assertNotNull(dstJmsEventBundle2);

        EventBundle dstEventBundle2 = dstJmsEventBundle2.reconstructEventBundle(fakeCoreSession);
        assertNotNull(dstEventBundle2);


        assertEquals(3, dstEventBundle2.getEvents().length);

        if (!(dstEventBundle2.getEvents()[0].getContext() instanceof EventContextImpl)) {
            fail();
        }
        if (!(dstEventBundle2.getEvents()[1].getContext() instanceof EventContextImpl)) {
            fail();
        }
        if (!(dstEventBundle2.getEvents()[2].getContext() instanceof DocumentEventContext)) {
            fail();
        }

        DocumentEventContext docCtx = (DocumentEventContext)dstEventBundle2.getEvents()[2].getContext();
        assertNotNull(docCtx.getSourceDocument());
        assertNotNull(docCtx.getDestination());

        assertEquals("02", docCtx.getSourceDocument().getRef().toString());
        assertEquals("03", docCtx.getDestination().toString());
        assertEquals("tata", docCtx.getPrincipal().getName());

    }
}
