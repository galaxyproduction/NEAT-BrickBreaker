import java.util.ArrayList;
import java.util.Comparator;

public class Network implements Comparable<Network>{
	Neat neat;
	
	int inputCount;
	int outputCount;
	ArrayList<Neuron> neurons;
	ArrayList<Synapse> synapses;
	
	float fitness; //Must be between 0 and 1
	boolean isActive = true;
	int output; //Index of output
	
	public Network(int inputs, int outputs, Neat neat_){//Constructor for initial networks
		neat = neat_;
		
		inputCount = inputs;
		outputCount = outputs;
		
		neurons = new ArrayList<Neuron>();
		synapses = new ArrayList<Synapse>();
		createNeurons();
		createSynapses();
	}
	
	public Network(ArrayList<Neuron> neurons_, ArrayList<Synapse> synapses_, Network net){//Constructor for cross breed networks
		neat = net.neat;
		
		inputCount = net.inputCount;
		outputCount = net.outputCount;
		
		neurons = new ArrayList<Neuron>(neurons_);
		synapses = new ArrayList<Synapse>(synapses_);
	}
	
	public void networkActivate(float[] inputs){
		reset();//Resets the Neurons in the network
		int activatedCount = 0; //Keeps track of the amount of Neurons activated
		for(int i = 0; i < inputCount; i++){ //Runs all Input neurons first
			neurons.get(i).sumOfInputs = inputs[i];
			neurons.get(i).neuronActivate();
			activatedCount++;
		}

		while(activatedCount != neurons.size()){ //Runs until all Neurons have activated
			for(Neuron n : neurons){
				if(n.currentInputCount >= n.totalInputCount && !n.hasActivated){ //If all of the neurons inputs have fired and it hasn't activated, then activate
					n.neuronActivate();
					activatedCount++;
				}
			}
		}
		
		int out = 0; //Used to find the highest output
		for(int j = 0; j < outputCount; j++){
			if(neurons.get(inputCount + j).sumOfInputs >= neurons.get(out + inputCount).sumOfInputs){
				out = j;
			}
		}
		output = out;//Sets output to highest output
	}
	
	public int compareTo(Network a){
		return (int)(this.fitness - a.fitness);
	}
	
	void printNetworkGeno(){//Prints the phenotype of all Neurons and Synapses, used mainly for debugging
		for(Neuron n : neurons){
			n.printNeuronPheno();
		}
		for(Synapse syn : synapses){
			syn.printSynapsePheno();
		}
	}
	
	void reset(){//Reset Neurons before network is activated
		for(Neuron n : neurons){
			n.sumOfInputs = 0;
			n.currentInputCount = 0;
			n.hasActivated = false;
		}
	}
	
	void setFitness(float fit){ //Must be between 0 and 1
		fitness = fit;
		isActive = false;
		neat.setMaxFit(fitness);
	}
	
	void createNeurons(){//Creates the initial Neurons
		for(int i = 0; i < inputCount; i++){
			neurons.add(new Neuron("Input", i));
		}
		for(int j = 0; j < outputCount; j++){
			neurons.add(new Neuron("Output", inputCount + j));
		}
	}
	
	void createSynapses(){ //Creates the initial Synapses
		int innoNum = 0;
		for(int i = 0; i < inputCount; i++){
			for(int j = 0; j < outputCount; j++){
				synapses.add(new Synapse(neurons.get(i), neurons.get(inputCount + j), innoNum));	
				innoNum++;
			}
		}
	}
}
