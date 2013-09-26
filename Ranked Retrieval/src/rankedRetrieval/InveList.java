package rankedRetrieval;

import java.util.ArrayList;

/*
 * Implement data structure and respective interface for inverted lists storage
 * This class contains DocID member for document ID store, Tf for term frequency,
 * DocLen for document length and a list of integer to save the position(s) that term appear.
 * unchanged since HW1
 */

public class InveList {
	protected int DocID = -1;
	protected int Tf = -1;
	protected int DocLen = -1;
	protected ArrayList<Integer> Pos = new ArrayList<Integer>();

	public InveList() {

	}

	public InveList(int DocID) {
		this.DocID = DocID;
	}

	public int getDocID() {
		return DocID;
	}
	public void setDocID(int DocID) {
		this.DocID = DocID;
	}

	public int getTf() {
		return Tf;
	}
	public void setTf(int Tf) {
		this.Tf = Tf;
	}

	public int getDocLen() {
		return DocLen;
	}
	public void setDocLen(int DocLen) {
		this.DocLen = DocLen;
	}

	public ArrayList<Integer> getPos() {
		return Pos;
	}
	public void setPos(ArrayList<Integer> Pos) {
		this.Pos = Pos;
	}
}