package soporte;

import java.io.Serializable;
import java.util.*;

/**
 * Clase para emular la funcionalidad de la clase java.util.Hashtable, pero implementada
 * en base al modelo de Resolucion de Colisiones por Direccionamiento Abierto. Modelo para
 * aplicar de base para el desarrollo del TPU.
 *
 * @author Ing. Valerio Frittelli.
 * @version Octubre de 2019.
 * @param <K> el tipo de los objetos que seran usados como clave en la tabla.
 * @param <V> el tipo de los objetos que seran los valores de la tabla.
 */
public class TSBHashTableDA<K,V> implements Map<K,V>, Cloneable, Serializable
{
    //************************ Constantes (privadas o publicas).

    // estados en los que puede estar una casilla o slot de la tabla...
    public static final int OPEN = 0;
    public static final int CLOSED = 1;
    public static final int TOMBSTONE = 2;

    //************************ Atributos privados (estructurales).

    // la tabla hash: el arreglo que contiene todos los objetos...
    private TSBArrayList<Map.Entry<K, V>> table;

    // el tama�o inicial de la tabla (tama�o con el que fue creada)...
    private int initial_capacity;
    
    // la cantidad de objetos que contiene la tabla...
    private int count;
    
    // el factor de carga para calcular si hace falta un rehashing...
    private float load_factor;
      
    
    //************************ Atributos privados (para gestionar las vistas).

    /*
     * (Tal cual est�n definidos en la clase java.util.Hashtable)
     * Cada uno de estos campos se inicializa para contener una instancia de la
     * vista que sea mas apropiada, la primera vez que esa vista es requerida. 
     * Las vistas son objetos stateless (no se requiere que almacenen datos, sino
     * que solo soportan operaciones), y por lo tanto no es necesario crear mas 
     * de una de cada una.
     */
    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K,V>> entrySet = null;
    private transient Collection<V> values = null;

    
    //************************ Atributos protegidos (control de iteracion).
    
    // conteo de operaciones de cambio de tamanio (fail-fast iterator).
    protected transient int modCount;
    
    
    //************************ Constructores.

    /**
     * Crea una tabla vaci�a, con la capacidad inicial igual a 11 y con factor 
     * de carga igual a 0.5f (que equivale a un nivel de carga del 50%).
     */    
    public TSBHashTableDA()
    {
        this(11, 0.5f);
    }
    
    /**
     * Crea una tabla vaci�a, con la capacidad inicial indicada y con factor 
     * de carga igual a 0.5f (que equivale a un nivel de carga del 50%).
     * @param initial_capacity la capacidad inicial de la tabla.
     */    
    public TSBHashTableDA(int initial_capacity)
    {
        this(initial_capacity, 0.5f);
    }

    /**
     * Crea una tabla vaci�a, con la capacidad inicial indicada y con el factor 
     * de carga indicado. Si la capacidad inicial indicada por initial_capacity 
     * es menor o igual a 0, la tabla sera creada de tamanio 11. Si el factor de
     * carga indicado es negativo, cero o mayor a 0.5, se ajustara a 0.5f. Si el
     * valor de initial_capacity no es primo, el tamanio se ajustara al primer
     * primo que sea mayor a initial_capacity.
     * @param initial_capacity la capacidad inicial de la tabla.
     * @param load_factor el factor de carga de la tabla.
     */
    public TSBHashTableDA(int initial_capacity, float load_factor)
    {
        if(load_factor <= 0 || load_factor > 0.5) { load_factor = 0.5f; }
        if(initial_capacity <= 0) { initial_capacity = 11; }
        else
        {
            if(!isPrime(initial_capacity))
            {
                initial_capacity = nextPrime(initial_capacity);
            }
        }
        
        this.table = new TSBArrayList<>(initial_capacity);
        for(int i=0; i<table.size(); i++)
        {
            Entry<K, V> e = new Entry<>(null, null);
            table.add(e);
        }
        
        this.initial_capacity = initial_capacity;
        this.load_factor = load_factor;
        this.count = 0;
        this.modCount = 0;
    }
    
    /**
     * Crea una tabla a partir del contenido del Map especificado.
     * @param t el Map a partir del cual se creara la tabla.
     */     
    public TSBHashTableDA(Map<? extends K,? extends V> t)
    {
        this(11, 0.5f);
        this.putAll(t);
    }
    
    
    //************************ Implementacion de metodos especificados por Map.
    
    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() 
    {
        return this.count;
    }

    /**
     * Determina si la tabla esta vaci�a (no contiene ningun elemento).
     * @return true si la tabla esta vaci�a.
     */
    @Override
    public boolean isEmpty() 
    {
        return (this.count == 0);
    }

    /**
     * Determina si la clave key esta en la tabla. 
     * @param key la clave a verificar.
     * @return true si la clave esta en la tabla.
     * @throws NullPointerException si la clave es null.
     */
    @Override
    public boolean containsKey(Object key) 
    {
        return (this.get((K)key) != null);
    }

    /**
     * Determina si alguna clave de la tabla esta asociada al objeto value que
     * entra como parametro. Equivale a contains().
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave esta asociada efectivamente a ese value.
     */    
    @Override
    public boolean containsValue(Object value)
    {
        return this.contains(value);
    }

    /**
     * Retorna el objeto al cual esta asociada la clave key en la tabla, o null 
     * si la tabla no contiene ningun objeto asociado a esa clave.
     * @param key la clave que sera buscada en la tabla.
     * @return el objeto asociado a la clave especificada (si existe la clave) o 
     *         null (si no existe la clave en esta tabla).
     * @throws NullPointerException si key es null.
     * @throws ClassCastException si la clase de key no es compatible con la 
     *         tabla.
     */
    @Override
    public V get(Object key) 
    {
        if(key == null) throw new NullPointerException("get(): parametro null");
        if (this.isEmpty()) return null; // Ya que no hay elementos
        K k; // Definimos una variable para hacer el casteo
        // Probamos que el casteo sea valido sino que arroje un ClassCastException
        try{k = (K) key;} 
        catch(ClassCastException c){throw new ClassCastException("get(): Llave incompatible");}
        
        
        // Obtenemos el codigo hash de la llave para partir a buscarlo a su casilla directa
        int hash = this.h(k);
        // Llamamos al metodo que busca el par para que busque su llave con su hash
        Entry <K, V> e = this.search_for_entry(k, hash);
        if (e != null && e.getState() != TOMBSTONE) {
            return e.getValue();
        }
        return null;
    }

    /**
     * Asocia el valor (value) especificado, con la clave (key) especificada en
     * esta tabla. Si la tabla conteni�a previamente un valor asociado para la 
     * clave, entonces el valor anterior es reemplazado por el nuevo (y en este 
     * caso el tamanio de la tabla no cambia). 
     * @param key la clave del objeto que se quiere agregar a la tabla.
     * @param value el objeto que se quiere agregar a la tabla.
     * @return el objeto anteriormente asociado a la clave si la clave ya 
     *         estaba asociada con alguno, o null si la clave no estaba antes 
     *         asociada a ningun objeto.
     * @throws NullPointerException si key es null o value es null.
     */
    @Override
    public V put(K key, V value) 
    {
       if(key == null || value == null) throw new NullPointerException("put(): parametro null");
       
       int ik = this.h((K)key);
       V old = null;
       Map.Entry<K, V> x = this.search_for_entry((K)key, ik);
       if(x != null) 
       {
           old = x.getValue();
           x.setValue(value);
       }
       else
       {
           if(this.load_level() >= this.load_factor) { this.rehash(); }
           int pos = search_for_OPEN(this.table, this.h(key));
           Map.Entry<K, V> entry = new Entry<>(key, value, CLOSED);
           table.set(pos, entry);
           this.count++;
           this.modCount++;
       }
       
       return old;
    }

    /**
     * Elimina de la tabla la clave key (y su correspondiente valor asociado).  
     * El metodo no hace nada si la clave no esta en la tabla. 
     * @param key la clave a eliminar.
     * @return El objeto al cual la clave estaba asociada, o null si la clave no
     *         estaba en la tabla.
     * @throws NullPointerException - if the key is null.
     */
    @Override
    public V remove(Object key) 
    {
        if(key == null) throw new NullPointerException("remove(): parametro null");
        int hash = this.h((K) key);
        int ind = search_for_index((K)key, hash);
        if (ind == -1) return null;
        Entry<K, V> e = (Entry<K, V>) table.get(ind);
        if (e != null) {
            e.setState(TOMBSTONE);
            V old = e.getValue();
            this.modCount ++;
            this.count --;
            table.set(ind, new Entry<K, V> (null, null));
            return old;
        }
        return null;
    }

    /**
     * Copia en esta tabla, todos los objetos contenidos en el map especificado.
     * Los nuevos objetos reemplazaran a los que ya existan en la tabla 
     * asociados a las mismas claves (si se repitiese alguna).
     * @param m el map cuyos objetos seran copiados en esta tabla. 
     * @throws NullPointerException si m es null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) 
    {
        if (m == null) {
            throw new NullPointerException("Parametro vacio");
        }
        for(Map.Entry<? extends K, ? extends V> e : m.entrySet())
        {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Elimina el contenido de la tabla, de forma de dejarla vaci�a. En esta
     * implementacion ademas, el arreglo de soporte vuelve a tener el tamanio que
     * inicialmente tuvo al ser creado el objeto.
     */
    @Override
    public void clear() 
    {
         // Se recrea la tabla de Map.Entry
        this.table = new TSBArrayList(initial_capacity);

        // Inicializo el vector de estados
        Entry <K, V> e;
        for (int i = 0; i < initial_capacity; i++) {
            e = new Entry<>(null, null);
        }
        this.count = 0;
        this.modCount++;

    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todas las claves (key)
     * contenidas en la tabla. El conjunto esta respaldado por la tabla, por lo 
     * que los cambios realizados en la tabla seran reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador esta actuando 
     * sobre el conjunto vista, el resultado de la iteracion sera indefinido 
     * (salvo que la modificacion sea realizada por la operacion remove() propia
     * del iterador, o por la operacion setValue() realizada sobre una entrada 
     * de la tabla que haya sido retornada por el iterador). El conjunto vista 
     * provee metodos para eliminar elementos, y esos metodos a su vez 
     * eliminan el correspondiente par (key, value) de la tabla (a traves de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll() 
     * y clear()). El conjunto vista no soporta las operaciones add() y 
     * addAll() (si se las invoca, se lanzara una UnsuportedOperationException).
     * @return un conjunto (un Set) a modo de vista de todas las claves
     *         mapeadas en la tabla.
     */
    @Override
    public Set<K> keySet() 
    {
        if(keySet == null) 
        { 
            // keySet = Collections.synchronizedSet(new KeySet()); 
            keySet = new KeySet();
        }
        return keySet;  
    }
        
    /**
     * Retorna una Collection (coleccion) a modo de vista de todos los valores
     * (values) contenidos en la tabla. La coleccion esta respaldada por la 
     * tabla, por lo que los cambios realizados en la tabla seran reflejados en 
     * la coleccion, y viceversa. Si la tabla es modificada mientras un iterador 
     * esta actuando sobre la coleccion vista, el resultado de la iteracion sera 
     * indefinido (salvo que la modificacion sea realizada por la operacion 
     * remove() propia del iterador, o por la operacion setValue() realizada 
     * sobre una entrada de la tabla que haya sido retornada por el iterador). 
     * La coleccion vista provee metodos para eliminar elementos, y esos metodos 
     * a su vez eliminan el correspondiente par (key, value) de la tabla (a 
     * traves de las operaciones Iterator.remove(), Collection.remove(), 
     * removeAll(), removeAll(), retainAll() y clear()). La coleccion vista no 
     * soporta las operaciones add() y addAll() (si se las invoca, se lanzara 
     * una UnsuportedOperationException).
     * @return una coleccion (un Collection) a modo de vista de todas los 
     *         valores mapeados en la tabla.
     */
    @Override
    public Collection<V> values() 
    {
        if(values==null)
        {
            // values = Collections.synchronizedCollection(new ValueCollection());
            values = new ValueCollection();
        }
        return values;    
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todos los pares (key, value)
     * contenidos en la tabla. El conjunto esta respaldado por la tabla, por lo 
     * que los cambios realizados en la tabla seran reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador esta actuando 
     * sobre el conjunto vista, el resultado de la iteracion sera indefinido 
     * (salvo que la modificacion sea realizada por la operacion remove() propia
     * del iterador, o por la operacion setValue() realizada sobre una entrada 
     * de la tabla que haya sido retornada por el iterador). El conjunto vista 
     * provee metodos para eliminar elementos, y esos metodos a su vez 
     * eliminan el correspondiente par (key, value) de la tabla (a traves de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll() 
     * and clear()). El conjunto vista no soporta las operaciones add() y 
     * addAll() (si se las invoca, se lanzara una UnsuportedOperationException).
     * @return un conjunto (un Set) a modo de vista de todos los objetos 
     *         mapeados en la tabla.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() 
    {
        if(entrySet == null) 
        { 
            // entrySet = Collections.synchronizedSet(new EntrySet()); 
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    
    //************************ Redefinicion de metodos heredados desde Object.
    
    /**
     * Retorna una copia superficial de la tabla. Las listas de desborde o 
     * buckets que conforman la tabla se clonan ellas mismas, pero no se clonan 
     * los objetos que esas listas contienen: en cada bucket de la tabla se 
     * almacenan las direcciones de los mismos objetos que contiene la original. 
     * @return una copia superficial de la tabla.
     * @throws java.lang.CloneNotSupportedException si la clase no implementa la
     *         interface Cloneable.    
     */ 
    @Override
    public Object clone() throws CloneNotSupportedException 
    {
        TSBHashTableDA<K, V> t = (TSBHashTableDA<K, V>)new TSBHashTableDA<>(this.table.size(), this.load_factor);
        // Copiamos 1 a 1 los elementos
        for(Map.Entry<K, V> entry : this.entrySet()){
            t.put(entry.getKey(), entry.getValue());
        }
        return t;
    }

    /**
     * Determina si esta tabla es igual al objeto especificado.
     * @param obj el objeto a comparar con esta tabla.
     * @return true si los objetos son iguales.
     */
    @Override
    public boolean equals(Object obj) 
    {
        if(!(obj instanceof Map)) { return false; }
        
        Map<K, V> t = (Map<K, V>) obj;
        if(t.size() != this.size()) { return false; }

        try 
        {
            Iterator<Map.Entry<K,V>> i = this.entrySet().iterator();
            while(i.hasNext()) 
            {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if(t.get(key) == null) { return false; }
                else 
                {
                    if(!value.equals(t.get(key))) { return false; }
                }
            }
        } 
        
        catch (ClassCastException | NullPointerException e) 
        {
            return false;
        }

        return true;    
    }

    /**
     * Retorna un hash code para la tabla completa.
     * @return un hash code para la tabla.
     */
    @Override
    public int hashCode() 
    {
        int hc = 0;
        // Haremos la sumatoria del hash code de cada elemento (Par K-V) del Entry
        for (Map.Entry<K, V> entry : table) {
            hc += entry.hashCode();
        }
        return hc;
    }
    
    /**
     * Devuelve el contenido de la tabla en forma de String.
     * @return una cadena con el contenido completo de la tabla.
     */
    @Override
    public String toString() 
    {
        // REVISAR... Asegurense de que funciona bien...
        StringBuilder cad = new StringBuilder("[");
        for(int i = 0; i < this.table.size(); i++)
        {
            Entry<K, V> entry = (Entry<K, V>) table.get(i);
            if(entry.getState() == CLOSED)
            {
                cad.append(entry.toString());
                cad.append(" ");
            }
        }
        cad.append("]");
        return cad.toString();
    }
    
    
    //************************ Metodos especi�ficos de la clase.

    /**
     * Determina si alguna clave de la tabla esta asociada al objeto value que
     * entra como parametro. Equivale a containsValue().
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave esta asociada efectivamente a ese value.
     */
    public boolean contains(Object value)
    {
        // Si el valor es nulo retorna falso
        if(value == null) return false;
        if (this.isEmpty()) return false; // Ya que no hay elementos
        if (this.entrySet == null) {
            entrySet = entrySet();
        }
        
        // Sino, llevaremos a cabo iteraciones con el iterador propio
        Iterator<Map.Entry<K,V>> i = this.entrySet.iterator();
        int ind = 0;
        while (i.hasNext()) {
            ind ++;
            Map.Entry<K, V> next = i.next();
            if (value.equals(next.getValue())) return true;
        }
        return false;
    }
    
    /**
     * Incrementa el tamanio de la tabla y reorganiza su contenido. Se invoca 
     * automaticamente cuando se detecta que la cantidad promedio de nodos por 
     * lista supera a cierto el valor critico dado por (10 * load_factor). Si el
     * valor de load_factor es 0.8, esto implica que el li�mite antes de invocar 
     * rehash es de 8 nodos por lista en promedio, aunque seria aceptable hasta 
     * unos 10 nodos por lista.
     */
    protected void rehash()
    {
        int old_length = this.table.size();
        
        // nuevo tamanio: primer primo mayor o igual al 50% del anterior...
        int new_length = nextPrime((int)(old_length * 1.5f));
        
        // crear el nuevo arreglo de tamanio new_length...
        TSBArrayList<Map.Entry<K,V>> temp = new TSBArrayList<>(new_length);
        Entry<K, V> e;
        for(int j=0; j<temp.size(); j++) { e = new Entry<>(null, null); temp.add(e);}
        
        // notificacion fail-fast iterator... la tabla cambio su estructura...
        this.modCount++;  
       
        // recorrer el viejo arreglo y redistribuir los objetos que tenia...
        for(int i=0; i<this.table.size(); i++)
        {
           // obtener un objeto de la vieja lista...
           Entry<K, V> x = (Entry<K, V>) table.get(i);

           // si la casilla esta cerrada...
           if(x.getState() == CLOSED)
           {
               // ...obtener el valor de dispersion en el nuevo arreglo...
               K key = x.getKey();
               int ik = this.h(key, temp.size());
               int y = search_for_OPEN(temp, ik);

               // ...insertar en el nuevo arreglo
               temp.set(y, x);
           }
        }
       
        // cambiar la referencia table para que apunte a temp...
        this.table = temp;
    }
    

    //************************ Metodos privados.
    
    /*
     * Funcion hash. Toma una clave entera k y calcula y retorna un i�ndice 
     * valido para esa clave para entrar en la tabla.     
     */
    private int h(int k)
    {
        return h(k, this.table.size());
    }
    
    /*
     * Funcion hash. Toma un objeto key que representa una clave y calcula y 
     * retorna un i�ndice valido para esa clave para entrar en la tabla.     
     */
    private int h(K key)
    {
        return h(key.hashCode(), this.table.size());
    }
    
    /*
     * Funcion hash. Toma un objeto key que representa una clave y un tamanio de 
     * tabla t, y calcula y retorna un i�ndice valido para esa clave dedo ese
     * tamanio.     
     */
    private int h(K key, int t)
    {
        return h(key.hashCode(), t);
    }
    
    /*
     * Funcion hash. Toma una clave entera k y un tamanio de tabla t, y calcula y 
     * retorna un i�ndice valido para esa clave dado ese tamanio.     
     */
    private int h(int k, int t)
    {
        if(k < 0) k *= -1;
        return k % t;        
    }

    private boolean isPrime(int n)
    {
        // negativos no admitidos en este contexto...
        if(n < 0) return false;

        if(n == 1) return false;
        if(n == 2) return true;
        if(n % 2 == 0) return false;

        int raiz = (int) Math.pow(n, 0.5);
        for(int div = 3;  div <= raiz; div += 2)
        {
            if(n % div == 0) return false;
        }

        return true;
    }

    private int nextPrime (int n)
    {
        if(n % 2 == 0) n++;
        for(; !isPrime(n); n+=2);
        return n;
    }

    /**
     * Calcula el nivel de carga de la tabla, como un numero en coma flotante entre 0 y 1.
     * Si este valor se multiplica por 100, el resultado es el porcentaje de ocupacion de la
     * tabla.
     * @return el nivel de ocupacion de la tabla.
     */
    private float load_level()
    {
        return (float) this.count / this.table.size();
    } 
    
    /*
     * Busca en la tabla un objeto Entry cuya clave coincida con key, a partir
     * de la posicion ik. Si lo encuentra, retorna ese objeto Entry. Si no lo
     * encuentra, retorna null. Aplica exploracion cuadratica.
     */
    private Entry<K, V> search_for_entry(K key, int ik)
    {
        int pos = search_for_index(key, ik);
        return pos != -1? (Entry<K, V>) table.get(pos) : null;
    }
    
    /*
     * Busca en la tabla un objeto Entry cuya clave coincida con key, a partir
     * de la posicion ik. Si lo encuentra, retorna su posicion. Si no lo encuentra,
     * retorna -1. Aplica exploracion cuadratica.
     */
    private int search_for_index(K key, int ik)
    {
        for(int j=0; ;j++)
        {
            int y = ik + (int)Math.pow(j, 2);
            y %= table.size();

            Entry<K, V> entry = (Entry<K, V>) table.get(y);
            if(entry.getState() == OPEN) { return -1; }
            if(key.equals(entry.getKey())) { return y; }
        }
    }

    /*
     * Retorna el i�ndice de la primera casilla abierta, a partir de la posicion ik,
     * en la tabla t. Aplica exploracion cuadratica.
     */
    private int search_for_OPEN(TSBArrayList<Map.Entry<K, V>> t, int ik)
    {
        for(int j=0; ;j++)
        {
            int y = ik + (int)Math.pow(j, 2);
            y %= t.size();

            Entry<K, V> entry = (Entry<K, V>) t.get(y);
            if(entry.getState() == OPEN) { return y; }
        }
    }
    
    

    //************************ Clases Internas.

    /*
     * Clase interna que representa los pares de objetos que se almacenan en la
     * tabla hash: son instancias de esta clase las que realmente se guardan en 
     * en cada una de las listas del arreglo table que se usa como soporte de 
     * la tabla. Lanzara una IllegalArgumentException si alguno de los dos 
     * parametros es null.
     */
    private class Entry<K, V> implements Map.Entry<K, V>
    {
        private K key;
        private V value;
        private int state;
        
        public Entry(K key, V value) 
        {
            this(key, value, OPEN);
        }

        public Entry(K key, V value, int state)
        {
            this.key = key;
            this.value = value;
            this.state = state;
        }

        @Override
        public K getKey() 
        {
            return key;
        }

        @Override
        public V getValue() 
        {
            return value;
        }

        public int getState() { return state; }

        @Override
        public V setValue(V value) 
        {
            if(value == null) 
            {
                throw new IllegalArgumentException("setValue(): parametro null...");
            }
                
            V old = this.value;
            this.value = value;
            return old;
        }

        public void setState(int ns)
        {
            if(ns >= 0 && ns < 3)
            {
                state = ns;
            }
        }
       
        @Override
        public int hashCode() 
        {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.key);
            hash = 61 * hash + Objects.hashCode(this.value);            
            return hash;
        }

        @Override
        public boolean equals(Object obj) 
        {
            if (this == obj) { return true; }
            if (obj == null) { return false; }
            if (this.getClass() != obj.getClass()) { return false; }
            
            final Entry other = (Entry) obj;
            if (!Objects.equals(this.key, other.key)) { return false; }
            if (!Objects.equals(this.value, other.value)) { return false; }            
            return true;
        }       
        
        @Override
        public String toString()
        {
            return "(" + key.toString() + ", " + value.toString() + ")";
        }
    }
    
    /*
     * Clase interna que representa una vista de todas los Claves mapeadas en la
     * tabla: si la vista cambia, cambia tambien la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no 
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus metodos gestionan en
     * forma directa el contenido de la tabla. Estan soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creacion de un Iterator (que incluye el metodo Iterator.remove()).
     */    
    private class KeySet extends AbstractSet<K> 
    {
        @Override
        public Iterator<K> iterator() 
        {
            return new KeySetIterator();
        }
        
        @Override
        public int size() 
        {
            return TSBHashTableDA.this.count;
        }
        
        @Override
        public boolean contains(Object o) 
        {
            return TSBHashTableDA.this.containsKey(o);
        }
        
        @Override
        public boolean remove(Object o) 
        {
            return (TSBHashTableDA.this.remove(o) != null);
        }
        
        @Override
        public void clear() 
        {
            TSBHashTableDA.this.clear();
        }
        
        private class KeySetIterator implements Iterator<K>
        {
            // Representa al indice actual
            private int current_index;
            
            // Representar al indice anterior
            private int last_index;

            // flag para controlar si remove() esta bien invocado...
            private boolean next_ok;
            
            // el valor que deberi�a tener el modCount de la tabla completa...
            private int expected_modCount;
            
            /*
             * Crea un iterador comenzando en la primera lista. Activa el 
             * mecanismo fail-fast.
             */
            public KeySetIterator()
            {
                next_ok = false;
                expected_modCount = TSBHashTableDA.this.modCount;
                current_index = -1;
                last_index = 0;
            }

            /*
             * Determina si hay al menos un elemento en la tabla que no haya 
             * sido retornado por next(). 
             */
            @Override
            public boolean hasNext() 
            {
                
                if (current_index >= table.size()) return false;
                // Buscamos el proximo indice cerrado
                for (int i = current_index+1; i < table.size(); i++) {
                    Entry<K, V> e = (Entry) table.get(i);
                    if(e.getState() == CLOSED) {
                        return true;
                    }
                }
                return false;
            }

            /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            @Override
            public K next() 
            {
                // control: fail-fast iterator...
                if(TSBHashTableDA.this.modCount != expected_modCount)
                {    
                    throw new ConcurrentModificationException("next(): modificacion inesperada de tabla...");
                }
                // Valida que existe uno nuevo
                if(!hasNext()) 
                {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                
                // Buscamos la proxima casilla cerrada
                int next_index = current_index+1;
                Entry<K, V> e = (Entry) table.get(next_index);
                while(e.getState() != CLOSED){
                    next_index ++;
                    e = (Entry) table.get(next_index);
                }
                last_index = current_index;
                current_index = next_index;

                
                // avisar que next() fue invocado con exito...
                next_ok = true;
                
                // Accedemos al par que est� en el indice actual
                K key = (K)(e.getKey());
                
                // y retornar la clave del elemento alcanzado...
                return key;
            }
            
            /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posicion anterior al que fue removido. El elemento removido es el
             * que fue retornado la ultima vez que se invoco a next(). El metodo
             * solo puede ser invocado una vez por cada invocacion a next().
             */
            @Override
            public void remove() 
            {
                // control: fail-fast iterator...
                if (TSBHashTableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("remove(): modificaci�n inesperada de tabla...");
                }

                if(!next_ok) 
                { 
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()..."); 
                }
                
                // Eliminacion del elemento retornado por el metodo next()
                Entry <K, V> e = (Entry) table.get(current_index);
                e.setState(TOMBSTONE);
                
                // El indice apunta al indice previo al eliminado
                current_index = last_index;
                
                
                // avisar que el remove() valido para next() ya se activo...
                next_ok = false;
                                
                // la tabla tiene un elementon menos...
                TSBHashTableDA.this.count--;

                // fail_fast iterator...
                TSBHashTableDA.this.modCount++;
                expected_modCount++;
            }     
        }
    }

    /*
     * Clase interna que representa una vista de todos los PARES mapeados en la
     * tabla: si la vista cambia, cambia tambien la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no 
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus metodos gestionan en
     * forma directa el contenido de la tabla. Estan soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creacion de un Iterator (que incluye el metodo Iterator.remove()).
     */    
    private class EntrySet extends AbstractSet<Map.Entry<K, V>> 
    {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() 
        {
            return new EntrySetIterator();
        }

        /*
         * Verifica si esta vista (y por lo tanto la tabla) contiene al par 
         * que entra como parametro (que debe ser de la clase Entry).
         */
        @Override
        public boolean contains(Object o) 
        {
            if(o == null) { return false; } 
            if (this.isEmpty()) return false; // Ya que no hay elementos
            if(!(o instanceof Entry)) { return false; }
            
            // Convertimos el elemento en un entry
            Entry<K, V> e = (Entry <K, V>) o, eAux;
            
            int indHash = TSBHashTableDA.this.h(e.getKey());
            int j = 1;
            
            for (int indiceCuadratico = indHash;  indiceCuadratico % table.size() != 0;indiceCuadratico += j^2) {
                eAux = (Entry <K, V>) table.get(indiceCuadratico);
                if (eAux.getState() == CLOSED) {
                    if (e.equals(eAux)) return true;
                }
                j++;
                if (indiceCuadratico >= table.size()) {
                    indiceCuadratico %= table.size();
                }
            }
            
            return false;
        }

        /*
         * Elimina de esta vista (y por lo tanto de la tabla) al par que entra
         * como parametro (y que debe ser de tipo Entry).
         */
        @Override
        public boolean remove(Object o) 
        {
            if(o == null) { throw new NullPointerException("remove(): parametro null");}
            if(!(o instanceof Entry)) { return false; }
            
            // Recorreremos con recorrido cuadratico el elemento
            Entry<K, V> e = (Entry<K, V>) o, eAux;
            int ih = TSBHashTableDA.this.h(e.getKey()), j = 0;
            for (int ic = ih; ic % table.size() == 0; ic += j^2) {
                eAux = (Entry <K, V>) table.get(ic);
                if (eAux.getState() == CLOSED) {
                    if (e.equals(eAux)) {
                        e.setValue(null);
                        TSBHashTableDA.this.modCount ++;
                        modCount++;
                        return true;}
                }
                j ++;
                if (ic >= table.size())ic %= table.size();
            }
            return false;
        }
        
        @Override
        public int size() 
        {
            return TSBHashTableDA.this.count;
        }

        @Override
        public void clear() 
        {
            TSBHashTableDA.this.clear();
        }
        
        private class EntrySetIterator implements Iterator<Map.Entry<K, V>>
        {
            // Colocamos el indice actual a recorrer
            private int current_index;
            
            // Colocamos el anterior indice para permitir el borrado
            private int last_index;
            
            // flag para controlar si remove() esta bien invocado...
            private boolean next_ok;
            
            // el valor que deberi�a tener el modCount de la tabla completa...
            private int expected_modCount;
            
            /*
             * Crea un iterador comenzando en la primera lista. Activa el 
             * mecanismo fail-fast.
             */
            public EntrySetIterator()
            {
                current_index = -1;
                last_index = 0;
                next_ok = false;
                expected_modCount = TSBHashTableDA.this.modCount;
            }

            /**
             * Determina si hay al menos un elemento en la tabla que no haya 
             * sido retornado por next().
             * @return un booleano para determinar si existe o no una casilla siguiente
             */
            @Override
            public boolean hasNext() 
            {
                if (current_index >= table.size()) return false;
                // Busco el siguiente indice de casilla cerrada
                Entry <K, V> e;
                for (int i = current_index+1; i < table.size(); i++) {
                    e = (Entry) table.get(i);
                    if (e.getState() == CLOSED) {
                        return true;
                    }
                }
                return false;
            }

            /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            @Override
            public Map.Entry<K, V> next() 
            {
                // control: fail-fast iterator...
                if(TSBHashTableDA.this.modCount != expected_modCount)
                {    
                    throw new ConcurrentModificationException("next(): modificacion inesperada de tabla...");
                }
                
                if(!hasNext()) 
                {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                
                
                int next_index = current_index+1;
                Entry<K, V> e = (Entry<K, V>) table.get(next_index);   
                while(e.getState() != CLOSED) {
                    next_index ++;
                    e = (Entry<K, V>) table.get(next_index);
                }
                last_index = current_index;
                current_index = next_index;
                // avisar que next() fue invocado con exito...
                next_ok = true;
                // y retornar el elemento alcanzado...
                return e;
            }
            
            /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posicion anterior al que fue removido. El elemento removido es el
             * que fue retornado la ultima vez que se invoco a next(). El metodo
             * solo puede ser invocado una vez por cada invocacion a next().
             */
            @Override
            public void remove() 
            {
                if(!next_ok) 
                { 
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()..."); 
                }
                
                Entry<K, V> e = (Entry<K, V>) table.get(current_index);
                e.setState(TOMBSTONE);
                
                last_index = current_index;

                // avisar que el remove() valido para next() ya se activo...
                next_ok = false;
                                
                // la tabla tiene un elementon menos...
                TSBHashTableDA.this.count--;

                // fail_fast iterator...
                TSBHashTableDA.this.modCount++;
                expected_modCount++;
            }     
        }
    }    
    
    /*
     * Clase interna que representa una vista de todos los VALORES mapeados en 
     * la tabla: si la vista cambia, cambia tambien la tabla que le da respaldo, 
     * y viceversa. La vista es stateless: no mantiene estado alguno (es decir, 
     * no contiene datos ella misma, sino que accede y gestiona directamente los
     * de otra fuente), por lo que no tiene atributos y sus metodos gestionan en
     * forma directa el contenido de la tabla. Estan soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creacion de un Iterator (que incluye el metodo Iterator.remove()).
     */ 
    private class ValueCollection extends AbstractCollection<V> 
    {
        @Override
        public Iterator<V> iterator() 
        {
            return new ValueCollectionIterator();
        }
        
        @Override
        public int size() 
        {
            return TSBHashTableDA.this.count;
        }
        
        @Override
        public boolean contains(Object o) 
        {
            return TSBHashTableDA.this.containsValue(o);
        }
        
        @Override
        public void clear() 
        {
            TSBHashTableDA.this.clear();
        }
        
        private class ValueCollectionIterator implements Iterator<V>
        {
            // Representa el indice actual
            private int current_index;
            
            // Representa el indice previo para la aplicacion del borrado
            private int last_index;

            // flag para controlar si remove() esta bien invocado...
            private boolean next_ok;
            
            // el valor que deberi�a tener el modCount de la tabla completa...
            private int expected_modCount;
            
            /*
             * Crea un iterador comenzando en la primera lista. Activa el 
             * mecanismo fail-fast.
             */
            public ValueCollectionIterator()
            {
                current_index = -1;
                last_index = 0;
                next_ok = false;
                expected_modCount = TSBHashTableDA.this.modCount;
            }

            /*
             * Determina si hay al menos un elemento en la tabla que no haya 
             * sido retornado por next(). 
             */
            @Override
            public boolean hasNext() 
            {
                if (current_index >= table.size()) return false;
                
                Entry<K, V> e;
                for (int i = current_index+1; i < table.size(); i++) {
                    e = (Entry<K, V>) table.get(i);
                    if (e.getState() == CLOSED) {
                        return true;
                    }
                }
                return false;
            }

            /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            @Override
            public V next() 
            {
                // control: fail-fast iterator...
                if(TSBHashTableDA.this.modCount != expected_modCount)
                {    
                    throw new ConcurrentModificationException("next(): modificacion inesperada de tabla...");
                }
                
                if(!hasNext()) 
                {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                
                // Se busca el proximo par k-v que est� cerrado
                int next_index = current_index+1;
                Entry<K, V> e = (Entry <K, V>) table.get(next_index);
                while(e.getState()!= CLOSED){
                    next_index++;
                    e = (Entry <K, V>) table.get(next_index);
                }
                
                // Se actualizan los indices
                last_index = current_index;
                current_index = next_index;
                // avisar que next() fue invocado con exito...
                next_ok = true;
                
                // y retornar la clave del elemento alcanzado...
                V value = e.getValue();
                return value;
            }
            
            /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posicion anterior al que fue removido. El elemento removido es el
             * que fue retornado la ultima vez que se invoco a next(). El metodo
             * solo puede ser invocado una vez por cada invocacion a next().
             */
            @Override
            public void remove() 
            {
                if(!next_ok) 
                { 
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()..."); 
                }
                
                // Se cambia el estado del elemento
                Entry<K, V> e = (Entry <K, V>) table.get(current_index);
                e.setState(TOMBSTONE);
                
                // Se apunta al elemento anterior
                
                last_index = current_index;
                
                // avisar que el remove() valido para next() ya se activo...
                next_ok = false;
                                
                // la tabla tiene un elemento menos...
                TSBHashTableDA.this.count--;

                // fail_fast iterator...
                TSBHashTableDA.this.modCount++;
                expected_modCount++;
            }     
        }
    }
}
