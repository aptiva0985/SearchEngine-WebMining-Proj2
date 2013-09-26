package rankedRetrieval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import rankedRetrieval.Node;

/*
 * This class and queryParse function will turn query into parse tree
 * and return it to the main function
 */

public class ParseTree {
	static int opNum ;
	static int nodeNum;
	static Node root = new Node();
	static int pointer;
	static int operatorLevel;
	static Node[] operatorList = new Node[10];
	static ArrayList <QtfItem> qtfList = new ArrayList <QtfItem>();
	

	ParseTree() {
		opNum = 0;
		nodeNum = 0;
		root= new Node("#root");
		pointer = 0;
		operatorLevel = 0;
		operatorList[0] = root;
	}


	static ArrayList <Character> punc = new ArrayList <Character> ();
	static ArrayList <String> stopword = new ArrayList <String> ();


	public static ArrayList<Node> queryParse(String queryInput, ArrayList<Node> queryTree) throws IOException {
		char[] tmp = {' ', ')', '-', '\\', '/', ':', ';', ',', '!', '?', '@', '#', '%'};
		for (Character k :tmp )
			punc.add(k);
		//prepare the punctuation list

		String stopFilePath = System.getProperty("user.dir") + "\\stoplist.txt";
		File stopFile = new File(stopFilePath);

		main.ReadFile(stopFile, stopword);
		//prepare the stopword list



		if (nodeNum == 0) {
			//there is a virtual #root tree for every parse tree
			queryTree.add(root);
			nodeNum++;
		}


		while (pointer != queryInput.length() - 1) {
			if (queryInput.charAt(pointer) == '#') {
				//query has explicit operator
				pointer++;
				if (queryInput.charAt(pointer) == 'A') {
					//we get an "#AND" operator for Indri
					//put it in the parse tree and move on
					Node newNode = new Node("#AND");
					newNode.setIsOperator(1);
					queryTree.add(newNode);

					operatorList[operatorLevel].addSonNode(newNode);
					opNum++;
					pointer+=3;
					operatorLevel++;
					operatorList[operatorLevel] = newNode;
				}
				else if (queryInput.charAt(pointer) == 'W') {
					//we get an "#WEIGHT" operator for Indri
					//put it in the parse tree and move on
					Node newNode = new Node("#WEIGHT");
					newNode.setIsOperator(1);
					queryTree.add(newNode);

					operatorList[operatorLevel].addSonNode(newNode);
					opNum++;
					pointer+=6;
					operatorLevel++;
					operatorList[operatorLevel] = newNode;
				}
				else if (queryInput.charAt(pointer) == 'N') {
					//we get an "#NEAR/n" operator
					//put it in the parse tree and move on
					String temp = "#NEAR/";
					pointer+=5;
					while (Character.isDigit(queryInput.charAt(pointer))) {
						char n = queryInput.charAt(pointer);
						temp += n;
						pointer++;
					}
					
					Node newNode = new Node(temp);
					newNode.setIsOperator(1);
					queryTree.add(newNode);

					operatorList[operatorLevel].addSonNode(newNode);
					opNum++;
					operatorLevel++;
					operatorList[operatorLevel] = newNode;
				}
				else if (queryInput.charAt(pointer) == 'U') {
					//we get an "#UW/n" operator
					//put it in the parse tree and move on
					String temp = "#UW/";
					pointer+=3;
					while (Character.isDigit(queryInput.charAt(pointer))) {
						char n = queryInput.charAt(pointer);
						temp += n;
						pointer++;
					}
					
					Node newNode = new Node(temp);
					newNode.setIsOperator(1);
					queryTree.add(newNode);

					operatorList[operatorLevel].addSonNode(newNode);
					opNum++;
					operatorLevel++;
					operatorList[operatorLevel] = newNode;
				}
				else if (queryInput.charAt(pointer) == 'S') {
					//we get an "#SUM" operator for BM25
					//put it in the parse tree and move on
					Node newNode = new Node("#SUM");
					newNode.setIsOperator(1);
					queryTree.add(newNode);

					operatorList[operatorLevel].addSonNode(newNode);
					opNum++;
					pointer+=3;
					operatorLevel++;
					operatorList[operatorLevel] = newNode;
				}
			}
			else {
				while (queryInput.charAt(pointer) != ')') {
					pointer++;
					if (queryInput.charAt(pointer) == '#') {
						queryParse(queryInput, queryTree);
						//if we have nested query
						//recursive call queryParse function
					}
					else {//if we get a query term, add it as a leaf node
						String queryWord = "";
						while (punc.indexOf(queryInput.charAt(pointer)) == -1) {
							queryWord+=queryInput.charAt(pointer);
							pointer++;
						}

						//drop stopwords
						if (stopword.indexOf(queryWord) != -1)
							continue;

//						//ignore terms that doesn't appear in index
//						if (EvalQuery.retrieval(queryWord) == null)
//							continue;

						//add current leaf node to the parse tree
						Node newNode = new Node();
						newNode.setName(queryWord);
						newNode.setIsOperator(0);
						
						queryTree.add(newNode);
						
						//for every query term, update the query term frequency list
						int existflag = 0;
						for (QtfItem curterm : qtfList) {
							if (curterm.getTermName().equals(queryWord)) {
								curterm.setqtf(curterm.getqtf() + 1);
								existflag = 1;
								break;
							}
						}
						if (existflag == 0) {
							QtfItem curterm = new QtfItem(queryWord);
							curterm.setqtf(1);
							qtfList.add(curterm);
						}
						//maintain a operator list to ensure recursive is right
						operatorList[operatorLevel].addSonNode(newNode);
					}
				}
				operatorLevel--;//a ")" implies the end of an operator's field ends
				if (pointer != queryInput.length() - 1){
					pointer++;
				}

			}
		}
		return queryTree;
		// return the parse tree
	}
}
