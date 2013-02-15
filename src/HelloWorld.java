import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.SensorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.RotateMoveController;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;
import lejos.util.PilotProps;
import lejos.nxt.UltrasonicSensor;

/**
 * Demonstration of use of the Behavior and Pilot classes to
 * implement a simple line following robot.
 * 
 * Requires a wheeled vehicle with two independently controlled
 * wheels with motors connected to motor ports A and C, and a light
 * sensor mounted forwards and pointing down, connected to sensor port 1.
 * 
 * Press ENTER to start the robot.
 * 
 * You can run the PilotParams sample to create a property file which 
 * sets the parameters of the Pilot to the dimensions
 * and motor connections for your robot.
 * 
 * @author Lawrie Griffiths
 *
 */
public class HelloWorld {
	
	static float distanceTravelled = 0;
	
	public static float getDistanceTravelled() {
		return distanceTravelled;
	}
	
	public static void main (String[] aArg)
	throws Exception
	{
     	final int SPEED = 5;
     	final int FREQUENCY = 100; //milliseconds
		
		PilotProps pp = new PilotProps();
    	pp.loadPersistentValues();
    	float wheelDiameter = Float.parseFloat(pp.getProperty(PilotProps.KEY_WHEELDIAMETER, "4.96"));
    	float trackWidth = Float.parseFloat(pp.getProperty(PilotProps.KEY_TRACKWIDTH, "13.0"));
    	RegulatedMotor leftMotor = PilotProps.getMotor(pp.getProperty(PilotProps.KEY_LEFTMOTOR, "B"));
    	RegulatedMotor rightMotor = PilotProps.getMotor(pp.getProperty(PilotProps.KEY_RIGHTMOTOR, "C"));
    	boolean reverse = Boolean.parseBoolean(pp.getProperty(PilotProps.KEY_REVERSE,"false"));
    	
		final UltrasonicSensor sonic = new UltrasonicSensor(SensorPort.S2);
		sonic.continuous();
		// Change last parameter of Pilot to specify on which 
		// direction you want to be "forward" for your vehicle.
		// The wheel and axle dimension parameters should be
		// set for your robot, but are not critical.
		final RotateMoveController pilot = new DifferentialPilot(wheelDiameter, trackWidth, leftMotor, rightMotor, reverse);
		final LightSensor light = new LightSensor(SensorPort.S3);
		
                
		
		pilot.setRotateSpeed(180);        
        pilot.setTravelSpeed(SPEED);
         /**
         * this behavior wants to take control when the light sensor sees the line
         */
		Behavior DriveForward = new Behavior()
		{

			private boolean suppress = false;
			public boolean takeControl() {return light.readValue() <= 50;}
			
			public void suppress() {
				pilot.stop();
			}
			public void action() {
				pilot.forward();
				while(light.readValue() <= 50) Thread.yield(); //action complete when not on line
			}					
		};
		
		Behavior OffLine = new Behavior()
		{
			private boolean suppress = false;
			
			public boolean takeControl() {return light.readValue() > 50;}

			public void suppress() {
				suppress = true;
			}
			
			public void action() {
				distanceTravelled += pilot.getMovement().getDistanceTraveled();
				int sweep = 10;
				while (!suppress) {
					pilot.rotate(sweep,true);
					sweep *= -2;
					while(!suppress && pilot.isMoving()) Thread.yield();
				}
				//pilot.stop();
				suppress = false;
			}
		};
		
		class DataChecker implements Runnable {
			
			//FileOutputStream out = null;
			File data = new File("data.csv");
			//BufferedWriter bw = null;
			
			public void run() {
				while(true) {
					if (light.readValue() <= 50) {
						try {
							//out = new FileOutputStream(data);
							//DataOutputStream dataOut = new DataOutputStream(out);
							//FileWriter fw = null;
							OutputStream outputStream = new FileOutputStream(data, true);
							Writer writer = new OutputStreamWriter(outputStream);
							writer.write(Float.toString(getDistanceTravelled() + pilot.getMovement().getDistanceTraveled()) + ",0," + String.valueOf(sonic.getDistance()) + '\n');
							writer.close();
							//dataOut.writeUTF(Float.toString(getDistanceTravelled() + pilot.getMovement().getDistanceTraveled()) + ",0," + String.valueOf(sonic.getDistance()) + '\n');
							//out.close();
						} catch(IOException e) {
							System.err.println("Failed to create output stream");
							System.exit(1);
						}		
					LCD.drawString(String.valueOf(sonic.getDistance()) + ',' + Float.toString(getDistanceTravelled() + pilot.getMovement().getDistanceTraveled()), 0, 1);
					try {
						Thread.sleep(FREQUENCY);
						}catch(Exception ex){	
							LCD.drawString(ex.getMessage(), 0, 2);
						}
					}
				}
			}
			
			public void main(String args[]) {
				(new Thread(new DataChecker())).start();
			}
		}

		Behavior[] bArray = {OffLine, DriveForward};
        LCD.drawString("Line ", 0, 1);
        Button.waitForAnyPress();
        Thread t = new Thread(new DataChecker());
        t.start();
	    (new Arbitrator(bArray)).start();
	}
}