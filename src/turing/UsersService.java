package turing;

import exception.AlreadyLoggedIn;
import exception.NotAuthorized;
import exception.UserNotRegistered;
import exception.UsernameAlreadyRegistered;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UsersService extends Remote {

    public void register(User u) throws UsernameAlreadyRegistered, RemoteException;
    public User getUser(String username) throws RemoteException;
    public void login(String username, String password) throws UserNotRegistered, NotAuthorized, AlreadyLoggedIn, RemoteException;
    public void logout(String username) throws RemoteException;
    public void emptyInvitations(String username) throws RemoteException;

}
