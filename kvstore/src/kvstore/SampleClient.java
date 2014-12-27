package kvstore;

import java.net.InetAddress;

public class SampleClient {

    public static void main(String[] args) throws Exception {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException("Need server IP address");
            }

            String hostname = args[0];

            if (hostname.charAt(0) == '$') {
                hostname = InetAddress.getLocalHost().getHostAddress();
            }
            System.out.println("Looking for server at " + hostname);

            KVClient client = new KVClient(hostname, 8080);

            System.out.println("put(\"foo\", \"bar\")");
            client.put("foo", "bar");
            System.out.println("put success!");

            System.out.println("get(\"foo\")");
            String value = client.get("foo");
            System.out.println("Get returned \"" + value + "\"");

            System.out.println("del(\"foo\")");
            client.del("foo");
            System.out.println("del success!");
        } catch (KVException kve) {
            System.out.println("ERROR: Unexpected KVException raised: ");
            Thread.sleep(100);
            System.out.println("Message: " + kve.getKVMessage().getMessage());
            Thread.sleep(100);
            kve.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
