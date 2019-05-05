import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Document implements Serializable { // TODO Inserire nella documentazione che è serializable

    private String name;
    private ArrayList<Section> sections;
    private User owner;
    private ArrayList<User> collaborators;
    private int portNumber;
    private transient Path path;

    public Document(Path path, String documentName, int sectionNumber, User owner, int port) throws IOException {
        this.path = path;
        this.name = documentName;
        this.path.toFile().mkdir();
        String basePath = this.path.toString();
        this.sections = new ArrayList<>();
        for(int i=1; i<sectionNumber+1; i++){
            this.sections.add(new Section(Paths.get(basePath, i + ".txt")));
        }
        this.collaborators = new ArrayList<>();
        this.owner = owner;
        this.portNumber = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Section> getSections() {
        return sections;
    }

    public void setSections(ArrayList<Section> sections) {
        this.sections = sections;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public ArrayList<User> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(ArrayList<User> collaborators) {
        this.collaborators = collaborators;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public void addCollaborator(User u){
        this.collaborators.add(u);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Document{" +
                "name='" + name + '\'' +
                ", sections=" + sections +
                ", owner=" + owner +
                ", collaborators=" + collaborators +
                ", portNumber=" + portNumber +
                ", path=" + path +
                '}';
    }
}
