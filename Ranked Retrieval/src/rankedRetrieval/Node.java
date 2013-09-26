package rankedRetrieval;

import java.util.ArrayList;

/*
 * Implement data structure and respective interface for node storage
 * This class contains Name member for node name store, an ArrayList to store all sonNode of this node and
 * an isOperator member to save whether this node is an operator(1) or not(0) and a Weight member to save its weight (determined by qtf).
 */


public class Node {
	protected String Name = "";
	protected ArrayList<Node> sonNode = new ArrayList<Node>();
	protected int isOperator = -1;
	protected double Weight = 0;

	public Node() {

	}

	public Node(String Name) {
		this.Name = Name;
	}

	public String getName() {
		return Name;
	}
	public void setName(String Name) {
		this.Name = Name;
	}

	public ArrayList<Node> getSonNode() {
		return sonNode;
	}
	public void addSonNode(Node sonNodeName) {
		this.sonNode.add(sonNodeName);
	}

	public int getIsOperator() {
		return isOperator;
	}
	public void setIsOperator(int isOperator) {
		this.isOperator = isOperator;
	}
	
	public double getWeight() {
		return Weight;
	}
	public void setWeight(double Weight) {
		this.Weight = Weight;
	}

}