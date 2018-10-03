/*
 * Process
 * Implements Comparable using remainingTime as comparison
 */
public class Process implements Comparable<Process> {

    //used to create unique ids
    private static int count = 0;
    
    private int id;
    private int arrivalTime;
    private final int serviceTime;
    private int remainingTime;
    private String status;
    private String remainingPageRequests;
    private int[] pageIds;
    private int blocked;

    private final String INIT_PAGE_REQUESTS;

    public Process(int arrival, int time, String pRequests, int[] pageIds) {
	id = 100 + count; //id's are 0 indexed
	count++; 
	arrivalTime = arrival;
        serviceTime = time;
	remainingTime = time;
	remainingPageRequests = pRequests;
	this.pageIds = pageIds;
	INIT_PAGE_REQUESTS = pRequests;
	blocked = 0;
    }

    //getters
    public int getId(){
	return id;
    }

    public int getArrivalTime(){
	return arrivalTime;
    }

    public int getServiceTime(){
	return serviceTime;
    }

    public int getRemainingTime(){
	return remainingTime;
    }

    public String getStatus(){
	return status;
    }

    public int[] getPageIds(){
	return pageIds;
    }

    public String getPageRequests(){
	return INIT_PAGE_REQUESTS;
    }

    public int getBlockedTime(){
	return blocked;
    }
    
    //setters
    //Note: no setter for id, as we don't want to change it
    public void setArrivalTime(int arrival){
	arrivalTime = arrival;
    }

    public void setRemainingTime(int time){
	remainingTime = time;
    }

    public void setStatus(String s){
	if(GlobalVariables.ALL_STATUS.contains(s))
	    status = s;
	else
	    System.out.println("error: invalid status '" + s
			       + "'. Status not  changed.");
    }

    public void setBlockedTime(int t){
	blocked = t;
    }
    
    //returns true if process is completed; false if not
    public boolean isDone(){
	return (remainingTime == 0);
    }
    
    /*
     * toString()
     * Prints out Process id (remainingTime)
     */
    public String toString() {
	return "Process " + id + "(" + remainingTime + ")";
    }

    //used in PriorityQueue for shortest time remaining first scheduler
    @Override
    public int compareTo(Process p) {
	if (this.remainingTime > p.getRemainingTime())
	    return 1;
	if (this.remainingTime < p.getRemainingTime())
	    return -1;
	return 0;
    }
    
    public boolean isReady(){
	return status.equals(GlobalVariables.READY_STATUS);
    }

    public char runOneUnit(){
	String[] pageRequests = remainingPageRequests.split("%");
	if(pageRequests.length < 2) {
	    System.out.println("Error: page request format");
	    return '?';
	}
	String pageIdString = pageRequests[0];
	String pageIndexString = pageRequests[1];
	GlobalVariables.write("\tCALLING: Process " + GlobalVariables.runningProcess.getId() + ", Page " + pageIdString + ", index " + pageIndexString);
	int pageId = Integer.parseInt(pageIdString);
	int pageIndex = Integer.parseInt(pageIndexString);
	char c = GlobalVariables.mm.callPage(pageId, pageIndex);
	//if page done, then remove page from pageRequests
	remainingPageRequests = remainingPageRequests.substring(pageIdString.length()+3); //remove %index%
	decrementRemainingTime();
        return c;
    }

    //decrements remaining time by 1
    private void decrementRemainingTime(){
	remainingTime--;
    }

    //decrements blocked time by 1
    public void decrementBlockedTime(){
	blocked--;
    }

    public String getPagesString(){
	String pages = "";
	for(int i = 0; i < pageIds.length; i++){
	    int pageId = pageIds[i];
	    PageTableRow r = GlobalVariables.mm.getPageTableRow(pageId);
	    pages += "\t\t" + r.toString() +"\n";
	}
	return pages;
    }
    
}
