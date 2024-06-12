package gruppo2;

import gruppo2.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

import static javafx.collections.FXCollections.observableArrayList;


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
    private AnchorPane statistics1;

    @FXML
    private TextField queryTf;

    @FXML
    private TextArea corpoDocumento;

    @FXML
    private Label statisticheDocumentoLabel;

    @FXML
    private Label collectionStatisticsLabel;

    @FXML
    private Button chiudiDocumento;

    @FXML
    private TableView<Document> tableView;

    @FXML
    private TableColumn<Document, String> titleColumn;

    @FXML
    private ProgressIndicator progressIndicator;

    private final ObservableList<Document> documents = observableArrayList();

    private static final ConcurrentMap<String, Integer> vocabolario = new ConcurrentHashMap<>();

    private static final Map<Document, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());

    private static final Map<Document, Map<String, Integer>> resultMapByDocument = new ConcurrentHashMap<>();

    private static List<String> stopwords;

    private boolean isFirstClick1 = true;

    private boolean isFirstClick2 = true;

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


    // Gestisce la query inserita dall'utente
    @FXML
    private void handleQuery() {
        QueryService queryService = new QueryService(resultMapByDocument, corrispondenzaSimiliarita);
        queryService.setQueryText(queryTf.getText());

        queryService.setOnSucceeded(event -> {
            List<Document> filteredDocuments = queryService.getValue();
            // Gestisci i documenti filtrati come necessario
            documents.setAll(filteredDocuments);
            showCollectionStatistics(filteredDocuments);
        });

        queryService.setOnFailed(event -> {
            Throwable throwable = queryService.getException();
            // Gestisci l'errore come necessario
        });

        queryService.restart();
    }



    //  Converte il testo di un documento  in un vettore di frequenze delle parole
    public static Map<String, Integer> textToVector(String text, String title) {
        Map<String, Integer> vector = new TreeMap<>();
        if(!Objects.equals(title, ""))
        {
            Arrays.stream(title.split("\\s+")).forEach(word -> vector.merge(word, 2, Integer::sum));
        }
        // ogni parola nel testo diventa una chiave nella mappa e il valore associato è il numero di occorrenze di quella parola
        Arrays.stream(text.split("\\s+")).forEach(word -> vector.merge(word, 1, Integer::sum));
        return vector;
    }


    // Calcola il coseno di similarità tra due vettori: misura quanto sono simili due vettori
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


    // Carica le stopwords da un file e le inserisce in una lista
    public void loadStopwords() throws IOException {
        File stopwordsFile = new File("stopwords-it.txt");

        if (stopwordsFile.exists()) {
            stopwords = Files.readAllLines(stopwordsFile.toPath());
        } else {
            // Se il file non esiste, la lista di stopwords sarà vuota
            stopwords = new ArrayList<>();
            System.out.println("Stopwords file not found.");
        }
    }


    @FXML
    private void folderSelection(ActionEvent event) throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());

            // Crea e configura il FolderService
            FolderService folderService = getFolderService(selectedDirectory);

            // Avvia il Service
            folderService.restart();
        } else {
            System.out.println("Operazione annullata");
        }
    }

    private FolderService getFolderService(File selectedDirectory) {
        FolderService folderService = new FolderService();
        folderService.setSelectedDirectory(selectedDirectory);

        folderService.setOnSucceeded(event1 -> {
            List<Document> documentsToUpdate = folderService.getValue();
            documents.setAll(documentsToUpdate);
            pane1.setVisible(false);
            pane2.setVisible(true);
            createVocabularyAndVectors(documents.stream().toList());
        });

        folderService.setOnFailed(event1 -> {
            folderService.getException().printStackTrace();
        });
        return folderService;
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

            Document document = new Document(file.getName(), title, body.toString());
            return document;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Crea il vocabolario e i vettori di documenti
    @FXML
    private void createVocabularyAndVectors(List<Document> documents) {
        // Crea e configura il VocabularyService
        VocabularyService vocabularyService = new VocabularyService(documents, resultMapByDocument);


        vocabularyService.setOnFailed(event -> {
            vocabularyService.getException().printStackTrace();
        });

        // Avvia il Service
        vocabularyService.restart();
    }

    // Pulisce il testo del documento e rimuove le stopwrods
    public static String cleanAndRemoveStopwords(String text) {
        /* Pulizia:
          - sostituisce tutti gli apostrofi con spazi;
          - rimuove tutti i caratteri non alfabetici e non spazi (\p{L} è una proprietà Unicode che rappresenta tutte le lettere);
          - converte tutto il testo in minuscolo per uniformità */
        String cleanedText = text.replaceAll("'", " ").replaceAll("[^\\p{L}\\s]", " ").toLowerCase();

        /* Rimozione stopwrdos:
           - divide il testo pulito in parole, usando gli spazi come delimitatori;
           - filtra le parole, rimuovendo quelle presenti nella lista delle stopwords;
           - colleziona le parole filtrate in una lista */
        List<String> words = Stream.of(cleanedText.split("\\s+")).filter(word -> !stopwords.contains(word)).collect(Collectors.toList());

        // Unisce le parole rimanenti in una singola stringa separata da spazi.
        return String.join(" ", words);
    }


    /* Divide il testo in parole usando gli spazi come delimitatori e aggiorna il vocabolario: per ogni parola aggiorna il relativo conteggio nel
    vocabolario (aggiunge la parola al vocabolario con un conteggio di 1 se non è presente, altrimenti incrementa il conteggio di 1) */
    public static void addWordsToVocabulary(String text) {
        Arrays.stream(text.split("\\s+")).forEach(parola -> vocabolario.merge(parola, 1, Integer::sum));
    }


    // Quando viene selezionato un documento con un click, viene mostrato il suo contenuto
    private void selezionaDocumento() {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
                if (documentoSelezionato != null) {
                    mostraContenutoDocumento(documentoSelezionato);
                }
            }
        });
    }


    private void mostraContenutoDocumento(Document documentoSelezionato) {
        // Crea e configura il DocumentContentService
        DocumentContentService documentContentService = new DocumentContentService(documentoSelezionato, pane2, paneDocumento, documentTitleLabel, corpoDocumento);

        documentContentService.setOnFailed(event -> {
            documentContentService.getException().printStackTrace();
        });

        // Avvia il Service
        documentContentService.restart();
    }

    // Chiude il documento che era stato selezionato
    @FXML
    public void chiudiDocumento() {
        statisticheDocumentoLabel.setText("");
        isFirstClick1 = true;
        paneDocumento.setVisible(false);
        pane2.setVisible(true);
    }


    @FXML
    public void statisticheDocumento(){
        Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
          if (isFirstClick1) {
              if (documentoSelezionato != null) {
                  mostrastatisticheDocumento(documentoSelezionato);
              }
        } else {
            statisticheDocumentoLabel.setText("");
        }
        isFirstClick1 = !isFirstClick1;
    }

    // Calcola le statistiche sul documento selezionato
    private void mostrastatisticheDocumento(Document documentoSelezionato) {
        // Crea e configura il DocumentStatisticsService
        DocumentStatisticsService documentStatisticsService = new DocumentStatisticsService(documentoSelezionato, resultMapByDocument, corrispondenzaSimiliarita);

        documentStatisticsService.setOnSucceeded(event -> {
            String statsMessage = documentStatisticsService.getValue();
            statisticheDocumentoLabel.setText(statsMessage);
        });

        documentStatisticsService.setOnFailed(event -> {
            documentStatisticsService.getException().printStackTrace();
        });

        // Avvia il Service
        documentStatisticsService.restart();
    }



    // Calcola le statistiche sull'intera collezione di documenti
    @FXML
    private void collectionStatistics(){
        ObservableList<Document> currentDocuments = tableView.getItems();
        if (isFirstClick2) {
            statistics1.setVisible(true);
            showCollectionStatistics(currentDocuments);
        } else {
            statistics1.setVisible(false);
        }
        isFirstClick2 = !isFirstClick2;
    }

    private void showCollectionStatistics(List<Document> documents) {
        // Crea e configura il CollectionStatisticsService
        CollectionStatisticsService collectionStatisticsService = new CollectionStatisticsService(documents, resultMapByDocument);

        collectionStatisticsService.setOnSucceeded(event -> {
            String statsMessage = collectionStatisticsService.getValue();
            collectionStatisticsLabel.setText(statsMessage);
        });

        collectionStatisticsService.setOnFailed(event -> {
            collectionStatisticsService.getException().printStackTrace();
        });

        // Avvia il Service
        collectionStatisticsService.restart();
    }


}
