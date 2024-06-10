package gruppo2;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import static gruppo2.FXMLDocumentController.getDocument;

public class DirectoryChecker {
    private static final String STATE_FILE = "directory_state.ser";
    private static final String DIR_PATH_FILE = "directory_path.ser";

    public static class DirectoryState implements Serializable {

        public Map<String, Long> fileStates;
        public List<Document> documents;

        public DirectoryState(Map<String, Long> fileStates, List<Document> documents) {
            this.fileStates = fileStates;
            this.documents = documents;
        }
    }

    public static DirectoryState loadPreviousState() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STATE_FILE))) {
            return (DirectoryState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new DirectoryState(new HashMap<>(), null);
        }
    }

    public static String loadPreviousDirPath() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DIR_PATH_FILE))) {
            return (String) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return "";
        }
    }

    public static void saveCurrentDirPath(String dirPath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DIR_PATH_FILE))) {
            oos.writeObject(dirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Long> getCurrentState(Path dir) {
        Map<String, Long> state = new HashMap<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".txt")) {
                        state.put(dir.relativize(file).toString(), attrs.lastModifiedTime().toMillis());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return state;
    }

    public static void checkForChanges(Map<String, Long> previousState, Map<String, Long> currentState, List<Document> documents, Path dir) {
        Map<String, Document> documentMap = new HashMap<>();
        for (Document doc : documents) {
            documentMap.put(doc.getFilename(), doc);
        }

        // Rileva aggiunte e modifiche
        for (String file : currentState.keySet()) {
            if (!previousState.containsKey(file)) {
                // File aggiunto
                System.out.println("File aggiunto: " + file);
                Document newDocument = readDocumentFromFile(dir.resolve(file).toFile());
                if (newDocument != null) {
                    documents.add(newDocument);
                }
            } else if (!currentState.get(file).equals(previousState.get(file))) {
                // File modificato
                System.out.println("File modificato: " + file);
                Document updatedDocument = readDocumentFromFile(dir.resolve(file).toFile());
                if (updatedDocument != null) {
                    documents.remove(documentMap.get(file));
                    documents.add(updatedDocument);
                }
            }
        }

        // Rileva rimozioni
        Iterator<Document> iterator = documents.iterator();
        while (iterator.hasNext()) {
            Document doc = iterator.next();
            String filePath = doc.getFilename();
            if (!currentState.containsKey(filePath)) {
                System.out.println("File rimosso: " + filePath);
                iterator.remove();
            }
        }
    }

    public static void saveCurrentState(Map<String, Long> state, List<Document> documents) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
            oos.writeObject(new DirectoryState(state, documents));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Document readDocumentFromFile(File file) {
        return getDocument(file);
    }
}
