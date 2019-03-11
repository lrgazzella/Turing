import communication.Body.*;
import communication.Communication;
import communication.Header;
import communication.OPS;
import communication.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;

public class Server {

    private Path basePath;
    private UsersHandler handler;

    public static void main(String[] args) {
        if(args.length != 1){
            usage();
            return;
        }

        try{
            new Server(args[0]).startServer();
        } catch(RemoteException e){
            e.printStackTrace();
        }
    }

    public Server(String path) {
        this.basePath = Paths.get(path);
        this.handler = new UsersHandler();
    }

    public void startServer() throws RemoteException {
        UsersService stub = (UsersService) UnicastRemoteObject.exportObject((UsersService) this.handler, 0);
        Registry registry = LocateRegistry.createRegistry(2048);
        registry.rebind("UsersService", stub);

        Selector selector = null;
        try{
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(5000));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch(IOException e){
            e.printStackTrace();
            return;
        }

        System.out.println("Server started");
        while(true) {
            try {
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = server.accept();
                        System.out.println("Accepting connection from " + clientChannel);
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        System.out.println("New request is coming");
                        SocketChannel client = (SocketChannel)key.channel();
                        Packet p = handleRequest(client);
                        Communication.sendHeader(client, p.getHeader());
                        if(p.getBody() != null) Communication.sendBody(client, p.getBody());
                        System.out.println("Request handled");
                    }
                } catch (Exception e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                    }
                }
            }
        }
    }

    public Packet handleRequest(SocketChannel client) throws IOException {
        Header hdr = Communication.receiveHeader(client);
        System.out.println("Ricevuto header: " + hdr.getOp());
        if(hdr.getOp() == OPS.CREATE){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, CreateBody.class)));
        }else if(hdr.getOp() == OPS.SHARE){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, ShareBody.class)));
        }else if(hdr.getOp() == OPS.SHOWDOC){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, ShowDocBody.class)));
        }else if(hdr.getOp() == OPS.SHOWSEC){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, ShowSecBody.class)));
        }else if(hdr.getOp() == OPS.LIST){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, ListBody.class)));
        }else if(hdr.getOp() == OPS.EDIT){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, EditBody.class)));
        }else if(hdr.getOp() == OPS.ENDEDIT){
            return this.handleCreate(new Packet(hdr, Communication.receiveBody(client, EndEditBody.class)));
        }
        return null;
    }

    public Packet handleCreate(Packet p) throws IOException {
        CreateBody body = (CreateBody)p.getBody();
        User u = this.handler.getUser(body.getUsername());
        if(u.hasDocument(body.getDocumentName())){
            return new Packet(new Header(OPS.ALREADYEXISTS), null);
        }else{
            u.addOwnedDoc(new Document(Paths.get(this.basePath.toString(), body.getDocumentName() + "_" + u.getUsername()), body.getDocumentName(), body.getSectionsNumber(), u));
            return new Packet(new Header(OPS.OK), null);
        }
    }

    public static void usage(){
        System.out.println("usage: server basepath");
    }

}
