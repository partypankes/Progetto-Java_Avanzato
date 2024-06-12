package gruppo2.service;

import gruppo2.DirectoryChecker;
import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FolderService è una classe che estende Service<List<Document>> per gestire e monitorare i documenti in una directory.
 * <p>
 * Questa classe permette di monitorare una directory per rilevare nuovi documenti o cambiamenti nei documenti esistenti,
 * e di aggiornare lo stato salvato della directory.
 */
public class FolderService extends Service<List<Document>> {
    private Path dir;
    private File selectedDirectory;

    /**
     * Costruttore predefinito per inizializzare FolderService.
     */
    public FolderService() {

    }

    /**
     * Imposta la directory selezionata e aggiorna il percorso.
     *
     * @param selectedDirectory la directory selezionata
     */
    public void setSelectedDirectory(File selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        this.dir = Paths.get(selectedDirectory.getAbsolutePath());
    }

    /**
     * Crea un task per gestire i documenti nella directory selezionata.
     *
     * @return il task per gestire i documenti nella directory selezionata
     */
    @Override
    protected Task<List<Document>> createTask() {
        return new Task<>() {
            @Override
            protected List<Document> call() {
                String previousDirPath = DirectoryChecker.loadPreviousDirPath();

                List<Document> documentsToUpdate;
                if (!previousDirPath.equals(dir.toString())) {
                    System.out.println("Nuova directory rilevata. Salvataggio del nuovo stato.");
                    Map<String, Long> currentState = DirectoryChecker.getCurrentState(dir);
                    List<Document> currentDocuments = readDocumentsFromDirectory(selectedDirectory);
                    DirectoryChecker.saveCurrentState(currentState, currentDocuments);
                    DirectoryChecker.saveCurrentDirPath(dir.toString());
                    documentsToUpdate = new ArrayList<>(currentDocuments);
                } else {
                    DirectoryChecker.DirectoryState previousState = DirectoryChecker.loadPreviousState();
                    Map<String, Long> currentState = DirectoryChecker.getCurrentState(dir);
                    DirectoryChecker.checkForChanges(previousState.fileStates, currentState, previousState.documents, dir);
                    DirectoryChecker.saveCurrentState(currentState, previousState.documents);
                    documentsToUpdate = new ArrayList<>(previousState.documents);
                }
                return documentsToUpdate;
            }

            /**
             * Legge i documenti dalla directory specificata.
             *
             * @param directory la directory da cui leggere i documenti
             * @return una lista di documenti letti dalla directory
             */
            private List<Document> readDocumentsFromDirectory(File directory) {
                List<Document> documentList = new ArrayList<>();
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".txt")) {
                            Document document = readDocumentFromFile(file);
                            if (document != null) {
                                documentList.add(document);
                            }
                        }
                    }
                }
                return documentList;
            }

            /**
             * Legge un documento dal file specificato.
             *
             * @param file il file da cui leggere il documento
             * @return il documento letto dal file, o null se non può essere letto
             */
            private Document readDocumentFromFile(File file) {
                return DirectoryChecker.readDocumentFromFile(file);
            }
        };
    }
}
