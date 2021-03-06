
package org.usfirst.frc.team2485.subsystems;

import org.usfirst.frc.com.kauailabs.nav6.frc.IMU;
import org.usfirst.frc.team2485.robot.Robot;
import org.usfirst.frc.team2485.util.CombinedSpeedController;
import org.usfirst.frc.team2485.util.DualEncoder;
import org.usfirst.frc.team2485.util.DummyOutput;
import org.usfirst.frc.team2485.util.InvertableEncoder;
import org.usfirst.frc.team2485.util.ThresholdHandler;
import org.usfirst.frc.team2485.util.UltrasonicWrapper;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * @author Anoushka Bose
 * @author Patrick Wamsley
 * @author Ben Clark
 * @author Aidan Fay
 * @author Camille Considine
 */
public class DriveTrain {

	private CombinedSpeedController leftDrive, rightDrive, centerDrive; 
	private Solenoid suspension;
	private DualEncoder dualEncoder;
	private InvertableEncoder centerEnc;
	private IMU imu;
	private UltrasonicWrapper sonicSensorWrapper; 

	//Drive Controls 
	private final double 
		NORMAL_SPEED_RATING = 0.75,
		FAST_SPEED_RATING = 1.0,
		SLOW_SPEED_RATING = 0.5;
	
	private double driveSpeed = FAST_SPEED_RATING; 
	
	private final double TRANSLATE_Y_DEADBAND = 0.2;
	private final double TRANSLATE_X_DEADBAND = 0.25;
	private final double ROTATION_DEADBAND = 0.2;
	
	private boolean strafeOnlyMode 		= false, 
					slowStrafeOnlyMode 	= false, 
					forcedNoStrafeMode 	= false; 
	
	private static final double SLOW_STRAFE_SCALAR = 0.6; 
	
	private static final double STRAFE_TUNING_PARAMETER = 1;
	
	//PID 
	private PIDController driveStraightPID;
	private PIDController imuPID;
	private PIDController strafePID; 
	private PIDController sonicStrafePID; 
	  
	private double desiredHeading = 0.0; 
	private boolean maintainingHeading = false; //use for auto and while !rotating
	
	private DummyOutput dummyDriveStraightEncoderOutput, dummyStrafeEncoderOutput, dummySonicStrafeOutput;
	private DummyOutput dummyImuOutput;

	private double lowEncRate = 40;
	private int imuOnTargetCounter = 0;
	private final int MINIMUM_IMU_ON_TARGET_ITERATIONS = 10;

	private static final double
		absTolerance_Imu_TurnTo 		= 1.0,
		absTolerance_Imu_DriveStraight 	= 2.0,
		absTolerance_Enc_DriveStraight 	= 3.0, 
		absTolerance_Enc_Strafe 		= 2.0;
	
	private static final double 
		driveStraightEncoder_Kp = 0.025,  
		driveStraightEncoder_Ki = 0.0, 
		driveStraightEncoder_Kd = 0.0;
	
	public static final double driveStraightEncoder_ONE_CONTAINER_Kp = 0.005;

	private static final double 
		strafeEncoder_Kp 		= 0.08,
		strafeEncoder_Ki 		= 0.0,
		strafeEncoder_Kd 		= 0.0,
		STRAFE_MAX_SIGNAL_DELTA = .045;
	
	private static final double
		sonicStrafe_Kp = 0.075, 
		sonicStrafe_Ki = 0, 
		sonicStrafe_Kd = 0; 
	
	private double lastStrafeValue = 0.0;

	private static final double
		driveStraightImu_Kp = 0.05, 
		driveStraightImu_Ki = 0.0,
		driveStraightImu_Kd = 0.01; 

	private static final double
		rotateImu_kP = 0.025,
		rotateImu_kI = 0.00,
		rotateImu_kD = 0.01;
	
	//Anti-tipping 
	private double oldXInput, oldYInput;   
	
	private static double MAX_DELTA_X = 0.02; 
	private static double MAX_DELTA_Y_NORMAL = 0.05, MAX_DELTA_Y_DANGER = 0.025; 

	public DriveTrain(CombinedSpeedController leftDrive, CombinedSpeedController rightDrive, 
						CombinedSpeedController center, Solenoid suspension, 
						IMU imu, Encoder leftEnc, Encoder rightEnc, Encoder centerEnc, Ultrasonic sonicSensor) {

		this.leftDrive 			= leftDrive; 
		this.rightDrive   		= rightDrive; 
		this.centerDrive		= center;
		this.suspension 		= suspension;
		this.imu            	= imu;
//		this.centerEnc			= new InvertableEncoder(centerEnc);
		this.dualEncoder		= new DualEncoder(leftEnc, rightEnc); 
		this.sonicSensorWrapper	= new UltrasonicWrapper(sonicSensor); //units set in UltraSonic constructor

		if (this.imu != null) 
			setImu(this.imu);
		
		dummyDriveStraightEncoderOutput = new DummyOutput();
		driveStraightPID = new PIDController(driveStraightEncoder_Kp, driveStraightEncoder_Ki, driveStraightEncoder_Kd,
				dualEncoder, dummyDriveStraightEncoderOutput);
		driveStraightPID.setAbsoluteTolerance(absTolerance_Enc_DriveStraight);
		
		dummySonicStrafeOutput = new DummyOutput();
		sonicStrafePID = new PIDController(sonicStrafe_Kp, sonicStrafe_Ki, sonicStrafe_Kd, sonicSensorWrapper, dummySonicStrafeOutput); 
		sonicStrafePID.setAbsoluteTolerance(absTolerance_Enc_Strafe); 
		
		if (this.centerEnc != null) {
			dummyStrafeEncoderOutput = new DummyOutput();
			strafePID = new PIDController(strafeEncoder_Kp, strafeEncoder_Ki, strafeEncoder_Kd, 
					this.centerEnc, dummyStrafeEncoderOutput);
			strafePID.setAbsoluteTolerance(absTolerance_Enc_Strafe);
		}
	}
	
	public void warlordDrive(double translateX, double translateY, double rotation) {
		
		translateX = ThresholdHandler.deadbandAndScale(translateX, TRANSLATE_X_DEADBAND, 0.1, 1);   
		translateY = -ThresholdHandler.deadbandAndScale(translateY, TRANSLATE_Y_DEADBAND, 0.1, 1);
		rotation   =  ThresholdHandler.deadbandAndScale(rotation, ROTATION_DEADBAND, 0.1, 1);
		
		if (strafeOnlyMode) {
			translateY = 0;
			rotation = 0;
		} else if (slowStrafeOnlyMode) {
			translateY = 0; 
			translateX *= SLOW_STRAFE_SCALAR; 
			rotation = 0; //no rotation if we only want to strafe
		} else if (forcedNoStrafeMode) {
			translateX = 0; 
			translateY *= 1; //drivers prefer to have it this way rather than a 1.25X boost. 
			rotation = 0; //no rotation if we only want to move forward
		}
		
		clamp(translateY); 
		clamp(translateX); 
		
		//prevents tipping from too much acc
		double dXInput = Math.abs(translateX - oldXInput), dYInput = Math.abs(translateY - oldYInput); 

		if (dXInput > MAX_DELTA_X) {

			if (translateX > oldXInput)
				translateX = oldXInput + MAX_DELTA_X; 
			else
				translateX = oldXInput - MAX_DELTA_X; 

			translateX = clamp(translateX); 
		}
		
		double currMaxDeltaY = translateY > oldYInput && oldYInput < 0 ? MAX_DELTA_Y_DANGER : MAX_DELTA_Y_NORMAL; 

		if (dYInput > currMaxDeltaY) {

			if (translateY > oldYInput)
				translateY = oldYInput + currMaxDeltaY; 
			else
				translateY = oldYInput - currMaxDeltaY; 

			translateY = clamp(translateY); 
		}

		oldXInput = translateX;
		oldYInput = translateY;

		if (Math.abs(rotation) >= 0.1) {
			maintainingHeading = false; 				 
			imuPID.disable();
			rotationalDrive(translateY, rotation);
		} else if(Math.abs(translateY) >= 0.1 || Math.abs(translateX) >= 0.1){
			
			if (!maintainingHeading) {
				maintainingHeading = true; 
				desiredHeading = imu.getYaw(); 

				setImuForDrivingStraight(); 
//				imuPID.reset();
				imuPID.setSetpoint(desiredHeading);
				imuPID.enable();
			}
			strafeDrive(translateX, translateY);
		} else
			Robot.drive.setMotors(0, 0, 0);
	}

	public void rotationalDrive(double translateY, double rotation) {

		setCenterWheel(0);
		oldXInput = 0; //resets old strafing input (used in acc limiting) 

		double rightDriveOutput, leftDriveOutput;
		
		rightDriveOutput = translateY - rotation; 
		leftDriveOutput = translateY + rotation; //check signs
				
		rightDriveOutput = clamp(rightDriveOutput); 
		leftDriveOutput	 = clamp(leftDriveOutput); 
		
		setLeftRight(leftDriveOutput, rightDriveOutput);
	}

	public void strafeDrive(double xInput, double yInput) {

		double yOutput = 0, xOutput = 0; 		
		double pidOut = dummyImuOutput.get(); 
		
		/* Code for strafe driving at any angle
		 * 
		 * Scales y input by tuning parameter to account for the varying speeds of for/rev and strafe wheels
		 * Divides by larger input to normalize one component
		 * Multiply by magnitude of controller input to set correct output values [0, 1]  
		 */
		double scaledYOutput = yInput / STRAFE_TUNING_PARAMETER; 

		yOutput = scaledYOutput / Math.max(Math.abs(xInput), Math.abs(scaledYOutput)) * 
				Math.sqrt(Math.pow(xInput, 2) + Math.pow(yInput, 2)); 

		xOutput = xInput / Math.max(Math.abs(xInput), Math.abs(scaledYOutput)) *
				Math.sqrt(Math.pow(xInput, 2) + Math.pow(yInput, 2)); 

		setMotors(clamp(yOutput + pidOut), clamp(yOutput - pidOut), xOutput);
	}

	public void setImuForDrivingStraight() {
		imuPID.setPID(driveStraightImu_Kp, driveStraightImu_Ki, driveStraightImu_Kd);
		imuPID.setAbsoluteTolerance(absTolerance_Imu_DriveStraight);
	}

	public void setImuForRotating() {
		imuPID.setPID(rotateImu_kP, rotateImu_kI, rotateImu_kD);
		imuPID.setAbsoluteTolerance(absTolerance_Imu_TurnTo);
	}
	
	private double clamp(double d) {	
		if (d > 1)
			return 1; 
		if (d < -1)
			return -1;
		return d; 
	}

	/**
	 * 
	 * Sets all drive motors to respective values
	 * 
	 * @param left
	 * @param right
	 * @param center
	 */
	public void setMotors(double left, double right, double center) {
		setLeftRight(left, right);
		setCenterWheel(center);
	}

	public void setImu(IMU imu) {
		this.imu = imu;

		dummyImuOutput = new DummyOutput();
		imuPID = new PIDController(rotateImu_kP, rotateImu_kI, rotateImu_kD, imu, dummyImuOutput);
		imuPID.setAbsoluteTolerance(absTolerance_Imu_DriveStraight);
		imuPID.setInputRange(-180, 180);
		imuPID.setContinuous(true);
	}

	/**
	 * Sends outputs values to the left and right side
	 * of the drive base.
	 *
	 * @param leftOutput
	 * @param rightOutput
	 */
	public void setLeftRight(double leftOutput, double rightOutput) {
		
		double scalar = driveSpeed + 0.05 * Robot.toteCounter.getCount();
		
		if (scalar > 1)
			scalar = 1;
		
		leftDrive.set(leftOutput * scalar);
		rightDrive.set(-rightOutput * scalar);
	}

	private void setCenterWheel(double val){
		
		double scalar = driveSpeed + 0.05 * Robot.toteCounter.getCount();
		
		if (scalar > 1)
			scalar = 1;
		
		centerDrive.set(val * scalar);
	}

	/**
	 * Switch into high speed mode
	 */
	public void setHighSpeed() {
		driveSpeed = FAST_SPEED_RATING;
	}

	/**
	 * Switch into low speed mode
	 */
	public void setLowSpeed() {
		driveSpeed = SLOW_SPEED_RATING;
	}

	/**
	 * Switch to normal speed mode
	 */
	public void setNormalSpeed() {
		driveSpeed = NORMAL_SPEED_RATING;
	}

	/**
	 * Sets maintainingHeading
	 * @param true or false
	 */
	public void setMaintainHeading(boolean b) {
		maintainingHeading = b; 
	}

	public void initPIDGyroRotate() {
		imuPID.setPID(rotateImu_kP, rotateImu_kI, rotateImu_kD);
		imuPID.setAbsoluteTolerance(absTolerance_Imu_TurnTo);
	}

	public double getAngle() {
		return imu == null ? 0 : imu.getYaw();
	}

	public void disableIMUPID() {
		imuPID.disable();
		setLeftRight(0,0);
		maintainingHeading = false;
	}

	public void disableDriveStraightPID() {
		driveStraightPID.disable();
		disableIMUPID();
		setLeftRight(0,0);
	}

	public void disableStrafePID() {
		if (strafePID != null)
			strafePID.disable();
		
		setCenterWheel(0);
		setLeftRight(0, 0);
	}

	public void dropCenterWheel(boolean solValue) {
		suspension.set(solValue);
	}

	/**
	 * Rotates the robot so that the IMU matches the angle
	 * @param angle
	 * @return true when finished
	 */
	public boolean rotateTo(double angle) { //may need to check for moving to fast when pid is on target
		if (imuPID == null) 
			throw new IllegalStateException("can't rotateTo when imu is null"); 

		if (!imuPID.isEnable()) {
			setImuForRotating();
			imuPID.setSetpoint(angle);
			imuPID.enable();
		}
		if (driveStraightPID != null && driveStraightPID.isEnable())
			driveStraightPID.disable();

		// Check to see if we're on target

		if (imuPID.onTarget()) 
			imuOnTargetCounter++;
		 else 
			imuOnTargetCounter = 0;

		if (imuOnTargetCounter >= MINIMUM_IMU_ON_TARGET_ITERATIONS) {
			setLeftRight(0, 0);
			imuPID.disable();
			return true;
		}

		double imuOutput = dummyImuOutput.get();
		setLeftRight(imuOutput, -imuOutput);
		return false;
	}
	
	/**
	 * Rotates angle degrees off of current angle.
	 * @param angle
	 * @return true when finished
	 */
	public boolean rotate(double angle){
		return rotateTo(imu.getYaw()+angle);
	}
	
	public boolean driveTo(double inches) {
		return driveTo(inches, imu.getYaw());
	}
	
	/**
	 * Drives robot forward to the setpoint. <p>
	 * 
	 * WARNING: After completition, encoders are not reset. This can be used to correct for a timeout. 
	 * 
	 * @param inches to drive forward
	 * @return true when robot has driven that many inches, false if not completed
	 */
	public boolean driveTo(double inches, double yawSetpoint) {

		if (driveStraightPID == null)
			throw new IllegalStateException("Attempting to driveTo but no PID controller");

		if (!driveStraightPID.isEnable()) {
			driveStraightPID.enable();
			System.out.println("Enabling driveStraight PID in driveTo " + dualEncoder.getDistance() + " , " + inches);
			driveStraightPID.setSetpoint(inches);
		}

		if (imuPID != null && !imuPID.isEnable()) {
			setImuForDrivingStraight();
			imuPID.setSetpoint(yawSetpoint);
			System.out.println("enabling IMU PID in driveTo");
			imuPID.enable();
		}

		double encoderOutput = dummyDriveStraightEncoderOutput.get();
		double leftOutput  = encoderOutput;
		double rightOutput = encoderOutput;

		double imuOutput = 0.0;
		
		if (imuPID != null)
			imuOutput = dummyImuOutput.get();

		
		leftOutput 	+= imuOutput;
		rightOutput -= imuOutput;
		
		setLeftRight(leftOutput, rightOutput);

		// done?
		if (driveStraightPID.onTarget() && Math.abs(dualEncoder.getRate()) < lowEncRate) {
			setLeftRight(0.0, 0.0);
			driveStraightPID.disable();
			imuPID.disable();
			return true;
		}
		return false;
	}
	
	public double getDistanceFromEncoders() {
		return dualEncoder.getDistance();
	}
	
	public double getDistanceFromCenterEncoders() {
		return centerEnc != null ? centerEnc.getDistance() : -1; 
	}
	
	public IMU getIMU() {
		return imu;
	}

	public double getAbsoluteRate(){
		return dualEncoder.getAbsoluteRate();
	}

	public double getRate(){
		return dualEncoder.getRate();
	}
	
	public boolean strafeTo(double inches) {
		return strafeTo(inches, imu.getYaw());
	}
	
	public boolean strafeTo(double inches, double yawSetpoint) {
		if (strafePID == null)
			throw new IllegalStateException("Attempting to strafeTo but no PID controller");

		if (!strafePID.isEnable()) {
			strafePID.enable();
			System.out.println("Enabling strafe PID in strafeTo");
			strafePID.setSetpoint(inches);
		}

		if (imuPID != null && !imuPID.isEnable()) {
			setImuForDrivingStraight(); //this is correct even though we are strafing because we are not rotating
			imuPID.setSetpoint(yawSetpoint);
			System.out.println("enabling IMU PID in strafeTo");
			imuPID.enable();
		}
		dropCenterWheel(true);
		
		double dummyEncoderOutput = dummyStrafeEncoderOutput.get();
		
		if (Math.abs(dummyEncoderOutput - lastStrafeValue) > STRAFE_MAX_SIGNAL_DELTA) {
			if (dummyEncoderOutput > lastStrafeValue)
				dummyEncoderOutput = lastStrafeValue + STRAFE_MAX_SIGNAL_DELTA;
			else
				dummyEncoderOutput =  lastStrafeValue - STRAFE_MAX_SIGNAL_DELTA;
		}
		if (dummyEncoderOutput > 1)
			dummyEncoderOutput = 1;
		else if (dummyEncoderOutput < -1)
			dummyEncoderOutput = -1;
		lastStrafeValue = dummyEncoderOutput;
		
		double imuOutput = 0.0;
		if (imuPID != null)
			imuOutput = dummyImuOutput.get();

		setCenterWheel(dummyEncoderOutput);
		setLeftRight(imuOutput, -imuOutput);

		// Check to see if we're on target
		if (strafePID.onTarget() && Math.abs(centerEnc.getRate()) < lowEncRate) {
			setCenterWheel(0.0);
			setLeftRight(0.0, 0.0);
			lastStrafeValue = 0;
			strafePID.disable();
			imuPID.disable();
			return true;
		}
		return false;
	}
	
	public boolean strafeToUsingSonicSensor(double inches) {
		return strafeToUsingSonicSensor(inches, imu.getYaw());
	}
	
	public boolean strafeToUsingSonicSensor(double inches, double yawSetpoint) {
		
		if (sonicStrafePID == null)
			throw new IllegalStateException("Attempting to strafeTo but no PID controller");

		if (!sonicStrafePID.isEnable()) {
			sonicStrafePID.enable();
			sonicStrafePID.setSetpoint(inches);
		}

		if (imuPID != null && !imuPID.isEnable()) {
			setImuForDrivingStraight();			//this is correct even though we are strafing...we are NOT rotating
			imuPID.setSetpoint(yawSetpoint);
			imuPID.enable();
		}
		dropCenterWheel(true);
		
		double dummySonicOutput = dummySonicStrafeOutput.get();
		
		if (Math.abs(dummySonicOutput - lastStrafeValue) > STRAFE_MAX_SIGNAL_DELTA) {
			if (dummySonicOutput > lastStrafeValue)
				dummySonicOutput = lastStrafeValue + STRAFE_MAX_SIGNAL_DELTA;
			else
				dummySonicOutput =  lastStrafeValue - STRAFE_MAX_SIGNAL_DELTA;
		}
		if (dummySonicOutput > 1)
			dummySonicOutput = 1;
		else if (dummySonicOutput < -1)
			dummySonicOutput = -1;
		lastStrafeValue = dummySonicOutput;
		
		double imuOutput = 0.0;
		if (imuPID != null)
			imuOutput = dummyImuOutput.get();

		setCenterWheel(dummySonicOutput);
		setLeftRight(imuOutput, -imuOutput);

		System.out.println("strafeTo encOutput and imuOutput " + dummySonicOutput + ", " + imuOutput);
		
//		
//		SmartDashboard.putNumber("Strafe Encoder error", strafePID.getError());
//		SmartDashboard.putNumber("Strafe Output from encoder", dummyEncoderOutput);
//		SmartDashboard.putNumber("IMU Output in strafeTo", imuOutput);

		//		System.out.println("leftEnc value: " + leftEnc.getDistance() + " rightEnc value: " + rightEnc.getDistance());
		//		System.out.println("dualEncoder: " + dualEncoder.getDistance());

		//		System.out.println("encoderPID output: " + encoderOutput + " imuPID output: " + imuOutput);
		//		System.out.println("error from enc PID " + driveStraightPID.getError());
		//		System.out.println("dual encoder rate: " + dualEncoder.getRate()); 
		//		System.out.println("signal sent: " + driveStraightPID.get());
		//		System.out.println("Kp from enc PID " + driveStraightPID.getP());

		//		just changed this sign
		
		

		// Check to see if we're on target //doesnt check speed 
		if (sonicStrafePID.onTarget()) {
			System.out.println("Reached PID on target");
			setCenterWheel(0.0);
			setLeftRight(0.0, 0.0);
			lastStrafeValue = 0;
			sonicStrafePID.disable();
			imuPID.disable();
			System.out.println("strafeTo finished inside of strafeTo");
			return true;
		}
		return false;
	}
	
	public double getUltrasonicDistance() {
		return sonicSensorWrapper.pidGet();
	}
	
	public void resetLastStrafeValue() {
		lastStrafeValue = 0.0;
	}

	public boolean strafeToWithoutMaintainingHeading(double inches) {
		if (strafePID == null)
			throw new IllegalStateException("Attempting to strafeTo but no PID controller");

		if (!strafePID.isEnable()) {
//			centerEnc.reset();	// not resetting...making auto relative to a starting 0
			strafePID.enable();
			System.out.println("Enabling strafe PID in strafeTo");
			strafePID.setSetpoint(inches);
		}

		dropCenterWheel(true);
		
		double dummyEncoderOutput = dummyStrafeEncoderOutput.get();
		
		if (Math.abs(dummyEncoderOutput - lastStrafeValue) > STRAFE_MAX_SIGNAL_DELTA) {
			if (dummyEncoderOutput > lastStrafeValue)
				dummyEncoderOutput = lastStrafeValue + STRAFE_MAX_SIGNAL_DELTA;
			else
				dummyEncoderOutput =  lastStrafeValue - STRAFE_MAX_SIGNAL_DELTA;
		}
		
		dummyEncoderOutput = clamp(dummyEncoderOutput); 
		
		lastStrafeValue = dummyEncoderOutput;

		setCenterWheel(dummyEncoderOutput);		

		// Check to see if we're on target
		if (strafePID.onTarget() && Math.abs(centerEnc.getRate()) < lowEncRate) {
			//			System.out.println("Reached PID on target");
			setCenterWheel(0.0);
			strafePID.disable();
			//			System.out.println("driveTo finished inside of driveTo");
			return true;
		}
		return false;
	}
	
	public void setStrafeOnlyMode(boolean b) {
		strafeOnlyMode = b; 
	}
	
	public void setSlowStrafeOnlyMode(boolean b) {
		slowStrafeOnlyMode = b; 
	}
	
	public void setForcedNoStrafeMode(boolean b) {
		forcedNoStrafeMode = b; 
		dropCenterWheel(!b);
	}

	public void setOutputRange(double min, double max) {
		driveStraightPID.setOutputRange(min, max);
	}

	public void resetEncoders() {
		dualEncoder.reset();
		if (centerEnc != null)
			centerEnc.reset();
	}

	public double getErrorFromDriveStraightPID() {
		return driveStraightPID.getError();
	}

	public double getErrorFromStrafePID() {
		return strafePID.getError();
	}

	public void disableSonicStrafePID() {
		sonicStrafePID.disable();
	}

	public void setDriveStraightPID(double kP, int i, int d) {
		driveStraightPID.setPID(kP, i, d);
	}
}

