/*import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ChatHandler extends Thread {

    public final int LENGTH = 512;

    //private String owner;
    private InetAddress group;
    private int port;
    private MulticastSocket ms;
    //private ArrayList<Message> messages;

    public ChatHandler(String owner, int port) throws IOException {
        //this.owner = owner;
        this.group = InetAddress.getByName("239.255.0.1");
        this.ms = new MulticastSocket(port);
        this.ms.joinGroup(group);
        //this.messages = new ArrayList<>();
    }

    @Override
    public void run() {
        System.out.println("Started");
        byte[] buf = new byte [2200];
        while (true) {
            DatagramPacket packet = new DatagramPacket (buf, buf.length);
            try {
                ms.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Ricevuto");
        }
    }

    public void sendMessage(Message m) throws IOException {
        String a = "MESSAGGIO";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeBytes(a);
        out.close();

        byte[] buf = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
        this.ms.send(packet);
    }

    /*public void printReveivedMessage(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for(Message m: this.messages){
            System.out.println(formatter.format(m.getDate()) + " " + m.getUsername() + ": " + m.getText());
        }
        this.messages.clear();
    }

}*/


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ChatHandler extends Thread {

    private String owner;
    private ArrayList<Message> history;
    private InetAddress group;
    private int port;
    private MulticastSocket ms;

    public ChatHandler(String owner, int port) throws IOException {
        this.group = InetAddress.getByName("239.255.0.1");
        this.port = port;
        this.ms = new MulticastSocket(port);
        this.ms.joinGroup(group);
        this.owner = owner;
        this.history = new ArrayList<>();
    }

    @Override
    public void run() {
        byte[] buf = new byte [2200];
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket (buf, buf.length);
            try {
                ms.receive(packet);
                this.history.add(Message.fromBytesToMessage(packet.getData()));
            } catch (IOException | ClassNotFoundException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendMessage(Message message) throws IOException {
        byte[] buf = Message.fromMessageToBytes(message);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
        this.ms.send(packet);
    }

    public void printReveivedMessage(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for(Message m: this.history){
            System.out.println(formatter.format(m.getDate()) + " " + m.getUsername() + ": " + m.getText());
        }
        this.history.clear();
    }

    public void stopChat() {
        this.interrupt();
        try {
            this.ms.leaveGroup(this.group);
            this.ms.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
