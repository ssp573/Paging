import java.util.Comparator;


public class SortByID implements Comparator<Process>{

	@Override
	public int compare(Process o1, Process o2) {
		// TODO Auto-generated method stub
		return o1.proc_ID-o2.proc_ID;
	}

}
