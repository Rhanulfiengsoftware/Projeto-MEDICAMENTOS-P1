package br.edu.unifan.farmacia;

import br.edu.unifan.farmacia.controller.MainController;
import br.edu.unifan.farmacia.repository.MedicamentoCsvRepository;
import br.edu.unifan.farmacia.repository.MedicamentoRepository;
import br.edu.unifan.farmacia.service.MedicamentoService;
import br.edu.unifan.farmacia.service.RelatorioService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Ponto de entrada da aplicação JavaFX. */
public class Main extends Application {

    /** Arquivo CSV de dados, relativo à pasta de execução. */
    private static final Path ARQUIVO_CSV = Paths.get("data", "medicamentos.csv");

    @Override
    public void start(Stage stage) {
        try {
            // Injeção de dependências "manual": repositório -> serviço -> controlador
            MedicamentoRepository repositorio = new MedicamentoCsvRepository(ARQUIVO_CSV);
            MedicamentoService service = new MedicamentoService(repositorio);
            RelatorioService relatorioService = new RelatorioService();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/main.fxml"));
            loader.setControllerFactory(tipo -> new MainController(service, relatorioService));

            Scene scene = new Scene(loader.load(), 1100, 720);
            stage.setTitle("Farmácia — Controle de Estoque");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro ao iniciar");
            alerta.setHeaderText("Não foi possível iniciar o sistema");
            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
