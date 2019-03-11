import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

public class Section {

    private ReentrantLock lock;
    private Boolean inEditing;
    private Path path;

    public Section(Path path) throws IOException {
        this.lock = new ReentrantLock();
        this.inEditing = false;
        this.path = path;
        new File(this.path.toString()).createNewFile();
    }
}
