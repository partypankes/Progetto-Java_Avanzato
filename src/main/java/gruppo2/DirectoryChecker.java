package gruppo2;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import static gruppo2.FXMLDocumentController.getDocument;

/**
 * La classe DirectoryChecker fornisce metodi per monitorare e tracciare le modifiche in una directory specificata.
 * Può rilevare file aggiunti, modificati e rimossi, e mantenere lo stato della directory tra le esecuzioni.
 */
public class DirectoryChecker {
    private static final String STATE_FILE = "directory_state.ser";
    private static final String DIR_PATH_FILE = "directory_path.ser";

    /**
     * Classe interna che rappresenta lo stato della directory, includendo i tempi di modifica dei file e i documenti.
     */
    public static class DirectoryState implements Serializable {

        public Map<String, Long> fileStates;
        public List<Document> documents;

        /**
         * Costruisce un oggetto DirectoryState.
         *
         * @param fileStates Una mappa contenente i nomi dei file e i loro ultimi tempi di modifica.
         * @param documents Una lista di oggetti Document che rappresentano i file nella directory.
         */
        public DirectoryState(Map<String, Long> fileStates, List<Document> documents) {
            this.fileStates = fileStates;
            this.documents = documents;
        }
    }

    /**
     * Carica lo stato precedentemente salvato della directory da un file.
     *
     * @return L'oggetto DirectoryState precedentemente salvato, o un nuovo oggetto DirectoryState se il caricamento fallisce.
     */
    public static DirectoryState loadPreviousState() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STATE_FILE))) {
            return (DirectoryState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new DirectoryState(new HashMap<>(), null);
        }
    }

    /**
     * Carica il percorso della directory precedentemente salvato da un file.
     *
     * @return Il percorso della directory precedentemente salvato, o una stringa vuota se il caricamento fallisce.
     */
    public static String loadPreviousDirPath() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DIR_PATH_FILE))) {
            return (String) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return "";
        }
    }

    /**
     * Salva il percorso della directory corrente in un file.
     *
     * @param dirPath Il percorso della directory da salvare.
     */
    public static void saveCurrentDirPath(String dirPath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DIR_PATH_FILE))) {
            oos.writeObject(dirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recupera lo stato corrente della directory specificata, inclusi i tempi di ultima modifica dei file .txt.
     *
     * @param dir Il percorso della directory da monitorare.
     * @return Una mappa contenente i nomi dei file e i loro tempi di ultima modifica.
     */
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

    /**
     * Verifica le modifiche tra lo stato precedente e lo stato corrente della directory, aggiornando di conseguenza la lista dei documenti.
     *
     * @param previousState Una mappa contenente lo stato precedente della directory.
     * @param currentState Una mappa contenente lo stato corrente della directory.
     * @param documents Una lista di oggetti Document che rappresentano i file nella directory.
     * @param dir Il percorso della directory monitorata.
     */
    public static void checkForChanges(Map<String, Long> previousState, Map<String, Long> currentState, List<Document> documents, Path dir) {
        Map<String, Document> documentMap = new HashMap<>();
        for (Document doc : documents) {
            documentMap.put(doc.filename(), doc);
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
            String filePath = doc.filename();
            if (!currentState.containsKey(filePath)) {
                System.out.println("File rimosso: " + filePath);
                iterator.remove();
            }
        }
    }

    /**
     * Salva lo stato corrente della directory in un file.
     *
     * @param state Una mappa contenente lo stato corrente della directory.
     * @param documents Una lista di oggetti Document che rappresentano i file nella directory.
     */
    public static void saveCurrentState(Map<String, Long> state, List<Document> documents) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
            oos.writeObject(new DirectoryState(state, documents));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legge un oggetto Document da un file.
     *
     * @param file Il file da leggere.
     * @return L'oggetto Document letto dal file.
     */
    public static Document readDocumentFromFile(File file) {
        return getDocument(file);
    }
}
