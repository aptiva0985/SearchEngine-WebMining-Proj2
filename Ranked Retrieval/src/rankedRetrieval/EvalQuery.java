package rankedRetrieval;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import rankedRetrieval.Node;
import rankedRetrieval.InveList;

/*
 * The queryEvaluation function use recursive method to execute the query
 * and return the query result with score for ranking
 */
public class EvalQuery {

	static Node currentNode;
	static int qtf;
	static ArrayList<InveList> finalResult;
	static ArrayList<ScoreList> scoreResult;

	EvalQuery() {//when starting query evaluation, start from root node
		currentNode = main.queryTree.get(0).getSonNode().get(0);
		finalResult = new ArrayList<InveList>();
		scoreResult = new ArrayList<ScoreList>();
	}

	public static ArrayList<InveList> BM25Retrieval (ArrayList<Node> queryTree) throws IOException {
		//retrieve process for BM25 model
		ArrayList<InveList> result = new ArrayList<InveList>();

		if (currentNode.getIsOperator() == 0) {
			//use recursive method
			//if current node is not an operator
			//return its inverted list
			for (QtfItem tmp : ParseTree.qtfList) {
				if (tmp.getTermName().equals(currentNode.getName())){
					qtf = tmp.getqtf();
					break;
				}
			}
			
			return retrieval(currentNode.getName());
		}
		else if (currentNode.getIsOperator() == 1) {
			//if current node is an operator
			//traverse all its son node and do corresponding operation
			for (Node childNode : currentNode.getSonNode()) {
				ArrayList<InveList> ret = new ArrayList<InveList>();

				Node tmp = currentNode;
				currentNode = childNode;
				ret = BM25Retrieval(queryTree);
				currentNode = tmp;

				if (result.size() == 0)
					//for the initial situation,
					//copy the first son node's inverted list to the temp result
					result = (ArrayList<InveList>) ret.clone();

				//do operation according to the operator type
				if (currentNode.getName().substring(0, 4).equals("#UW/"))
					result = uwOperate(result ,ret);
				else if (currentNode.getName().substring(0, 4).equals("#SUM"))
					scoreResult = sumOperate(ret);
				else if (currentNode.getName().substring(0, 6).equals("#NEAR/"))
					result = nearOperate(result ,ret);
			}
		}
		//the final result save in this ArrayList
		finalResult = (ArrayList<InveList>) result.clone();
		return result;
	}
	
	public static ArrayList<InveList> IndriRetrieval (ArrayList<Node> queryTree) throws IOException {
		//retrieve process for Indri model
		ArrayList<InveList> result = new ArrayList<InveList>();
		
		if (currentNode.getIsOperator() == 0) {
			//use recursive method
			//if current node is not an operator
			//return its inverted list
			
			//get the qtf
			for (QtfItem tmp : ParseTree.qtfList) {
				if (tmp.getTermName().equals(currentNode.getName())){
					qtf = tmp.getqtf();
					break;
				}
			}
			return retrieval(currentNode.getName());
		}
		else if (currentNode.getIsOperator() == 1) {
			int weightFlag = 1;
			double totalWeight = 0;
			double tmpWeight = 0;
			
			//get the weight term
			if (currentNode.getName().length() == 7 && currentNode.getName().substring(0, 7).equals("#WEIGHT")) {
				for (Node childNode : currentNode.getSonNode()) {
					if (weightFlag == 1) {
						tmpWeight = Double.parseDouble(childNode.getName());
						totalWeight += tmpWeight;
						
						weightFlag = 0;
					}
					else if (weightFlag == 0) {
						childNode.setWeight(tmpWeight);
						weightFlag = 1;
						tmpWeight = 0;
					}
				}
			}
			//if current node is an operator
			//traverse all its son node and do corresponding operation
			for (Node childNode : currentNode.getSonNode()) {
				ArrayList<InveList> retInve = new ArrayList<InveList>();
				ArrayList<ScoreList> retScore = new ArrayList<ScoreList>();

				if (currentNode.getName().length() == 7 && currentNode.getName().substring(0, 7).equals("#WEIGHT")) {
					if (weightFlag == 1) {
						weightFlag = 0;
						continue;
					}
					else
						weightFlag = 1;
				}
				
				//for Inverted list that need to be converted into Score list
				//apply a implicit score operator 
				if (!currentNode.getName().contains(".")) {
					Node tmp = currentNode;
					currentNode = childNode;
					retInve = IndriRetrieval(queryTree);
					
					if (tmp.getName().equals("#WEIGHT") || tmp.getName().equals("#AND"))
						retScore = scoreOperate(retInve);
					
					currentNode = tmp;
				}
				else
					continue;

				if (result.size() == 0)
					//for the initial situation,
					//copy the first son node's inverted list to the temp result
					result = (ArrayList<InveList>) retInve.clone();

				//do operation according to the operator type
				if (currentNode.getName().substring(0, 4).equals("#UW/"))
					result = uwOperate(result ,retInve);
				else if (currentNode.getName().substring(0, 4).equals("#AND")){
					scoreResult = andOperate(retScore);
				}
				else if (currentNode.getName().substring(0, 6).equals("#NEAR/"))
					result = nearOperate(result ,retInve);
				else if (currentNode.getName().substring(0, 7).equals("#WEIGHT")){
					scoreResult = weightOperate(retScore, childNode.getWeight(), totalWeight);
				}
			}
		}
		//the final result save in this ArrayList
		finalResult = (ArrayList<InveList>) result.clone();
		return result;
	}

	public static ArrayList<InveList> retrieval (String nodeName) throws IOException {
		//get inverted list for certain query term from local file
		ArrayList<InveList> retrievalResult = new ArrayList<InveList>();
		String filePath = System.getProperty("user.dir") + "\\clueweb09_wikipedia_15p_invLists\\" + nodeName +".inv";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			return null;
		}

		String line = br.readLine();
		line = br.readLine();
		if (line == null){
	        br.close();
			return retrievalResult;
		}

		//get every inverted list item and store it into the InveList structure
        for (; line != null; line = br.readLine()) {
        	String[] para = line.split(" ");
        	InveList temp= new InveList();

        	temp.setDocID(Integer.parseInt(para[0]));
        	temp.setTf(Integer.parseInt(para[1]));
        	temp.setDocLen(Integer.parseInt(para[2]));
        	for (int i = 3; i < temp.getTf() + 3; i++)
        		temp.Pos.add(Integer.parseInt(para[i]));

        	retrievalResult.add(temp);
        }
        br.close();

		return retrievalResult;
	}

	public static ArrayList<ScoreList> sumOperate (ArrayList<InveList>ret){
		//this function can execute "#SUM" operator
		final double N = 890630; 
		final double k1 = 1.2;
		final double k3 = 1;
		final double b = 0.75;
		final double avg_doclen = 1301;
		
		double idf = 0;
		double tf = 0;
		double usrw = 0;
		int i = 0;
		int j = 0;
						
		while (j < ret.size()) {//for a inverted list longer than scorelist
			if ((i == scoreResult.size()) && (j < ret.size())){
				while (j < ret.size()) {
					//calculate the score
					idf = Math.log((N - (double)ret.size() + 0.5) / ((double)ret.size() + 0.5));
					tf = ((double)ret.get(j).getTf())/((double)ret.get(j).getTf() + k1 * ((1.0 - b) + b * (double)ret.get(j).getDocLen() / avg_doclen));
					usrw = ((k3 + 1.0) * (double)qtf) /(k3 + (double)qtf);
					
					//add it into the scorelist
					ScoreList tmp = new ScoreList(ret.get(j).getDocID());
					tmp.setScore(idf * tf * usrw);
					scoreResult.add(i, tmp);
					i++;
					j++;
				}
				break;
			}
			
			//use two pointer to traverse the lists
			int ID1 = scoreResult.get(i).getDocID();
			int ID2 = ret.get(j).getDocID();
			
			if (ID1 == ID2) {//if DocIDs match, update score
				idf = Math.log((N - (double)ret.size() + 0.5) / ((double)ret.size() + 0.5));
				tf = ((double)ret.get(j).getTf())/((double)ret.get(j).getTf() + k1 * ((1.0 - b) + b * (double)ret.get(j).getDocLen() / avg_doclen));
				usrw = ((k3 + 1.0) * (double)qtf) /(k3 + (double)qtf);
				
				scoreResult.get(i).setScore(scoreResult.get(i).getScore() + idf * tf * usrw);
				
				i++;
				j++;
			}
			else if (ID1 < ID2) {
				i++;
			}
			else if (ID1 > ID2) {//if new terms occur, add score
				idf = Math.log((N - (double)ret.size() + 0.5) / ((double)ret.size() + 0.5));
				tf = ((double)ret.get(j).getTf())/((double)ret.get(j).getTf() + k1 * ((1.0 - b) + b * (double)ret.get(j).getDocLen() / avg_doclen));
				usrw = ((k3 + 1.0) * (double)qtf) /(k3 + (double)qtf);
				
				ScoreList tmp = new ScoreList(ID2);
				tmp.setScore(idf * tf * usrw);
				scoreResult.add(i, tmp);
				
				i++;
				j++;
			}
			
		}
		return scoreResult;
	}

	public static ArrayList<ScoreList> andOperate (ArrayList<ScoreList>ret){
		//this function can execute "#AND" operator
		final double lambda = 0.5;
		final double mu = 1;
		final double lenC = 1158815080;
		final double avg_doclen = 1301;
		
		double smoothing1 = 0;
		double smoothing2 = 0;
		double ctf = 0;
		
		int i = 0;
		int j = 0;
		
		if (scoreResult.size() == 0)
			scoreResult = (ArrayList<ScoreList>) ret.clone();
		else {
			ScoreList tmp = new ScoreList(0);
			tmp.setScore(ret.get(ret.size() - 1).getScore() + scoreResult.get(scoreResult.size() - 1).getScore());
			scoreResult.set(scoreResult.size() - 1, tmp);
		}
			
						
		while (j < ret.size() - 1) {//for a parameter longer than original result
			if ((i == scoreResult.size()) && (j < ret.size() - 1)){
				while (j < ret.size() - 1) {
					double tmpScore = ret.get(j).getScore() / ParseTree.qtfList.size();
					
					ScoreList tmp = new ScoreList(ret.get(j).getDocID());
					tmp.setScore(tmpScore);
					scoreResult.add(i, tmp);
					i++;
					j++;
				}
				break;
			}

			//use two pointer to traverse the lists
			int ID1 = scoreResult.get(i).getDocID();
			int ID2 = ret.get(j).getDocID();
			
			if (ID1 == ID2) {//if DocIDs match, update score
				double tmpScore = ret.get(j).getScore() / ParseTree.qtfList.size();
				
				scoreResult.get(i).setScore(scoreResult.get(i).getScore() + tmpScore);
				
				i++;
				j++;
			}
			else if (ID1 < ID2) {
				scoreResult.get(i).setScore(scoreResult.get(i).getScore() + scoreResult.get(scoreResult.size() - 1).getScore());
				
				i++;
			}
			else if (ID1 > ID2) {//if new terms occur, add score
				double tmpScore = ret.get(j).getScore() / ParseTree.qtfList.size();
				
				ScoreList tmp = new ScoreList(ID2);
				double defaultScore =  scoreResult.get(scoreResult.size() - 1).getScore();
				tmp.setScore(tmpScore + defaultScore);
				scoreResult.add(i, tmp);
				
				i++;
				j++;
			}
		}
		
		return scoreResult;
	}
	
	public static ArrayList<ScoreList> weightOperate (ArrayList<ScoreList> retScore, double currentWeight, double totalWeight){
		//this function can execute "#WEIGHT" operator
		final double lambda = 0.6;
		final double mu = 1000;
		final double lenC = 1158815080;
		
		double smoothing1 = 0;
		double smoothing2 = 0;
		double ctf = 0;
		
		int i = 0;
		int j = 0;
		
		if (scoreResult.size() == 0)
			scoreResult = (ArrayList<ScoreList>) retScore.clone();
		else {
			ScoreList tmp = new ScoreList(0);
			tmp.setScore(retScore.get(retScore.size() - 1).getScore() + scoreResult.get(scoreResult.size() - 1).getScore());
			scoreResult.set(scoreResult.size() - 1, tmp);
		}
		
		while (j < retScore.size() - 1) {//for a parameter longer than original result
			
			if ((i == scoreResult.size()) && (j < retScore.size() - 1)){
				while (j < retScore.size()){
					double tmpScore = retScore.get(j).getScore() * (currentWeight / totalWeight);
					
					ScoreList tmp = new ScoreList(retScore.get(j).getDocID());
					tmp.setScore(tmpScore);
					scoreResult.add(tmp);
					i++;
					j++;
					
				}
				break;
			}
			
			//use two pointer to traverse the lists
			int ID1 = scoreResult.get(i).getDocID();
			int ID2 = retScore.get(j).getDocID();
			
			if (ID1 == ID2) {//if DocIDs match, update score
				double tmpScore = retScore.get(j).getScore() * (currentWeight / totalWeight);
				
				scoreResult.get(i).setScore(scoreResult.get(i).getScore() + tmpScore);			
				i++;
				j++;
			}
			else if (ID1 < ID2) {
				scoreResult.get(i).setScore(scoreResult.get(i).getScore() + scoreResult.get(scoreResult.size() - 1).getScore());
				i++;
			}
			else if (ID1 > ID2) {//if new terms occur, add score
				double tmpScore = retScore.get(j).getScore() * (currentWeight / totalWeight);
				
				ScoreList tmp = new ScoreList(ID2);
				double defaultScore =  scoreResult.get(scoreResult.size() - 1).getScore();
				tmp.setScore(tmpScore + defaultScore);
				scoreResult.add(i, tmp);
				i++;
				j++;
			}
		}
		
		return scoreResult;
	}
	
	public static ArrayList<ScoreList> scoreOperate (ArrayList<InveList>ret){
		ArrayList<ScoreList> result = new ArrayList<ScoreList>();
		//this function can execute implicit "#SCORE" operator
		final double lambda = 0.6;
		final double mu = 1000;
		final double lenC = 1158815080;
		final double avg_doclen = 1301;
		
		double smoothing1 = 0;
		double smoothing2 = 0;
		double ctf = 0;
		
		for (InveList cur : ret)
			ctf+= cur.getTf();
		
		//traverse entire inverted list
		//calculate socre for every item
		//write back to a scorelist
		for (InveList cur : ret) {
			smoothing1 = lambda * (cur.getTf() + mu * ctf / lenC) / (cur.getDocLen() + mu);
			smoothing2 = (1 - lambda) * ctf / lenC;
			double tmpScore = Math.log(smoothing1 + smoothing2);
			
			ScoreList tmp = new ScoreList (cur.getDocID());
			tmp.setScore(tmpScore);
			result.add(tmp);
		}
		smoothing1 = lambda * (mu * ctf / lenC) / (avg_doclen + mu);
		smoothing2 = (1 - lambda) * ctf / lenC;
		double tmpScore = Math.log(smoothing1 + smoothing2);
		ScoreList tmp = new ScoreList (0);
		tmp.setScore(tmpScore);
		result.add(tmp);
		
		return result;
	}

	public static ArrayList<InveList> nearOperate (ArrayList<InveList>result , ArrayList<InveList>ret){
		int n = Integer.parseInt(currentNode.getName().substring(6, 7));
		int i = 0;
		int j = 0;
		//two pointers for two inverted list of two documents
		//and the near range is stored in variable n
		ArrayList<InveList> tempResult = new ArrayList<InveList>();

		while ((i < result.size()) && (j < ret.size())) {
			int ID1 = result.get(i).getDocID();
			int ID2 = ret.get(j).getDocID();

			if (ID1 == ID2) {
				//if two pointers point to same document
				//get two position lists
				ArrayList<Integer> posList1 = result.get(i).getPos();
				ArrayList<Integer> posList2 = ret.get(j).getPos();
				ArrayList<Integer> samePosList = new ArrayList<Integer>();
				int posPointer1 = 0;
				int posPointer2 = 0;
				//have two new pointer for position lists
				int sameCount = 0;

				while ((posPointer1 < posList1.size()) && (posPointer2 < posList2.size())) {
					if (posList2.get(posPointer2) - posList1.get(posPointer1) > n) {
						posPointer1++;
					}
					else if (posList2.get(posPointer2) - posList1.get(posPointer1) < 0) {
						posPointer2++;
					}
					else if (posList2.get(posPointer2) - posList1.get(posPointer1) <= n){
						//only document with word1 prior to words within n-1 words can be treat as a match
						sameCount++;
						samePosList.add(posList2.get(posPointer2));
						posPointer1++;
						posPointer2++;
					}
				}
				if (sameCount != 0){
					//if this document has position matches
					//add it into the result
					//score will be match time
					InveList tmp = new InveList(result.get(i).getDocID());
					tmp.setDocLen(result.get(i).getDocLen());
					tmp.setTf(sameCount);
					tmp.setPos(samePosList);
					tempResult.add(tmp);
				}
				i++;
				j++;
			}
			else if (ID1 > ID2)
				//if two pointers point to different document
				//move the smaller pointer
				j++;
			else if (ID1 < ID2)
				//if two pointers point to different document
				//move the smaller pointer
				i++;
		}


		return tempResult;
	}
	
	public static ArrayList<InveList> uwOperate (ArrayList<InveList>result , ArrayList<InveList>ret) throws IOException{
		int n = Integer.parseInt(currentNode.getName().substring(4, 5));
		ArrayList<Integer> DocPointer = new ArrayList<Integer>();
		//pointers for two inverted lists of documents in near operator
		//and the window size is stored in variable n
		ArrayList<InveList> tempResult = new ArrayList<InveList>();
		ArrayList<ArrayList<InveList>> TermInveList = new ArrayList<ArrayList<InveList>>();
		int TermAmount = currentNode.getSonNode().size();
		
		//initialize the inverted list array for window match
		for (int i = 0; i < TermAmount; i++) {
			DocPointer.add(i, 0);
			TermInveList.add(i, retrieval(currentNode.getSonNode().get(i).getName()));
		}
		
		while (DocPointerEnd(TermInveList, DocPointer) == 0) {//traverse until any pointer come to end
			if (DocMatch(TermInveList, DocPointer) == 1) {//if all DocID match
				//start to match positions
				ArrayList<Integer> PosPointer = new ArrayList<Integer>();
				ArrayList<ArrayList<Integer>> PosList = new ArrayList<ArrayList<Integer>>();
				int sameCount = 0;
				ArrayList<Integer> samePosList = new ArrayList<Integer>();
				
				//initialize the position list array for window match
				for (int i = 0; i < TermAmount; i++) {
					PosPointer.add(i, 0);
					PosList.add(i, TermInveList.get(i).get(DocPointer.get(i)).getPos());
				}

				while (PosPointerEnd(PosList, PosPointer) == 0) {
					int MatchIndex = WinMatch (PosList, PosPointer, n);
					//find out whether all positions are in the same window
					if (MatchIndex != 0) {
						sameCount++;
						samePosList.add(PosList.get(MatchIndex).get(PosPointer.get(MatchIndex)));
						for (int i = 0; i < TermAmount; i++)
							PosPointer.set(i, PosPointer.get(i) + 1);
					}
					else 
						PosPointer.set(MinPosPointer(PosList, PosPointer), PosPointer.get(MinPosPointer(PosList, PosPointer)) + 1);
				}
				if (sameCount != 0){
					//if these documents has position matches
					//add it into the result
					//score will be match time
					InveList tmp = new InveList(TermInveList.get(0).get(DocPointer.get(0)).getDocID());
					tmp.setDocLen(TermInveList.get(0).get(DocPointer.get(0)).getDocLen());
					tmp.setTf(sameCount);
					tmp.setPos(samePosList);
					tempResult.add(tmp);
				}
				
				for (int i = 0; i < TermAmount; i++)
					DocPointer.set(i, DocPointer.get(i) + 1);
			}
			else
				DocPointer.set(MinDocPointer(TermInveList, DocPointer), DocPointer.get(MinDocPointer(TermInveList, DocPointer)) + 1);
		}

		return tempResult;
	}
	
	public static int DocMatch(ArrayList<ArrayList<InveList>> TermInveList, ArrayList<Integer> DocPointer){
		//function to find out DocID match
		int flag = 1;
		for (int i = 0; i < (DocPointer.size() - 1); i++) {
			if (TermInveList.get(i).get(DocPointer.get(i)).getDocID() != TermInveList.get(i + 1).get(DocPointer.get(i + 1)).getDocID()){
				flag = 0;
				break;
			}
		}
		return flag;
	}
	
	public static int WinMatch (ArrayList<ArrayList<Integer>> PosList, ArrayList<Integer> PosPointer, int n) {
		//function to find out window match
		int matchflag = 1;
		int minIndex = MinPosPointer(PosList, PosPointer);
		for (int i = 0; i < PosPointer.size(); i++) {
			if (PosList.get(i).get(PosPointer.get(i)) - PosList.get(minIndex).get(PosPointer.get(minIndex)) > n){
				matchflag = 0;
				break;
			}
		}
		return matchflag;
		
	}
	
	public static int DocPointerEnd (ArrayList<ArrayList<InveList>> TermInveList, ArrayList<Integer> DocPointer) {
		//function to find out if any Doc pointer reach end
		int flag = 0;
		for (int i = 0; i < DocPointer.size(); i++){
			if (DocPointer.get(i) == TermInveList.get(i).size()) {
				flag = 1;
				break;
			}
		}
		return flag;
	}
	
	public static int PosPointerEnd (ArrayList<ArrayList<Integer>> PosList, ArrayList<Integer> PosPointer) {
		//function to find out if any position pointer reach end
		int flag = 0;
		for (int i = 0; i < PosPointer.size(); i++){
			if (PosPointer.get(i) == PosList.get(i).size()) {
				flag = 1;
				break;
			}
		}
		return flag;
	}
	
	public static int MinDocPointer (ArrayList<ArrayList<InveList>> TermInveList, ArrayList<Integer> DocPointer) {
		//function to find out the smallest Doc pointer
		int MinVal = TermInveList.get(0).get(DocPointer.get(0)).getDocID();
		int MinIndex = 0;
		
		for (int i = 0; i < DocPointer.size(); i++) {
			if (MinVal > TermInveList.get(i).get(DocPointer.get(i)).getDocID()){
				MinVal = TermInveList.get(i).get(DocPointer.get(i)).getDocID();
				MinIndex = i;
			}
		}
		
		return MinIndex;
	}
	public static int MinPosPointer (ArrayList<ArrayList<Integer>> PosList, ArrayList<Integer> PosPointers) {
		//function to find out the smallest position pointer
		int MinVal = PosList.get(0).get(PosPointers.get(0));
		int MinIndex = 0;
		
		for (int i = 0; i < PosPointers.size(); i++) {
			if (MinVal > PosList.get(i).get(PosPointers.get(i))){
				MinVal = PosList.get(i).get(PosPointers.get(i));
				MinIndex = i;
			}
		}
		
		return MinIndex;
	}

}
