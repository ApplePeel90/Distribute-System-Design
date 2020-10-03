import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;


import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ServerTest {
    public static CurrState currState;
    public static List<Receiver> receivers = new ArrayList<>();
    public static List<SctpChannel> senders = new ArrayList<>();
    public static int NODE; //Current Node's ID
    public static int totoalNumOfNodes;
    public static String HOST;

    public static HashMap<String, List<String>> NodeMapping = new HashMap<>();
    public static HashMap<String, String> neighborMapping = new HashMap<String, String>(); //neighbor's host name maps to port

    // Port number to open server for clients to connect
    // Client should connect to same port number that server opens
    static int PORT;

    /**
     * Read the configuration file from each node
     * @param Node, current node's id
     * @throws FileNotFoundException
     */
    public static void readConfig(int Node) throws FileNotFoundException {
        FileReader file = new FileReader("/home/012/q/qx/qxw170003/AOS_P1/configuration.txt");
        Scanner scanner = new Scanner(file);

        // Get number of Nodes involved
        int count = 0;
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            if (s.length() == 0 || !Character.isDigit(s.charAt(0))) continue;
            count = Integer.valueOf(s);
            break;
        }
        totoalNumOfNodes = count;

        // Read Node, ID, HostName
        int partOne = count;
        while (partOne > 0) {
            String s = scanner.nextLine();
            if (s.length() == 0 || !Character.isDigit(s.charAt(0))) continue;
            String[] arr = s.split(" ");
            List<String> list = new ArrayList<>();
            list.add(arr[1]);
            list.add(arr[2]);
            NodeMapping.put(arr[0], list);
            partOne--;
        }

        // Read Neighbors
        int index = 0;
        while (index < count) {
            String s = scanner.nextLine();
            if (s.length() == 0 || !Character.isDigit(s.charAt(0))) continue;
            if (index == NODE) {
                if (s.contains("#")) s = s.substring(0, s.indexOf("#"));
                String[] arr = s.split(" ");
                for (String i : arr) {
                    neighborMapping.put(NodeMapping.get(i).get(0), NodeMapping.get(i).get(1));
                }
                break;
            }
            index++;
        }

    }

    public static void main(String[] args) throws Exception {
        NODE = Integer.parseInt(args[0]);  // Current node's id
        HOST = args[1];  // Current node's hostname
        PORT = Integer.parseInt(args[2]);  // Current node's openning port

        readConfig(NODE);

        HashMap<Receiver, Boolean> receiverTracker = new HashMap<>();
        List<HashSet<Integer>> hoppingNeighbors = new ArrayList<>();
        HashSet<Integer> reached = new HashSet<>();
        HashSet<Integer> zeroHop = new HashSet<>();
        zeroHop.add(NODE);
        hoppingNeighbors.add(zeroHop);
        reached.add(NODE);

        currState = new CurrState(0, 0, NODE, totoalNumOfNodes, neighborMapping.size(), receiverTracker, hoppingNeighbors, reached);

        InetSocketAddress openAddr = new InetSocketAddress(PORT); // Get address from port number
        SctpServerChannel ssc = SctpServerChannel.open();//Open server channel
        ssc.bind(openAddr);//Bind server channel to address
        System.out.println("Started Listenning ...");
        // Sleep to ensure all servers are listenning
        Thread.sleep(5000);
        // Connect to neighbors
        for (String neighborName : neighborMapping.keySet()) {
            InetSocketAddress addr = new InetSocketAddress(neighborName, Integer.valueOf(neighborMapping.get(neighborName)));
            System.out.println(addr.getHostName() + " " + addr.getPort());
            SctpChannel sndSC = SctpChannel.open(addr, 0, 0);
            senders.add(sndSC);
            System.out.println("Connected to Neighbor");
        }

        int recevierCount = 0;
        while (recevierCount++ < currState.numberOfNeighbors) {
            SctpChannel rcvSC = ssc.accept(); // Wait for incoming connection from client

            Receiver rcv = new Receiver(rcvSC, currState); // Create a Receiver thread to handle incoming messages for each client
            currState.receiverTracker.put(rcv, true);
            receivers.add(rcv);
        }
        Synchronizer synchronizer = new Synchronizer(currState, senders);

        synchronizer.start();
        for (Receiver receiver : receivers) receiver.start();

        synchronizer.join();
        for (Receiver receiver : receivers) receiver.join();


//        int eccentricity = 0;
//        for(int i = 0; i < totoalNumOfNodes; i++){
//            System.out.print(i + " Hopping Neighbors: ");
//            if(hoppingNeighbors.get(i).size() != 0) eccentricity = i;
//            for(int j : hoppingNeighbors.get(i)){
//                System.out.print(j + " ");
//            }
//            System.out.println();
//        }
//        System.out.println("Eccentricity: " + eccentricity);
//        Thread.sleep(5000);



        // Write the result to files
        BufferedWriter writer = new BufferedWriter(new FileWriter(("congiguration-" + NODE+".dat")));
        int eccentricity = 0;
        for(int i = 0; i < totoalNumOfNodes; i++){
            if(hoppingNeighbors.get(i).size() != 0) eccentricity = i;
            for(int j : hoppingNeighbors.get(i)){
                writer.write(j+" ");
            }
            writer.write("\n");
        }
        writer.write(eccentricity+"");
        writer.close();
    }
}

/**
 * Object to broadcast messages to neighbors
 * Check whether the node could move to next round and notify all the waiting Receiver thread
 */
class Synchronizer extends Thread {
    private CurrState currState;
    private List<SctpChannel> senders;

    public Synchronizer(CurrState currState, List<SctpChannel> senders) {
        this.currState = currState;
        this.senders = senders;
    }

    public void run() {
        while (currState.round_id <= currState.totoalNumOfNodes) {
            synchronized (currState) {
                try {
                    if (currState.msgLeftToRcv == 0) {
                        currState.round_id++;
                        currState.msgLeftToRcv = currState.numberOfNeighbors;

                        // When entering a new round, send a message to each neighbor
                        for(SctpChannel sender : senders){
                            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                            Message message = new Message("Node " +  currState.NODE  + " Sent a Message at Round " + currState.round_id, currState.round_id, currState.NODE, currState.hoppingNeighbors);
                            sender.send(message.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer
                            System.out.println(message.message);
                        }
                        currState.hoppingNeighbors.add(new HashSet<Integer>());
                        for(Receiver rcv : currState.receiverTracker.keySet()) currState.receiverTracker.put(rcv, false);
                        currState.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Object to handle incoming messages from neighbors
 * Each neighbor will be allocated one Receiver thread
 */
class Receiver extends Thread {
    CurrState currState;

    // Size of ByteBuffer to accept incoming messages
    static int MAX_MSG_SIZE = 4096;
    SctpChannel serverClient;

    Receiver(SctpChannel sc, CurrState currState) {
        this.serverClient = sc;
        this.currState = currState;
    }

    public void run() {
        try {
            while (currState.round_id <= currState.totoalNumOfNodes) {
                synchronized (currState) {
                    if(currState.receiverTracker.get(this)) {
                        currState.wait();
                    }
                    ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);

                    serverClient.receive(buf, null, null); // Messages are received over SCTP using ByteBuffer

                    Message msg = Message.fromByteBuffer(buf);
                    int msg_id = msg.round_id;
                    HashSet<Integer> nextHop = msg.hoppingNeighbors.get(msg.hoppingNeighbors.size() - 1);

                    // Add the current node to hopping neighbors if the node has not been reach before
                    for(int node : nextHop){
                        if(!currState.reached.contains(node)) {
                            currState.hoppingNeighbors.get(currState.hoppingNeighbors.size() - 1).add(node);
                            currState.reached.add(node);
                        }
                    }
                    currState.receiverTracker.put(this, true);
                    currState.msgLeftToRcv--;

                    System.out.println("Node " + currState.NODE + " Received a Message at round " + msg_id + " from Node " + msg.srcNode);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}

/**
 * Object to be synchronized between Receiver thread and Synchronizer thread
 * CurrState contains the node's current running state
 */
class CurrState {
    int NODE;
    int round_id;
    int msgLeftToRcv;
    int numberOfNeighbors;
    int totoalNumOfNodes;
    HashMap<Receiver, Boolean> receiverTracker;
    List<HashSet<Integer>> hoppingNeighbors;
    HashSet<Integer> reached;


    public CurrState(int round_id, int msgLeftToRcv, int NODE, int totoalNumOfNodes, int numberOfNeighbors, HashMap<Receiver, Boolean> receiverTracker, List<HashSet<Integer>> hoppingNeighbors, HashSet<Integer> reached) {
        this.round_id = round_id;
        this.msgLeftToRcv = msgLeftToRcv;
        this.NODE = NODE;
        this.totoalNumOfNodes = totoalNumOfNodes;
        this.numberOfNeighbors = numberOfNeighbors;
        this.receiverTracker = receiverTracker;
        this.hoppingNeighbors = hoppingNeighbors;
        this.reached = reached;
    }
}