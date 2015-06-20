package me.itzg.etcd;

import me.itzg.etcd.keys.Node;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Geoff Bourne
 * @since 6/20/2015
 */
public class EtcdUtilsTest {

    @Test
    public void testExtractParentPath() throws Exception {
        final Node node = new Node();
        node.setKey("/one/two/three");

        final String[] result = EtcdUtils.extractParentPath(node);
        assertArrayEquals(new String[]{"one", "two"}, result);
    }


    @Test
    public void testExtractNameFromNode() throws Exception {
        final Node node = new Node();
        node.setKey("/one/two/three");

        assertEquals("three", EtcdUtils.extractNameFromNode(node));
    }

}