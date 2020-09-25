import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import java.net.*;
import java.nio.ByteBuffer;

public class ServerTest {
    public static int round_id = 0;
    public static int numOfClients = 0;
    public static int numOfExecutedMsg = 0;
    // Port number to open server for clients to connect
    // Client should connect to same port number that server opens
    static int PORT = 8888;

    public static void main(String[] args) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number
        SctpServerChannel ssc = SctpServerChannel.open();//Open server channel
        ssc.bind(addr);//Bind server channel to address
        try{
            int counter=0;
            System.out.println("Server Started ....");
            while(true){
                counter++;
                numOfClients = counter;
                SctpChannel sc = ssc.accept(); // Wait for incoming connection from client
                System.out.println(" >> " + "Client No:" + counter + " started!");
                ClientThread sct = new ClientThread(sc, counter); //send  the request to a separate thread
                sct.start();
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }
}

class Server extends Thread{
    ServerTest st;

    public Server(ServerTest st){
        this.st = st;
    }

    public void run(){
        try{
            
        }
    }
}

class ClientThread extends Thread {
    // Size of ByteBuffer to accept incoming messages
    static int MAX_MSG_SIZE = 4096;
    SctpChannel serverClient;
    int clientNo;
    ClientThread(SctpChannel sc, int counter){
        serverClient = sc;
        clientNo=counter;
    }
    public void run(){
        try{
            while(true){
                ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
                serverClient.receive(buf, null, null); // Messages are received over SCTP using ByteBuffer
                Message msg = Message.fromByteBuffer(buf);
                int msg_id = msg.round_id;
                String hostName = msg.hostName;

                if(Server.round_id < msg_id) this.sleep(500);

                Server.numOfExecutedMsg++;
                if(Server.numOfExecutedMsg == Server.numOfClients) {
                    Server.round_id++;
                }


                System.out.println("Client with hostname " + hostName + " sent a message at round " + msg_id);




                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                Message reply = new Message("Server: Message received on round " + msg_id, 0, hostName);
                serverClient.send(reply.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer

                System.out.println("Message sent to client: " + reply.message);
            }
//            serverClient.close();
        }catch(Exception ex){
            System.out.println(ex);
        }finally{
            System.out.println("Client -" + clientNo + " exit!! ");
        }
    }
}