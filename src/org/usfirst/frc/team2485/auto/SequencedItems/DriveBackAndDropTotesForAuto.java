package org.usfirst.frc.team2485.auto.SequencedItems;

import org.usfirst.frc.team2485.auto.SequencedItem;
import org.usfirst.frc.team2485.robot.Robot;
import org.usfirst.frc.team2485.subsystems.Clapper;

/**
 * @author Patrick Wamsley
 */

public class DriveBackAndDropTotesForAuto implements SequencedItem {
	
	private boolean finished = false;
	private double speed, error, timeout; 
	
	public DriveBackAndDropTotesForAuto(double maxSpeed, double timeout) {
		this.speed = maxSpeed;
		this.timeout = timeout;
		this.error = 50; 
	}
	
	public DriveBackAndDropTotesForAuto(double speed) {
		this(speed, 1); 
	}
	
	@Override
	public void run() {
		
		error = Math.abs(Robot.drive.getDistanceFromEncoders() + 155); 
		
		System.out.println("error in DriveBackAndDropTotesForAuto = " + error);
		
		if (error > 70) {
			Robot.drive.setLeftRight(speed, speed);
			Robot.ratchet.retractRatchet();
		} else if (error > 35) {
			Robot.drive.setLeftRight(speed, speed);
			Robot.clapper.setSetpoint(Clapper.ABOVE_RATCHET_SETPOINT + 20);
		} else if (error > 15) {
			Robot.drive.setLeftRight(-.5, -.5);
			Robot.clapper.setSetpoint(Clapper.LOADING_SETPOINT);
		} else if (error > 7) {
			Robot.rollers.reverseTote(.5); 
		} else if (error > 0) {
			Robot.drive.setLeftRight(-.3, -.3);
			Robot.clapper.openClapper(); 
			finished = true; 
		} else 
			Robot.drive.setLeftRight(0, 0); //happiness
	}

	@Override
	public double duration() {
		return finished ? .1 : timeout;
	}

}
