package turing;

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
    private ConcurrentHashMap<User, SocketChannel> usersChannel; // Mantiene l'associazione tra il singolo client e il relativo SocketChannel su cui vuole ricevere gli inviti
    private Selector selector;
    private int nextPort;

    public static void main(String[] args) {
        if(args.length != 1){
            usage();
            return;
        }

        try{
            new Server(args[0]).startServer(); // Avvio il server
        } catch(RemoteException e){
            e.printStackTrace();
        }
    }

    public Server(String path) {
        this.basePath = Paths.get(path);
        this.handler = new UsersHandler();
        this.selector = null;
        this.usersChannel = new ConcurrentHashMap();
        this.nextPort = 1024; // Prima porta disponibile da assegnare a un documento
        Server.deleteFolder(this.basePath.toFile()); // Elimino tutto quello che conteneva la cartella basePath
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
            serverSocketChannelCommunications.register(selector, SelectionKey.OP_ACCEPT); // Registro nella select il SocketChannel su cui riceverò messaggi
            serverSocketChannelInvitations.register(selector, SelectionKey.OP_ACCEPT); // Registro nella select il SocketChannel che utilizzerò per mandare inviti
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
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ); // Quando mi arriva una connessione da parte di un client, questa la registro sulla select con OP_READ
                    } else if (key.isReadable()) {
                        handleRequest((SocketChannel)key.channel()); // Vuol dire che un client mi ha inviato una richiesta, allora passo a gestirla
                    } else if (key.isWritable()) {
                        Communication.send((SocketChannel)key.channel(), (Packet) key.attachment()); // TODO
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
        }else if(pkt.getHeader().getOp() == OPS.CREATEASSOCIATION){ // In questo caso il SocketChannel del client è quello su cui vorrà ricevere gli inviti
            this.usersChannel.put(this.handler.getUser(pkt.getBody().getUsername()), client);
            Communication.send(client, new Packet(new Header(OPS.OK), null));
        }
    }

    private void handleEndEdit(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        Section s = d.getSections().get(p.getBody().getSectionNumber() - 1); // Prendo la sezione che stava modificando
        Communication.send(client, new Packet(new Header(OPS.OK), null));
        Communication.read(client, s.getPath().toFile(), p.getBody().getBytesNumber()); // Leggo il file aggiornato
        s.endedit(); // Chiamo la endedit() per dire che può essere modificata da altri utenti. La chiamo dopo aver ricevuto il file altrimento potrebbe succedere che un altro utente entra in modifica di un file non aggiornato
    }

    private void handleEdit(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null)
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null)); // L'utente stava cercando di modificare una sezione di un documento inesistente
        else {
            Section s = d.getSections().get(p.getBody().getSectionNumber() - 1);
            if(s.edit()){
                Body b = new Body();
                b.setOther(String.valueOf(d.getPortNumber()));
                b.setBytesNumber(s.getPath().toFile().length());
                Communication.send(client, new Packet(new Header(OPS.OK), b));
                Communication.send(client, s.getPath().toFile()); // Invio la versione più aggiornata del file che vuole modificare
            }else{ // Se s.edit() torna falso vuol dire che c'è già un altro utente che sta modificando questa sezione
                Communication.send(client, new Packet(new Header(OPS.ALREADYINEDITING), null));
            }
        }
    }

    private void handleList(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Body b = new Body();
        b.setOther(this.buildListString(u)); // Creo la lista che voglio inviare e la invio

        Communication.send(client, new Packet(new Header(OPS.OK), b));
    }

    private String buildListString(User u){
        ArrayList<ListMember> list = new ArrayList<>();
        ArrayList<Document> docs = new ArrayList<>();
        docs.addAll(u.getOwnedDocs().values());
        docs.addAll(u.getCollaborationDocs().values());
        for(Document d: docs){
            ArrayList<String> collaborators = new ArrayList<>();
            for(User collaborator: d.getCollaborators())
                collaborators.add(collaborator.getUsername());
            list.add(new ListMember(d.getName(), d.getOwner().getUsername(), collaborators));
        }
        return new Gson().toJson(list);
    }

    private void handleShare(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        User collaborator = this.handler.getUser(p.getBody().getCollaborator());

        if(collaborator == null || u.equals(collaborator)){ // Se non esiste alcun utente con quel username o se sta cercando di condividere un file con se stesso ritorno l'errore NOSUCHUSER
            Communication.send(client, new Packet(new Header(OPS.NOSUCHUSER), null));
        }else {
            if (!u.isOwner(p.getBody().getDocumentName())) { // Se non sono l'owner di quel documento non posso condividerlo
                Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
            } else {
                Document d = u.getOwnedDocs().get(p.getBody().getDocumentName());
                d.addCollaborator(collaborator); // Aggiungo l'utente ai collaboratori
                collaborator.addCollaborationDoc(d);
                if (!collaborator.isLogged()) { // Se il collaboratore appena aggiunto non è online, aggiungo un invito alla sua lista di inviti pendenti
                    collaborator.addInvitation(new Invitation(u.getUsername(), p.getBody().getDocumentName()));
                } else { // Altimenti glielo comunico subito tramite il SocketChannel salvato all'inizio
                    Body b = new Body();
                    b.setDocumentName(p.getBody().getDocumentName());
                    b.setUsername(u.getUsername());
                    Packet toAdd = new Packet(new Header(OPS.OK), b);

                    SelectionKey newKey = this.usersChannel.get(collaborator).register(selector, SelectionKey.OP_WRITE);
                    newKey.attach(toAdd);
                }
                Communication.send(client, new Packet(new Header(OPS.OK), null)); // Comunico all'utente che ha condiviso il file che l'operazione è andata a buon fine
            }
        }
    }

    private void handleCreate(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        if(u.hasDocument(p.getBody().getDocumentName())){ // Se ha già un documento con quel nome
            Communication.send(client, new Packet(new Header(OPS.ALREADYEXISTS), null));
        }else{
            // Creo il documento e lo aggiungo alla lista dei documenti di cui l'utente è owner
            u.addOwnedDoc(new Document(Paths.get(this.basePath.toString(), p.getBody().getDocumentName() + "_" + u.getUsername()), p.getBody().getDocumentName(), p.getBody().getSectionsNumber(), u, this.nextPort));
            this.nextPort ++; // Aggiorno la nextPort
            Communication.send(client, new Packet(new Header(OPS.OK), null));
        }
    }

    private void handleShowDoc(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null){ // Se non c'è alcun documento con quel nome ritorno un messaggio di errore
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
        }else{
            File toSend = joinFiles(d); // Unisco tutte le sezioni di quel documento
            Body b = new Body();
            b.setBytesNumber(toSend.length());
            b.setOther(new Gson().toJson(getSectionsInEditing(d))); // Prendo la lista di tutte le sezioni in modifica
            Communication.send(client, new Packet(new Header(OPS.OK), b));
            Communication.send(client, toSend); // Lo invio
            toSend.delete(); // Elimino il file temporaneo
        }
    }

    private void handleShowSec(SocketChannel client, Packet p) throws IOException {
        User u = this.handler.getUser(p.getBody().getUsername());
        Document d = u.getDocument(p.getBody().getDocumentName());
        if(d == null || d.getSections().size() < p.getBody().getSectionNumber()){ // Se non esiste un documento con quel nome o se sta cercando di scaricare una sezione che non esiste, ritorno un messaggio di errore
            Communication.send(client, new Packet(new Header(OPS.NOSUCHFILE), null));
        }else{
            Section s = d.getSections().get(p.getBody().getSectionNumber() - 1);
            File toSend = s.getPath().toFile();
            Body b = new Body();
            b.setBytesNumber(toSend.length());
            b.setOther(new Gson().toJson(s.getInEditing()));
            Communication.send(client, new Packet(new Header(OPS.OK), b));
            Communication.send(client, toSend); // Invio il file
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
        for (Section s : d.getSections()) { // Ogni sezione la scrivo sul file temporaneo
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
