package soporte;

import negocio.Estadisticas;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Dataset {
    private File file;

    public Dataset(String ruta) {
        file = new File(ruta);
    }

    /**
     * Se lee el archivo para cargar el Dataset
     */
    public Estadisticas cargarDataset()
    {
        String linea;
        String [] campos;
        String [] difGeneros;
        Boolean primera = true;
        Estadisticas estadisticas = new Estadisticas();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()){
                linea = scanner.nextLine();
                /**
                 * Salteamosla primera liena de titulos de cada columna
                 */
                if (primera) {
                    primera = false;
                    continue;
                }
                /**
                 * Generamos la separacion de cada campo mediante las comas
                 */
                campos = linea.split(",");
                /**
                 * Generamos la separacion de cada genero que comparten lugar en el mismo campo
                 */
                String separador = "\\|";
                /**
                 * Realizamos un ciclo por cada linea y por cada genero para la misma serie
                 */
                difGeneros = campos[4].split(separador);
                for (int i = 1; i < 2; i++) {
                    for (int j = 0; j < difGeneros.length; j++) {
                        estadisticas.agregar(difGeneros[j],campos[0], (int)Double.parseDouble(campos[5]));
                    }

                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("No se pudo abrir "+file.getName());
        }
        finally {
            return estadisticas;
        }
    }

}
