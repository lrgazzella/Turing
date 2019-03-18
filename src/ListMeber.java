import java.util.ArrayList;

public class ListMeber {

    private String documentName;
    private String owner;
    private ArrayList<String> collaborators;

    public ListMeber(String documentName, String owner, ArrayList<String> collaborators) {
        this.documentName = documentName;
        this.owner = owner;
        this.collaborators = collaborators;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ArrayList<String> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(ArrayList<String> collaborators) {
        this.collaborators = collaborators;
    }
}
