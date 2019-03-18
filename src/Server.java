import com.google.gson.Gson;
import communication.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private Path basePath;
    private UsersHandler handler;
    private ConcurrentHashMap<User, SocketChannel> usersChannel; // Mantiene le associazioni l'utente e il suo SocketChannel in cui vuole ricevere gli inviti
    private Selector selector;
    private int nextPort;

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
        this.selector = null;
        this.usersChannel = new ConcurrentHashMap();
        this.nextPort = 1024;
        Server.deleteFolder(this.basePath.toFile());
    }

    public void startServer() throws RemoteException {
        UsersService stub = (UsersService) UnicastRemoteObject.exportObject((UsersService) this.handler, 0);
        Registry registry = LocateRegistry.createRegistry(2048);
        registry.rebind("UsersService", stub);

        try{
            ServerSocketChannel serverSocketChannelCommunications = ServerSocketChannel.open();
            ServerSocketChannel serverSocketChannelInvitations = ServerSocketChannel.open();
            serverSocketChannelCommunications.socket().bind(new InetSocketAddress(5000));
            serverSocketChannelInvitations.socket().bind(new InetSocketAddress(5001));
            serverSocketChannelCommunications.configureBlocking(false);
            serverSocketChannelInvitations.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannelCommunications.register(selector, SelectionKey.OP_ACCEPT);
            serverSocketChannelInvitations.register(selector, SelectionKey.OP_ACCEPT);
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
                        handleRequest((SocketChannel)key.channel());
                    } else if (key.isWritable()) {
                        Communication.send((SocketChannel)key.channel(), (Packet) key.attachment());
                        ((SocketChannel)key.channel()).register(selector, 0);
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

    private void handleRequest(SocketChannel client) throws IOException {
        Packet pkt = Communication.read(client);
        System.out.println("Request: " + pkt.getHeader().getOp());
        if(pkt.getHeader().getOp() == OPS.CREATE){
            this.handleCreate(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.SHARE){
            this.handleShare(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.SHOWDOC){
            this.handleShowDoc(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.SHOWSEC){
            this.handleShowSec(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.LIST){
            this.handleList(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.EDIT){
            this.handleEdit(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.ENDEDIT){
            this.handleEndEdit(client, pkt);
        }else if(pkt.getHeader().getOp() == OPS.CREATEASSOCIATION){
            this.usersChannel.put(this.handler.getUser(pkt.getBody().getUsername()), client);
            Communication.send(client, new Packet(new Header(OPS.OK), null));
        }
    }

    private void handleEndEdit(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        Section s = d.getSections().get(p.getBody().getSectionNumber() - 1);
        s.endedit();
        Communication.send(client, new Packet(new Header(OPS.OK), null));
        Communication.read(client, s.getPath().toFile(), p.getBody().getBytesNumber());
    }

    private void handleEdit(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null)
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
        else {
            Section s = d.getSections().get(p.getBody().getSectionNumber() - 1);
            if(s.edit()){
                Body b = new Body();
                b.setOther(String.valueOf(d.getPortNumber()));
                b.setBytesNumber(s.getPath().toFile().length());
                Communication.send(client, new Packet(new Header(OPS.OK), b));
                Communication.send(client, s.getPath().toFile());
            }else{
                Communication.send(client, new Packet(new Header(OPS.ALREADYINEDITING), null));
            }
        }
    }

    private void handleList(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Body b = new Body();
        b.setOther(this.buildListString(u));

        Communication.send(client, new Packet(new Header(OPS.OK), b));
    }

    private String buildListString(User u){
        ArrayList<ListMeber> list = new ArrayList<>();
        ArrayList<Document> docs = new ArrayList<>();
        docs.addAll(u.getOwnedDocs().values());
        docs.addAll(u.getCollaborationDocs().values());
        for(Document d: docs){
            ArrayList<String> collaborators = new ArrayList<>();
            for(User collaborator: d.getCollaborators())
                collaborators.add(collaborator.getUsername());
            list.add(new ListMeber(d.getName(), d.getOwner().getUsername(), collaborators));
        }
        return new Gson().toJson(list);
    }

    private void handleShare(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        User collaborator = this.handler.getUser(p.getBody().getCollaborator());

        if(collaborator == null || u.equals(collaborator)){
            Communication.send(client, new Packet(new Header(OPS.NOSUCHUSER), null));
        }else {
            if (!u.isOwner(p.getBody().getDocumentName())) {
                Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
            } else {
                Document d = u.getOwnedDocs().get(p.getBody().getDocumentName());
                d.addCollaborator(collaborator);
                collaborator.addCollaborationDoc(d);
                if (!collaborator.isLogged()) { // utente non online -> aggiungo un invito alla sua lista di inviti non ancora visti
                    collaborator.addInvitation(new Invitation(u.getUsername(), p.getBody().getDocumentName()));
                } else {
                    Body b = new Body();
                    b.setDocumentName(p.getBody().getDocumentName());
                    b.setUsername(u.getUsername());
                    Packet toAdd = new Packet(new Header(OPS.OK), b);

                    SelectionKey newKey = this.usersChannel.get(collaborator).register(selector, SelectionKey.OP_WRITE);
                    newKey.attach(toAdd);
                }
                Communication.send(client, new Packet(new Header(OPS.OK), null));
            }
        }
    }

    private void handleCreate(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        if(u.hasDocument(p.getBody().getDocumentName())){
            Communication.send(client, new Packet(new Header(OPS.ALREADYEXISTS), null));
        }else{
            u.addOwnedDoc(new Document(Paths.get(this.basePath.toString(), p.getBody().getDocumentName() + "_" + u.getUsername()), p.getBody().getDocumentName(), p.getBody().getSectionsNumber(), u, this.nextPort));
            this.nextPort ++;
            Communication.send(client, new Packet(new Header(OPS.OK), null));
        }
    }

    private void handleShowDoc(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null){
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
        }else{
            File toSend = joinFiles(d);
            Body b = new Body();
            b.setBytesNumber(toSend.length());
            b.setOther(new Gson().toJson(getSectionsInEditing(d)));
            Communication.send(client, new Packet(new Header(OPS.OK), b));
            Communication.send(client, toSend);
            toSend.delete();
        }
    }

    private void handleShowSec(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null || d.getSections().size() < p.getBody().getSectionNumber()){
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
        }else{
            Section s = d.getSections().get(p.getBody().getSectionNumber() - 1);
            File toSend = s.getPath().toFile();
            Body b = new Body();
            b.setBytesNumber(toSend.length());
            b.setOther(new Gson().toJson(s.getInEditing()));
            Communication.send(client, new Packet(new Header(OPS.OK), b));
            Communication.send(client, toSend);
        }
    }

    private ArrayList<Integer> getSectionsInEditing(Document d){
        ArrayList<Integer> r = new ArrayList<>();
        for(int i=0; i<d.getSections().size(); i++){
            if(d.getSections().get(i).getInEditing()){
                r.add(i+1);
            }
        }
        return r;
    }

    private File joinFiles(Document d) throws IOException {
        Path pathNewFile = Paths.get(d.getPath().toString(), "tmp" + new Timestamp(System.currentTimeMillis()).getTime());
        File f = new File(pathNewFile.toString());
        f.createNewFile();
        for (Section s : d.getSections()) {
            List<String> lines = Files.readAllLines(s.getPath(), StandardCharsets.UTF_8);
            Files.write(pathNewFile, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        return f;
    }

    private static void deleteFolder(File folder){
        File[] files = folder.listFiles();
        if(files != null) {
            for(File f: files) {
                if(f.isDirectory())
                    deleteFolder(f);
                f.delete();
            }
        }
    }

    private static void usage(){
        System.out.println("usage: server basepath");
    }

}
