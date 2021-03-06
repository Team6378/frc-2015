package org.usfirst.frc.team2485.auto.SequencedItems;

import org.usfirst.frc.team2485.auto.SequencedItem;
import org.usfirst.frc.team2485.robot.Robot;

/**
 * @author Patrick Wamsley
 */
public class StrafeToWithoutMaintainingHeading implements SequencedItem {

	private final double distance; 
	private boolean finished; 
	
	public  StrafeToWithoutMaintainingHeading(double inches) {
		distance = inches; 
		finished = false; 
	}
	@Override
	public void run() {
		finished = Robot.drive.strafeToWithoutMaintainingHeading(distance);  
	}

	@Override
	public double duration() {
		return finished ? 0 : 3; 
	}

}
