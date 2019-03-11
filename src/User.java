import javax.print.Doc;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User implements Serializable {

    private String username;
    private String password;
    private ConcurrentHashMap<String, Document> ownedDocs;
    private ConcurrentHashMap<String, Document> collaboratorDocs;
    private SocketAddress address;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.ownedDocs = new ConcurrentHashMap<>();
        this.collaboratorDocs = new ConcurrentHashMap<>();
        this.address = null;
    }

    public User(String username, String password, ConcurrentHashMap<String, Document> ownedDocs, ConcurrentHashMap<String, Document> collaboratorDocs, SocketAddress address) {
        this.username = username;
        this.password = password;
        this.ownedDocs = ownedDocs;
        this.collaboratorDocs = collaboratorDocs;
        this.address = address;
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

    public Map<String, Document> getOwnedDocs() {
        return ownedDocs;
    }

    public void setOwnedDocs(ConcurrentHashMap<String, Document> ownedDocs) {
        this.ownedDocs = ownedDocs;
    }

    public Map<String, Document> getCollaboratorDocs() {
        return collaboratorDocs;
    }

    public void setCollaboratorDocs(ConcurrentHashMap<String, Document> collaboratorDocs) {
        this.collaboratorDocs = collaboratorDocs;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public boolean hasDocument(String documentName){
        return this.collaboratorDocs.containsKey(documentName) || this.ownedDocs.containsKey(documentName);
    }

    public void addOwnedDoc(Document d){
        this.ownedDocs.put(d.getName(), d);
    }

}
