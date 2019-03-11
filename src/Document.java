import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Document {

    private String name;
    private ArrayList<Section> sections;
    private User owner;
    private ArrayList<User> collaborators;
    private int portNumber; // used to create a Multicast UDP TODO
    private Path path;

    public Document(Path path, String documentName, int sectionNumber, User owner) throws IOException {
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
}
