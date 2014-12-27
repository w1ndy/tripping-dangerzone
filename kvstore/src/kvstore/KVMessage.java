package kvstore;

import static kvstore.KVConstants.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import kvstore.xml.KVMessageType;
import kvstore.xml.ObjectFactory;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is the object that is used to generate the XML based messages
 * for communication between clients and servers.
 */
public class KVMessage implements Serializable {

    private String msgType;
    private String key;
    private String value;
    private String message;

    public static final long serialVersionUID = 6473128480951955693L;

    /**
     * Construct KVMessage with only a type.
     *
     * @param msgType the type of this KVMessage
     */
    public KVMessage(String msgType) {
        this(msgType, null);
    }

    /**
     * Construct KVMessage with type and message.
     *
     * @param msgType the type of this KVMessage
     * @param message the content of this KVMessage
     */
    public KVMessage(String msgType, String message) {
        this.msgType = msgType;
        this.message = message;
    }

    /**
     * Construct KVMessage from the InputStream of a socket.
     * Parse XML from the InputStream with unlimited timeout.
     *
     * @param  sock Socket to receive serialized KVMessage through
     * @throws KVException if we fail to create a valid KVMessage. Please see
     *         KVConstants.java for possible KVException messages.
     */
    public KVMessage(Socket sock) throws KVException {
        this(sock, 0);
    }

    /**
     * Construct KVMessage from the InputStream of a socket.
     * This constructor parses XML from the InputStream within a certain timeout
     * or with an unlimited timeout if the provided argument is 0.
     *
     * @param  sock Socket to receive serialized KVMessage through
     * @param  timeout total allowable receipt time, in milliseconds
     * @throws KVException if we fail to create a valid KVMessage. Please see
     *         KVConstants.java for possible KVException messages.
     */
    public KVMessage(Socket sock, int timeout) throws KVException {
        // implement me
    }

    /**
     * Constructs a KVMessage by copying another KVMessage.
     *
     * @param kvm KVMessage with fields to copy
     */
    public KVMessage(KVMessage kvm) {
        // implement me
    }

    

    /**
     * Validates and creates the KVMessageType XML root element for this KVMessage
     *
     * @throws JAXBException
     * @throws KVException
     */
    private JAXBElement<KVMessageType> getXMLRoot() throws JAXBException, KVException {
        ObjectFactory factory = new ObjectFactory();
        KVMessageType xmlStore = factory.createKVMessageType();
        //implement me
        return factory.createKVMessage(xmlStore);
    }

    /**
     * Generate the serialized XML representation for this message. See the spec
     * for details on the expected output format.
     *
     * @return the XML string representation of this KVMessage
     * @throws KVException
     *             with ERROR_INVALID_FORMAT or ERROR_PARSER
     */
    public String toXML() throws KVException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            marshalTo(os);
        }
        catch (KVException e) {
            throw new KVException(KVConstants.ERROR_INVALID_FORMAT);
        }
        catch (JAXBException e) {
            throw new KVException(KVConstants.ERROR_PARSER);
        }
        return os.toString();
    }

    /**
     * Grab XML from an InputStream and create a KVMessageType object.
     *
     * @param is InputStream to get XML from
     * @return KVMessageType from XML
     * @throws JAXBException
     */
    private KVMessageType unmarshal(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return ((JAXBElement<KVMessageType>)unmarshaller.unmarshal(new NoCloseInputStream(is))).getValue();
    }

    /**
     * Export XML from this KVMessage object and marshal it to the OutputStream
     * @param os OutputStream to marshal to
     * @throws JAXBException
     * @throws KVException
     */
    private void marshalTo(OutputStream os) throws JAXBException, KVException {
        JAXBContext jc = JAXBContext.newInstance(KVMessageType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.marshal(getXMLRoot(), os);
    }
    

    /**
     * Send serialized version of this KVMessage over the network.
     * You must call sock.shutdownOutput() in order to flush the OutputStream
     * and send an EOF (so that the receiving end knows you are done sending).
     * Do not call close on the socket. Closing a socket closes the InputStream
     * as well as the OutputStream, preventing the receipt of a response.
     *
     * @param  sock Socket to send XML through
     * @throws KVException with ERROR_INVALID_FORMAT, ERROR_PARSER, or
     *         ERROR_COULD_NOT_SEND_DATA
     */
    public void sendMessage(Socket sock) throws KVException {
        // implement me
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsgType() {
        return msgType;
    }


    @Override
    public String toString() {
        try {
            return this.toXML();
        } catch (KVException e) {
            // swallow KVException
            return e.toString();
        }
    }

    /*
     * InputStream wrapper that allows us to reuse the corresponding
     * OutputStream of the socket to send a response.
     * Please read about the problem and solution here:
     * http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html
     */
    private class NoCloseInputStream extends FilterInputStream {
        public NoCloseInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {} // ignore close
    }

    /* http://stackoverflow.com/questions/2567416/document-to-string/2567428#2567428 */
    public static String printDoc(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }


}
