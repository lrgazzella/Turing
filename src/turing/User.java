package turing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class User implements Serializable {

    private String username;
    private String password;
    private ConcurrentHashMap<String, Document> ownedDocs;
    private ConcurrentHashMap<String, Document> collaborationDocs;
    private List<Invitation> invitations;
    private boolean logged;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.ownedDocs = new ConcurrentHashMap<>();
        this.collaborationDocs = new ConcurrentHashMap<>();
        this.invitations = Collections.synchronizedList(new ArrayList<>()); // E' stato deciso di rendere la lista degli inviti concorrente poichè viene modificata sia dal client che dal server
        this.logged = false;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConcurrentHashMap<String, Document> getOwnedDocs() {
        return ownedDocs;
    }

    public void setOwnedDocs(ConcurrentHashMap<String, Document> ownedDocs) {
        this.ownedDocs = ownedDocs;
    }

    public ConcurrentHashMap<String, Document> getCollaborationDocs() {
        return collaborationDocs;
    }

    public void setCollaborationDocs(ConcurrentHashMap<String, Document> collaborationDocs) {
        this.collaborationDocs = collaborationDocs;
    }

    public List<Invitation> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
    }

    public void addInvitation(Invitation i){
        this.invitations.add(i);
    }

    public void addCollaborationDoc(Document d){
        this.collaborationDocs.put(d.getName(), d);
    }

    public boolean hasDocument(String documentName){
        return this.collaborationDocs.containsKey(documentName) || this.ownedDocs.containsKey(documentName);
    }

    public boolean isOwner(String documentName) {
        return this.ownedDocs.get(documentName) != null;
    }

    public void addOwnedDoc(Document d){
        this.ownedDocs.put(d.getName(), d);
    }

    /**
     * Prima controlla gli owned e poi i collaboration
     * */
    public Document getDocument(String documentName){
        Document d = this.ownedDocs.get(documentName);
        if(d != null) return d;
        else return this.collaborationDocs.get(documentName);
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    @Override
    public boolean equals(Object obj) throws ClassCastException{
        return (User.class.cast(obj).getUsername().equals(this.username));
    }
}
