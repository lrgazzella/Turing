import communication.Body.CreateBody;
import communication.Communication;
import communication.Header;
import communication.OPS;
import communication.Packet;
import exception.UserNotRegistered;
import exception.UsernameAlreadyRegistered;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    private STATE state;
    private User currentUser;
    private SocketChannel socketChannel;

    public static void main(String[] args){
        try{
            new Client().startClient();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Client(){
        this.socketChannel = null;
        this.state = STATE.STARTED;
        this.currentUser = null;


        
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
                    int sectionNumber;
                    try{
                        sectionNumber = Integer.parseInt(args[2]);
                    }catch(NumberFormatException e){
                        usage();
                        return;
                    }
                    this.handleCreate(documentName, sectionNumber);
                }else System.out.println("Errore: Non sei loggato");
                break;
            case "share":

                break;
            case "show":

                break;
            case "list":

                break;
            case "edit":

                break;
            case "end-edit":

                break;
            case "send":

                break;
            case "receive":

                break;
            default:
                usage();
                break;
        }
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

    public void handleLogin(String user, String pw) throws IOException, ClassNotFoundException{
        Registry r = LocateRegistry.getRegistry(2048);
        UsersService usersServer;
        try {
            // Login RMI
            usersServer = (UsersService) r.lookup("UsersService");
            usersServer.login(user, pw); // Provo a fare il login. Se c'è un errore entro nei catch, altrimenti mi connetto con il server
            System.out.println("Login eseguito con successo.");
            // Connessione TCP
            this.socketChannel = SocketChannel.open();
            SocketAddress address = new InetSocketAddress("127.0.0.1", 5000);
            this.socketChannel.connect(address);
            this.currentUser = usersServer.getUser(user);
            this.state = STATE.LOGGED;
            usersServer.getUser(user).setAddress(this.socketChannel.getLocalAddress());
        } catch (UserNotRegistered ex) {
            System.out.println("Username non registrato");
        } catch(NotAuthorized ex) {
            System.out.println("Errore. Password errata");
        } catch (NotBoundException | RemoteException ex) { ex.printStackTrace();
            System.out.println("Si è verificato un errore. Riprova.");
        }
    }

    public void handleLogout() throws IOException, ClassNotFoundException{
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

    public void handleCreate(String documentName, int sectionNumber) throws IOException, ClassNotFoundException{
        Communication.sendHeader(this.socketChannel, new Header(OPS.CREATE));
        System.out.println("Header inviato");
        Communication.sendBody(this.socketChannel, new CreateBody(this.currentUser.getUsername(), documentName, sectionNumber));
        Header hdr = Communication.receiveHeader(this.socketChannel);
        if(hdr.getOp() == OPS.OK) System.out.println("Documento " + documentName + " creato con successo composto da " + sectionNumber + " sezioni.");
        else System.out.println("Errore: " + printError(hdr.getOp()));
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
        System.out.println("  end-edit <doc> <sec>             fine modifica della sezione del doc.\n");
        System.out.println("  send <msg>                       invia un msg sulla chat");
        System.out.println("  receive                          visualizza i msg ricevuti sulla chat");
    }

    public enum STATE{
        STARTED,
        LOGGED,
        EDIT
    }
}
