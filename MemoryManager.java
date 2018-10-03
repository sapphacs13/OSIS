import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;
import java.io.BufferedWriter;

public class MemoryManager{

    private final int INIT_DISK_SIZE = 25;

    private final static int EMPTY_FRAME = 0;
    private final String LRU = "LRU"; //constant for LRU policy
    
    private String policy; //stores memory policy
    private int[] mainMem;
    private int[] diskMem;
    private int numPageFaults; //counter for page faults
    private HashMap<Integer, PageTableRow> pageTable;
    private ArrayList<ArrayBlockingQueue<Integer>> pageRemovalOrders; //list of queues to keep track of which pages to move for each process


    /*
     * constructor takes in:
     * -the number of processes to set mainmem size (unrealistic but acceptable for this simulation)
     * -string of which policy to run; runs LRU if string is LRU, otherwise runs FCFS
     * -writer to print results to file
     */
    public MemoryManager(int numProcesses, String pol){
        mainMem = new int[GlobalVariables.MAIN_MEM_SIZE];
	diskMem = new int[INIT_DISK_SIZE];
	policy = pol;
	numPageFaults = 0; 
	pageTable = new HashMap<Integer, PageTableRow>();
	pageRemovalOrders = new ArrayList<ArrayBlockingQueue<Integer>>();
	for(int i = 0; i < numProcesses; i++){
	    pageRemovalOrders.add(new ArrayBlockingQueue<Integer>(GlobalVariables.RESIDENT_SET_SIZE));
	}
    }

    /*
     * Public Methods:
     * -getNumPageFaults
     *     returns current number of page faults
     * -addPagesToTableAndDisk
     *     takes in a process and adds its pages to the 
     *     memorymanagers' page table (with no address stored)
     *     and disk memory
     * -callPage
     *     calls page; if its in mainmem and using LRU, does necessary work for LRU
     *     if page isn't in mainmem, iterates numPageFaults and starts work to add page to mainmem
     * -getPageTableRow
     *     returns pagetablerow object when passed in the page id
     * -addProcessToMain
     *     adds first four pages of process to mainmem
     * -removeProcessFromMain
     *     removes process pages from mainmem
     * -roomInMainMem
     *     returns true/false whether or not there is room for a process' resident set in mainmem
     */
    public int getNumPageFaults(){
	return numPageFaults;
    }

    public void addPagesToTableAndDisk(Process p){
	int[] pageIds = p.getPageIds();
	int procId = p.getId();
	for(int i=0; i < pageIds.length; i++){
	    int pageId = pageIds[i];

	    PageTableRow row;
	    if (i+1 == pageIds.length){ //special process for page with exit code
		row = PageTableRow.generateEndPage(pageId, procId);
	    } else {
		row = new PageTableRow(pageId, procId);
	    }

	    pageTable.put(pageId, row); //initially, no pages are in main memory
	    addPageToDisk(pageId);
	}
    }

    //checks if passed in pageid is in main
    //if in main, and policy is LRU, then we reorder the queue;
    //in an OS, the page would then be used, regardless of policy
    //if not in main, calls add page to main and counts page faults
    public char callPage(int pageId, int pageIndex){
	PageTableRow r = pageTable.get(pageId);
	if(r.isInMainMem()){
	    if(policy.equals(LRU)){
		ArrayBlockingQueue<Integer> pageRemovalOrder = getQueueFromProcessId(r.getProcessId());
		pageRemovalOrder.remove(pageId);
		//System.out.println(pageRemovalOrder); //testing
		pageRemovalOrder.offer(pageId);
	    }   
	} else if (!r.isInMainMem()){
	    //block process
	    numPageFaults++;
	    addPageToMain(pageId);
	}
	
	//use page
	return r.runPageOneUnit(pageIndex);
    }
    
    public PageTableRow getPageTableRow(int pageId) {
	return pageTable.get(pageId);
    }

    //adds first 4 pages of process to main
    public void addProcessToMain(Process p){
	int[] pageIds = p.getPageIds();
	for(int i = 0; i < GlobalVariables.RESIDENT_SET_SIZE; i++){
	    addPageToMain(pageIds[i]);
	}
    }

    public void removeProcessFromMain(Process p){
	int[] pageIds = p.getPageIds();
	for(int i = 0; i < pageIds.length; i++){
	    int pageId = pageIds[i];
	    PageTableRow r = pageTable.get(pageId);
	    if(r.isInMainMem())
		removePageFromMain(pageId);
	}
    }

    public boolean roomInMainMem(){
	for(int i = 0; i < mainMem.length; i++){
	    if(mainMem[i] == 0)
		return true;
	}
	return false;
    }

    /*
     * Private Helper Methods
     * -expandDisk
     * -addPageToDisk
     * -removePageInDisk
     * -addPageToMain
     * -removePageFromMain
     * -placePageInMain
     * -replacePageInMain
     * -processReachedCapacity
     * -getQueueFromProcessId
     */

    //helper method to treat diskMem as unlimited size
    private void expandDisk(){
	int[] temp = new int[diskMem.length*2];
	for(int i = 0; i < diskMem.length; i++){
	    temp[i] = diskMem[i];
	}
	diskMem = temp;
    }

    //adds page to disk; if disk is full, calls expandDisk()
    private void addPageToDisk(int pageId) {
	//for loop for first fit placement policy
	for (int i = 0; i < diskMem.length; i++){
	    if(diskMem[i] == EMPTY_FRAME) {
		diskMem[i] = pageId;
		return;
	    } else {
		if (i+1 == diskMem.length)
		    expandDisk(); //if disk is full, expand disk
	    }
	}
    }

    private void removePageFromDisk(int pageId){
	for (int i = 0; i < diskMem.length; i++){
	    if(diskMem[i] == pageId)
		diskMem[i] = 0;
	}
    }

    //adds page to main; if process has a full resident set, replaces page, otherwise places it
    private void addPageToMain(Integer pageId){
	PageTableRow row = pageTable.get(pageId);
	int processId = row.getProcessId();

	if (processReachedCapacity(processId)){
	    replacePageInMain(pageId);
	} else {
	    placePageInMain(pageId);
	}
    }

    //removes page from main, resets its address in the page table, and moves page to disk
    private void removePageFromMain(int pageId){
	PageTableRow row = pageTable.get(pageId);
	if(row.getAddress() >= 0){
	    mainMem[row.getAddress()] = 0;
	    row.resetAddress();
	    addPageToDisk(pageId);
	}
    }

    /*
     * Uses first fit to find spot for page in mainMem; removes page from disk, and sets its address.
     * Adds page to queue to keep track of order of removal of pages for FIFO and LRU
     * Has check for if there isn't an empty spot in mainmem, but since we've already checked that
     * the processes' resident set isn't full, we should never reach that
     */
    private void placePageInMain(int pageId){
	for (int i = 0; i < mainMem.length; i++){
	    if(mainMem[i] == EMPTY_FRAME) {
		mainMem[i] = pageId;
		removePageFromDisk(pageId);
		PageTableRow row = pageTable.get(pageId);
		row.setAddress(i);
		ArrayBlockingQueue<Integer> pageRemovalOrder = getQueueFromProcessId(row.getProcessId());
		pageRemovalOrder.offer(pageId);
		return;
	    } else {
		if (i+1 == mainMem.length)
		    System.out.println("memory error: reached end of main mem");
	    }
	}
    }

    /*
     * Uses queue unique to page's process to remove the correct
     * page from process' resident set
     */
    private void replacePageInMain(int pageId){
	PageTableRow r = pageTable.get(pageId);
	ArrayBlockingQueue<Integer> pageRemovalOrder = getQueueFromProcessId(r.getProcessId());
	int removedPageId = pageRemovalOrder.poll();
	//removes chosen page from main memory
        removePageFromMain(removedPageId);
	
	placePageInMain(pageId);
    }

    //counts the number of pages in mainmem that match the processId
    //had looped through mainmem before we created queues for each process
    //changed code to check queue size for better optimization
    private boolean processReachedCapacity(int processId){
	return getQueueFromProcessId(processId).size() >= GlobalVariables.RESIDENT_SET_SIZE;

	// for each page in mainMem, check the procId in the PageTable
	/*int counter = 0;
	for (int i=0; i < mainMem.length; i++){
	    int pageId = mainMem[i];
	    //System.out.println("main mem");
	    //System.out.println(pageId);
	    if(pageId != 0){
		PageTableRow row = pageTable.get(pageId);
		int procId = row.getProcessId();
		if (procId == processId){
		    counter ++;
		}
	    }
	    }*/

	//System.out.println(counter);
        //return counter >= RESIDENT_SET_SIZE;
    }

    //calculates index of queue in arraylist using processid
    private ArrayBlockingQueue<Integer> getQueueFromProcessId(int pid){
	int index = pid%100;
	return pageRemovalOrders.get(index);
    }

}
