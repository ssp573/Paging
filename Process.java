
public class Process {
	int proc_ID;
	double A;
	double B;
	double C;
	int num_references;
	boolean first_ref;
	int word;
	int num_faults;
	int residency;
	int num_evict;
	
	public Process(int ID, double a, double b, double c,int num_ref){
		proc_ID=ID;
		A=a;
		B=b;
		C=c;
		num_faults=0;
		residency=0;
		num_evict=0;
		num_references=num_ref;
		first_ref=true;
	}

}
