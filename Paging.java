import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;


public class Paging {
	static Scanner sc= null;
	static int Quantum=3;
	static int random=0;		//counter for random numbers
	public static void main(String args[]) throws IOException {
		int machine_size=Integer.parseInt(args[0]);
		int page_size=Integer.parseInt(args[1]);
		int process_size=Integer.parseInt(args[2]);
		int job_mix=Integer.parseInt(args[3]);
		int num_ref=Integer.parseInt(args[4]);
		String repl_algo=args[5];
		
		//Echoing the input
		
		System.out.println("The machine size is " + machine_size);
        System.out.println("The page size is " + page_size);
        System.out.println("The process size is " + process_size);
        System.out.println("The job mix number is " + job_mix);
        System.out.println("The number of references per process is " + num_ref);
        System.out.println("The replacement algorithm is " + repl_algo);
        System.out.println("The level of debugging output is "+args[6]);
        System.out.println();
		
        //Generating a list of random numbers
		File f=new File("random-numbers");
		sc=new Scanner(f);
		ArrayList<Integer> Random_Numbers=new ArrayList<>();
		while(sc.hasNext()){
			Random_Numbers.add(sc.nextInt());
		}
		sc.close();
		
		//Frame Table where each entry contains page number and process number of the page which should be residing there
		int[][] Frame_Table=new int[machine_size/page_size][2];
		
		//Creating processes as specified in the documentation
		ArrayList<Process> Processes = new ArrayList<Process>();
		if (job_mix == 1)
		{
			Process proc= new Process(1,1,0,0,num_ref);
			Processes.add(proc);
		}
		else if(job_mix==2){
			for (int i=0;i<4;i++){
				Process proc= new Process(i+1,1,0,0,num_ref);
				Processes.add(proc);
			}
		}
		else if(job_mix==3){
			for (int i=0;i<4;i++){
				Process proc= new Process(i+1,0,0,0,num_ref);
				Processes.add(proc);
			}
		}
		else if(job_mix==4){
			Processes.add(new Process(1,0.75,0.25,0,num_ref));
			Processes.add(new Process(2,0.75,0,0.25,num_ref));
			Processes.add(new Process(3,0.75,0.125,0.125,num_ref));
			Processes.add(new Process(4,0.5,0.125,0.125,num_ref));
		}
		
		//Initializing frame records with all zeroes
		for (int i=0;i<machine_size/page_size;i++){
			for(int j=0;j<2;j++){
				Frame_Table[i][j]=0;
			}
		}
		
		//Calling relevant functions according to page replacement algorithm used
		if (repl_algo.equalsIgnoreCase("fifo")){
			fifo(num_ref, Random_Numbers,Processes,process_size,page_size,Frame_Table);
			PrintOutput(Processes,process_size,page_size);
		}
		else if(repl_algo.equalsIgnoreCase("lru")){
			lru(num_ref, Random_Numbers,Processes,process_size,page_size,Frame_Table);
			PrintOutput(Processes,process_size,page_size);
		}
		else if(repl_algo.equalsIgnoreCase("random")){
			random(num_ref, Random_Numbers,Processes,process_size,page_size,Frame_Table, machine_size);
			PrintOutput(Processes,process_size,page_size);
		}
		
	}
	
	//Demand Paging with FIFO page replacement
	static void fifo(int num_ref, ArrayList<Integer> Random_Num,ArrayList<Process> Processes,int process_size,int page_size,int[][] Frame_Table){
		ArrayList<Page> Page_Queue=new ArrayList<>();
		int references=1;		//counter to record total number of references and time
		while(references<=num_ref*Processes.size()){
			Process curr_proc = Processes.get(0);
			//running for one process for the given quantum
			for (int i=0; i<Quantum ;i++){
				if (curr_proc.num_references>0){
					int next=0;			//next is the word for this time period
					if (curr_proc.first_ref){
						next=(111*curr_proc.proc_ID+process_size)%process_size;
						curr_proc.first_ref=false;
					}
					else{
							next=curr_proc.word;	//if not first reference for the process, take the word calculated after the previous reference for that process
					}
						int page_num=next/page_size;
						Page curr_page=null;
						//checking if this current page has already been referenced before, if not, create new page 
						for (Page pg : Page_Queue){
							if (pg.page_num==page_num && pg.proc_ID==curr_proc.proc_ID){
								curr_page=pg;
							}
						}
						if (curr_page==null){
							curr_page=new Page(page_num,curr_proc.proc_ID);
						}
						int count=0;
						int frame_num=-1;
						//checking if current page is in Frame Table
						for (int[] record : Frame_Table) {
								if (record[0]==curr_page.page_num && record[1]==curr_page.proc_ID){
									frame_num=count;
							}
							count++;
						}
						if (frame_num==-1){
							//Check if there is a free frame in the frame table in reverse order(Highest frame number first). If there is one, use it for current frame.
							boolean free=false;
							for (int index=Frame_Table.length-1;index>=0;index--){
								if (Frame_Table[index][0]==0 && Frame_Table[index][1]==0){
									curr_page.in_time=references;
									Frame_Table[index][0]=curr_page.page_num;
									Frame_Table[index][1]=curr_proc.proc_ID;
									Page_Queue.add(curr_page);
									//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault, using free frame "+index);
									curr_proc.num_faults++;
									free=true;
									break;
								}
							}
							//If there is no frame available, evict a page by fifo page replacement and use that frame for current page
							if (free==false){
								int count2=0;
								Page page_to_evict=Page_Queue.get(0); 		//Page to evict is the first one in the Page_Queue, i.e: first one in. 
								Page_Queue.remove(page_to_evict);
								for (int[] record: Frame_Table){
									if (record[0]==page_to_evict.page_num && record[1]==page_to_evict.proc_ID){
										curr_page.in_time=references;
										record[0]=curr_page.page_num;
										record[1]=curr_page.proc_ID;
										Page_Queue.add(curr_page);
										//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault,evicting page "+page_to_evict.page_num+" of "+page_to_evict.proc_ID+" from frame num "+count2);
										
										//Updating process numbers on eviction
										for (Process proc:Processes){
											if (proc.proc_ID==page_to_evict.proc_ID){
												proc.residency+=references-page_to_evict.in_time;
												proc.num_evict++;
											}
										}
										curr_proc.num_faults++;
										
									}
									count2++;
								}
							}
						}
						//If page is found in the frame table, 
						else{
							//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Hit in frame "+frame_num);
						}
					curr_proc.num_references--;
					
					//Removing any process with no references left
					if (curr_proc.num_references==0){
						Processes.remove(curr_proc);
						Processes.add(curr_proc);
					}
					references++;
					
					//Calculating the word for next reference
					if (random>Random_Num.size()-1){
						random=0;
					}				//resetting random counter to 0 if we reach the last index of the Random numbers' list
					int r=Random_Num.get(random);
					//System.out.println(curr_proc.proc_ID+" uses random number "+r+" in cycle "+references);
					random++;
					double prob= r/(Integer.MAX_VALUE+1d);
					
					if (prob< curr_proc.A){
						curr_proc.word=((next+1)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B){
						curr_proc.word=((next-5)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B+curr_proc.C){
						curr_proc.word=((next+4)+process_size)%process_size;
					}
					else{
						curr_proc.word=Random_Num.get(random)%process_size;
						random++;
					}
				}
				if (references%3==0){
					Processes.remove(curr_proc);
					Processes.add(curr_proc);
				}
			}
		}
		
	}
	
	//Demand Paging with LRU page replacement
	static void lru(int num_ref, ArrayList<Integer> Random_Num,ArrayList<Process> Processes,int process_size,int page_size,int[][] Frame_Table){
		ArrayList<Page> Page_Queue=new ArrayList<>();
		int references=1;			//counter to record total number of references and time
		while(references<=num_ref*Processes.size()){
			Process curr_proc = Processes.get(0);
			//Running ffor each process for quantum number of time-steps
			for (int i=0; i<Quantum ;i++){
				if (curr_proc.num_references>0){
					int next=0;
					if (curr_proc.first_ref){
						next=(111*curr_proc.proc_ID+process_size)%process_size;
						curr_proc.first_ref=false;
					}
					else{
							next=curr_proc.word;
					}
						//Logic for identifying current page to reference
						Page curr_page=null;
						int page_num=next/page_size;
						for (Page pg : Page_Queue){
							if (pg.page_num==page_num && pg.proc_ID==curr_proc.proc_ID){
								curr_page=pg;
							}
						}
						if (curr_page==null){
							curr_page=new Page(page_num,curr_proc.proc_ID);
						}
						
						//Checking if the current page is in the Frame Table
						int count=0;
						int frame_num=-1;
						for (int[] record : Frame_Table) {
								if (record[0]==page_num && record[1]==curr_proc.proc_ID){
									frame_num=count;
							}
							count++;
						}
						if (frame_num==-1){
							//Check if a free frame is available and if so use it.
							boolean free=false;
							for (int index=Frame_Table.length-1;index>=0;index--){
								if (Frame_Table[index][0]==0 && Frame_Table[index][1]==0){
									//frame_to_add=count1;
									curr_page.in_time=references;
									Frame_Table[index][0]=curr_page.page_num;
									Frame_Table[index][1]=curr_proc.proc_ID;
									if (Page_Queue.contains(curr_page)){
										Page_Queue.remove(curr_page);
									}
									Page_Queue.add(curr_page);
									//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault, using free frame "+index);
									curr_proc.num_faults++;
									free=true;
									break;
								}
							}
							//If no frame is free, page eviction algorithm - LRU
							if (free==false){
								int count2=0;
								
								//Page to evict is the first page in the page queue. The difference with FIFO is that we remove and a page to the page queue every time there is a hit in the frame table. 
								//This means the page to be evicted is not the first page in, but the one least recently used.
								Page page_to_evict=Page_Queue.get(0);
								Page_Queue.remove(page_to_evict);
								for (int[] record: Frame_Table){
									if (record[0]==page_to_evict.page_num && record[1]==page_to_evict.proc_ID){
										curr_page.in_time=references;
										record[0]=curr_page.page_num;
										record[1]=curr_page.proc_ID;
										if (Page_Queue.contains(curr_page)){
											Page_Queue.remove(curr_page);
										}
										Page_Queue.add(curr_page);
										//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault,evicting page "+page_to_evict.page_num+" of "+page_to_evict.proc_ID+" from frame num "+count2);
										for (Process proc:Processes){
											if (proc.proc_ID==page_to_evict.proc_ID){
												proc.residency+=references-page_to_evict.in_time;
												proc.num_evict++;
											}
										}
										curr_proc.num_faults++;
										
									}
									count2++;
								}
							}
						}
						else{
							//removing and adding the page at page hit to simulate LRU
							Page_Queue.remove(curr_page);
							Page_Queue.add(curr_page);
							//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Hit in frame "+frame_num);
						}
					curr_proc.num_references--;
					if (curr_proc.num_references==0){
						Processes.remove(curr_proc);
						Processes.add(curr_proc);
					}
					references++;
					
					//Next word calculation
					if (random>Random_Num.size()-1){
						random=0;
					}
					int r=Random_Num.get(random);
					//System.out.println(curr_proc.proc_ID+" uses random number "+r+" in cycle "+references);
					random++;
					double prob= r/(Integer.MAX_VALUE+1d);
					if (prob< curr_proc.A){
						curr_proc.word=((next+1)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B){
						curr_proc.word=((next-5)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B+curr_proc.C){
						curr_proc.word=((next+4)+process_size)%process_size;
					}
					else{
						curr_proc.word=Random_Num.get(random)%process_size;
						random++;
					}
				}
				
				//Changing the process for the next quantum by changing their order
				if (references%3==0){
					Processes.remove(curr_proc);
					Processes.add(curr_proc);
				}
			}
		}
	}
	
	//Demand Paging with Random page replacement
	static void random(int num_ref, ArrayList<Integer> Random_Num,ArrayList<Process> Processes,int process_size,int page_size,int[][] Frame_Table,int machine_size){
		ArrayList<Page> Page_Queue=new ArrayList<>();
		int references=1;
		
		while(references<=num_ref*Processes.size()){
			Process curr_proc = Processes.get(0);
			//Running each process quantum number of times
			for (int i=0; i<Quantum ;i++){
				if (curr_proc.num_references>0){
					int next=0;
					if (curr_proc.first_ref){
						next=(111*curr_proc.proc_ID+process_size)%process_size;
						curr_proc.first_ref=false;
					}
					else{
							next=curr_proc.word;
					}
						//current page calculation
						int page_num=next/page_size;
						Page curr_page=null;
						for (Page pg : Page_Queue){
							if (pg.page_num==page_num && pg.proc_ID==curr_proc.proc_ID){
								curr_page=pg;
							}
						}
						if (curr_page==null){
							curr_page=new Page(page_num,curr_proc.proc_ID);
						}
						
						//checking if the page is in the frame table 
						int count=0;
						int frame_num=-1;
						for (int[] record : Frame_Table) {
								if (record[0]==curr_page.page_num && record[1]==curr_page.proc_ID){
									frame_num=count;
							}
							count++;
						}
						//if not in frame table
						if (frame_num==-1){
							//Checking if there is a free frame
							boolean free=false;
							for (int index=Frame_Table.length-1;index>=0;index--){
								if (Frame_Table[index][0]==0 && Frame_Table[index][1]==0){
									curr_page.in_time=references;
									Frame_Table[index][0]=curr_page.page_num;
									Frame_Table[index][1]=curr_proc.proc_ID;
									Page_Queue.add(curr_page);
									//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault, using free frame "+index);
									curr_proc.num_faults++;
									free=true;
									break;
								}
							}
							//If no free frame, using random eviction to make way for current page
							if (free==false){
								int count2=0;
								if (random>Random_Num.size()-1){
									random=0;
								}
								int r=Random_Num.get(random);
								random++;
								//System.out.println("Random number used for eviction is "+ r);
								//frame number to evict is random number generated mod length of Frame Table
								int frame_to_evict=r%Frame_Table.length;
								Page page_to_evict=null;
								//Identifying the page in the frame to evict
								for (Page pg:Page_Queue){
									if (pg.page_num==Frame_Table[frame_to_evict][0] && pg.proc_ID==Frame_Table[frame_to_evict][1]){
										page_to_evict=pg;
										break;
									}	
								}
								Page_Queue.remove(page_to_evict);
								for (int[] record: Frame_Table){
									if (record[0]==page_to_evict.page_num && record[1]==page_to_evict.proc_ID){
										curr_page.in_time=references;
										record[0]=curr_page.page_num;
										record[1]=curr_page.proc_ID;
										Page_Queue.add(curr_page);
										//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Fault,evicting page "+page_to_evict.page_num+" of "+page_to_evict.proc_ID+" from frame num "+count2);
										for (Process proc:Processes){
											if (proc.proc_ID==page_to_evict.proc_ID){
												proc.residency+=references-page_to_evict.in_time;
												proc.num_evict++;
											}
										}
										curr_proc.num_faults++;
										
									}
									count2++;
								}
							}
						}
						else{
							
							//System.out.println(curr_proc.proc_ID+" references word "+next+" (page "+page_num+") at time "+references+": Hit in frame "+frame_num);
						}
					curr_proc.num_references--;
					if (curr_proc.num_references==0){
						Processes.remove(curr_proc);
						Processes.add(curr_proc);
					}
					references++;
					
					//new word calculations
					if (random>Random_Num.size()-1){
						random=0;
					}
					int r=Random_Num.get(random);
					//System.out.println(curr_proc.proc_ID+" uses random number "+r+" in cycle "+references);
					random++;
					double prob= r/(Integer.MAX_VALUE+1d);
					if (prob< curr_proc.A){
						curr_proc.word=((next+1)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B){
						curr_proc.word=((next-5)+process_size)%process_size;
					}
					else if(prob<curr_proc.A+curr_proc.B+curr_proc.C){
						curr_proc.word=((next+4)+process_size)%process_size;
					}
					else{
						curr_proc.word=Random_Num.get(random)%process_size;
						random++;
					}
				}
				
				//changing the next process after finishing the quantum
				if (references%3==0){
					Processes.remove(curr_proc);
					Processes.add(curr_proc);
				}
			}
		}
	}
	
	//Function to print the output
	static void PrintOutput(ArrayList<Process> Processes, int process_size,int page_size){
		int total_faults=0;
		int total_evicts=0;
		double sum_res=0;
		Collections.sort(Processes,new SortByID());
		System.out.println();
		
		//For each process
		for(Process proc:Processes){
			if (proc.num_evict==0){
				System.out.println("Process "+proc.proc_ID+" had "+ proc.num_faults+" faults.\n\tWith no evictions, the average residence is undefined." );
			}
			else{
				System.out.println("Process "+proc.proc_ID+" had "+ proc.num_faults+" faults and "+(double)proc.residency/(double)proc.num_evict+" average residency" );
			}
			total_faults+= proc.num_faults;
			total_evicts+=proc.num_evict;
			sum_res+=proc.residency;
		}
		
		//Overall output
		double overall_avg_res=(double)sum_res/(double)total_evicts;
		if (total_evicts==0){
			System.out.println("\nThe total number of faults is "+total_faults+ ".\n\tWith no evictions, the overall average residence is undefined.");

		}
		else{
			System.out.println("\nThe total number of faults is "+total_faults+" and the overall average residency is "+overall_avg_res);
		}
	}
}