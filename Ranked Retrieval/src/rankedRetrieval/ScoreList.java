package rankedRetrieval;

/*
 * Implement data structure and respective interface for score lists storage
 * This class contains DocID member for document ID store and Score for document score.
 * (new data structure since HW#1)
 */

public class ScoreList {
	protected int DocID = -1;
	protected double Score = 0;

	public ScoreList() {

	}

	public ScoreList(int DocID) {
		this.DocID = DocID;
	}

	public int getDocID() {
		return DocID;
	}
	public void setDocID(int DocID) {
		this.DocID = DocID;
	}

	public double getScore() {
		return Score;
	}
	public void setScore(double Score) {
		this.Score = Score;
	}
}