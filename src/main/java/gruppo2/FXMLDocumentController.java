package gruppo2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gruppo2.DirectoryChecker.*;

public class FXMLDocumentController implements Initializable {

    @FXML
    private Label documentTitleLabel;

    @FXML
    private AnchorPane pane1;

    @FXML
    private AnchorPane pane2;

    @FXML
    private AnchorPane paneDocumento;

    @FXML
    private TextField queryTf;

    @FXML
    private TextArea corpoDocumento;

    @FXML
    private TextArea statisticheDocumento;

    @FXML
    private Button chiudiDocumento;

    @FXML
    private TableView<Document> tableView;

    @FXML
    private TableColumn<Document, String> titleColumn;

    private final ObservableList<Document> documents = FXCollections.observableArrayList();

    private static ConcurrentMap<String, Integer> vocabolario = new ConcurrentHashMap<>();

    private static Map<Document, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());

    private static Map<Document, Map<String, Integer>> resultMapByDocument = new ConcurrentHashMap<>();

    private List<String> stopwords;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableView.setItems(documents);
        try {
            loadStopwords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        selezionaDocumento();


    }

    private void addCloseRequestHandler(Node node) {
        Stage stage = (Stage) node.getScene().getWindow();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                shutdownExecutorService();
            }
        });
    }

    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    @FXML
    private void handleQuery() {
        String queryText = queryTf.getText();
        if (queryText == null || queryText.trim().isEmpty()) {
            System.out.println("ciao");
            List<Document> allDocuments = new ArrayList<>(resultMapByDocument.keySet());
            System.out.println(allDocuments);
            tableView.setItems(FXCollections.observableArrayList(allDocuments));
        } else {
            String cleanedQuery = cleanAndRemoveStopwords(queryText);
            Map<String, Integer> queryVector = textToVector(cleanedQuery);
            corrispondenzaSimiliarita.clear();
            for (Map.Entry<Document, Map<String, Integer>> entry : resultMapByDocument.entrySet()) {
                double similarity = calculateCosineSimilarity(entry.getValue(), queryVector);
                if (similarity > 0) {
                    corrispondenzaSimiliarita.put(entry.getKey(), similarity);
                }
            }

            List<Map.Entry<Document, Double>> sortedSimilarities = corrispondenzaSimiliarita.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            List<Document> sortedDocuments = sortedSimilarities.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            tableView.setItems(FXCollections.observableArrayList(sortedDocuments));
            System.out.println("ciao2");
        }
    }

    private Map<String, Integer> textToVector(String text) {
        Map<String, Integer> vector = new TreeMap<>();
        Arrays.stream(text.split("\\s+")).forEach(word -> vector.merge(word, 1, Integer::sum));
        return vector;
    }

    private static double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : vector1.keySet()) {
            int val1 = vector1.get(key);
            int val2 = vector2.getOrDefault(key, 0);

            dotProduct += val1 * val2;
            normA += Math.pow(val1, 2);
            normB += Math.pow(val2, 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        } else {
            return dotProduct / (Math.sqrt(normA) * (Math.sqrt(normB)));
        }
    }

    public void loadStopwords() throws IOException {
        File stopwordsFile = new File("stopwords-it.txt");
        if (stopwordsFile.exists()) {
            this.stopwords = Files.readAllLines(stopwordsFile.toPath());
        } else {
            this.stopwords = new ArrayList<>();
            System.out.println("Stopwords file not found.");
        }
    }

    @FXML
    private void folderSelection(ActionEvent event) throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);
        addCloseRequestHandler(pane1);
        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());

            // Specifica il percorso della directory da monitorare
            Path dir = Paths.get(selectedDirectory.getAbsolutePath());

            // Esegui operazioni di controllo della directory in modo asincrono
            executorService.submit(() -> {
                // Carica il percorso della directory precedente
                String previousDirPath = loadPreviousDirPath();

                List<Document> documentsToUpdate;
                if (!dir.toString().equals(previousDirPath)) {
                    // Nuova directory rilevata
                    System.out.println("Nuova directory rilevata. Salvataggio del nuovo stato.");
                    Map<String, Long> currentState = getCurrentState(dir);
                    List<Document> currentDocuments = readDocumentsFromDirectory(selectedDirectory);
                    saveCurrentState(currentState, currentDocuments);
                    saveCurrentDirPath(dir.toString());

                    // Ordina i documenti per titolo
                    documentsToUpdate = new ArrayList<>(currentDocuments);

                } else {
                    // Carica lo stato precedente della directory
                    DirectoryState previousState = loadPreviousState();

                    // Ottieni lo stato attuale della directory
                    Map<String, Long> currentState = getCurrentState(dir);

                    // Confronta gli stati e rileva le modifiche
                    checkForChanges(previousState.fileStates, currentState, previousState.documents, dir);

                    // Salva lo stato attuale per il confronto futuro
                    saveCurrentState(currentState, previousState.documents);

                    // Ordina i documenti per titolo
                    documentsToUpdate = new ArrayList<>(previousState.documents);
                }

                // Ordina i documenti per titolo
                documentsToUpdate.sort(Comparator.comparing(Document::getTitle));

                // Aggiorna la collezione di documenti
                Platform.runLater(() -> {
                    documents.setAll(documentsToUpdate);
                    // Passa alla vista successiva
                    pane1.setVisible(false);
                    pane2.setVisible(true);
                    createVocabularyAndVectors(documents.stream().toList());
                });
            });
        } else {
            System.out.println("Operazione annullata");
        }


    }



    private List<Document> readDocumentsFromDirectory(File directory) {
        List<Document> documentList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            List<Future<Document>> futures = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    futures.add(executorService.submit(() -> readDocumentFromFile(file)));
                }
            }

            for (Future<Document> future : futures) {
                try {
                    Document document = future.get();
                    if (document != null) {
                        documentList.add(document);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        return documentList;
    }

    private Document readDocumentFromFile(File file) {
        return getDocument(file);
    }

    static Document getDocument(File file) {
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)))) {
            if (!scanner.hasNextLine()) {
                return null;
            }

            String title = scanner.nextLine();
            StringBuilder body = new StringBuilder();

            while (scanner.hasNextLine()) {
                body.append(scanner.nextLine()).append("\n");
            }

            return new Document(file.getName(), title, body.toString().trim());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createVocabularyAndVectors(List<Document> documents) {
        List<Future<Void>> futures = new ArrayList<>();

        for (Document document : documents) {
            futures.add(executorService.submit(() -> {
                String cleanedText = cleanAndRemoveStopwords(document.getTitle() + " " + document.getDocument_text());
                addWordsToVocabulary(cleanedText);
                Map<String, Integer> documentVector = textToVector(cleanedText);
                resultMapByDocument.put(document, documentVector);
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private String cleanAndRemoveStopwords(String text) {
        String cleanedText = text.replaceAll("'", " ").replaceAll("[^\\p{L}\\s]", " ").toLowerCase();
        List<String> words = Stream.of(cleanedText.split("\\s+"))
                .filter(word -> !stopwords.contains(word))
                .collect(Collectors.toList());
        return String.join(" ", words);
    }

    private void addWordsToVocabulary(String text) {
        Arrays.stream(text.split("\\s+")).forEach(parola -> vocabolario.merge(parola, 1, Integer::sum));
    }

    private void selezionaDocumento() {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Doppio click
                Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
                if (documentoSelezionato != null) {
                    mostraContenutoDocumento(documentoSelezionato);
                }
            }
        });
    }

    private void mostraContenutoDocumento(Document documentoSelezionato) {
        pane2.setVisible(false);
        paneDocumento.setVisible(true);
        corpoDocumento.setText(documentoSelezionato.getDocument_text());
        documentTitleLabel.setText(documentoSelezionato.getTitle());
        mostrastatisticheDocumento(documentoSelezionato);
    }

    @FXML
    public void chiudiDocumento() {
        paneDocumento.setVisible(false);
        pane2.setVisible(true);
    }

    @FXML
    public void mostrastatisticheDocumento(Document documentoSelezionato) {
        String testoDocumento = documentoSelezionato.getDocument_text();
        int sentenceCount = testoDocumento.split("[.!?]").length;

        // Testo già pulito e senza stopwords
        String cleanedText = cleanAndRemoveStopwords(testoDocumento);
        Map<String, Integer> documentVector = textToVector(cleanedText);

        // Calcolo delle statistiche
        int totalWords = documentVector.values().stream().mapToInt(Integer::intValue).sum();
        int uniqueWords = documentVector.size();

        // Le 5 parole più comuni
        List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        // Creazione del messaggio di statistica
        StringBuilder statsMessage = new StringBuilder();
        statsMessage.append("Numero totale di parole: ").append(totalWords).append("\n");
        statsMessage.append("Numero di parole uniche: ").append(uniqueWords).append("\n");
        statsMessage.append("Numero di frasi: ").append(sentenceCount).append("\n");
        statsMessage.append("Le 5 parole più comuni sono:\n");
        commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        // Impostazione del messaggio di statistica
        statisticheDocumento.setText(statsMessage.toString());
    }
}
