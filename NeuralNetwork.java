package neuralnetworksround2;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

/**
 * Neural Network Object
 * Uses java.io. File / FileWriter, java.util.Random / Scanner
 * 
 * Uses pooling if the input size is greater than the input size that the system is trained to. 
 * Pooling is default set to average the values to make it fit but can be set to max values. 
 *      Pooling algorithm will drop values off the end of the vector if the input and first layer are close enough in size.
 * 
 * 
 * 
 * @author Joshua Culwell
 */
public class NeuralNetwork {
    Random rand = new Random();
    private class Neuron{
        private double[] weights; //the weights that get multiplied with the previous layers output to find the value
        private double bias; //The bias that will be added to the value which is unaffected by previsous layer output
        private double value; //the value found after the weights and previous layers outputs are multiplied
        private int size;//Holds the amount of weights there are and therefore how many inputs it can take
        //Backpropagation Variables
        private double error;
        public Neuron(int size){
            int range = sigmoid ? 0 : -1;
            
            weights = new double[size];
            bias = rand.nextDouble(range,1); 
            for(int i = 0; i < size; i++) weights[i] = rand.nextDouble(range,1);//Propagate weights with random values (will get changed during back propagation) 
            this.size = size;
        }
    }
    private class NeuronLayer{
        private Neuron[] neurons;//Array of neurons in each layer
        private int size;//Amount of neurons in the layer
        public NeuronLayer(int size, int prevLayerSize){
            neurons = new Neuron[size];
            for(int i = 0; i < neurons.length; i++)neurons[i] = new Neuron(prevLayerSize);//Creates the layers with the right number of weights to correspond to the previous layers size. 
            this.size = size;
        }
    }

    private NeuronLayer[] layers;//Array of each layer within the neural network
    private boolean loaded;//Used to indicate if the NN has been initialized and avoid errors
    private boolean sigmoid;//Used to know if output is T/F or if it is dynamic output values and if pooling is used
    private File file;//If loaded from a file it will use this file in the future.
    public double learningRate;
    
    /**
     * Neural Network Constructor.Takes in an array of integers to dictate the input layer, output, and any hidden layers (output layer will act as a "hidden layer")
     * Takes in the learning rate -- How quickly back propagation can shift it 
     * @param layerSizes
     * @param learningRate
     * @param sigmoid
     * @param bias
     */
    public NeuralNetwork(int[] layerSizes, double learningRate, boolean sig){
        layers = new NeuronLayer[layerSizes.length];
        for(int i = 0; i < layers.length; i++)layers[i] = new NeuronLayer(layerSizes[i], (i > 0) ? layerSizes[i-1] : 0);
        loaded = true;
        sigmoid = sig;
        this.learningRate = learningRate;
    }
    
    /**
     * Generic construction of the NeuralNetwork. Sets loaded as false to keep things clean.
     */
    public NeuralNetwork(){ loaded = false;}
    
    public double activation(double sum){ return sigmoid ? sigmoid(sum) : tahn(sum);}
    private static double sigmoid(double sum){ return 1.0 / (1.0 + Math.exp(-sum));}
    private static double tahn(double sum){ return (Math.exp(sum) - Math.exp(-sum))/(Math.exp(sum) + Math.exp(-sum));}
    
    public double divActivation(double sum){ return sigmoid ? divSigmoid(sum) : divTahn(sum);}
    private static double divSigmoid(double sum){ return sigmoid(sum) * (1 - sigmoid(sum));}
    private static double divTahn(double sum){ return (1 / ((Math.exp(sum) + Math.exp(-sum)) / 2)) * (1 / ((Math.exp(sum) + Math.exp(-sum)) / 2));}
    /**
     * ForwarePropagate runs the net using the given inputs. Inputs must be the same size as the input layer at this time. 
     * Currently doesn't use a sigmoid/equalization function. 
     * 
     * @param inputs
     * @return
     */
    public double[] forwardPropagate(double[] inputs){
        if(loaded == false) return new double[0];
//        if(inputs.length > layers[0].neurons.length){
//            double[] newInput = this.pool(inputs, layers[0].neurons.length, false);
//            inputs = newInput;
//        }
        for(int i = 0; i < layers[0].neurons.length; i++) layers[0].neurons[i].value = inputs[i]; 
        for(int i = 1; i < layers.length; i++){
            for(int j = 0; j < layers[i].neurons.length; j++){
                double sum = 0;
                for(int k = 0; k < layers[i].neurons[j].weights.length; k++) sum += layers[i].neurons[j].weights[k] * layers[i-1].neurons[k].value;
                sum /= layers[i].neurons[j].weights.length;
                sum += layers[i].neurons[j].bias;
                layers[i].neurons[j].value = sum;
            }
        }
        double[] output = new double[layers[layers.length-1].size];
        for(int j = 0; j < output.length; j++) output[j] = activation(layers[layers.length-1].neurons[j].value);        
        return output;
    }
    
    public static double cost(double[] outputs, double[] expectedOutputs){
        double cost = 0;
        for(int i = 0; i< outputs.length; i++) cost += Math.pow((outputs[i] - expectedOutputs[i]), 2);
        return cost;
    }
    /**
     * BackPropagation algorithm. Uses fancy math that took forever to learn.
     * WORKS!!!
     * 
     * @param inputs
     * @param expectedOutputs
     * @return The cost of the current system (1 iteration before current) 
     */
    public double backPropagate(double[] inputs, double[] expectedOutputs){
        double[] outputs = this.forwardPropagate(inputs);
        //Bottom layer: derivative of the cost with respect to the activation
        for(int i = 0; i < outputs.length; i++)layers[layers.length-1].neurons[i].error = 2 * (outputs[i] - expectedOutputs[i]) * divActivation(layers[layers.length-1].neurons[i].value);
        //Every other layer: derivative of the neurons of the layer below itself with respect to it's own activation
        for(int i = layers.length-2; i > 0; i--){
            for(int j = 0; j < layers[i].neurons.length; j++){
                double sum = 0;
                for(int k = 0; k < layers[i+1].neurons.length; k++)sum += layers[i+1].neurons[k].weights[j] * layers[i+1].neurons[k].error;
                layers[i].neurons[j].error = sum * divActivation(layers[i].neurons[j].value);
            }
        }
        //Updating the values and final derivative
        for(int i = 1; i < layers.length; i++){
            for(int j = 0; j < layers[i].neurons.length; j++){
                //Bias: Derivative of the cost with respect to the activation (calculated earlier)
                double delta = learningRate * layers[i].neurons[j].error;
                layers[i].neurons[j].bias -= delta;
                //Weights: Derivative of the cost with respect to each indivisual weight using derivative of activation calculated earlier
                for(int k = 0; k < layers[i].neurons[j].weights.length; k++)layers[i].neurons[j].weights[k] -= delta * activation(layers[i-1].neurons[k].value);
            }
        }
        return cost(outputs, expectedOutputs);
    }
        
    //----------------SAVING/LOADING DATA BELOW THIS LINE---------------------\\
    
    /**
     * Compiles all the weights in the neural network into a 3D array
     * First index = neuron layer
     * Second index = neurons in the layer
     * Third index = weights in the neuron
     * @return 3D array of all weights in the NN 
     */
    public double[][][] saveWeights(){
        if(loaded == false) return new double[0][0][0];
        double[][][] weights = new double[layers.length][][];
        for(int i = 0; i< weights.length; i++){
            weights[i] = new double[layers[i].neurons.length][];
            for(int j = 0; j < weights[i].length; j++){
                weights[i][j] = new double[layers[i].neurons[j].weights.length];
                for(int k = 0; k < weights[i][j].length; k++){
                    weights[i][j][k] = layers[i].neurons[j].weights[k];
                }
            }
        }
        return weights;
    }
    /**
     * Compiles all the biases in the neural network into a 2D array
     * First index = neuron layer
     * Second index = bias for the neuron in that layer
     * @return 2D array of the biases in the NN
     */
    public double[][] saveBiases(){
        if(loaded == false) return new double[0][0];
        double[][] biases = new double[layers.length][];
        for(int i = 0; i < biases.length; i++){
            biases[i] = new double[layers[i].neurons.length];
            for(int j = 0; j < biases[i].length; j++){
                biases[i][j] = layers[i].neurons[j].bias;
            }
        }
        return biases;
    }
    /**
     * Loads the NN with the given inputs 
     * @param weights 3D array of doubles for the weights in the NN
     * @param biases 2D array of doubles for the biases in the NN
     * @param learningRate double for how quickly it "learns"
     * @param sigmoid boolean on if a sigmoid function is used 
     * @return boolean to indicate if the loading was successful
     */
    public boolean loadValues(double[][][] weights, double[][] biases, double learningRate, boolean sigmoid){
        loaded = false;
        layers = new NeuronLayer[weights.length];
        for(int i = 0; i < layers.length; i++)layers[i] = new NeuronLayer(weights[i].length, (i > 0) ? weights[i-1].length : 0);
        for(int i = 0; i < layers.length; i++){
            for(int j = 0; j < layers[i].neurons.length; j++){
                layers[i].neurons[j].bias = biases[i][j];
                for(int k = 0; k < layers[i].neurons[j].weights.length; k++){
                    layers[i].neurons[j].weights[k] = weights[i][j][k];
                }
            }
        }
        this.learningRate = learningRate;
        this.sigmoid = sigmoid;
        loaded = true;
        
        return loaded;
    }
    /**
     * Saves all weights and biases into a file 
     * @param file
     * @return boolean to indicate a successful/failed save
     */
    public boolean saveToFile(File file){
        try{
            this.file = file;
            FileWriter writer = new FileWriter(file);
            String toWrite = ""; 
            boolean newLine = false;
            double[][][] weights = this.saveWeights();
            double[][] biases = this.saveBiases();                
            
            
            toWrite += learningRate + " " + sigmoid + "\n";
            
            for(int i = 0; i < weights.length; i++){
                toWrite += "l\n";
                for(int j = 0; j < weights[i].length; j++){
                    toWrite += "n\n";
                    newLine = false;
                    for(int k = 0; k < weights[i][j].length; k++){
                        toWrite += weights[i][j][k]+ ",";
                        newLine = true;
                    }
                    toWrite += newLine ? "\n" : "";
                }
            }
            toWrite += "b\n";
            for(int i = 0; i < biases.length; i++){
                toWrite += "l\n";
                newLine = false;
                for(int j = 0; j < biases[i].length; j++){
                    toWrite +=biases[i][j] + ",";
                    newLine = true;
                }
                toWrite += newLine ? "\n" : "";
            }
            writer.write(toWrite);
            writer.close();
            return true;
        }catch(Exception E){
            E.printStackTrace();
            return false;
        }
    }
    /**
     * Used to save to the file that was originally used to load from
     * @return if the save was successful
     */
    public boolean save(){
        if(!loaded) return false;
        if(file == null) return false;
        return this.saveToFile(file);
    }
    /**
     * A way to load saved weights and biases from a file into the NN.
     * Will load a new NN if one is already loaded. 
     * @param file
     * @return
     */
    public boolean loadFromFile(File file){
        this.file = file;
        try{
            Scanner in = new Scanner(file);
            double weights[][][];
            double biases[][];
            
            int i, j, l = 0, n = 0;
            String temp;
            while(in.hasNextLine()){
                temp = in.nextLine();
                if(temp.equals("b")) break;
                else if(temp.equals("l"))l++;
            }
            in.close();
            in = new Scanner(file);
            int[] neurons = new int[l];
            i = -1;
            while(in.hasNextLine()){
                temp = in.nextLine();
                if(temp.equals("b")){
                    if(n != 0) neurons[i] = n;
                    break;
                }else if(temp.equals("l")){
                    if(n != 0){
                        neurons[i] = n;
                        n = 0;
                    }
                    i++;
                }else if(temp.equals("n"))n++;
            }
            weights = new double[l][][];
            for(i = 0; i < weights.length; i++)weights[i] = new double[neurons[i]][];
            
            in = new Scanner(file);
            i = -1;
            j = -1;
            String value;
            
            value = in.nextLine();
            String[] splitValue = value.split(" ");
            double learningRate = Double.parseDouble(splitValue[0]);
            boolean sigmoid = splitValue[1].equals("true");
            
            while(in.hasNextLine()){
                value = in.nextLine();
                if(value.equals("b"))break;
                else if(value.equals("l")){
                    i++;
                    j = -1;
                }else if(value.equals("n")) j++;
                else{
                    splitValue = value.split(",");
                    weights[i][j] = new double[splitValue.length];
                    for(int k = 0; k < splitValue.length; k++){
                        weights[i][j][k] = Double.parseDouble(splitValue[k]);
                    }
                }
            }
            
            biases = new double[weights.length][];
            for(i = 0; i < biases.length; i++)biases[i] = new double[weights[i].length];
            
            i = -1;
            while(in.hasNextLine()){
                value = in.nextLine();
                if(value.equals("l")) i++;
                else{
                    splitValue = value.split(",");
                    for(j = 0; j < splitValue.length; j++){
                        biases[i][j] = Double.parseDouble(splitValue[j]);
                    }
                }
            }
            in.close();
            this.loadValues(weights, biases, learningRate, sigmoid);
            
            return true;
        }catch(Exception E){
            E.printStackTrace();
            return false;
        }
    }
    
    //----------------Redundant Functions and Extras----------------\\
    public int trainToAccuracy(double[] inputs, double[] outputs, double accuracy, int maxRepetitions){
        double cost = 1; 
        int count = 0;
        while(cost > accuracy && count < maxRepetitions){
            this.backPropagate(inputs,outputs);
            cost = cost(this.forwardPropagate(inputs), outputs);
            count ++;
        }
        return count;
    }
    
    /**
     * Used to slim down the data into the size of the neural network input layer
     * Used only when input size is greater than first layer size
     * @param input
     * @param outputSize
     * @param max
     * @return
     */
    public double[] pool(double[] input, int outputSize, boolean max){
        double[] output = new double[outputSize];
        int step = Math.round((float)input.length / outputSize) ;
        System.out.println(step);
        double avg = 0;
        double maximum = 0;
        int i = 0, j = 0, count = 0;
        while(i != input.length){
            if(max) maximum = Math.max(input[i++], maximum);
            else avg += input[i++];
            count ++;
            
            if(j == output.length) break;
            if(count == step){
                if(max){
                    output[j++] = maximum;
                    maximum = 0;
                    count = 0;
                }else{
                    output[j++] = avg / count;
                    avg = 0;
                    count = 0;
                }
            }
        }
        if(max && maximum != 0) output[output.length - 1] = maximum;
        else if(!max && avg != 0) output[output.length - 1] = avg / count;        
        return output;
    }
    /**
     * Used to grow the data into the size of the neural network input layer
     * Used only when input size is less than first layer size
     * @param input
     * @param outputSize
     * @return
     */
    public double[] extend(double[] input, int outputSize){
        int toFill = outputSize - input.length;
        int step = Math.round((float)outputSize / toFill);
        double[] output = new double[outputSize];
        int count = 0, shift = 0, i = 0;
        double diff = 0;
        while(i < output.length){
            output[i++] = input[count++];
            if(count == input.length) break;
            if(i % step == 0){
                count --;
                output[i-1] = (input[count-1] + input[count]) / 2;
            }
        }
        if(i != output.length) output[i] = input[input.length - 1];
        return output;
    }
    
    public double[] run(double[] inputs){return this.forwardPropagate(inputs);}
}
