import java.util.*;

public class Neat {
	Random rnd = new Random();
	Network[] population; //Holds all Networks
	ArrayList<ArrayList<Network>> species = new ArrayList<ArrayList<Network>>(); //Holds the networks for each species, each inner ArrayList is a new species
	
	int generation = 0;
	int neuronId = 0; //Keeps track of the Id
	int synInnoNum = 0; //Keeps track of the synapse's innovation numbers
	
	float maxFitness; //Used to scale network's fitness between 0 and 1
	
	float max = 0;
	float min = 200;
	
	//Percentages of mutating  
	float mutateWeight = 0.6f;
	float mutateNeuron = 0.05f;
	float mutateSynapse = 0.05f;
	
	public Neat(int netCount, int inputs_, int outputs_){ //Neat's constructor
		population = new Network[netCount];
		for(int i = 0; i < population.length; i++){
			population[i] = new Network(inputs_, outputs_, this);
		}
		
		neuronId = inputs_ + outputs_ - 1; //Sets the neuronId to the amount on initial neurons
		synInnoNum = inputs_ * outputs_ -1; //Sets the synInnoNum to the amount of initial synapses
	}
	
	public void runNeat(){ //Runs the genetic algorithm
		recalcFitness();
		speciation();
		fitnessSharing();
		mutate();
		generation++;
		System.out.println("Generation: "+ generation);
	}
	
	Network crossover(Network parA, Network parB){
		Network parentA = parA;//Selects parents based on fitness
		Network parentB = parB;//Selects parents based on fitness
		
		ArrayList<Neuron> childNeurons = new ArrayList<Neuron>(); //Child Neurons for new Network
		for(Neuron neuronA : parentA.neurons){ //Adds all Neurons from parentA
			childNeurons.add(new Neuron(neuronA.type, neuronA.id));
		}
		for(Neuron neuronB : parentB.neurons){//Adds all neurons from parentB that are not in parentA
			boolean addNeuron = true;
			for(Neuron neuronChild : childNeurons){
				if(neuronB.id == neuronChild.id){
					addNeuron = false;
				}
			}
			if(addNeuron){
				childNeurons.add(new Neuron(neuronB.type, neuronB.id));
			}
		}
		
		for(Synapse synA : parentA.synapses){ //Sets the neuron reference in the 'In' and Out'- 
			synA.in = findNeuron(synA.in.id, childNeurons); //-to the newly created Neurons in childNeurons
			synA.out = findNeuron(synA.out.id, childNeurons);
		}
		for(Synapse synB : parentB.synapses){  //Sets the neuron reference in the 'In' and Out'- 
			synB.in = findNeuron(synB.in.id, childNeurons);//-to the newly created Neurons in childNeurons
			synB.out = findNeuron(synB.out.id, childNeurons);
		}
					
		ArrayList<Synapse> childSynapses = new ArrayList<Synapse>();//Child Synapses for the new Network
		
		for(Synapse synA : parentA.synapses){//Adds all Synapses from parentA
			childSynapses.add(new Synapse(synA));
		}

		for(Synapse synB : parentB.synapses){//Adds all Synapses from parentB that are not in parentA
			boolean add = true;
			for(Synapse synC : childSynapses){
				if(synB.innovationNum == synC.innovationNum){//50% chance to change the new synape's attributes to parentB's
					add = false;
					if(0.5 > rnd.nextFloat()){
						synC.weight = synB.weight;
						synC.enabled = synB.enabled;
					}
				}
			}
			if(add){
				childSynapses.add(new Synapse(synB));
			}
		}
		
		return new Network(childNeurons, childSynapses, parentA); //Creates new Network from childNeurons and ChildSynapses
	}
		
	void mutate(){ //Mutates the networks in 3 ways: Weight, New Neuron, and new Synapse
		for(Network net : population){
			if(mutateWeight > rnd.nextFloat()){ //Mutates the weight of a synapse in a network
				net.synapses.get(rnd.nextInt(net.synapses.size())).weight = -1 + rnd.nextFloat() * (1 - -1);
			}
			
			if(mutateNeuron > rnd.nextFloat()){ //Adds new Neuron
				Synapse disSyn = net.synapses.get(rnd.nextInt(net.synapses.size()));
				disSyn.enabled = false; //Sets disSyn enabled to false
				Neuron newNeuron = new Neuron("Hidden", newNeuronId()); //Creates a new Neuron
				net.neurons.add(newNeuron);
				net.synapses.add(new Synapse(disSyn.in, newNeuron, newSynInnoNum(disSyn.in.id, newNeuron.id))); //Creates a new Synapse between the old 'In' and the new Neuron
				net.synapses.add(new Synapse(newNeuron, disSyn.out, newSynInnoNum(newNeuron.id, disSyn.out.id))); //Creates a new Synapse between the new Neuron and the old 'Out'
			}
			
			if(mutateSynapse > rnd.nextFloat()){
				Neuron inputN;
				Neuron outputN;
				while(true){//Gets the input Neuron, 
					inputN = net.neurons.get(rnd.nextInt(net.neurons.size()));
					if(!inputN.type.equals("Output")){//Cannot be an output
						break;
					}
				}
				while(true){//Gets the output Neuron
					outputN = net.neurons.get(rnd.nextInt(net.neurons.size()));
					if(inputN.type.equals("Hidden") && outputN.type.equals("Output") && outputN.id != inputN.id){//Hidden Input neurons must connect to output neurons
						break;
					}
					if(inputN.type.equals("Input") && !outputN.type.equals("Input") && outputN.id != inputN.id){
						break;
					}
				}
				
				boolean add = true;
				for(Synapse syn : net.synapses){//Checks if the synapse is already in the network
					if(syn.in.id == inputN.id && syn.out.id == outputN.id){
						add = false;
					}
				}
				if(add){//If it isn't in the network then it is added
					net.synapses.add(new Synapse(inputN, outputN, newSynInnoNum(inputN.id, outputN.id)));
				}
			}
		}
	}
	
	void speciation(){ //Seperates the population into species
		float incompatThresh = 0.4f; //Max distance between networks in a species
		float killPercent = 0.05f; //Kills n% of the lowest performing population 
		
		for(Network n : population){ //Addes each network to a specices or creates a new one
			boolean addToSpecies = false; //Checks if network was added to a species
			for(int i = 0; i < species.size(); i++){
				if(distComp(n, species.get(i).get(0)) < incompatThresh){ //Check if the distance is less than the Compatibility Threshhold
					species.get(i).add(n);
					addToSpecies = true;
					break;
				}
			}
			
			if(!addToSpecies){ //If network wasn't add to an existing species a new one is created
				species.add(new ArrayList<Network>());
				species.get(species.size()-1).add(n);
			}
		}
		
		for(int j = 0; j < species.size(); j++){ //Kills the lowest prefroming genomes
			if(species.get(j).size() > 1){
				int killAmount = (species.get(j).size() * killPercent < 1) ? 1 : Math.round((species.get(j).size() * killPercent)); //Amount of genomes to kill
				
				Collections.sort(species.get(j));
				
				for(int k = 0; k < killAmount; k++){
					species.get(j).remove(k);
				}
			}
		}
	}
	
	void fitnessSharing(){ //Adjust the fitness of each network in each species and reproduces the species
		float[] speciesFit = new float[species.size()]; //Array holds all the adjusted fitnesses for each species
		int[] speciesReproduce = new int[species.size()]; //Array holds all the amount each species gets to reproduce
		float N = 0;
		for(int i = 0; i < species.size(); i++){
			speciesFit[i] = adjustedFitness(species.get(i));
			N += speciesFit[i];
		}
		
		int total = 0; //Keeps track of total amount of networks to reproduce
		for(int j = 0; j < species.size(); j++){
			speciesReproduce[j] = Math.round((speciesFit[j] / N) * population.length);
			total += speciesReproduce[j];
			System.out.println("Networks in species " + (j + 1) + ": " +speciesReproduce[j]);
		}
		
		if(population.length - total > 0){ //Addes amount to a random species if total < population.length
			speciesReproduce[rnd.nextInt(speciesReproduce.length)] += population.length - total;
		}
		
		System.out.println("Max: " + max);
		System.out.println("Min: " + min);
		System.out.println("Max Distance: " + (max - min));
		
		Network[] popCopy = new Network[population.length];
		int index = 0;
		for(int k = 0; k < speciesReproduce.length; k++){ //Reproduces the networks based on species
			for(int l = 0; l < speciesReproduce[k]; l++){
				if(index <= popCopy.length - 1){
					popCopy[index] = crossover(species.get(k).get(rnd.nextInt(species.get(k).size())),species.get(k).get(rnd.nextInt(species.get(k).size())));
					index++;
				}
			}
		}
		
		population = popCopy.clone();
	}
	
	float adjustedFitness(ArrayList<Network> speciesO){ //Adjusted the fitness of each species
		float sum = 0;
		for(int i = 0; i < speciesO.size(); i++){
			sum += speciesO.get(i).fitness / speciesO.size();
		}
		return sum;
	}
	
	float distComp(Network a, Network b){ //Returns the distance of compatibility between two networks
		float c1 = 1f; //Coefficent of disjointed and excess genes
		float c3 = 0.4f; //Coefficent of average weight between matching genes
		
		int disjoint = 0; //Nums of different genes
		float avgWeightDiff = 0; //Average of the weight differences
		int N;  //Synapse size of the larger genome
		int matchingGenes = 0;
		
		Network larger;
		Network smaller;
		
		if(a.synapses.size() > b.synapses.size()){//Assigns the larger and smaller networks
			larger = a;
			smaller = b;
			N = a.synapses.size();
		}else{
			larger = b;
			smaller = a;
			N = b.synapses.size();
		}
		
		for(int i = 0; i < larger.synapses.size(); i++){ //Calculates the aveWeightDiff and disjointed genes
			if(i < smaller.synapses.size()){ //Adds all disjointed genes and calcs avgWeightDiff
				if(larger.synapses.get(i).innovationNum == smaller.synapses.get(i).innovationNum){ //Checks if same synapse
					avgWeightDiff += larger.synapses.get(i).weight - smaller.synapses.get(i).weight;
					matchingGenes++;
				}else{ //Adds to disjoint if not the same synapse
					disjoint++;
				}
			}else{ //Adds all excess genes in the larger network
				disjoint++;
			}
		}
		
		
		avgWeightDiff /= matchingGenes;
		
		float dist = (c1 * disjoint) / N + c3 * avgWeightDiff; //Distance of compatibility
			if(dist < min){ //Varible to keep track of the min and max distances between networks
				min = dist;
			}
			if(dist > max){
				max = dist;
			}
		return dist;
	}
	
	Network selectReject(){ //Selects a parent based on its fitness
		Network parent = null;
		while(parent == null){
			int parentId = rnd.nextInt(population.length);
			if(population[parentId].fitness > rnd.nextFloat()){ //Higher fitness are more likely to be chosen than lower fitness
				parent = population[parentId];
			}
		}
		return parent;
	}
	
	Neuron findNeuron(int id, ArrayList<Neuron> neurons){//Used to set the old Synapse's 'In' and 'Out' neurons to the newly created neurons
		Neuron foundN = null;
		for(Neuron n : neurons){
			if(n.id == id){
				foundN = n;
				break;
			}
		}
		return foundN;
	}
	
	public void setMaxFit(float fit){ //Sets the maxFitness to the highest fitness among the networks
		if(fit > maxFitness){
			maxFitness = fit;
		}
	}
	
	public void recalcFitness(){//Scales the fitness of the networks between 0 and 1
		for(Network net : population){
			net.fitness = net.fitness / maxFitness;
		}
	}
	
	int newSynInnoNum(int inId, int outId){//New Synapse Innovation Number
		for(Network net : population){
			for(Synapse syn : net.synapses){
				if(inId == syn.in.id && outId == syn.out.id){
					return syn.innovationNum;//If the synapse exist in the network, it returns the innovation number of that synapse
				}
			}	
		}
		synInnoNum++;
		return synInnoNum;//If synapse doesn't already exist, return a new innovation number
	}
	
	int newNeuronId(){ //New Neuron Id
		neuronId++;
		return neuronId;
	}
}
