import java.util.Random;

public class Synapse{
	Random rnd = new Random();
	
	int innovationNum;
	Neuron in;
	Neuron out;
	
	float weight;
	boolean enabled = true;
	
	public Synapse(Neuron in_, Neuron out_, int innoNum){//Creates new synapse with new attributes
		in = in_;
		in.outputs.add(this);
		out = out_;
		out.totalInputCount++;
		innovationNum = innoNum;
		weight = -1 + rnd.nextFloat() * (1 - -1);//Weight from -1 to 1
	}
	
	public Synapse(Synapse syn){//Creates synapses with preexisting attributes
		enabled = syn.enabled;
		in = syn.in;
		in.outputs.add(this);
		out = syn.out;
		out.totalInputCount++;
		innovationNum = syn.innovationNum;
		weight = syn.weight;

	}
	
	public void fireSynapse(float inputSum){//Fires the Synapse
		if(enabled){//Only fires if enabled 
			out.sumOfInputs += inputSum * weight;
		}
		out.currentInputCount++;//Still adds to the total inputCount even if not enabled
	}
	
	void printSynapsePheno(){//Prints the synapse's phenotype (Debugging)
		System.out.println("////////////////////////");
		System.out.println("*InnovationNum: " +innovationNum+" *");
		System.out.println("*In Neuron Id: "+in.id+" *");
		System.out.println("*Out Neuron Id: "+out.id+" *");
		System.out.println("*Weight: "+weight+" *");
		System.out.println("*Enabled: "+enabled+" *");
		System.out.println("////////////////////////\n");
	}
}
