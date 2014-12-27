package kvstore;

/**
 * This is the simple interface that all of the KeyValue servers,
 * Caches and Stores should implement.
 *
 */
public interface KeyValueInterface {
    /**
     * Insert Key, Value pair into the storage unit
     * @param key is the object used to index into the store
     * @param value is the object corresponding to a unique key
     * @throws KVException if there is an error when inserting the entry into
     *         the store
     */
    public void put(String key, String value) throws KVException;

    /**
     * Retrieve the object corresponding to the provided key
     * @param key is the object used to index into the store
     * @return the value corresponding to the provided key
     * @throws KVException if there is an error when looking up the object store
     */
    public String get(String key) throws KVException;

    /**
     * Delete the object corresponding to the provided key
     * @param key is the object used to index into the store
     * @throws KVException if there is an error when looking up the object store
     */
    public void del(String key) throws KVException;

}
