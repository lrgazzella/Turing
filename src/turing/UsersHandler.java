package turing;

import exception.AlreadyLoggedIn;
import exception.NotAuthorized;
import exception.UserNotRegistered;
import exception.UsernameAlreadyRegistered;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

public class UsersHandler extends RemoteServer implements UsersService { // Implementazione dei metodi accessibili da remoto

    private ConcurrentHashMap<String, User> users;

    public UsersHandler() {
        this.users = new ConcurrentHashMap<>();
    }

    @Override
    public void register(User u) throws UsernameAlreadyRegistered, RemoteException {
        if(this.users.containsKey(u.getUsername())){ // Controlla se già esiste un utente con lo stesso username
            throw new UsernameAlreadyRegistered();
        }else{
            this.users.put(u.getUsername(), u); // Se non esiste aggiunge l'utete u nella hash map
        }
    }

    @Override
    public User getUser(String username) throws RemoteException {
        return this.users.get(username);
    }

    @Override
    public void login(String username, String password) throws UserNotRegistered, NotAuthorized, AlreadyLoggedIn, RemoteException {
        User u;
        if((u = this.getUser(username)) == null){ // Controllo se esiste un utente registrato con quell'username
            throw new UserNotRegistered();
        }else if(!u.getPassword().equals(password)){ // Controllo se ha inserito la password corretta
            throw new NotAuthorized();
        }else if(u.isLogged()){ // Controllo se è già loggato
            throw new AlreadyLoggedIn();
        }
        u.setLogged(true);
    }

    @Override
    public void logout(String username) throws RemoteException {
        this.getUser(username).setLogged(false);
    }

    @Override
    public void emptyInvitations(String username) throws RemoteException {
        this.getUser(username).getInvitations().clear();
    }

}
