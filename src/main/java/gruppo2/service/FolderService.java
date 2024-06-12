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

public class FolderService extends Service<List<Document>> {
    private Path dir;
    private File selectedDirectory;


    public FolderService() {

    }

    public void setSelectedDirectory(File selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        this.dir = Paths.get(selectedDirectory.getAbsolutePath());
    }

    @Override
    protected Task<List<Document>> createTask() {
        return new Task<>() {
            @Override
            protected List<Document> call() throws Exception {
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

            private Document readDocumentFromFile(File file) {
                return DirectoryChecker.readDocumentFromFile(file);
            }
        };
    }
}
