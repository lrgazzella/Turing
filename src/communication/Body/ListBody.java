package communication.Body;

public class ListBody implements Body {

    private String username;

    public ListBody(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
