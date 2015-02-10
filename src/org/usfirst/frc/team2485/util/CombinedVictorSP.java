package org.usfirst.frc.team2485.util;

import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.VictorSP;

public class CombinedVictorSP implements SpeedController {

	private VictorSP[] speedControllerList;
	
	public CombinedVictorSP(VictorSP... speedControllerList) {

		this.speedControllerList = speedControllerList;
	}
	
	@Override
	public void pidWrite(double output) {
		//TODO Auto-generated method stub
	}

	@Override
	public double get() {
		
		double sum = 0;
		for(SpeedController s : speedControllerList) {
			sum += s.get(); 
		}
		double average = sum / speedControllerList.length;
		return average;
	}

	@Override
	public void set(double speed, byte syncGroup) {
		
		for(SpeedController s : speedControllerList) {
			s.set(speed, syncGroup);
		}
	}

	@Override
	public void set(double speed) {
		
		for(SpeedController s : speedControllerList) {
			s.set(speed);
		}
	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

}
