package com.dkcompany.dmsintegration.as4client;

import javax.xml.bind.*;
import javax.xml.transform.Result;
import java.io.InputStream;
import java.util.concurrent.Callable;

public class JaxbThreadSafe {

    private final ThreadLocal<Unmarshaller> unmarshaller;
    private final ThreadLocal<Marshaller> marshaller;

    public JaxbThreadSafe(JAXBContext jaxb) {
        this.unmarshaller = ThreadLocal.withInitial(() -> safe(jaxb::createUnmarshaller));
        this.marshaller = ThreadLocal.withInitial(() -> safe(jaxb::createMarshaller));
    }

    private static <T> T safe(Callable<T> fn) {
        try {
            return fn.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object unmarshal(InputStream is) throws JAXBException {
        return unmarshaller.get().unmarshal(is);
    }

    public void marshal(Object jaxbElement, Result result) throws JAXBException {
        marshaller.get().marshal(jaxbElement, result);
    }
}

