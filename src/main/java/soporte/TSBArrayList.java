package soporte;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/**
 * Una clase para emular el concepto de lista implementada sobre un arreglo, tal
 * como la clase java.util.ArrayList de Java (y al estilo de la clase List de 
 * Python). Se apl�c� una estrategia de desarrollo basada en emular en todo lo
 * posible el comportamiento de la clase Java.util.ArrayList tal como se define
 * en la documentaci�n javadoc de la misma, pero sin entrar en el c�digo fuente
 * original (o sea, una estrategia de desarrollo tipo "clean room": se puede
 * analizar la documentaci�n y los requerimientos, pero no el c�digo fuente 
 * fuente ya existente). 
 * 
 * En esta segunda versi�n (y definitiva) la clase TSBArrayList deriva de la
 * clase AbsttactList (tal como java.util.ArrayList) e implementa las mismas
 * interfaces que implementa java.util.ArrayList. Se siguen aqu� todas las 
 * recomendaciones de implementaci�n disponibles en la documentaci�n javadoc de
 * la clase AbstractList. 
 * 
 * @author Ing. Valerio Frittelli - Ing. Felipe Steffolani.
 * @version Agosto de 2017 - Version 2.0 (final).
 * @param <E> la clase cuyos objetos ser�n admisibles para la lista.
 */
public class TSBArrayList<E> extends AbstractList<E> 
             implements List<E>, RandomAccess, Cloneable, Serializable
{
    // el arreglo que contendr� los elementos...
    private Object[] items;
    
    // el tama�o inicial del arreglo...
    private int initial_capacity;
    
    // la cantidad de casillas realmente usadas...
    private int count;

    /**
     * Crea una lista con capacidad inicial de 10 casilleros, pero ninguno
     * ocupado realmente: la lista est� vac�a a todos los efectos pr�cticos. 
     * Este constructor es sugerido desde la documentaci�n de la clase 
     * AbstractList.
     */
    public TSBArrayList()
    {
        this(10);
    }
    
    /**
     * Crea una lista conteniendo los elementos de la colecci�n que viene como
     * par�metro, en el orden en que son retornados por el iterador de esa
     * colecci�n. Si par�metro c es null, el m�todo lanza una excepci�n de 
     * NullPointerException. Este constructor es sugerido desde la documentaci�n 
     * de la clase AbstractList.
     * @param c la colecci�n cuyos elementos ser�n copiados en la lista.
     * @throws NullPointerException si la referencia c es null.
     */
    public TSBArrayList(Collection<? extends E> c)
    {
        this.items = c.toArray();
        initial_capacity = c.size();
        count = c.size();
    }

    /**
     * Crea una lista con initialCapacity casilleros de capacidad, pero ninguno
     * ocupado realmente: la lista est� vac�a a todos los efectos pr�cticos. Si 
     * el valor de initialCapacity es <= 0, el valor se ajusta a 10.
     * @param initialCapacity la capacidad inicial de la lista.
     */
    public TSBArrayList(int initialCapacity)
    {
        if (initialCapacity <= 0) 
        {
            initialCapacity = 10;
        }
        items = new Object[initialCapacity];
        initial_capacity = initialCapacity;
        count = 0;
    }
    
    /**
     *  Metodo encargado de colocar un nuevo elemento a la lista
     * si este no excede el tama�o minimo
     */
    @Override
    public boolean add(E e){
        if (e == null) {
            throw new IllegalArgumentException("add(E e): Elemento nulo");
        }
        if (count >= size()) {
            ensureCapacity((count+1) *2);
        }
        items[count] = e;
        count ++;
        return true;
    }


    /**
     * A�ade el objeto e en la posisic�n index de la lista . La inserci�n ser� 
     * rechazada si la referencia e es null (nen ese caso, el m�todo sale sin
     * hacer nada). Si index coincide con el tama�o de la lista, el objeto e 
     * ser� agregado exactamente al final de la lista, como si se hubiese 
     * invocado a add(e). Este m�todo es sugerido desde la documentaci�n de la 
     * clase AbstractList.
     * @param index el �ndice de la casilla donde debe quedar el objeto e.
     * @param e el objeto a agregar en la lista.
     * @throws IndexOutOfBoundsException si index < 0 o index > size().
     */
    @Override
    public void add(int index, E e)
    {        
        if(index > count || index < 0)
        {
            throw new IndexOutOfBoundsException("add(): �ndice fuera de rango...");
        }
        
        if(e == null) return;
        
        if(count == items.length) this.ensureCapacity(items.length * 2);
        
        int t = count - index;
        System.arraycopy(items, index, items, index+1, t);
        items[index] = e;
        count++;
        
        // detecci�n r�pida de fallas en el iterador (fail-fast iterator)...
        // modCount se hereda desde AbstractList y es protected...
        this.modCount++;  
    }   
    
    /**
     * Elimina todo el contenido de la lista, y reinicia su capacidad al valor
     * de la capacidad con que fue creada originalmente. La lista queda vac�a 
     * luego de invocar a clear().
     */
    @Override
    public void clear()
    {
        items = new Object[initial_capacity];
        count = 0;
        
        // detecci�n r�pida de fallas en el iterador (fail-fast iterator)...
        // modCount se hereda desde AbstractList y es protected...
        this.modCount = 0; 
    }
    
    /**
     * Retorna una copia superficial de la lista (no se clonan los objetos que
     * la lista contiene: se retorna una lista que contiene las direcciones de
     * los mismos objetos que contiene la original).
     * @return una copia superficial de la lista.
     * @throws java.lang.CloneNotSupportedException si la clase no implementa la
     *         interface Cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        TSBArrayList<?> temp = (TSBArrayList<?>) super.clone();
        temp.items = new Object[count];
        System.arraycopy(this.items, 0, temp.items, 0, count);

        // detecci�n r�pida de fallas en el iterador (fail-fast iterator)...
        // modCount se hereda desde AbstractList y es protected...
        temp.modCount = 0; 

        return temp;
    }
    
    /**
     * Devuelve true si la lista contiene al elemento e. Si e es null el m�todo
     * retorna false. Puede lanzar una excepci�n de ClassCastException si la clase
     * de e no es compatible con el contenido de la lista.
     * @param e el objeto a buscar en la lista.
     * @return true si la lista contiene al objeto e.
     * @throws ClassCastException si e no es compatible con los objetos de la lista.
     */
    @Override
    public boolean contains(Object e)
    {
        if(e == null) return false;
        
        for(int i=0; i<count; i++)
        {
            if(e.equals(items[i])) return true;
        }
        return false;
    }    
    
    /**
     * Aumenta la capacidad del arreglo de soporte, si es necesario, para 
     * asegurar que pueda contener al menos un n�mero de elementos igual al 
     * indicado por el par�metro minCapacity.
     * @param minCapacity - la m�nima capacidad requerida.
     */
    public void ensureCapacity(int minCapacity)
    {
        if(minCapacity == items.length) return;
        if(minCapacity < count) return;
        
        Object[] temp = new Object[minCapacity];
        System.arraycopy(items, 0, temp, 0, count);
        items = temp;
    }
    
    /**
     * Retorna el objeto contenido en la casilla index. Si el valor de index no 
     * es v�lido, el m�todo lanzar� una excepci�n de la clase
     * IndexOutOfBoundsException. Este m�todo es sugerido desde la documentaci�n 
     * de la clase AbstractList.
     * @param index �ndice de la casilla a acceder.
     * @return referencia al objeto contenido en la casilla index.
     * @throws IndexOutOfBoundsException si index < 0 o index >= size().
     */
    @Override
    public E get(int index)
    {
        if (index < 0 || index > count)
        {   
            throw new IndexOutOfBoundsException("get(): �ndice fuera de rango...");
        }
        return (E) items[index];
    }
    
    /**
     * Devuelve true si la lista no contiene elementos.
     * @return true si la lista est� vac�a.
     */
    @Override
    public boolean isEmpty()
    {
        return (count == 0);
    }   
    
    /**
     * Remueve de la lista el elemento contenido en la posici�n index. Los 
     * objetos ubicados a la derecha de este, se desplazan un casillero a la 
     * izquierda. El objeto removido es retornado. La capacidad de la lista no
     * se altera. Si el valor de index no es v�lido, el m�todo lanzar� una 
     * excepci�n de IndexOutOfBoundsException. Este m�todo es sugerido desde la 
     * documentaci�n de la clase AbstractList.
     * @param index el �ndice de la casilla a remover.
     * @return el objeto removido de la lista.
     * @throws IndexOutOfBoundsException si index < 0 o index >= size().
     */
    @Override
    public E remove(int index)
    {
        if(index >= count || index < 0)
        {
            throw new IndexOutOfBoundsException("remove(): �ndice fuera de rango...");
        }
        
        int t = items.length;
        if(count < t/2) this.ensureCapacity(t/2);
        
        Object old = items[index];
        int n = count;
        System.arraycopy(items, index+1, items, index, n-index-1);
        count--;
        items[count] = null;
        
        // detecci�n r�pida de fallas en el iterador (fail-fast iterator)...
        // modCount se hereda desde AbstractList y es protected...
        this.modCount++; 
        
        return (E) old;
    }

    /**
     * Reemplaza el objeto en la posici�n index por el referido por element, y
     * retorna el objeto originalmente contenido en la posici�n index. Si el 
     * valor de index no es v�lido, el m�todo lanzar� una excepci�n de la clase
     * IndexOutOfBoundsException. Este m�todo es sugerido desde la documentaci�n 
     * de la clase AbstractList.
     * @param index �ndice de la casilla a acceder.
     * @param element el objeto que ser� ubicado en la posici�n index.
     * @return el objeto originalmente contenido en la posici�n index.
     * @throws IndexOutOfBoundsException si index < 0 o index >= size().
     */
    @Override
    public E set(int index, E element)
    {
        if (index < 0 || index >= count)
        {
            throw new IndexOutOfBoundsException("set(): �ndice fuera de rango...");
        }
        Object old = items[index];
        items[index] = element;
        return (E) old;
    }

    /**
     * Retorna el tama�o de la lista: la cantidad de elementos realmente 
     * contenidos en ella. Este m�todo es sugerido desde la documentaci�n de la 
     * clase AbstractList.
     * @return la cantidad de elementos que la lista contiene.
     */
    public int count(){
        return count;
    }
    
    @Override
    public int size(){
        return items.length;
    }
       
    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append('{');
        for (int i=0; i<count; i++)
        {
            buff.append(items[i]);
            if(i < count-1)
            {
                buff.append(", ");
            }
        }
        buff.append('}');
        return buff.toString();
    }
    
    /**
     * Ajusta el tama�o del arreglo de soporte, para que coincida con el tama�o
     * de la lista. Puede usarse este m�todo para que un programa ahorre un poco
     * de memoria en cuanto al uso de la lista, si es necesario.
     */
    public void trimToSize()
    {
        if(count == items.length) return;
        
        Object temp[] = new Object[count];
        System.arraycopy(items, 0, temp, 0, count);
        items = temp;
    }
}
