import java.util.Random;
import java.util.ArrayList;
import java.io.*;

/* 
 * The generator generates a set of random processes from the input:
 * total simulation time, average inter-arrival time between processes, and
 * the round robin quantum.
 */

public class Generator {
    private int batchTime; //max time for simulation/batch
    private int avgInterArrival; //average interval arrival
    private ArrayList<Process> processes;

    private final int MAX_SERVICE_TIME; //determined by the min number of processes and batchTime
    private final int MIN_NUM_PROCESSES = 2; //we want at least 2 process in each batch
    private final int MAX_NUM_PAGES = 16;
    private final int MIN_NUM_PAGES = 4;

    public Generator(int batchTime, int avgInterArrival) {
	processes = new ArrayList<Process>();
	MAX_SERVICE_TIME = batchTime/MIN_NUM_PROCESSES; //ensures at least x processes
	this.batchTime = batchTime;
	this.avgInterArrival = avgInterArrival;
    }

    /*
     * generateNewList()
     * generates processes for Generator's list
     * 
     * We use a variable called nextProcessStartFCFS. It's
     * used to make sure that all processes finish before
     * simulation time, taking into account the arrival time of
     * the next process. It is named nextProcessStartFCFS
     * because in First Come First Serve, it would represent
     * the time the next process can run. However, it is still
     * useful for generating processes for any scheduling type,
     * although the time represents something different in
     * different schedulers.
     */
    public void generateNewList() {
	Random rand = new Random();
	processes = new ArrayList<Process>();
	

	int arrivalTime = 0;
	int nextProcessStartFCFS = 0;
	int serviceTime = rand.nextInt(MAX_SERVICE_TIME)+1;

	int pageIdTracker = 1000; //to give each page a unique id

	while(canAddProcess(nextProcessStartFCFS, arrivalTime)) {
	    if(processCanFinish(arrivalTime, serviceTime) && 
	       allProcessesCanFinish(serviceTime, nextProcessStartFCFS)) {

		//generate pages for process; create processRequests and pageIds variables
		Random r = new Random();
		int numPages = r.nextInt((MAX_NUM_PAGES + 1) - MIN_NUM_PAGES) + MIN_NUM_PAGES;
		int[] pageIds = new int[numPages];
		for(int i=0; i < numPages; i++){
		    pageIds[i] = pageIdTracker; //so the process knows what pages it has
		    pageIdTracker ++;
		}
		//note: the last page id generated is the "end page"
		int lastPageIndex = numPages-1;
		int lastPageLength = serviceTime%GlobalVariables.PAGE_LENGTH;
		lastPageLength = lastPageLength == 0 ? GlobalVariables.PAGE_LENGTH : lastPageLength;
		String processRequests = "";
		System.out.println("service time: " + serviceTime);
		for(int i = 0; i < serviceTime - 1; i++){ //generate last page later
		    //for each unit of service time, pick a random pageid from process to request (ie add to String pageRequests)
		    int nextRequest = -1;
		    int index = r.nextInt(GlobalVariables.PAGE_LENGTH);
		    
		    if(lastPageLength != 1) { //if end page contains normal characters
			nextRequest = r.nextInt(numPages);
			if (nextRequest == lastPageIndex) 
			    index = r.nextInt(lastPageLength - 1); //doesn't randomly call exit character
		    } else { //if end page only contains exit character
			nextRequest = r.nextInt(numPages-1); //ignore last page		
		    }
		    
		    processRequests += pageIds[nextRequest] + "%" + index + "%";
		    //format of String pageRequests pageId%index%pageId%index%pageId%index%
		}
		processRequests += pageIds[lastPageIndex] + "%" + (lastPageLength-1) + "%"; //adds end page

		//add process to list
		Process p = new Process(arrivalTime, serviceTime, processRequests, pageIds);
		processes.add(p);
		GlobalVariables.write("Process " + p.getId() + " Page Requests: " + processRequests);

		arrivalTime += avgInterArrival;
		//this ensures all generated processes will finish before batch time is up
		if (serviceTime < avgInterArrival)
		    nextProcessStartFCFS += avgInterArrival; // the next process begins when it arrives
		else 
		    nextProcessStartFCFS += serviceTime; // the next process begins when this one ends
	    }
	    //if a process can be added but this specific one doesn't,
	    //generate a new service time and check again
	    serviceTime = rand.nextInt(MAX_SERVICE_TIME)+1;
	}
	GlobalVariables.processes = processes;
    }

    /*
     * Checks if simulation has room for an additional process
     * Sees if the nextProcessStartFCFS is less than the total batch time,
     * and if the next process arrival time is less than or equal to batch time
     */
    private boolean canAddProcess(int nextProcessStartFCFS, int arrivalTime){
	return ((nextProcessStartFCFS < batchTime) && (arrivalTime + avgInterArrival <= batchTime));
    }

    /*
     * Checks if process at specific arrival time and service time could finish in simulation
     */
    private boolean processCanFinish(int arrival, int service) {
	return (arrival + service) < batchTime;
    }

    /*
     * Returns true if inputted service time summed with all past service times
     * allows for the rest of the processes to finish.
     */
    private boolean allProcessesCanFinish(int service, int summedService) {
	int remainingProcesses = MIN_NUM_PROCESSES - processes.size();
	int newSummedService = summedService + service;

	return (batchTime - newSummedService) > remainingProcesses;
    }

    /*
     * generates a process list from file
     * Used for testing
     */
    /*public void generateFromFile(String fileName){
	resetList();
	try{
	    String line = null;
	    FileReader fileReader = new FileReader(fileName);
	    BufferedReader bufferedReader = new BufferedReader(fileReader);
	    int processIdTracker = 100; 
	    while ((line = bufferedReader.readLine()) != null){
		String[] processTimes = line.split("\\s+");
		processes.add(new Process(processIdTracker, Integer.parseInt(processTimes[0]), Integer.parseInt(processTimes[1])));
		processIdTracker++;
	    }
	} catch (Exception ex){
	    System.out.println(ex);
	}
	sortProcesses();
	}*/

    /*
     * Sorts the process list by arrival time
     * Used to ensure processes in file are in proper order
     */
    /*private void sortProcesses(){ //insertion sort
	System.out.println(processes);
	for (int i = 1; i < processes.size(); i++){
	    Process key = processes.get(i);
	    int keyTime = key.getArrivalTime();
	    int j = i;
	    while ((j >0) && (processes.get(j-1).getArrivalTime() > keyTime)){
		processes.set(j, processes.get(j-1));
		j--;
	    }
	    processes.set(j, key);
	}
	System.out.println(processes);
	}*/



    public int getBatchTime() {
	return batchTime;
    }

    public int getAvgInterArrival() {
	return avgInterArrival;
    }
}
