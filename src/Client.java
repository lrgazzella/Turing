import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.*;
import exception.AlreadyLoggedIn;
import exception.NotAuthorized;
import exception.UserNotRegistered;
import exception.UsernameAlreadyRegistered;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

public class Client {

    private STATE state;
    private User currentUser;
    private SocketChannel socketChannel;
    private ChatHandler ch;
    private InviteHandler ih;
    private String documentNameInEditng;
    private Integer sectionNumberInEditing;
    private File inEditing;
    private Path basePath;

    public static void main(String[] args){
        try{
            if(args.length != 1){
                usage();
                return;
            }
            new Client(args[0]).startClient();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Client(String basePath){
        this.basePath = Paths.get(basePath);
        this.socketChannel = null;
        this.state = STATE.STARTED;
        this.currentUser = null;
        this.ch = null;
        this.documentNameInEditng = null;
        this.sectionNumberInEditing = null;
        this.inEditing = null;
        this.ih = null;
    }

    public void startClient() throws IOException, ClassNotFoundException {
        Scanner keyboard = new Scanner(System.in);

        while(true){
            System.out.println("$: ");
            String command = keyboard.nextLine();
            String [] commandList = command.split(" ");
            if(commandList[0].equals("exit")) break;
            handleCommand(commandList);
        }

        keyboard.close();
    }

    public void handleCommand(String[] args) throws IOException, ClassNotFoundException {
        switch (args[0]) {
            case "register":
                if(this.state == STATE.STARTED){
                    if(args.length < 3) {
                        usage();
                        return;
                    }
                    String usernameTmp = args[1];
                    String password = args[2];
                    this.handleRegister(usernameTmp, password);
                } else System.out.println("Errore: Non puoi registrarti mentre sei loggato") ;
                break;
            case "login":
                if(this.state == STATE.STARTED){
                    if(args.length < 3) {
                        usage();
                        return;
                    }
                    String usernameTmp = args[1];
                    String password = args[2];
                    handleLogin(usernameTmp, password);
                } else System.out.println("Errore. Sei già loggato");
                break;
            case "logout":
                if(this.state == STATE.LOGGED){
                    this.handleLogout();
                }else System.out.println("Errore: Non sei loggato");
                break;
            case "create":
                if(this.state == STATE.LOGGED){
                    if(args.length != 3){
                        usage();
                        return;
                    }

                    String documentName = args[1];
                    int sectionsNumber;
                    try{
                        sectionsNumber = Integer.parseInt(args[2]);
                    }catch(NumberFormatException e){
                        usage();
                        return;
                    }
                    this.handleCreate(documentName, sectionsNumber);
                }else System.out.println("Errore: Non sei loggato");
                break;
            case "share":
                if(this.state == STATE.LOGGED){
                    if(args.length != 3){
                        usage();
                        return;
                    }

                    String documentName = args[1];
                    String collaborator = args[2];

                    this.handleShare(documentName, collaborator);
                }else System.out.println("Errore: Non sei loggato");
                break;
            case "show":
                if(this.state == STATE.LOGGED){
                    if(args.length != 3 && args.length != 2){
                        usage();
                        return;
                    }

                    String documentName = args[1];
                    if(args.length == 2){
                        this.handleShowDoc(documentName);
                    }else{
                        int sectionNumber;
                        try{
                            sectionNumber = Integer.parseInt(args[2]);
                        }catch(NumberFormatException e){
                            usage();
                            return;
                        }
                        this.handleShowSec(documentName, sectionNumber);
                    }
                }
                break;
            case "list":
                if(this.state == STATE.LOGGED){
                    handleList();
                } else System.out.println("Errore: Non sei loggato");
                break;
            case "edit":
                if(this.state == STATE.LOGGED){
                    if(args.length != 3){
                        usage();
                        return;
                    }

                    String documentName = args[1];
                    int sectionNumber;
                    try{
                        sectionNumber = Integer.parseInt(args[2]);
                    }catch(NumberFormatException e){
                        usage();
                        return;
                    }

                    this.handleEdit(documentName, sectionNumber);
                } else {
                    if(this.state == STATE.EDIT){
                        System.out.println("Errore: Stai già modificando una sezione");
                    }else System.out.println("Errore: Non sei loggato");
                }
                break;
            case "end-edit":
                if(this.state == STATE.EDIT){
                    if(args.length != 1){
                        usage();
                        return;
                    }

                    this.handleEndEdit();
                } else System.out.println("Errore: Non sei in editing");
                break;
            case "send":
                if(this.state == STATE.EDIT){
                    if(args.length != 2){
                        usage();
                        return;
                    }

                    this.ch.sendMessage(new Message(this.currentUser.getUsername(), args[1], new Date()));
                    System.out.println("Messaggio inviato");
                } else System.out.println("Errore: Non sei in editing");
                break;
            case "receive":
                if(this.state == STATE.EDIT){
                    if(args.length != 1){
                        usage();
                        return;
                    }
                    this.ch.printReveivedMessage();
                } else System.out.println("Errore: Non sei in editing");
                break;
            default:
                usage();
                break;
        }
    }

    public void handleEndEdit() throws IOException {
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setDocumentName(this.documentNameInEditng);
        b.setSectionNumber(this.sectionNumberInEditing);
        b.setBytesNumber(this.inEditing.length());

        Communication.send(this.socketChannel, new Packet(new Header(OPS.ENDEDIT), b));
        Packet pkt = Communication.read(this.socketChannel);
        if(pkt.getHeader().getOp() == OPS.OK){
            Communication.send(this.socketChannel, this.inEditing);
            this.state = STATE.LOGGED;
            this.inEditing.delete();
            this.inEditing = null;
            this.ch.interrupt();
            System.out.println("Sezione " + this.sectionNumberInEditing + " del documento " + this.documentNameInEditng + " aggiornata con successo");
            this.documentNameInEditng = null;
            this.sectionNumberInEditing = null;
        }else System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
    }

    public void handleEdit(String documentName, int sectionNumber) throws IOException, ClassNotFoundException{
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setDocumentName(documentName);
        b.setSectionNumber(sectionNumber);

        Communication.send(this.socketChannel, new Packet(new Header(OPS.EDIT), b));
        Packet pkt = Communication.read(this.socketChannel);
        if(pkt.getHeader().getOp() == OPS.OK){
            this.state = STATE.EDIT;
            this.documentNameInEditng = documentName;
            this.sectionNumberInEditing = sectionNumber;
            this.inEditing = new File(String.valueOf(Paths.get(this.basePath.toString(), documentName + sectionNumber + ".txt")));
            Communication.read(socketChannel, this.inEditing, pkt.getBody().getBytesNumber());
            this.ch = new ChatHandler(this.currentUser.getUsername(), Integer.parseInt(pkt.getBody().getOther()));
            this.ch.start();
            System.out.println("Sezione " + sectionNumber + " del documento " + documentName + " scaricata con successo");
        }else System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
    }

    public void handleList() throws IOException, ClassNotFoundException {
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        Communication.send(this.socketChannel, new Packet(new Header(OPS.LIST), b));
        Packet pkt = Communication.read(this.socketChannel);

        if(pkt.getHeader().getOp() == OPS.OK){
            ArrayList<ListMeber> list = new Gson().fromJson(pkt.getBody().getOther(), new TypeToken<ArrayList<ListMeber>>(){}.getType());
            for(ListMeber m: list){
                System.out.println(m.getDocumentName() + ": ");
                System.out.println("  Creatore: " + m.getOwner());
                Iterator i = m.getCollaborators().iterator();
                if(i.hasNext()) System.out.print("  Collaboratori: ");
                while(i.hasNext()){
                    System.out.print(i.next());
                    if(i.hasNext()) System.out.print(", ");
                    if(!i.hasNext()) System.out.println();
                }
            }
        }else System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
    }

    public void handleRegister(String user, String pw) throws RemoteException {
        Registry r = LocateRegistry.getRegistry(2048);
        UsersService usersServer;
        try {
            usersServer = (UsersService) r.lookup("UsersService");
            usersServer.register(new User(user, pw));
            System.out.println("Registrazione eseguita con successo.");
        } catch (UsernameAlreadyRegistered ex) {
            System.out.println("Username già utilizzato da un altro utente");
        } catch (NotBoundException | RemoteException ex) {
            System.out.println("Si è verificato un errore. Riprova.");
            ex.printStackTrace();
        }
    }

    public void handleLogin(String user, String pw) throws IOException {
        Registry r = LocateRegistry.getRegistry(2048);
        UsersService usersServer;
        try {
            // Login RMI -> ovvero controllo se posso fare il login
            usersServer = (UsersService) r.lookup("UsersService");
            usersServer.login(user, pw); // Provo a fare il login. Se c'è un errore entro nei catch, altrimenti mi connetto con il server
            System.out.println("Login eseguito con successo.");
            /*
               Se posso fare il login allora instauro la connessione -> 2 connessioni, una per le richieste
               e una in cui rimanere in ascolto per gli inviti
             */

            // Connessione TCP per le richieste normali
            this.socketChannel = SocketChannel.open();
            SocketAddress address = new InetSocketAddress("127.0.0.1", 5000);
            this.socketChannel.connect(address);
            this.currentUser = usersServer.getUser(user);
            // Svuoto la history degli inviti
            if(this.currentUser.getInvitations().size() != 0) System.out.println("Mentre eri offline:");
            for(Invitation i: this.currentUser.getInvitations()){
                System.out.println("  " + i.getOwnerDocument() + " ti ha invitato a collaborare al documento: " + i.getDocumentName());
            }
            usersServer.emptyInvitations(user);
            // Altra connessione in cui ricevere gli inviti
            SocketAddress invitationAddress = new InetSocketAddress("127.0.0.1", 5001);
            SocketChannel invitationChannel = SocketChannel.open();
            invitationChannel.connect(invitationAddress);
            Body b = new Body();
            b.setUsername(user);
            Communication.send(invitationChannel, new Packet(new Header(OPS.CREATEASSOCIATION), b));
            if(Communication.read(invitationChannel).getHeader().getOp() == OPS.OK){
                this.ih = new InviteHandler(invitationChannel);
                ih.start();
                this.state = STATE.LOGGED;
            } else {
                this.socketChannel.finishConnect();
                invitationChannel.finishConnect();
                usersServer.logout(user);
                System.out.println("Si è verificato un errore. Riprova.");
            }
        } catch (UserNotRegistered ex) {
            System.out.println("Username non registrato");
        } catch(NotAuthorized ex) {
            System.out.println("Errore. Password errata");
        } catch(AlreadyLoggedIn ex){
            System.out.println("Sei già loggato");
        } catch (NotBoundException | RemoteException ex) { ex.printStackTrace();
            System.out.println("Si è verificato un errore. Riprova.");
        }
    }

    public void handleLogout() throws IOException {
        Registry r = LocateRegistry.getRegistry(2048);
        UsersService usersServer;
        try {
            usersServer = (UsersService) r.lookup("UsersService");
            usersServer.logout(this.currentUser.getUsername());
            this.currentUser = null;
            this.state = STATE.STARTED;
            System.out.println("Logout eseguito con successo.");
        }catch (NotBoundException | RemoteException ex) {
            System.out.println("Si è verificato un errore. Riprova.");
        }
    }

    public void handleCreate(String documentName, int sectionsNumber) throws IOException{
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setSectionsNumber(sectionsNumber);
        b.setDocumentName(documentName);
        Communication.send(this.socketChannel, new Packet(new Header(OPS.CREATE), b));

        Packet pkt = Communication.read(this.socketChannel);
        if(pkt.getHeader().getOp() == OPS.OK) System.out.println("Documento " + documentName + " creato con successo composto da " + sectionsNumber + " sezioni.");
        else System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
    }

    public void handleShare(String documentName, String collaborator) throws IOException {
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setCollaborator(collaborator);
        b.setDocumentName(documentName);
        Communication.send(this.socketChannel, new Packet(new Header(OPS.SHARE), b));
        Packet pkt = Communication.read(this.socketChannel);

        if(pkt.getHeader().getOp() == OPS.OK){
            System.out.println("Documento " + documentName + " condiviso con " + collaborator + " con successo");
        }else System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
    }

    public void handleShowSec(String documentName, int sectionNumber) throws IOException {
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setDocumentName(documentName);
        b.setSectionNumber(sectionNumber);
        Communication.send(this.socketChannel, new Packet(new Header(OPS.SHOWSEC), b));

        Packet pkt = Communication.read(this.socketChannel);
        if(pkt.getHeader().getOp() == OPS.OK){
            Communication.read(this.socketChannel, new File(String.valueOf(Paths.get(this.basePath.toString(), documentName + sectionNumber + ".txt"))), pkt.getBody().getBytesNumber());
            Boolean isInEditing = new Gson().fromJson(pkt.getBody().getOther(), Boolean.class);
            System.out.println("Sezione " + sectionNumber + " del documento " + documentName + ", scaricata con successo");
            if(isInEditing) System.out.println("   -> in modifica");
        }else{
            System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
        }
    }

    public void handleShowDoc(String documentName) throws IOException {
        Body b = new Body();
        b.setUsername(this.currentUser.getUsername());
        b.setDocumentName(documentName);
        Communication.send(this.socketChannel, new Packet(new Header(OPS.SHOWDOC), b));
        Packet pkt = Communication.read(this.socketChannel);

        if(pkt.getHeader().getOp() == OPS.OK){
            Communication.read(this.socketChannel, new File(String.valueOf(Paths.get(this.basePath.toString(), documentName  + ".txt"))), pkt.getBody().getBytesNumber());
            ArrayList<Double> sectionsInEditing = new Gson().fromJson(pkt.getBody().getOther(), ArrayList.class);
            if(sectionsInEditing.size() != 0){
                System.out.println("Sezioni in modifica: ");
                for(Double i: sectionsInEditing)
                    System.out.println("  -> " + i.intValue());
            }
            System.out.println("Documento " + documentName + " scaricato con successo");
        }else{
            System.out.println("Errore: " + printError(pkt.getHeader().getOp()));
        }
    }

    public String printError(OPS op){
        return op.toString();
    }

    public static void usage(){
        System.out.println("usage : turing COMMAND [ ARGS ...]\n");
        System.out.println("commands :");
        System.out.println("  register <username> <password>   registra l' utente");
        System.out.println("  login <username> <password>      effettua il login");
        System.out.println("  logout                           effettua il logout\n");
        System.out.println("  create <doc> <numsezioni>        crea un documento");
        System.out.println("  share <doc> <username>           condivide il documento");
        System.out.println("  show <doc> <sec>                 mostra una sezione del documento");
        System.out.println("  show <doc>                       mostra l' intero documento");
        System.out.println("  list                             mostra la lista dei documenti\n");
        System.out.println("  edit <doc> <sec>                 modifica una sezione del documento");
        System.out.println("  end-edit                         fine modifica della sezione del doc.\n");
        System.out.println("  send <msg>                       invia un msg sulla chat");
        System.out.println("  receive                          visualizza i msg ricevuti sulla chat");
    }

    public enum STATE{
        STARTED,
        LOGGED,
        EDIT
    }
}
