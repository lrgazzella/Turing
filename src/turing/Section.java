package turing;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

public class Section implements Serializable{ // Poichè dovrà essere inviato, implementa la classe Serializable

    private ReentrantLock lock;
    private Boolean inEditing;
    private transient Path path; // transient per evitare che venga spedito ed esposto il file system

    public Section(Path path) throws IOException {
        this.lock = new ReentrantLock();
        this.inEditing = false;
        this.path = path;
        new File(this.path.toString()).createNewFile(); // Crea la sezione su disco
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void setLock(ReentrantLock lock) {
        this.lock = lock;
    }

    public Boolean getInEditing() {
        return inEditing;
    }

    public void setInEditing(Boolean inEditing) {
        this.inEditing = inEditing;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * @return true se può essere editata, false altrimenti
     * */
    public boolean edit(){
        boolean r = false;
        this.lock.lock();
        if(!this.inEditing)
            this.inEditing = r = true;
        this.lock.unlock();
        return r;
    }

    public void endedit(){
        this.lock.lock();
        this.inEditing = false;
        this.lock.unlock();
    }

}
