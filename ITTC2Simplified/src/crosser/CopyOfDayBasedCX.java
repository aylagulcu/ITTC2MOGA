package crosser;

import java.util.ArrayList;
import java.util.HashSet;

import constraints.ClashConstraint;
import constraints.ClashSoftConstraint;
import constraints.ConstraintBase;
import constraints.CurriculumCompactnessConstraint;
import constraints.HardConstraint;
import constraints.InstructorTimeAvailabilityConstraint;
import data.convertionManager;
import data.dataHolder;
import data.parameters;
import ga.Individual;

public class CopyOfDayBasedCX extends crosserBase {
	
	int e; int currentTime; int currentRoom;
	int t1; int t2;
	
	int dayP; int[] dayPen= new int[parameters.numDays]; 
	int nmbDays= 1; 
	HashSet<Integer> selectedDays= new HashSet<Integer>();
	int eventParent; int eventChild;

	ArrayList<ConstraintBase> myConstraints= new ArrayList<ConstraintBase>();
	ArrayList<HardConstraint> myHardConstraints= new ArrayList<HardConstraint>();
	Individual child1= new Individual();
	Individual child2= new Individual();
	
	HashSet<Integer> eventsToMove= new HashSet<Integer>();
	
	public CopyOfDayBasedCX(crossoverManager mngr) {
		super(mngr);
		myOffSprings= new Individual[2];
		
		this.myConstraints.add(new InstructorTimeAvailabilityConstraint(100));
		this.myConstraints.add(new CurriculumCompactnessConstraint());
		
		this.myHardConstraints.add(new ClashConstraint(100));
		this.myHardConstraints.add(new InstructorTimeAvailabilityConstraint(100));
	}
	
	public Individual[] cross(Individual ind1, Individual ind2) {

//		System.out.println("CX started:");
		// Assumes that all matrices all up to date!!! 
		this.myOffSprings= new Individual[2];
		child1= ind1.clone(); 
		child2= ind2.clone();
		
		// Step1 : Select the best days in each of the parents.
		// For each day, find a total penalty of all the events scheduled on that day for the constraints:
		// Clash, TimeAvailability, CurriculumCompactness ---> Constraints that may be improved by time change in the child
		// Why not others: Room is not changed; 
		// MinWorkingDays for the current day returns 0 penalty if the event is schduled on the current day. How to evalute if it is not???
		
		// Step 2:
		// Create Chil A: Copy Parent A's best day to Parent B. 
		// Move only to empty and feasible positions. 
		// Remove duplicates
		// Create Child B the same way.
		
		// Evaluate Day penalties
		for (int day=0; day< parameters.numDays; day++){
			dayP= 0;
			for (int room=0; room< parameters.numRooms; room++){
				for (int time= day* parameters.numDailyPeriods; time< (day+1)*parameters.numDailyPeriods; time++){
					eventParent= ind1.dataMatrix[room][time];
					if (eventParent== parameters.UNUSED_EVENT) continue;
					for (ConstraintBase constr: myConstraints){
						dayP+= constr.computeEvent(ind1, eventParent, time, room);
					} // end constr for each
				} // end time for
			} // end room for
			dayPen[day]= dayP;
//			System.out.println("Day"+ day+ " penalty: "+ dayP);
		} // end day for
//		System.out.println("Min Day: "+ minDay+ " with penalty: "+ minDayP);	
		selectDays(dayPen);
		
		eventsToMove.clear();
		for (int day: selectedDays) {
			// Now copy from ind1 to child 2:
			for (int room = 0; room < parameters.numRooms; room++) {
//				for (int time = day * parameters.numDailyPeriods; time < (day + 1)
//						* parameters.numDailyPeriods; time++) {
				int time= this.myRandom.nextInt(parameters.numTimeSlots);
					eventParent = ind1.dataMatrix[room][time];
					if (eventParent == parameters.UNUSED_EVENT)
						continue;
					if (child2.dataMatrix[room][time] == parameters.UNUSED_EVENT) {
						currentTime = convertionManager.intToTime(child2.Data[eventParent]);
						currentRoom = convertionManager.intToRoom(child2.Data[eventParent]);
						assign(child2, eventParent, time, room, currentTime,currentRoom);// Returns true if new position assignment is positive.
					} // end if
					else
						eventsToMove.add(eventParent);
//				} // end time for
			} // end room for
		} // en day for
		// eventsToMove contains the events that could not have been adopted from the parent.
		// Now try to find a good position for each of these events using Micro SA:
		myCXManager.applyMicroSA(child2, eventsToMove);
		
		// Now individual 2 to child 1:
		// Find the best day in ind2:
		for (int day=0; day< parameters.numDays; day++){
			dayP= 0;
			for (int room=0; room< parameters.numRooms; room++){
				for (int time= day* parameters.numDailyPeriods; time< (day+1)*parameters.numDailyPeriods; time++){
					eventParent= ind2.dataMatrix[room][time];
					if (eventParent== parameters.UNUSED_EVENT) continue;
					for (ConstraintBase constr: myConstraints){
						dayP+= constr.computeEvent(ind2, eventParent, time, room);
					} // end constr for each
				} // end time for
			} // end room for
			dayPen[day]= dayP;
		} // end day for
		selectDays(dayPen);
		
//		System.out.println("Min Day: "+ minDay+ " with penalty: "+ minDayP);	
		eventsToMove.clear();
		for (int day: selectedDays) {
			// Now copy from ind2 to child 1:
			for (int room = 0; room < parameters.numRooms; room++) {
				int time= this.myRandom.nextInt(parameters.numTimeSlots);
//				for (int time = day * parameters.numDailyPeriods; time < (day + 1)
//						* parameters.numDailyPeriods; time++) {
					eventParent = ind2.dataMatrix[room][time];
					if (eventParent == parameters.UNUSED_EVENT)
						continue;
					if (child1.dataMatrix[room][time] == parameters.UNUSED_EVENT) {
						currentTime = convertionManager.intToTime(child1.Data[eventParent]);
						currentRoom = convertionManager.intToRoom(child1.Data[eventParent]);
						assign(child1, eventParent, time, room, currentTime,currentRoom); // Returns true if new position assignment is positive.
					} // end if
					else
						eventsToMove.add(eventParent);
//				} // end time for
			} // end room for
		} // end day for
		myCXManager.applyMicroSA(child1, eventsToMove);
		
		myOffSprings[0]= child1;
		myOffSprings[1]= child2;
		return myOffSprings;
	}

	private void selectDays(int[] dayPen2) {
//		System.out.println("Day penalties: ");
//		for (int p: dayPen2)
//			System.out.print("\tPenalty:\t"+ p);
//		System.out.println();
		
		selectedDays.clear();
		
		double total=0;
		for (int i: dayPen2)
			total+= i;
		
		double[] newPen= new double[dayPen2.length];
		for (int d=0; d< newPen.length; d++){
			newPen[d]= total / dayPen2[d];
		} // end d for
		
		total=0; // total of new penalty values:
		for (double i: newPen)
			total+= i;
		
		int[] sortedDays= new int[newPen.length];
		for (int d=0; d< newPen.length; d++)
			sortedDays[d]= d;

		int temp;
		for (int i=0; i< sortedDays.length; i++)
			for (int j=i+1; j< sortedDays.length; j++){
				if (newPen[sortedDays[j]]> newPen[sortedDays[i]]){
					temp= sortedDays[i];
					sortedDays[i]= sortedDays[j];
					sortedDays[j]= temp;
				} // end if
			} // end j for
				
//		for (int i: sortedDays)
//			System.out.println("sorted day: "+ i+ " new penalty(=actual total/actual ind penalty):"+ newPen[i]);
		
		double randPenalty;
		int day;
		while(selectedDays.size()< nmbDays){
			randPenalty= (myRandom.nextFloat() * total);
			double partialTotal=0;
			for(int i=0; i< newPen.length; i++){
				partialTotal+= newPen[sortedDays[i]];
				if (partialTotal >= randPenalty){
					day = sortedDays[i];
					selectedDays.add(day);
					break;
				}
			} // end i for
		} // end while
	
//		System.out.println("Selected Days");
//		for (int d : selectedDays)
//			System.out.print("\tDay:\t"+d);
//		
//		System.out.println();
//		System.out.print("Total : "+ total+ "  Random penalty: "+ randPen+ "  Second day: "+ days[1]);
//		System.out.println();
	}

	private boolean assign(Individual child, int event, int newTime, int newRoom, int currTime, int currRoom) {
		// assigns if new position is feasible
		// it not, takes back all the changes.
		
		// update data matrix:
		child.dataMatrix[newRoom][newTime]= event;
		child.dataMatrix[currRoom][currTime]= parameters.UNUSED_EVENT;

		// update curriculum compactness matrix:
		curList= dataHolder.eventCurriculums.get(event);
		for (int cur: curList){
			assert child.timeCurriculum[currTime][cur]> 0;
			child.timeCurriculum[currTime][cur]--; // old position
			child.timeCurriculum[newTime][cur]++; // new position
		} // end for each
		
		int evOrigVal= child.Data[event];
		child.Data[event] = convertionManager.eventValuesToInt(dataHolder.eventCourseId[event], 1, newTime, newRoom);
		if (checkFeasForMyConstr(child, event, newTime, newRoom)){
			return true; // return the current updated matrices
		} // end if
		
//		System.out.println("event cannot be assigned due to feasibility: "+ event);
		// if not returned true:
		child.Data[event] = evOrigVal; // To original values
		// matrix to original values:
		child.dataMatrix[currRoom][currTime]= event;
		child.dataMatrix[newRoom][newTime]= parameters.UNUSED_EVENT;

		// curriculum compactness matrix to original:
		curList= dataHolder.eventCurriculums.get(event);
		for (int cur: curList){
			child.timeCurriculum[currTime][cur]++; // original position
			child.timeCurriculum[newTime][cur]--; // new position
		} // end for each

		return false;
	}

	private boolean checkFeasForMyConstr(Individual child, int event, int newTime, int newRoom) {
		assert event!= parameters.UNUSED_EVENT;
		for (HardConstraint hc: this.myHardConstraints){
			if (!hc.checkEventFeasibilityInSA(child, event, newTime, newRoom))
				return false;
		}
		return true;
	}

}
