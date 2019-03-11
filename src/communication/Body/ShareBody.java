package communication.Body;

public class ShareBody implements Body {

    private String username;
    private String collaborator;
    private String documentName;

    public ShareBody(String username, String collaborator, String documentName) {
        this.username = username;
        this.collaborator = collaborator;
        this.documentName = documentName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(String collaborator) {
        this.collaborator = collaborator;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}
