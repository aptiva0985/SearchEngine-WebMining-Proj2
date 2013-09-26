package rankedRetrieval;

/*
 * Implement data structure and respective interface for qtf lists storage
 * This class contains QueryName member and qtf for query term frequency.
 * (new data structure since HW#1)
 */

public class QtfItem {
	protected String TermName = "";
	protected int qtf = 0;

	public QtfItem() {

	}

	public QtfItem(String TermName) {
		this.TermName = TermName;
	}

	public String getTermName() {
		return TermName;
	}
	public void setQueryName(String TermName) {
		this.TermName = TermName;
	}

	public int getqtf() {
		return qtf;
	}
	public void setqtf(int qtf) {
		this.qtf = qtf;
	}
}