package negocio;

import soporte.TSBHashTableDA;

import java.util.Collection;

public class Estadisticas {

    private TSBHashTableDA tabla;
    /**
     * Creamos una hashtable para guardar las estadisticas
     */
    public Estadisticas() {
        tabla = new TSBHashTableDA<>();
    }
    /**
     * Agregamos un nuevo registro en la hastable
     */
    public void agregar(Object clave, String nombreSerie, int rating)
    {
        Resultado item = (Resultado) tabla.get(clave);
        if(item== null)
            tabla.put(clave,new Resultado(nombreSerie, rating));
        else
            item.actualizar(nombreSerie,rating);
    }

    /**
     * Se busca segun un genero en la tabla
     */
    public Object buscar(Object clave)
    {
        return tabla.get(clave);
    }
    /**
     * Se obtiene los generos en la tabla
     */
    public Collection getGeneros()
    {
        return tabla.keySet();
    }


}
