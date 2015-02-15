
package org.usfirst.frc.team2485.robot;

import org.usfirst.frc.com.kauailabs.nav6.frc.IMU;
import org.usfirst.frc.com.kauailabs.nav6.frc.IMUAdvanced;
import org.usfirst.frc.team2485.auto.Sequencer;
import org.usfirst.frc.team2485.auto.SequencerFactory;
import org.usfirst.frc.team2485.subsystems.*;
import org.usfirst.frc.team2485.util.CombinedVictorSP;
import org.usfirst.frc.team2485.util.Controllers;
import org.usfirst.frc.team2485.util.DualEncoder;
import org.usfirst.frc.team2485.util.ThresholdHandler;

import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.VictorSP;

/**
 * @author Anoushka
 * @author Aidan
 * @author Ben
 * @author Patrick
 * @author Camille
 * @author Maunu
 */ 
public class Robot extends IterativeRobot {
	
	//subsystems 
	public static DriveTrain drive;
	public static Strongback strongback; 
	public static Clapper clapper;
	public static Fingers fingers;
	public static RatchetSystem ratchet;
	public static Claw claw;

	
	private VictorSP left, left2, right, right2, leadScrewMotor, leftBelt, rightBelt, clapperLifter1, clapperLifter2;
	private CombinedVictorSP center; 
	 
	private Encoder leftEnc, rightEnc, centerEnc;
	private DualEncoder dualEncoder;
	
	private Solenoid suspension, longFingerActuators, shortFingerActuators, latchActuator;
	private Compressor compressor;
	private DoubleSolenoid ds, clapperActuator;
	private IMUAdvanced imu;
	private SerialPort ser;
	private CameraServer camServer;
	
	private AnalogInput toteDetector;
	
	private Sequencer autoSequence;
	private AnalogPotentiometer clapperPot;
	private CombinedVictorSP combinedVictorSP;
	private Sequencer teleopSequence;
	
	int degrees;
	private double curPos;
	private double lastPos;
	private double lastVelocity;
	private static double curVelocity;
	
//	boolean fingersOn = true;
	
    public void robotInit() {

    	left     	= new VictorSP(14); //left: 14,15
    	left2 	    = new VictorSP(15);
    	right       = new VictorSP(0); //right: 0, 1
    	right2  	= new VictorSP(1);
    	leftBelt    = new VictorSP(9);
    	rightBelt   = new VictorSP(7);
    	clapperLifter1 = new VictorSP(13);
    	clapperLifter2 = new VictorSP(3);
    	longFingerActuators  = new Solenoid(6);
    	shortFingerActuators = new Solenoid(4);
    	latchActuator = new Solenoid(2);
    	
    	center = new CombinedVictorSP(new VictorSP(11), new VictorSP(12)); //center: 9   changed to 13 1/31/15
    	suspension = new Solenoid(3); //may need two solenoids
    	clapperActuator = new DoubleSolenoid(7, 1);
    	clapperPot = new AnalogPotentiometer(1); 
    	
    	leftEnc = new Encoder(0, 1);
    	rightEnc = new Encoder(4, 5);
    	dualEncoder = new DualEncoder(leftEnc, rightEnc);
    	
    	toteDetector = new AnalogInput(0);
    	
    	leadScrewMotor = new VictorSP(2); 
    	
    	leftEnc.setDistancePerPulse(.0414221608);
    	rightEnc.setDistancePerPulse(.0414221608); 
    	
//    	ds = new DoubleSolenoid(7, 7);

    	compressor = new Compressor();
    	try{
    		ser = new SerialPort(57600, SerialPort.Port.kUSB);
    		byte update_rate_hz = 50;
    		imu = new IMUAdvanced(ser, update_rate_hz);
    	
    	} catch(Exception ex) {
    		ex.printStackTrace();
    	}
    	
    	if(imu != null) {
    		LiveWindow.addSensor("IMU", "Gyro", imu);
    	}
    	
    	drive = new DriveTrain(left, left2, right, right2, center, suspension, imu, leftEnc, rightEnc, centerEnc);
//    	drive.setSolenoid(false);
    	
    	//clapper = new Clapper(6, 5);
    	clapper = new Clapper(clapperLifter1, clapperLifter2, clapperActuator, clapperPot);
    	fingers = new Fingers(leftBelt,rightBelt,longFingerActuators,shortFingerActuators);
    	ratchet = new RatchetSystem(latchActuator);
//    	clapper.close();
    	
    	strongback = new Strongback(leadScrewMotor, imu); 
    	
    	
//    	camServer = CameraServer.getInstance();
//        //camServer.setQuality(50);
//        //the camera name (ex "cam0") can be found through the roborio web interface
//        camServer.startAutomaticCapture("cam1");
    	
        Controllers.set(new Joystick(0), new Joystick(1));
    	
        
        
    	System.out.println("initialized");
    }

    public void autonomousInit() {
    	imu.zeroYaw();
    	leftEnc.reset();
    	rightEnc.reset();
    	dualEncoder.reset();
    	strongback.disablePid(); 
    	
    	autoSequence = SequencerFactory.createAuto(SequencerFactory.THREE_TOTE_STRAIGHT);
//    	autoSequence.reset();
//    	autoSequence = null;
    	
    }
  
    public void autonomousPeriodic() {
//    	System.out.println("left/right " + leftEnc.getDistance() + "\t\t" + rightEnc.getDistance());
//    	System.out.println("dualEnc " + dualEncoder.getDistance());
    	
//    	drive.setLeftRight(-.7, -.7);
//    	 autoSequence.run();
    	 
    	 if (autoSequence != null) {
//    		System.out.println("running teleop sequence");
    		if (autoSequence.run()) {
    			autoSequence = null;
//    			clapper.setManual(); 
    		}
    	}
    	 
    }
    
    public void teleopInit() {
    	System.out.println("teleop init");
    	imu.zeroYaw();
    	
    	drive.setMaintainHeading(false);

    	
//    	drive.setSolenoid(false);
    	
    	leftEnc.reset();
    	rightEnc.reset();
    	
    	clapper.setManual();
    	
		//strongback.enablePid();
		
		teleopSequence = null; 
		System.out.println(clapper.getPotValue());
    }

    public void teleopPeriodic() {
    	
    	compressor.start();

    	strongback.checkSafety();
    	strongback.setSetpoint(0);
    	
//    	System.out.println("teleop enabled" );

//    	System.out.println(clapper.getError() + " : " + clapper.getSetpoint() + clapper.clapperPID.isEnable());
    	
    	
    	//double adjustedJoystickXAxis = (ThresholdHandler.handleThreshold(Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_X,0), 0.1));
    	if (Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_X,(float) 0.1) != 0) {//if the joystick is moved
    		//System.out.println("in Robot, lift joystick value is " + adjustedJoystickXAxis);
    		clapper.liftManually((Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_X,(float) 0.1)));//right is up
    	}
    	else if (clapper.isManual()){
    		clapper.setSetpoint(clapper.getPotValue());//set the setpoint to where ever it left off
//    		System.out.println("setting clapper setpoint in isManual detection, teleopPeriodic, getPotValue() is " + clapper.getPotValue());
    	}
    	else if (clapper.isBelowLowestSetPoint()) {
    		clapper.clapperPID.disable();
    	}
    	
//		System.out.println(imu.getRoll());
    	
        drive.warlordDrive(-Controllers.getAxis(Controllers.XBOX_AXIS_LX, 0),
        					Controllers.getAxis(Controllers.XBOX_AXIS_LY, 0),
                			Controllers.getAxis(Controllers.XBOX_AXIS_RX, 0));
    	
        if(Controllers.getButton(Controllers.XBOX_BTN_RBUMP)) {
        		drive.setQuickTurn(true);
        } else
        	drive.setQuickTurn(false);
        
        if(Controllers.getButton(Controllers.XBOX_BTN_A)) {
        	drive.dropCenterWheel(true);
        }
        
        if(Controllers.getButton(Controllers.XBOX_BTN_B)) {
        	drive.dropCenterWheel(false);
        }
        
//        
//    	System.out.println("current setpoint is: " + drive.imuPID.getSetpoint());
//    	System.out.println("current error is: " + drive.imuPID.getError());
//        
//    	System.out.println("Pot value: " + pot.get());
//    	System.out.println("Enc value: " + encoder.get());
//    	System.out.println("IMU pitch: " + imu.getPitch());
//    	System.out.println("IMU yaw: " + imu.getYaw());
//    	System.out.println("IMU roll: " + imu.getRoll());

    	//basic controls for intake arm
        if (!(clapper.isManual()))
        		fingers.handleTote((Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_Y)),
        				Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_Z));
    
        if(teleopSequence == null) {
        	if ((Controllers.getJoystickAxis(Controllers.JOYSTICK_AXIS_THROTTLE) > 0)) {
//      		System.out.println("clapper should be open");
	      		clapper.openClapper();
	      	
	      	//TODO: figure out how to only open the clapper if a sequence isn't running
       		}
      		else {
//      		System.out.println("clappers should close");
      			clapper.closeClapper();
      		}
        }
        
       	if (Controllers.getJoystickButton(7)) {
       		System.out.println("fingers should close now");
       		fingers.setFingerPosition(Fingers.CLOSED);
       	}
       	if (Controllers.getJoystickButton(9)) {
       		System.out.println("fingers should go parallel");
       		fingers.setFingerPosition(Fingers.PARALLEL);
       	}
       	if (Controllers.getJoystickButton(11)) {
       		System.out.println("fingers should open");
       		fingers.setFingerPosition(Fingers.OPEN);
       	}
       	if (Controllers.getJoystickButton(1)) {
       		teleopSequence = SequencerFactory.toteSuckIn();
       	}
       	if (Controllers.getJoystickButton(3)) {
       		clapper.setSetpoint(clapper.LOADING_SETPOINT);
       	}
       	if (Controllers.getJoystickButton(4)) {
       		clapper.setSetpoint(clapper.COOP_ONE_TOTE_SETPOINT);
       	}
       	if (Controllers.getJoystickButton(5)) {
       		teleopSequence = SequencerFactory.createIntakeToteRoutineBackup();
       	}
       	if (Controllers.getJoystickButton(12)) {
       		System.out.println("hook should release");
       		ratchet.releaseToteStack();
       	}
       	if (Controllers.getJoystickButton(10)) {
       		System.out.println("hook should go back to normal");
       		ratchet.setDefaultRatchetPosition();
       	}
    	if (Controllers.getJoystickButton(8)) {
       		teleopSequence = SequencerFactory.createIntakeToteRoutine();
       	}
       	
    	if (Controllers.getJoystickButton(2)){
    		teleopSequence = null;
    	}
    	
       	if (teleopSequence != null) {
//       		System.out.println("running teleop sequence");
       		if (teleopSequence.run()) {
       			teleopSequence = null;
//       			clapper.setManual(); 
       		}
       	}
       	
       	
       	
       	double curPos = dualEncoder.getDistance();
       	curVelocity = curPos-lastPos;
//       	System.out.println(imu.getWorldLinearAccelX() +"," + imu.getWorldLinearAccelY() + "," + imu.getWorldLinearAccelZ() + "," + imu.getPitch() + "," + imu.getRoll() + "," + imu.getYaw() + "," + curPos + "," + curVelocity + "," + (curVelocity - lastVelocity));
       	
       	SmartDashboard.putString("Clapper and Container", clapper.getPercentHeight() +"," + 0 + "," + strongback.getIMURoll());
       	
       	SmartDashboard.putDouble("RPM", (int) drive.getAbsoluteRate());
       	
       	lastPos = curPos;
       	lastVelocity = curVelocity;
       	
//       	System.out.println("IMU pitch is " + imu.getPitch());
//       	System.out.println("setpoint " + clapper.getSetpoint() + " potValue " + clapper.getPotValue() + " pid controlled " + clapper.isAutomatic());
       	
    }
    
    public static double getCurVelocity() {
		return curVelocity;
	}

	public void disabledPeriodic() {
//    	System.out.println(clapper.getPotValue());
    	
    	int counter = 0;
    	
    	if (Controllers.getButton(Controllers.XBOX_BTN_A)) {
    		counter++;
    	}
    	
    	if(counter > 50) {
    		degrees += 30;
//    		System.out.println("degrees is now " + degrees);
    		counter = 0;
    	}
    	
    }
    
    public void testInit() {
    	clapper.setManual();
    	leftEnc.reset();
    	rightEnc.reset();
    	drive.disableDriveStraightPID();
    	drive.disableIMUPID();
    	imu.zeroYaw();
    	done = false;
    }
    
    private boolean done = false;
    public void testPeriodic() {

    	compressor.start();

//    	drive.setLeftRight(.2, -.2);
//    	drive.driveTo(60);
    	
//    	degrees = 30;
    	
//    	drive.dropCenterWheel(false);
//    	if(!done && drive.rotateTo(30)) {
//    		done = true;
//    		System.out.println("just finished rotateTo inside of testPeriodic");
//    	}
    	
//    	System.out.println(imu.getYaw());
    	
//    	
//    	  if (Controllers.getButton(Controllers.XBOX_BTN_START))
//          	drive.tuneDriveKp(.005);
//          if (Controllers.getButton(Controllers.XBOX_BTN_BACK))
//          	drive.tuneDriveKp(-.005);
//          if (Controllers.getButton(Controllers.XBOX_BTN_Y)) 
//          	drive.resetButtonClicked(); 
          
          
//    	System.out.println("Imu yaw: " + imu.getYaw());
//    	System.out.println("Imu pitch: " + imu.getPitch());
//    	
//    	left.set(-.5);
//    	left2.set(-.5); 
//    	right.set(.5);
//    	right2.set(.5);
    	
//    	leadScrewMotor.set(-.05);
//    	System.out.println(strongback.leadScrewImuPID.isEnable());

//    	strongback.enablePid(); 
//		System.out.println(strongback.getError() + " output " + .leadScrewImuPID.get());
    	
//    	SmartDashboard.putString("Clapper and Container", clapper.getPercentHeight() +"," + 0 + "," + imu.getRoll());
       	
//       	SmartDashboard.putInt("IPS",    (int) drive.getAbsoluteRate());
       	

    }
    
}
