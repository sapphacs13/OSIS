import java.util.Random;

public class PageTableRow {
    private int processId;
    private int address;

    private final int PAGE_ID;
    private final String PAGE_CONTENTS;


    public PageTableRow(int pageId, int procId, int addr) {
	PAGE_ID = pageId;
	processId = procId;
	address = addr;
        PAGE_CONTENTS = generatePageContents();
    }

    public PageTableRow(int pageId, int procId) {
	PAGE_ID = pageId;
	processId = procId;
	address = GlobalVariables.NOT_IN_MAIN_MEM;
	PAGE_CONTENTS = generatePageContents();
    }

    public PageTableRow(int pageId, int procId, String instr){
	PAGE_ID = pageId;
	processId = procId;
	address = GlobalVariables.NOT_IN_MAIN_MEM;
	PAGE_CONTENTS = instr;
    }

    //end page should be procLength%pageLength
    public static PageTableRow generateEndPage(int pageId, int procId){
	Random rand = new Random();
	String temp = "";
	System.out.println("proc id " + procId);
	int procLength = GlobalVariables.getProcess(procId).getServiceTime();
	int endPageLength = procLength%GlobalVariables.PAGE_LENGTH;
	endPageLength = endPageLength == 0 ? GlobalVariables.PAGE_LENGTH : endPageLength;
	if (endPageLength > 1){
	    for (int i = 0; i < endPageLength-1; i++){ //-1 so ! is end character
		char c = GlobalVariables.PAGE_ALPHABET.charAt(rand.nextInt(GlobalVariables.PAGE_ALPHABET.length()));
		temp += c;
	    }
	}
	temp += "!"; // add !
	return new PageTableRow(pageId, procId, temp);
    }

    private String generatePageContents(){
	Random rand = new Random();
	String temp = "";
	for (int i = 0; i < GlobalVariables.PAGE_LENGTH; i ++){
	    char c = GlobalVariables.PAGE_ALPHABET.charAt(rand.nextInt(GlobalVariables.PAGE_ALPHABET.length()));
	   temp += c;
	}
        return temp;
    }

    public int getProcessId(){
	return processId;
    }

    public void setProcessID(int id){
	processId = id;
    }

    public int getAddress(){
	return address;
    }

    public void setAddress(int addr){
	address = addr;
    }

    public void resetAddress(){
	address = GlobalVariables.NOT_IN_MAIN_MEM;
    }

    public boolean isInMainMem(){
	return address != GlobalVariables.NOT_IN_MAIN_MEM;
    }

    public int checkPageId(){
	return PAGE_ID;
    }

    public char runPageOneUnit(int pageIndex){
        return PAGE_CONTENTS.charAt(pageIndex);
    }

    public String toString(){
	return "page " + PAGE_ID + ": " + PAGE_CONTENTS;
    }
}
