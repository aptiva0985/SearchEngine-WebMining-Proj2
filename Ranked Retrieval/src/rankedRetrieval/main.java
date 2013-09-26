package rankedRetrieval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rankedRetrieval.ScoreList;

import rankedRetrieval.Node;
import rankedRetrieval.ParseTree;
/*
 * This is the main function
 * It will call all other functions to complete the retrieval procedure
 */
public class main {
	static int queryID = 0;
	static String inputPath = System.getProperty("user.dir") + "\\input.txt";
	//query file (a text file that store all queries)should be given by user
	static String outputPath = System.getProperty("user.dir") + "\\output.txt";
	//user can also edit the result output path and file name
	static String retrievalType = "Indri";
	//We have two different approaches retrieving documents, BM25 and Indri.
	
	static ArrayList<String> queryInput = new ArrayList<String>();
	static ArrayList<Node> queryTree = new ArrayList<Node>();

	public static void main(String[] args) throws IOException {
		long startMili=System.currentTimeMillis();
		File inputFile = new File(inputPath);

		queryInput = ReadFile(inputFile,queryInput);//get all query inputs
		for (String query:queryInput) {//traverse each query
			queryTree = new ArrayList<Node>();
			queryID = Integer.parseInt(query.split(":")[0]);

			//create the parse tree for current
			new ParseTree();
			queryTree = ParseTree.queryParse(query.split(":")[1], queryTree);

			//get evaluation result with different model
			new EvalQuery();
			if (retrievalType.equals("BM25")) {
				EvalQuery.BM25Retrieval(queryTree);
			}
			else if (retrievalType.equals("Indri")) {
				EvalQuery.IndriRetrieval(queryTree);
			}

			//ranking evaluation result
			Mycomparator comp = new Mycomparator();
			Collections.sort(EvalQuery.scoreResult,comp);

			//output ranked result to appointed file
			File outputFile = new File(main.outputPath);

			WriteFile(EvalQuery.scoreResult,outputFile);
			System.out.println(query.split(":")[1]);
		}

		System.out.println("Finished!");
		long endMili=System.currentTimeMillis();
		System.out.println("Running time is "+(endMili-startMili)+"ms");
	}

	public static ArrayList<String> ReadFile(File ReadFile, ArrayList<String> inputData) throws IOException{
		//Open input stream to get query input file

		FileInputStream IOStream = new FileInputStream(ReadFile);
		InputStreamReader read = new InputStreamReader(IOStream);
		BufferedReader reader = new BufferedReader(read);

		String str;
		while ((str = reader.readLine()) != null)//read date data line by line to the end of file
			inputData.add(str);

		//Close input stream
		reader.close();
		read.close();
		IOStream.close();
		return inputData;
	}

	public static void WriteFile(ArrayList<ScoreList> currentResult,File WriteFile) throws IOException{
		//write all ranked results to output file

		FileOutputStream IOStream = new FileOutputStream(WriteFile, true);
		OutputStreamWriter write = new OutputStreamWriter(IOStream);
		BufferedWriter writer = new BufferedWriter(write);
		String str = "";
		writer.write("rank,docid,score");
		writer.newLine();
		writer.flush();
		write.flush();

		//for (int i = 0; (i < currentResult.size()); i++){
		for (int i = 0; (i < 50) && (i < currentResult.size()); i++){
			ScoreList tmp = currentResult.get(i);
			
			if (tmp.getDocID() == 0)
				continue;

			////output string for trec_eval results
			//str = queryID + " Q0 " + tmp.getDocID() + " " + (i+1) + " " + tmp.getScore() + " run-1";
			str = (i+1) + "," + tmp.getDocID() + "," + tmp.getScore();


			writer.write(str);
			writer.newLine();
			writer.flush();
			write.flush();
		}

		writer.close();
		write.close();
		IOStream.close();
	}
	
	public static class Mycomparator implements Comparator<Object>{
		//Comparator for Ranked approach
		//the first sort key is score
		//the second sort key is DocID

	    public int compare(Object o1,Object o2) {
	    	ScoreList result1 = (ScoreList)o1;
	    	ScoreList result2 = (ScoreList)o2;
	    	if(result1.getScore() < result2.getScore())
	    		return 1;
	    	else if (result1.getScore() > result2.getScore())
	    		return -1;
	    	else if (result1.getScore() == result2.getScore()) {
	    		if (result1.getDocID() < result2.getDocID())
	        	   return 1;
	    	}
	    	return -1;
	    }
	}
}