package me.itzg.etcd;

import me.itzg.etcd.keys.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoff Bourne
 * @since 6/20/2015
 */
public class EtcdUtils {
    /**
     * Extracts just the name part of a node's key-path
     *
     * @param node
     * @return
     */
    public static String extractNameFromNode(Node node) {
        final String key = node.getKey();
        final List<String> parts = splitToList(key);
        return parts.get(parts.size() - 1);
    }

    private static List<String> splitToList(String key) {
        final String[] parts = key.split("/");
        List<String> ret = new ArrayList<String>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                ret.add(part);
            }
        }
        return ret;
    }

    public static String[] extractParentPath(Node node) {
        final String key = node.getKey();
        final List<String> parts = splitToList(key);

        final String[] ret = new String[parts.size() - 1];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = parts.get(i);
        }
        return ret;
    }

    /**
     * Simple wrapper to throw {@link IllegalStateException} when given node is null which was due
     * to not existing.
     * @param node
     * @return the value from the node
     * @throws IllegalStateException when node is null, i.e. doesn't exist
     */
    public static String extractValue(Node node) {
        if (node == null) {
            throw new IllegalStateException("Node did not exist");
        }
        return node.getValue();
    }

    public static int extractIntValue(Node node) {
        if (node == null) {
            throw new IllegalStateException("Node did not exist");
        }
        return Integer.parseInt(node.getValue());
    }

    public static String join(String[] path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                sb.append("/");
            }
            sb.append(path[i]);
        }
        return sb.toString();
    }
}
