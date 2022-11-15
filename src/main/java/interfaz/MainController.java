package interfaz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import negocio.Estadisticas;
import soporte.Dataset;

import java.io.File;
import java.util.Collection;

public class MainController {
    public ComboBox cboGeneros;
    private Estadisticas estadisticas;

    public TextArea txtResultado;
    private Boolean buscar = true;


    public void initialize() {
        permitirBusqueda(true);
    }

    /**
     * Permite seleccionar el archivo a procesar y cargar el combobox de generos
     * con todos los géneros existentes.
     */
    public void onCargarClick(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Dataset ds = new Dataset(file.getPath());
            estadisticas = ds.cargarDataset();
            buscar = false;
            Collection generos = estadisticas.getGeneros();
            ObservableList ol = FXCollections.observableArrayList(generos);
            ol.sort(null);
            cboGeneros.setItems(ol);
            permitirBusqueda(false);
            buscar = true;
        }
    }

    /**
     * Al selecionar un genero del combobox nos devuelve en el txtResultado las cantidad de series totales
     * que posee el genero y lista las series qeu pertenecen a el junto con su puntuacion redondeada al entero.
     */
    public void onBuscarClick(ActionEvent actionEvent) {
        if (buscar) {
            Object resultado = estadisticas.buscar(cboGeneros.getValue());
            txtResultado.setText(resultado.toString());
        }
    }

    private void permitirBusqueda(Boolean desactivado) {

        cboGeneros.setDisable(desactivado);
        txtResultado.setDisable(desactivado);
    }

}