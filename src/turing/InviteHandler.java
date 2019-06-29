package turing;

import communication.Communication;
import communication.OPS;
import communication.Packet;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class InviteHandler extends Thread {

    public SocketChannel c;

    public InviteHandler(SocketChannel c){
        this.c = c;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Packet pkt = Communication.read(c); // Aspetto di ricevere un Packet, ovvero un invito
                if (pkt.getHeader().getOp() == OPS.OK) { // Quando arriva lo comunico all'utente
                    System.out.println("  ---> " + pkt.getBody().getUsername() + " ti ha invitato a collaborare al documento: " + pkt.getBody().getDocumentName());
                }
            } catch (IOException e) {
                try {
                    c.finishConnect();
                } catch (IOException e1) {
                    Thread.currentThread().interrupt();
                }
                Thread.currentThread().interrupt();
            }
        }
    }
}
