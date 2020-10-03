//import com.sun.nio.sctp.MessageInfo;
//import com.sun.nio.sctp.SctpChannel;
//import java.net.InetAddress;
//
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.net.Socket;
//
//import java.net.*;
//import java.io.*;
//public class Client {
//    public static int round_id = 0;
//    public static String hostName;
//
//    public static Message message = new Message("", round_id, hostName);
//    // PORT to connect to server
//    // Port number should be same as the port opened by the server
//    static int PORT = 8888;
//
//
//
//    // Size of ByteBuffer to accept incoming messages
//    static int MAX_MSG_SIZE = 4096;
//
//    public static void main(String[] args) throws Exception {
//        try{
//            // Get address of server using name and port number
//            InetSocketAddress addr = new InetSocketAddress("localhost", PORT);
//            SctpChannel sc = SctpChannel.open(addr, 0, 0); // Connect to server using the address
//            System.out.println("Connected to Server");
//            while(true){
//                Thread.sleep(2000);
//                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
//                sc.send(message.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer
//                message.round_id++;
//                Thread.sleep(2000);
//            }
//        }catch(Exception e){
//            System.out.println(e);
//        }
//    }
//}
//
//
//
//
//
//
//
////    Socket socket=new Socket("localhost",8888);
////    DataInputStream inStream=new DataInputStream(socket.getInputStream());
////    DataOutputStream outStream=new DataOutputStream(socket.getOutputStream());
////
////    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
////    String clientMessage="",serverMessage="";
////            while(!clientMessage.equals("bye")){
////                    System.out.println("Enter number :");
////                    clientMessage=br.readLine();
////                    outStream.writeUTF(clientMessage);
////                    outStream.flush();
////                    serverMessage=inStream.readUTF();
////                    System.out.println(serverMessage);
////                    }
