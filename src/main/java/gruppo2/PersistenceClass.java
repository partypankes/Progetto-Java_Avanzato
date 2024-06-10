package gruppo2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class PersistenceClass implements Serializable {
    List<Document> collection;
    LocalDateTime changeTime;
    String path;

    public PersistenceClass(List<Document> collection, LocalDateTime changeTime, String path) {
        this.collection = collection;
        this.changeTime = changeTime;
        this.path = path;
    }

    public void persistenceWriteOBJ() throws IOException {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("persistence.bin"))) {
            oos.writeObject(this);
        }
    }
}
