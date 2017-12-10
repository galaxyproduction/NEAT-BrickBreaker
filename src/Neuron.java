import java.util.ArrayList;

public class Neuron{
	String type;
	int id;
	float sumOfInputs = 0;
	ArrayList<Synapse> outputs = new ArrayList<Synapse>();//Holds all outputs synapses

	int currentInputCount;
	int totalInputCount;

	boolean hasActivated = false; //Keeps track of whether the network has been activated
	
	public Neuron(String type_, int id_){//Neuron Constructor
		type = type_;
		id = id_;
	}
	
	void neuronActivate(){//Activates the Neuron
		if(type != "Input"){//Does not use activation function if it is a input Neuron
			sumOfInputs = sigmoid(sumOfInputs);
		}
		for(Synapse syn : outputs){
			syn.fireSynapse(sumOfInputs);				
		}
		hasActivated = true;
	}
	
	void printNeuronPheno(){//Prints the phenotype of the neuron (Debugging)
		System.out.println("///////////////");
		System.out.println("*Type: " + type+"  *");
		System.out.println("*Neuron Id: " +id+" *");
		System.out.println("*Current Inputs: "+currentInputCount+" *");
		System.out.println("*Total Inputs: "+totalInputCount+" *");
		System.out.println("///////////////\n");
		
	}
	
	float sigmoid(float x){//Sigmoid activation function
		return (float) (x / Math.sqrt(1 + Math.pow(x, 2)));
	}
}
