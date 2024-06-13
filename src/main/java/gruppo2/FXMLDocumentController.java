package gruppo2;

import gruppo2.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.collections.FXCollections.observableArrayList;

/**
 * Controller per la gestione dell'interfaccia FXML.
 * Implementa la logica per il caricamento dei documenti, la gestione delle stopwords,
 * la visualizzazione dei documenti e delle statistiche di collezione.
 */
public class FXMLDocumentController implements Initializable {

    private static final ConcurrentMap<String, Integer> vocabolario = new ConcurrentHashMap<>();
    private static final Map<Document, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());
    private static final Map<Document, Map<String, Integer>> resultMapByDocument = new ConcurrentHashMap<>();
    private static List<String> stopwords = new ArrayList<>();
    private final ObservableList<Document> documents = observableArrayList();
    private File directoryChoosed;

    @FXML
    public Button stopwordsButton;
    @FXML
    public Button folderButton;
    @FXML
    private Label documentTitleLabel;
    @FXML
    private AnchorPane pane1;
    @FXML
    private AnchorPane pane2;
    @FXML
    private AnchorPane loadingPane;
    @FXML
    private AnchorPane paneDocument;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextField queryTf;
    @FXML
    private TextArea bodyDocument;
    @FXML
    private Label statisticsDocumentLabel;
    @FXML
    private Label collectionStatisticsLabel;
    @FXML
    private TableView<Document> tableView;
    @FXML
    private TableColumn<Document, String> titleColumn = new TableColumn<>("Title");
    @FXML
    private Button startButton;
    @FXML
    private Button hiddenButton;

    /**
     * Converte il testo in un vettore di frequenze delle parole.
     *
     * @param text  Il testo da convertire.
     * @param title Il titolo del documento.
     * @return La mappa delle parole con le rispettive frequenze.
     */
    public static Map<String, Integer> textToVector(String text, String title) {
        Map<String, Integer> vector = new TreeMap<>();
        if (!Objects.equals(title, "")) {
            Arrays.stream(title.split("\\s+")).forEach(word -> vector.merge(word, 2, Integer::sum));
        }

        Arrays.stream(text.split("\\s+")).forEach(word -> vector.merge(word, 1, Integer::sum));
        return vector;
    }

    /**
     * Calcola la similarità coseno tra due vettori.
     *
     * @param vector1 Il primo vettore.
     * @param vector2 Il secondo vettore.
     * @return Il valore della similarità coseno.
     */
    public static double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
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

    /**
     * Estrae il documento da un file.
     *
     * @param file Il file da cui estrarre il documento.
     * @return Il documento estratto.
     */
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

            return new Document(file.getName(), title, body.toString());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Pulisce il testo e rimuove le stopwords.
     *
     * @param text Il testo da pulire.
     * @return Il testo pulito senza stopwords.
     */
    public static String cleanAndRemoveStopwords(String text) {

        String cleanedText = text.replaceAll("'", " ").replaceAll("[^\\p{L}\\s]", " ").toLowerCase();


        List<String> words = Stream.of(cleanedText.split("\\s+")).filter(word -> !stopwords.contains(word)).collect(Collectors.toList());


        return String.join(" ", words);
    }

    /**
     * Aggiunge le parole al vocabolario.
     *
     * @param text Il testo da cui estrarre le parole.
     */
    public static void addWordsToVocabulary(String text) {
        Arrays.stream(text.split("\\s+")).forEach(parola -> vocabolario.merge(parola, 1, Integer::sum));
    }

    /**
     * Inizializza il controller.
     *
     * @param url  L'URL di inizializzazione.
     * @param rb   Le risorse di inizializzazione.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().title()));
        tableView.setItems(documents);

        selectionDocument();
    }

    /**
     * Gestisce l'input della query.
     */
    @FXML
    private void handleQuery() {
        QueryService queryService = new QueryService(resultMapByDocument, corrispondenzaSimiliarita);
        queryService.setQueryText(queryTf.getText());

        queryService.setOnSucceeded(event -> {
            List<Document> filteredDocuments = queryService.getValue();
            documents.setAll(filteredDocuments);
            showCollectionStatistics(filteredDocuments);

        });

        queryService.restart();
    }

    /**
     * Carica le stopwords da un file scelto dall'utente.
     *
     * @throws IOException Se c'è un errore di lettura del file.
     */
    @FXML
    public void loadStopwords() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File stopwordsFile = fileChooser.showOpenDialog(pane1.getScene().getWindow());
        System.out.println(stopwordsFile);
        if (stopwordsFile != null) {
            stopwords = Files.readAllLines(stopwordsFile.toPath());
            stopwordsButton.setText(stopwordsFile.getName());
        } else {
            stopwordsButton.setText("STOPWORDS");
            stopwords = null;
            System.out.println("Operazione annullata");
        }
    }

    /**
     * Gestisce la selezione di una cartella.
     */
    @FXML
    private void folderSelection() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");

        directoryChoosed = directoryChooser.showDialog(null);
        startButton.setDisable(directoryChoosed == null);

        if (directoryChoosed != null) {
            folderButton.setText(directoryChoosed.getName());
        } else {
            folderButton.setText("FOLDER");
        }

    }

    /**
     * Ottiene il servizio per la gestione della cartella selezionata.
     *
     * @param selectedDirectory La cartella selezionata.
     * @return Il servizio di gestione della cartella.
     */
    private FolderService getFolderService(File selectedDirectory) {
        FolderService folderService = new FolderService();
        folderService.setSelectedDirectory(selectedDirectory);

        folderService.setOnSucceeded(event1 -> {
            List<Document> documentsToUpdate = folderService.getValue();
            documents.setAll(documentsToUpdate);
            documents.sort(Comparator.naturalOrder());
            createVocabularyAndVectors(documents.stream().toList());
        });

        return folderService;
    }

    /**
     * Avvia il processo di caricamento dei documenti dalla cartella selezionata.
     */
    @FXML
    private void start() {
        System.out.println("Cartella selezionata: " + directoryChoosed.getAbsolutePath());

        FolderService folderService = getFolderService(directoryChoosed);
        folderService.restart();
        pane1.setVisible(false);
        loadingPane.setVisible(true);
    }

    /**
     * Crea il vocabolario e i vettori di frequenze per i documenti caricati.
     *
     * @param documents La lista dei documenti caricati.
     */
    @FXML
    private void createVocabularyAndVectors(List<Document> documents) {
        VocabularyService vocabularyService = new VocabularyService(documents, resultMapByDocument);
        progressBar.progressProperty().bind(vocabularyService.progressProperty());
        vocabularyService.setOnSucceeded(event -> {
            System.out.println(vocabolario);
            showCollectionStatistics(documents);
            loadingPane.setVisible(false);
            pane2.setVisible(true);
        });

        vocabularyService.restart();
    }

    /**
     * Gestisce la selezione di un documento dalla tabella.
     */
    private void selectionDocument() {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Document selectedDocument = tableView.getSelectionModel().getSelectedItem();
                if (selectedDocument != null) {
                    showDocumentContent(selectedDocument);
                    showDocumentStatistics(selectedDocument);
                }
            }
        });
    }

    /**
     * Mostra il contenuto di un documento selezionato.
     *
     * @param selectedDocument Il documento selezionato.
     */
    private void showDocumentContent(Document selectedDocument) {

        DocumentContentService documentContentService = new DocumentContentService(selectedDocument, pane2, paneDocument, documentTitleLabel, bodyDocument);

        documentContentService.restart();
    }

    /**
     * Chiude la visualizzazione del documento e torna alla vista principale.
     */
    @FXML
    public void closeDocument() {
        statisticsDocumentLabel.setText("");
        paneDocument.setVisible(false);
        pane2.setVisible(true);
        tableView.getSelectionModel().clearSelection();
        hiddenButton.requestFocus();
    }

    /**
     * Mostra le statistiche di un documento selezionato.
     *
     * @param selectedDocument Il documento selezionato.
     */
    private void showDocumentStatistics(Document selectedDocument) {

        DocumentStatisticsService documentStatisticsService = new DocumentStatisticsService(selectedDocument, resultMapByDocument, corrispondenzaSimiliarita);

        documentStatisticsService.setOnSucceeded(event -> {
            String statsMessage = documentStatisticsService.getValue();
            statisticsDocumentLabel.setText(statsMessage);
        });

        documentStatisticsService.restart();
    }

    /**
     * Mostra le statistiche della collezione di documenti caricati.
     *
     * @param documents La lista dei documenti caricati.
     */
    private void showCollectionStatistics(List<Document> documents) {

        CollectionStatisticsService collectionStatisticsService = new CollectionStatisticsService(documents, resultMapByDocument);

        collectionStatisticsService.setOnSucceeded(event -> {
            String statsMessage = collectionStatisticsService.getValue();
            collectionStatisticsLabel.setText(statsMessage);
        });

        collectionStatisticsService.restart();
    }

    /**
     * Test di navigazione web.
     */
    @FXML
    private void imageTest() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.diem.unisa.it"));
        } catch (IOException | URISyntaxException ignored) {

        }
    }
}
