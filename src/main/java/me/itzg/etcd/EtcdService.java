package me.itzg.etcd;

import me.itzg.etcd.keys.Node;
import me.itzg.etcd.keys.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Geoff Bourne
 * @since 6/17/2015
 */
public class EtcdService {
    public static final String SEP = "/";
    private static final HttpHeaders HEADERS_FORM_URLENCODED = new HttpHeaders();
    private static Logger LOG = LoggerFactory.getLogger(EtcdService.class);

    static {
        HEADERS_FORM_URLENCODED.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    private final int machineCount;
    private final URI[] machines;
    private int machinePos;

    private RestTemplate restTemplate = new RestTemplate();
    private URI currentMachine;

    public EtcdService(URI[] machines) {
        machineCount = machines.length;
        synchronized (this) {
            this.machines = machines;
            machinePos = 0;
            currentMachine = machines[machinePos];
        }
    }

    public void delete(final String... path) throws IOException, EtcdException {
        access(new Accessor<Void>() {
            @Override
            public Void access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                restTemplate.delete(builtUri);

                return null;
            }
        });
    }

    public void ensureDir(final String... parts) throws IOException, EtcdException {
        access(new Accessor<Void>() {
            @Override
            public Void access(URI uri) throws RestClientException, IOException {
                final UriComponentsBuilder uriComponentsBuilder = createKeysUriBuilder(uri);

                boolean makeTheRest = false;

                for (String part : parts) {
                    if (!part.startsWith(SEP)) {
                        part = SEP + part;
                    }
                    uriComponentsBuilder.path(part);

                    final URI partUri = uriComponentsBuilder.build().toUri();

                    if (makeTheRest) {
                        createDir(partUri);
                    } else {
                        try {
                            final ResponseEntity<Response> getResponse = restTemplate.getForEntity(partUri, Response.class);
                        } catch (HttpClientErrorException e) {
                            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                                makeTheRest = true;
                                createDir(partUri);
                            }
                        }
                    }

                }
                return null;
            }
        });
    }

    public void put(final String value, final String... path) throws IOException, EtcdException {
        access(new Accessor<Void>() {
            @Override
            public Void access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                final ResponseEntity<Response> response = doPut(builtUri,
                        "value", value);

                return null;
            }
        });
    }

    /**
     * @param value the value of the key
     * @param path  path to the key to put
     * @return true if the key did not previously exist
     */
    public boolean putIfNotExists(final String value, final String... path) throws IOException, EtcdException {
        return access(new Accessor<Boolean>() {
            @Override
            public Boolean access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                final ResponseEntity<Response> response;
                try {
                    doPut(builtUri,
                            "prevExist", "false",
                            "value", value);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
                        return false;
                    }
                    throw e;
                }

                return true;
            }
        });
    }

    public boolean createDirIfNotExists(final String... path) throws IOException, EtcdException {
        return access(new Accessor<Boolean>() {
            @Override
            public Boolean access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                try {
                    doPut(builtUri,
                            "prevExist", "false",
                            "dir", "true");

                    return true;
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
                        return false;
                    }
                    throw e;
                }
            }
        });
    }

    /**
     * @param path
     * @return the etcd node at that path or null if it didn't exist
     * @throws IOException
     * @throws EtcdException
     */
    public Node get(final String... path) throws IOException, EtcdException {
        return access(new Accessor<Node>() {
            @Override
            public Node access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                final Response response = doGet(builtUri);
                return response != null ? response.getNode() : null;
            }
        });
    }

    /**
     *
     * @param path the base path of the nodes to bulk-get
     * @return a prepared getter or null if the initial path didn't exist
     * @throws IllegalStateException if the given path was not a directory
     */
    public BulkGetter bulkGet(final String... path) throws IllegalStateException, IOException, EtcdException {
        return access(new Accessor<BulkGetter>() {
            @Override
            public BulkGetter access(URI uri) throws RestClientException, IOException {
                final URI builtUri = buildPathUri(uri, path);

                final Response response = doGet(builtUri);
                if (response == null) {
                    return null;
                }

                final Node node = response.getNode();
                if (!node.isDir()) {
                    throw new IllegalStateException("Node is not a directory: " + node.getKey());
                }

                return new BulkGetter(node.getKey());
            }
        });
    }

    /**
     * @param newValue
     * @param prevIndex the existing <code>modifiedIndex</code> of the node to update
     * @param key       the path to the key to update
     * @return true if the update was successful
     * @throws IOException
     * @throws EtcdException
     */
    public boolean updateKeyAtomically(final String newValue, final int prevIndex, final String key) throws IOException, EtcdException {
        return access(new Accessor<Boolean>() {
            @Override
            public Boolean access(URI uri) throws RestClientException, IOException {
                final UriComponentsBuilder uriBuilder = createKeysUriBuilder(uri)
                        .path(key);
                final URI builtUri = uriBuilder.build().toUri();

                try {
                    doPut(builtUri,
                            "value", newValue,
                            "prevIndex", String.valueOf(prevIndex));

                    return true;
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
                        return false;
                    }
                    throw e;
                }
            }
        });
    }

    public boolean updateKeyAtomically(final String newValue, final String previousValue, final String... path) throws IOException, EtcdException {
        return access(new Accessor<Boolean>() {
            @Override
            public Boolean access(URI uri) throws RestClientException, IOException, EtcdException {
                final URI builtUri = buildPathUri(uri, path);

                try {
                    doPut(builtUri,
                            "prevValue", previousValue,
                            "value", newValue);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
                        return false;
                    }
                    else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        throw new EtcdException(builtUri.toString(), EtcdError.EcodeKeyNotFound);
                    }
                    throw e;
                }

                return true;
            }
        });
    }

    public void deleteKey(final String key) throws IOException, EtcdException {
        access(new Accessor<Void>() {
            @Override
            public Void access(URI uri) throws RestClientException, IOException, EtcdException {
                final UriComponentsBuilder uriBuilder = createKeysUriBuilder(uri)
                        .path(key);
                final URI builtUri = uriBuilder.build().toUri();

                try {
                    restTemplate.delete(builtUri);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        throw new EtcdException(builtUri.toString(), EtcdError.EcodeKeyNotFound);
                    }
                    throw e;
                }

                return null;
            }
        });
    }

    /**
     *
     * @param key
     * @param prevIndex
     * @return true if this caller was able to perform the deletion
     * @throws IOException
     * @throws EtcdException
     */
    public boolean deleteKeyAtomically(final String key, final int prevIndex) throws IOException, EtcdException {
        return access(new Accessor<Boolean>() {
            @Override
            public Boolean access(URI uri) throws RestClientException, IOException, EtcdException {
                final UriComponentsBuilder uriBuilder = createKeysUriBuilder(uri)
                        .path(key)
                        .queryParam("prevIndex", String.valueOf(prevIndex));
                final URI builtUri = uriBuilder.build().toUri();

                try {
                    restTemplate.delete(builtUri);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        throw new EtcdException(builtUri.toString(), EtcdError.EcodeKeyNotFound);
                    }
                    else if (e.getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
                        return false;
                    }
                    throw e;
                }

                return true;
            }
        });
    }

    private URI buildPathUri(URI uri, String[] path) {
        final UriComponentsBuilder uriBuilder = createKeysUriBuilder(uri);
        if (!path[0].startsWith(SEP)) {
            path[0] = SEP + path[0];
        }
        uriBuilder.path(EtcdUtils.join(path));
        return uriBuilder.build().toUri();
    }

    protected <T> T access(Accessor<T> accessor) throws RestClientException, EtcdException, IOException {
        for (int tries = 0; tries < machineCount; ++tries) {
            try {

                return accessor.access(currentMachine);

            } catch (ConnectException e) {
                LOG.info("Connection to {} failed: {}", currentMachine, e.getMessage());
                synchronized (this) {
                    if (++machinePos >= machines.length) {
                        machinePos = 0;
                    }
                    currentMachine = machines[machinePos];
                }
            }
        }

        throw new NoUsableMachinesException();
    }

    private void createDir(URI builtUri) {
        final ResponseEntity<Response> createResponse = doPut(builtUri,
                "dir", "true",
                "prevExist", "false");
        LOG.debug("createDir of {} got {}", builtUri, createResponse);
    }

    private UriComponentsBuilder createKeysUriBuilder(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .path("/v2/keys");
    }

    protected ResponseEntity<Response> doPut(URI builtUri, String... params) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < params.length - 1; ++i) {
            body.add(params[i], params[i + 1]);
        }

        final RequestEntity<LinkedMultiValueMap<String, String>> req =
                new RequestEntity<>(body, HEADERS_FORM_URLENCODED, HttpMethod.PUT, builtUri);

        return restTemplate.exchange(builtUri, HttpMethod.PUT, req, Response.class);
    }

    /**
     * Takes care of catching "not found" and returning null in that case
     * @param builtUri
     * @return the response containing the node or null if not found
     */
    protected Response doGet(URI builtUri) {
        try {
            final Response response = restTemplate.getForObject(builtUri, Response.class);
            return response;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    protected interface Accessor<T> {
        T access(URI uri) throws RestClientException, IOException, EtcdException;
    }

    public class BulkGetter {

        private final String baseKey;

        public BulkGetter(String baseKey) {

            this.baseKey = baseKey.startsWith(SEP) ? baseKey.substring(1) : baseKey;
        }

        public Node get(final String subKey) throws IOException, EtcdException {
            return access(new Accessor<Node>() {
                @Override
                public Node access(URI uri) throws RestClientException, IOException {
                    final UriComponentsBuilder uriBuilder = createKeysUriBuilder(uri)
                        .pathSegment(baseKey, subKey);
                    final URI builtUri = uriBuilder.build().toUri();

                    final Response response = doGet(builtUri);
                    return response != null ? response.getNode() : null;
                }
            });
        }
    }
}
