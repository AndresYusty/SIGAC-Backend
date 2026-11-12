package com.university.sigac.core.storage;

import java.io.IOException;
import java.io.InputStream;

/** Where the bytes live. Keys are already validated, relative and separated by "/". */
interface FileStore {

    void write(String key, byte[] data) throws IOException;

    void write(String key, InputStream in) throws IOException;

    /** @throws java.io.FileNotFoundException when the key does not exist */
    byte[] read(String key) throws IOException;

    boolean exists(String key);

    /** Size in bytes, or -1 when the key does not exist. */
    long size(String key) throws IOException;

    void delete(String key) throws IOException;
}
