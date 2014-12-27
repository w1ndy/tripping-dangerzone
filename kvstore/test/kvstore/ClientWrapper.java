package kvstore;

public class ClientWrapper extends KVClient {

    public ClientWrapper(String server, int port) {
        super(server, port);
    }

    @Override
    public void put(String key, String value) throws KVException {
        System.out.format("put(%s, %s)\n",
                          Utils.truncateString(key),
                          Utils.truncateString(value));
        try {
            super.put(key, value);
        } catch (KVException kve) {
            System.out.format(" -> EXCEPTION: %s\n",
                              kve.getKVMessage().getMessage());
            throw kve;
        }
    }

    @Override
    public String get(String key) throws KVException {
        System.out.format("get(%s)", Utils.truncateString(key));
        String val = null;
        try {
            val = super.get(key);
        } catch (KVException kve) {
            System.out.format(" -> EXCEPTION: %s\n",
                              kve.getKVMessage().getMessage());
            throw kve;
        }
        System.out.format(" -> %s\n", Utils.truncateString(val));
        return val;
    }

    @Override
    public void del(String key) throws KVException {
        System.out.format("del(%s)\n", Utils.truncateString(key));
        try {
            super.del(key);
        } catch (KVException kve) {
            System.out.format(" -> EXCEPTION: %s\n",
                              kve.getKVMessage().getMessage());
            throw kve;
        }
    }
}
