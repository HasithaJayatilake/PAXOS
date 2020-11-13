import java.io.*;
import java.net.*;


public class Server{
    private InetAddress group;
    private int port;

    public Server(InetAddress group, int port) throws Exception {
        this.group = group;
        this.port = port;
    }

    void sendMessage(String message, String messageType, PeerThread peerThread) {
        peerThread.getPeer().outgoingMessageCount ++;

        try {

            // Reset prepare-ok count every time a proposal is sent
            if (messageType.equals("proposal")){
                peerThread.setPrepareOkCount(0);
            }

            // Reset accept-ok count each time a new accept-request is sent
            if (messageType.equals("acceptRequest")){
                peerThread.setAcceptOkCount(0);
            }

            DatagramSocket socket = new DatagramSocket();
            byte[] msg = message.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, this.group, this.port);
            socket.send(packet);
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InetAddress getGroup() {
        return group;
    }

    public int getPort() {
        return port;
    }

}
