package org.usfirst.frc.team2485.subsystems;

import edu.wpi.first.wpilibj.Solenoid;

/**
 * Hook which holds totes and foot which keeps totes alined. 
 * 
 * @author Ben Clark
 */
public class RatchetSystem {

	private Solenoid ratchetActuator;
	
	public RatchetSystem(Solenoid ratchetActuator) {
		this.ratchetActuator = ratchetActuator;
	}
	
	public RatchetSystem(int ratchetActuatorPort) {
		this(new Solenoid(ratchetActuatorPort));
	}
	
	public void retractRatchet() {
		ratchetActuator.set(true);
	}
	
	public void extendRatchet() {
		ratchetActuator.set(false);
	}

	public boolean isExtended() {
		return !ratchetActuator.get();
	}

}
