import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;


import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ServerTest {
    public static int round_id = 0;
    public static int numOfClients = 2;
    public static CurrState currState = new CurrState(round_id, numOfClients);
    public static Server server = new Server(currState);
    public static List<ClientThread> clients = new ArrayList<>();

    // Port number to open server for clients to connect
    // Client should connect to same port number that server opens
    static int PORT = 8888;


    public static void main(String[] args) throws Exception {
//        server.start();
//        server.join();
        InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number
        SctpServerChannel ssc = SctpServerChannel.open();//Open server channel
        ssc.bind(addr);//Bind server channel to address
        int counter = 0;
        System.out.println("Server Started ....");
        while (counter < numOfClients) {
//        while (counter < numOfClients) {
            System.out.println("COUNTER: " + counter);
            counter++;
//            numOfClients = counter;
            SctpChannel sc = ssc.accept(); // Wait for incoming connection from client
            System.out.println(" >> " + "Client No:" + counter + " started!");
            ClientThread sct = new ClientThread(sc, counter, currState); //send  the request to a separate thread
            clients.add(sct);
        }


        for (ClientThread client : clients) client.start();
        server.start();


        for (ClientThread client : clients) client.join();
        server.join();

    }
}


class CurrState {
    int round_id;
    int msgLeftForCurrRound;

    public CurrState(int round_id, int msgLeftForCurrRound) {
        this.round_id = round_id;
        this.msgLeftForCurrRound = msgLeftForCurrRound;
    }
}

//class ServerClient {
//    CurrState currState;
//
//    public ServerClient (CurrState currState){
//        this.currState = currState;
//    }
//
//    public synchronized void incomeMsg() throws Exception{
//
//    }
//
//}

class Server extends Thread {
    private CurrState currState;

    public Server(CurrState currState) {
        this.currState = currState;
    }

    public void run() {


        while (true) {
            synchronized (currState) {

                try {
                    Thread.sleep(1000);
//                    System.out.println(currState.msgLeftForCurrRound);
                    if (currState.msgLeftForCurrRound == 0) {
                        currState.round_id++;
                        // HARD CODE, need change later
                        currState.msgLeftForCurrRound = 2;
                        System.out.println("RESET LeftMSG");
                        currState.notifyAll();
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            System.out.println("Out of Synchronize");
        }
    }
}

class ClientThread extends Thread {
    CurrState currState;


    // Size of ByteBuffer to accept incoming messages
    static int MAX_MSG_SIZE = 4096;
    SctpChannel serverClient;
    int clientNo;

    ClientThread(SctpChannel sc, int counter, CurrState currState) {
        this.serverClient = sc;
        this.clientNo = counter;
        this.currState = currState;
    }

    public void run() {
        try {
            while (true) {
                synchronized (currState) {

                    ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
                    serverClient.receive(buf, null, null); // Messages are received over SCTP using ByteBuffer
                    System.out.println("-------------------------");
                    Message msg = Message.fromByteBuffer(buf);
                    int msg_id = msg.round_id;
//                String hostName = msg.hostName;

                    while (msg_id > currState.round_id) {

                        System.out.println("Round_ID: " + currState.round_id);
                        System.out.println("MSG_ID: " + msg_id);
                        currState.wait();

                        System.out.println("Waiting");
                    }

                    currState.msgLeftForCurrRound--;


                    System.out.println("Client with hostname " + "hostName" + " sent a message at round " + msg_id);

//                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
//                Message reply = new Message("Server: Message received on round " + msg_id, 0, hostName);
//                serverClient.send(reply.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer
//
//                System.out.println("Message sent to client: " + reply.message);
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