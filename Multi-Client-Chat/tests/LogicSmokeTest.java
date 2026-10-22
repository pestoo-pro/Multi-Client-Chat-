import ma.chat.client.logic.ChatClient;
import ma.chat.server.logic.ChatServer;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Headless end-to-end test of the JavaFX-independent "logic" packages of both projects.
 * Needs no JavaFX and no Maven: ChatServer, ClientHandler and ChatClient only use java.net / java.io.
 */
public class LogicSmokeTest {

    static int failures = 0;
    static final int PORT = 39871;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    static class Peer {
        final ChatClient client = new ChatClient("127.0.0.1", PORT);
        final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();

        Peer(String username) throws Exception {
            client.setMessageListener(inbox::add);
            client.connect();
            client.sendUsername(username);
            client.startReceiving();
        }

        void say(String text) throws Exception {
            client.sendMessage(text);
        }

        /** Next received line, or null after the timeout. */
        String next(long ms) throws InterruptedException {
            return inbox.poll(ms, TimeUnit.MILLISECONDS);
        }

        /** Waits for a line matching the regex. */
        String expect(String regex) throws InterruptedException {
            long end = System.currentTimeMillis() + 3000;
            Pattern p = Pattern.compile(regex);
            while (System.currentTimeMillis() < end) {
                String s = inbox.poll(100, TimeUnit.MILLISECONDS);
                if (s != null && p.matcher(s).matches()) return s;
            }
            return null;
        }

        boolean disconnectedWithin(long ms) throws Exception {
            Thread t = Thread.startVirtualThread(() -> {
                try {
                    client.awaitDisconnection();
                } catch (InterruptedException ignored) {
                }
            });
            t.join(ms);
            return !t.isAlive();
        }
    }

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer(PORT);
        Thread.startVirtualThread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                System.out.println("server error: " + e);
            }
        });
        Thread.sleep(500);
        check("server reports running", server.isRunning());

        Peer alice = new Peer("alice");
        Peer bob = new Peer("bob");
        Peer guest = new Peer("");
        Thread.sleep(300);
        check("active users lists named users only, sorted",
                server.getActiveUsernames().equals(List.of("alice", "bob")));

        alice.say("hello everyone");
        String re = "\\[\\d\\d:\\d\\d\\] alice: hello everyone";
        check("bob receives '[HH:mm] alice: hello everyone'", bob.expect(re) != null);
        check("read-only guest receives broadcasts", guest.expect(re) != null);
        check("sender also receives the broadcast", alice.expect(re) != null);

        guest.say("can I talk?");
        check("read-only send is refused with a message",
                guest.expect("READ-ONLY MODE: You cannot send messages\\.") != null);
        check("refused message is not broadcast", bob.next(400) == null);

        guest.say("allUsers");
        check("read-only guest cannot run allUsers",
                guest.expect("READ-ONLY MODE: You cannot send messages\\.") != null);

        bob.say("allUsers");
        check("allUsers answers the requester with the named users",
                bob.expect("Active users: alice, bob") != null);
        check("allUsers answer is private (alice sees nothing)", alice.next(400) == null);

        Peer alice2 = new Peer("alice");
        Thread.sleep(300);
        check("duplicate username is accepted (documented limitation)",
                server.getActiveUsernames().equals(List.of("alice", "alice", "bob")));
        alice2.say("bye");
        check("duplicate client can leave", alice2.disconnectedWithin(3000));

        bob.say("bye");
        check("'bye' closes bob's connection", bob.disconnectedWithin(3000));
        Thread.sleep(300);
        check("server user list drops bob", server.getActiveUsernames().equals(List.of("alice")));

        alice.say("end");
        check("'end' closes alice's connection", alice.disconnectedWithin(3000));

        guest.client.disconnect();
        check("read-only client can disconnect (LEAVE)", guest.disconnectedWithin(3000));

        Peer last = new Peer("zoe");
        Thread.sleep(300);
        server.stop();
        check("server stop closes remaining connections", last.disconnectedWithin(3000));
        check("server reports stopped", !server.isRunning());

        System.out.println(failures == 0 ? "\nALL TESTS PASSED" : "\n" + failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
