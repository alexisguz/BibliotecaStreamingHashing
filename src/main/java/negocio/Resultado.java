package negocio;

public class Resultado {
    private int series;
    private StringBuffer registro;

    /**
     * Se inicializa el contador de series para ese genero y se genera un resultado con la serie y su puntuacion
     */
    public Resultado(String nombreSerie, int rating) {
        series = 1;
        registro = new StringBuffer(nombreSerie + " con puntuación " + rating);
    }

    /**
     * Se actualiza el contador de serie para ese genero y se muestra un resultado con la serie y su puntuacion
     */

    public void actualizar(String nombreSerie, int rating) {
        series += 1;
        registro.append("\n" + nombreSerie + " con puntuación " + rating);
    }

    /**
     * Se totaliza la cantidad de series que hay para ese genero
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(series + " series en este Genero.\n");
        sb.append("-----------------------------------\n");
        sb.append(registro);
        return sb.toString();
    }
}
