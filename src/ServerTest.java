import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;



import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ServerTest {
    public static int round_id = 0;
    public static int numOfClients;
    public static CurrState currState;
    //    public static Synchronizer synchronizer = new Synchronizer(currState);
    public static List<Receiver> receivers = new ArrayList<>();
    //    public static List<Sender> senders = new ArrayList<>();
    public static List<SctpChannel> senders = new ArrayList<>();
    public static int NODE;

    public static String HOST;

    public static HashMap<String, List<String>> NodeMapping = new HashMap<>();
    public static HashMap<String, String> neighborMapping = new HashMap<String, String>(); //neighbor's host name maps to port

    // Port number to open server for clients to connect
    // Client should connect to same port number that server opens
    static int PORT;

    public static void readConfig(int Node) throws FileNotFoundException {
        FileReader file = new FileReader("/home/012/q/qx/qxw170003/AOS_P1/configuration.txt");
        Scanner scanner = new Scanner(file);

        // Get number of Nodes involved
        int count = 0;
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            if (s.charAt(0) == '#') continue;
            count = Integer.valueOf(s);
            break;
        }

        int partOne = count;
        while (partOne > 0) {
            String s = scanner.nextLine();
            if (s.charAt(0) == '#') continue;
            String[] arr = s.split(" ");
            List<String> list = new ArrayList<>();
            list.add(arr[1]);
            list.add(arr[2]);
            NodeMapping.put(arr[0], list);
            partOne--;
        }

        int index = 0;
        while (index < count) {
            String s = scanner.nextLine();
            if (s.charAt(0) == '#') continue;
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
        NODE = Integer.parseInt(args[0]);
        HOST = args[1];
        PORT = Integer.parseInt(args[2]);

        readConfig(NODE);
        currState = new CurrState(0, 0, 0, NODE);
        currState.numberOfNeighbors = neighborMapping.size();
//        currState.senderTracking = new HashMap<>();
        currState.msgLeftToRcv = 0;
        numOfClients = neighborMapping.size();



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
//            Sender senderThread = new Sender(sndSC, currState);
//            senders.add(senderThread);
//            currState.senderTracking.put(senderThread, false);
            System.out.println("Connected to Neighbor");
        }

        int counter = 0;
        int senderCount = 0;
        int recevierCount = 0;
//        System.out.println("Server Started ....");
        while (recevierCount++ < numOfClients) {
//            System.out.println("COUNTER: " + counter);
//            counter++;
            SctpChannel rcvSC = ssc.accept(); // Wait for incoming connection from client
//            System.out.println(" >> " + "Client No:" + counter + " started!");
//            Client connect to Server

            Receiver rcv = new Receiver(rcvSC, counter, currState); //send  the request to a separate thread
            receivers.add(rcv);
        }
        Synchronizer synchronizer = new Synchronizer(currState, senders);

        synchronizer.start();
//        for (Sender sender : senders) sender.start();
        for (Receiver receiver : receivers) receiver.start();

        synchronizer.join();
//        for (Sender sender : senders) sender.join();
        for (Receiver receiver : receivers) receiver.join();

    }
}


class CurrState {
    int NODE;
    int round_id;
    int msgLeftToRcv;
    int msgLeftToSnd;
    int numberOfNeighbors;
//    HashMap<Sender, Boolean> senderTracking;


    public CurrState(int round_id, int msgLeftToRcv, int msgLeftToSnd, int NODE) {
        this.round_id = round_id;
        this.msgLeftToRcv = msgLeftToRcv;
        this.msgLeftToSnd = msgLeftToSnd;
        this.NODE = NODE;
    }
}


//class Sender extends Thread {
//    CurrState currState;
//    SctpChannel sc;
//
//    public Sender(SctpChannel sc, CurrState currState) {
//        this.sc = sc;
//        this.currState = currState;
//    }
//
//    public void run() {
//        while (true) {
//            synchronized (currState) {
//                System.out.println("--Inside SENDER");
//                if (currState.senderTracking.get(this)) {
//                    try {
//                        // System.out.println("Sender Waiting.");
//                        currState.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
//                Message message = new Message("Node " +  currState.NODE  + " Sent a Message at Round " + currState.round_id, currState.round_id, "HostName");
//                try {
//                    sc.send(message.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer
//                    System.out.println(message.message);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                currState.senderTracking.put(this, true);
//                System.out.println("--Leave SENDER");
//            }try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}


class Synchronizer extends Thread {
    private CurrState currState;
    private List<SctpChannel> senders;

    public Synchronizer(CurrState currState, List<SctpChannel> senders) {
        this.currState = currState;
        this.senders = senders;
    }

    public void run() {
        while (true) {
            synchronized (currState) {
//                System.out.println("--Inside SYNCHRONIZER");
                try {
                    Thread.sleep(1000);
                    // System.out.println("Insied Syschronizer ---- currState.msgLeftToRcv: " + currState.msgLeftToRcv);
                    if (currState.msgLeftToRcv == 0) {
                        currState.round_id++;
                        currState.msgLeftToRcv = currState.numberOfNeighbors;
//                        for (Sender s : currState.senderTracking.keySet()) {
//                            currState.senderTracking.put(s, false);
//                        }
                        for(SctpChannel sender : senders){
                            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                            Message message = new Message("Node " +  currState.NODE  + " Sent a Message at Round " + currState.round_id, currState.round_id, "HostName");
                            sender.send(message.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer
                            System.out.println(message.message);
                        }


                        currState.notifyAll();
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                System.out.println("--Leave SYNCHRONIZER");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Receiver extends Thread {
    CurrState currState;

    // Size of ByteBuffer to accept incoming messages
    static int MAX_MSG_SIZE = 4096;
    SctpChannel serverClient;
    int clientNo;

    Receiver(SctpChannel sc, int counter, CurrState currState) {
        this.serverClient = sc;
        this.clientNo = counter;
        this.currState = currState;
    }

    public void run() {
        try {
            while (true) {
                synchronized (currState) {
//                    System.out.println("--Inside RECEIVER");
                    ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
//                    System.out.println(buf);
                    serverClient.receive(buf, null, null); // Messages are received over SCTP using ByteBuffer
//                    System.out.println("After receive");
                    Message msg = Message.fromByteBuffer(buf);
//                    System.out.println("Built Message");
                    int msg_id = msg.round_id;

                    while (msg_id > currState.round_id) {
                        // System.out.println("Receiver Waiting.");
                        currState.wait();
                    }
                    currState.msgLeftToRcv--;
                    System.out.println("Node " + currState.NODE + " Received a Message at round " + msg_id);
//                    System.out.println("--Leave RECEIVER");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            serverClient.close();
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            System.out.println("Client -" + clientNo + " exit!! ");
        }
    }
}