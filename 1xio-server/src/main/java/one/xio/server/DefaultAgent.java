package one.xio.server;


import one.xio.proto.HttpMethod;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import static one.xio.proto.ProtoUtil.threadPool;
//import javolution.text.Text;

/**
 * Hello world!
 */
public class DefaultAgent extends Agent {
    public DefaultAgent(String...a) throws IOException {
        super(a);
        //To change body of created methods use File | Settings | File Templates.
    }
    //    public ByteBuffer token;

    // '$' as universal polymorphic default predicate,
    // when in doubt '$ $($...  $){} is the dominant generic predicate expression


    @Override
    public void onAccept(SelectionKey key) {
        onAccept(key, get$());
    }

    @Override
    public void onConnect(SelectionKey key) {
        onConnect(key, get$());
    }

    @Override
    public void onRead(SelectionKey key) {
        onRead(key, get$());
    }

    @Override
    public void onWrite(SelectionKey key) {
        onWrite(key, get$());
    }


    static public void main(String... a) throws IOException {

        final Agent agent = new DefaultAgent(a);

        Object killMe=new Object();
        synchronized (killMe){
            try {
                killMe.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }
}
