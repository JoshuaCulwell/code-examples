package pkg466project1;

import java.util.Iterator;

/**
 * Uses a linked list (nested) and a linear probing hash table (nested)
 * Linear probing is used to map the vertices to an integer index within the table array.
 * 
 * @author Joshua Culwell
 * @param <Data>
 */
public class Graph<Data> {
    public static class LL<Data> implements Iterable<Data>{
        private class Node<Data>{//Node holds the data in the doubley linked list
            private Node next,prev;//Linkes
            private final Data data;//Generic Data held in the node
            private Node(Data data){this.data = data;}//Constructor to make the node
        }
        private class IteratorList<Data> implements Iterator<Data>{//IteratorList is used to make the linked list an iterator
            Node<Data> current;//Holds where the iterator is looking
            public IteratorList(LL<Data> ll){current = (Node<Data>)ll.root;}//Constructor
            @Override//Overrides the super in Iterator
            public boolean hasNext() {return current != null;}//Checks if there is more to iterate
            @Override//Overrides the super in Iterator
            public Data next() {//Deos that actual iteration
                Data data = current.data;//Grabs data to return
                current = current.next;//Moves on
                return data;//Retruns data grabbed before
            }
        }
        
        private Node<Data> root, end;//Linked list datapoints
        private int size;
        public LL(){size = 0;}
        public void add(Data data){//Add a datapoint (node) to the front 
            size ++;//Increase size
            Node<Data> toPut = new Node(data);//Make the new node with the data
            if(root == null){//If there is no first node
                root = end = toPut;//Set first to last to the new node
                return;//Terminate function
            }
            toPut.next = root;//Set the new node.next to the root
            root = toPut;//set root to point at the new first node
            root.next.prev = root;//CHECK THIS
            if(size == 2) end.prev = toPut;//If this is the second node to be added make sure end looks at proper node
        }
        public void add_back(Data data){//Adds a new datapoint (node) to the back
            size ++;//Increase size
            Node<Data> toPut = new Node(data);//Create a new node using data
            if(root == null){//If root is null
                root = end = toPut;//set root and end as the new node
                return;//Terminate function
            }
            end.next = toPut;//set last node to put
            toPut.prev = end;//reset end
            end = end.next;//set end down to the actual end
        }
        public void enqueue(Data data){this.add_back(data);}
        public Data poll(){//for a queue poll returns/delets front value (same as dequeue)
            Data toReturn = root.data;//Grab root data
            this.remove(root.data);//Delete root
            return toReturn;//Return old root data
        }
        public Data pop(){return this.poll();}
        public Data dequeue(){return this.poll();}
        public void remove(Data data){
            size --;//Decrease size
            Node<Data> toDelete = get_node(data);//Get the node wanted to delete (correct pointer)
            if(toDelete == root && toDelete == end) root = end = null;//Set first/last to null if we just deleted the only node
            else if(toDelete == root){//if deleting first one 
                root = root.next;//set root to root.next
                root.prev = null;//set root.prev is null
            }else if(toDelete == end){//If we are deleting the end
                end = end.prev;//Set end to end.prev
                end.next = null;//Make sure the last node is null
            }else{//If we are deleting something in the middle
                toDelete.prev.next = toDelete.next;//Set itself .prev.next to itself.next (no longer pointing to itself)
                toDelete.next.prev = toDelete.prev;//Set itself .next.prev to itself.prev (no longer pointing to itself at all)
            }
        }
        private Node get_node(Data data){//GetNode function returns the node based off data
            Node<Data> temp = root;//Set first iterator to root
            while(temp != null){//Iterate through till null (end.next = null)
                if(temp.data.equals(data)) return temp; //If we find the node return it
                temp = temp.next;//iterate
            }
            return null;//if we don't find it return null
        }
        public boolean contains(Data data){
            Node<Data> temp = root;//Set first iterator to root
            while(temp != null){//While it has not reached the end
                if(temp.data.equals(data)) return true;//If it exists return true
                temp = temp.next;//Iterate
            }
            return false;//If doesn't exist return false;
        }
        public boolean isEmpty(){return size == 0;}
        public int size(){return size;}
        @Override //Override super iterable function
        public Iterator<Data> iterator() {return new IteratorList(this);}//Make linked list iterable
        
        @Override //Override super toString function
        public String toString(){
            String toString = "";//Create a string
            Node<Data> temp = root;//Make an iterator
            while(temp != null){//Iterate through LL
                toString += temp.data+" ";//Add data to the string
                temp = temp.next;//Iterate
            }
            return toString;//Return the string
        }
    }
    public static class HashTable<Data>{
        private Data[] keys;//Array of the "keys"
        private LL<Data>[] vals;//Array of linked lists to hold the edges
        private int size = 4, filled = 0;
        private boolean resize = true;
        private HashTable(int size){
            keys = (Data[]) new Object[this.size = size];//Generic array creation using object (also sets size)
            vals = new LL[size];//array of linked lists
            resize = false; //If resize is false it will not resize. 
        }
        private HashTable(){
            keys = (Data[]) new Object[size];
            vals = new LL[size];
        }
        private int hash(Data key){return Math.abs(key.hashCode()) % size;}//Hash function
        public boolean contains(Data key){return this.get(key) != null;}//Contains function (if it contains the key given
        private void resize(){//Resize function to double size of arrays
            HashTable<Data> newST = new HashTable(size * 2);//Creates a new self
            for(int i = 0; i < size; i++){//Iterates through origional arrays
                if(keys[i] != null) newST.put(keys[i], vals[i]);//Puts arrays into new self
            }
            size = newST.size;//Update values
            keys = newST.keys;//^
            vals = newST.vals;//^
        }
        public void put(Data key, LL val){//Put function using linear if collision
            if(filled == size-1 && resize) this.resize();
            int i;//index
            for(i = this.hash(key); keys[i] != null; i = (i+1) % size){//Go through array to find empty spot
                if(keys[i].equals(key)){//If we find the value while doing it update the LL
                    vals[i] = val;
                    return;//Terminate if this happens
                }
            }
            filled ++;//Increase filled once empty spot is found
            keys[i] = key;//Set the empty key spot to the key we want
            vals[i] = val;//set the empty val spot to the val we want
        }
        public LL<Data> get(Data key){//Get function
            for(int i = hash(key); keys[i] != null; i = (i+1) % size){//Finds the linked list associated with the key
                if(keys[i].equals(key)) return vals[i];//returns it
            }
            return null;//If it isn't found return null
        }
        public Data get_key_at_index(int index){return keys[index];}
        public LL<Data> get_val_at_index(int index){return vals[index];}
        public Data getFirst(Data key){//Function to get the first value in the linked list
            for(int i = hash(key); keys[i] != null; i = (i+1) % size){//Iterate through keys to find right one
                if(keys[i].equals(key) && vals[i].size > 0) return (Data)vals[i].root.data;//If found return the root data
            }
            return null;//If not found return null
        }
        public int loc(Data key){//loc function is to find the location in the array of a particular kye
            for(int i = hash(key); keys[i] != null; i = (i+1) % size){//Iterate through
                if(keys[i].equals(key)) return i;//Return i once found
            }
            return -1;//If it is not found return -1 (will break since it is private)
        }
        public Iterable<Data> keys(){//Iterable keys
            LL<Data> ll = new LL();//Make a new linked list
            for(int i = 0; i< size; i++) if(keys[i] != null) ll.add(keys[i]);//Fill linked list with the keys array
            return ll;//return it
        }
        public Data[] keys_arr(){ return keys;}
    }
    private HashTable<Data> vertices;
    private boolean undirected = false;
     
    /**
     * Constructor for the Graph class.
     * Does not take any variables. 
     * Automatically directed
     */
    public Graph(){ vertices = new HashTable();}

    /**
     * Constructor for the Graph class.
     * Takes in a boolean for if it is directed or not. 
     * @param directed: false makes an undirected graph.
     */
    public Graph(boolean directed){ 
        vertices = new HashTable();
        undirected = !directed;
    }
    /**
     * Constructor for the Graph class.
     * Takes in the number of vertices in the graph.
     * @param size
     */
    public Graph(int size){ vertices = new HashTable(size);}//Creates a new linearprobingST using size

    /**
     * Constructor for the Graph class.
     * Takes in the size for the graph and boolean for directed or not
     * @param size 
     * @param directed
     */
    public Graph(int size, boolean directed){
        vertices = new HashTable(size);
        undirected = !directed;
    }
    /**
     * Put function creates a new vertex using the given data
     * @param data
     */
    public void put(Data data){
        if(vertices.contains(data)) return;//If it already exists terminate
        vertices.put(data, new LL());//Make a new vertex with am empty linked list
    }

    /**
     * Put function creates 2 new verticies using the 2 given data instances then creates an edge between them
     * 
     * @param data1
     * @param data2
     */
    public void put(Data data1, Data data2){
        if(!vertices.contains(data1)) vertices.put(data1, new LL());//If it doesn't exist create a new vertex
        if(!vertices.contains(data2)) vertices.put(data2, new LL());//^ 
        this.make_edge(data1, data2);//Make edge between the two
    }

    /**
     * Makes an edge between 2 existing vertices given
     * @param data1
     * @param data2
     */
    public void make_edge(Data data1, Data data2){
        LL<Data> temp1 = vertices.get(data1);//Get the linked list for data1
        LL<Data> temp2 = vertices.get(data2);//^ (data2)
        if(!temp1.contains(data2))temp1.add(data2);//If it doesn't already contain the edge add the edge
        if(undirected) if(!temp2.contains(data1))temp2.add(data1);//^ 
        vertices.put(data1, temp1);//Put it back in (updates value)
        if(undirected) vertices.put(data2, temp2);//^ 
    }
    
    /**
     * Removes the edge between the 2 existing vertices
     * @param data1
     * @param data2
     */
    public void remove_edge(Data data1, Data data2){
        LL<Data> temp1 = vertices.get(data1);//Get the linked list for data1
        LL<Data> temp2 = vertices.get(data2);//^ (data2)
        if(temp1.contains(data2)) temp1.remove(data2);
        if(temp2.contains(data1)) temp2.remove(data1);
        vertices.put(data1, temp1);
        vertices.put(data2, temp2);
    }
    /**
     * Returns an iterable of the edges connected to a given vertex
     * @param data
     * @return Iterable of edges
     */
    public Iterable<Data> get(Data data){return vertices.get(data);}

    /**
     * Returns the first connected vertex to the given vertex
     * @param data
     * @return first connected vertex
     */
    public Data get_first(Data data){return vertices.getFirst(data);}
    
    /**
     * Returns the key from the given index in the hash table.
     * @param index
     * @return
     */
    public Data get_key_at_index(int index){return vertices.get_key_at_index(index);}
    public LL<Data> get_val_at_index(int index){return vertices.get_val_at_index(index);}
    /**
     * Breadth First Search algorithm to find the path between two vertices in the graph
     * Will return an empty iterable if no path.
     * @param from - starting vertex
     * @param to - ending vertex
     * @return Iterable of the path
     */
    public Iterable<Data> BFS(Data from, Data to){
        LL<Data> visited = new LL();//New Linked List to hold the visited vertices
        LL<Data> queue = new LL();//New Linked List to act as a queue
        
        queue.add_back(from);//Add starting node to the queue
        visited.add(from);//Add starting node to visited
        
        Graph<Data> parent = new Graph(true);//Create a new graph to hold the children/parent (directed)
        
        boolean found = false;//Found a path is set to false 
        while(queue.size != 0){//While the queue is not empty
            Data current = queue.poll();//Grab the first one "in line"
            if(current.equals(to)){//If we found the path
                found = true;//Set found to true
                break;//Don't keep looking
            }
            for(Data neighbor : vertices.get(current)){//Iterate through neighboring vertices
                if(!visited.contains(neighbor)){//If we haven't visited that vertex yet
                    queue.add_back(neighbor);//Add it to the queue
                    parent.put(neighbor, current);//Mark current as it's parent
                    visited.add(neighbor);//Add it to visited
                }
            }
        }
        LL<Data> path = new LL();//Create a new Linked List for the path
        if(found){//If we did find a path
            path.add(to);//Add the distination to the path
            while(parent.get_first(to) != null){//While there is a parent
                path.add(parent.get_first(to));//Add the parent of the child
                to = parent.get_first(to);//update the child to be the parent
            }
        }
        return path;//Return our new path
    }
    
    /**
     * Function to find the longest path starting from the given vertex 
     * Uses an implementation of Breadth First Search
     * @param from: the starting vertex
     * @return the length of the longest path 
     */
    public int longest_path(Data from){
        int[] distance = new int[vertices.size];//New integer array to hold the length of each path for each vertex
        for(int i = 0; i < distance.length; i++) distance[i] = -1;//Fill the array with 1
        distance[vertices.loc(from)] = 1;
        LL<Data> queue = new LL();//Make a new queue - BFS implimentation basically
        queue.add(from);//Add our first vertex
        int maxDistance = 0;//Set max distance to 0 (if not found returns 0)
        while(!queue.isEmpty()){//While there are things in teh queue
            Data parent = queue.poll();//Grab the top one
            int parentIndex = vertices.loc(parent);//Does linear probing to get existing location (won't hash to the same spot)
            for(Data neighbor : vertices.get(parent)){//For each neighbor to our just pulled vertex
                int neighborIndex = vertices.loc(neighbor);
                if(distance[neighborIndex] == -1 || !undirected){
                    distance[neighborIndex] = distance[parentIndex] + 1;//Set the length of the path to it as the path to the parent + 1
                    if(distance[neighborIndex] > maxDistance) maxDistance = distance[neighborIndex];
                    queue.add(neighbor);//Add it to the queue
                }
            }
        }
        return maxDistance;//Return the max distance.
    }
    
    /**
     * Returns an iterable of all the vertices in the graph
     * @return Iterable of vertices
     */
    public Iterable<Data> vertices(){return vertices.keys();}
    
    /**
     * Returns a hashtable of all the vertices in the graph
     * @return Graph.HashTable of all vertices. 
     */
    public HashTable<Data> get_vertices(){return vertices;}
    /**
     * Keys function returns all the vertices in the graph
     * @return an iterable of all vertices (not edges)
     */
    public Iterable<Data> keys(){return this.vertices();}

    /**
     * Returns the array of keys
     * @return Data array of the keys in the graph
     */
    public Data[] keys_arr(){return this.vertices.keys_arr();}
}