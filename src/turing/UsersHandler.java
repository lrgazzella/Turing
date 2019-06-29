package turing;

import exception.AlreadyLoggedIn;
import exception.NotAuthorized;
import exception.UserNotRegistered;
import exception.UsernameAlreadyRegistered;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

public class UsersHandler extends RemoteServer implements UsersService {

    private ConcurrentHashMap<String, User> users;

    public UsersHandler() {
        this.users = new ConcurrentHashMap<>();
    }

    @Override
    public void register(User u) throws UsernameAlreadyRegistered, RemoteException {
        if(this.users.containsKey(u.getUsername())){
            throw new UsernameAlreadyRegistered();
        }else{
            this.users.put(u.getUsername(), u);
        }
    }

    @Override
    public User getUser(String username) throws RemoteException {
        return this.users.get(username);
    }

    @Override
    public void login(String username, String password) throws UserNotRegistered, NotAuthorized, AlreadyLoggedIn, RemoteException {
        User u;
        if((u = this.getUser(username)) == null){
            throw new UserNotRegistered();
        }else if(!u.getPassword().equals(password)){
            throw new NotAuthorized();
        }else if(u.isLogged()){
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
