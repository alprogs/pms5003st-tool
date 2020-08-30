package pms5003st;

import lombok.extern.slf4j.Slf4j;
import pms5003st.driver.PMS5003STDriver;
import pms5003st.driver.PMS5003STMeasurement;

@Slf4j
public class App {

	private void doProcess(String serialDevice, String sensorMode) {
		log.info("START");

		try (PMS5003STDriver driver = PMS5003STDriver.getInstance( serialDevice )) {
			// connect to PMS5003ST
			driver.open();

			// set sensor mode
			driver.setSensorMode( sensorMode );

			while(true) {

				// get measure data
				PMS5003STMeasurement data 	= driver.measure( sensorMode );
				if (data != null) {
					log.info( data.toString() );
				}

				// interval
				Thread.sleep( 1 * 1000 );
			}

		} catch (Exception e) {
			log.error( e.toString() );
		}

	}

    public static void main(String[] args) {
		App app 	= new App();

		/*
		 * PARAMETER1: serial device
		 * PARAMETER2: set sensor mode
		 *             (PMS5003STDriver.ACTIVE_MODE OR PMS5003STMeasurement.PASSIVE_MODE)
		 */
		//app.doProcess("/dev/ttyUSB0", PMS5003STDriver.ACTIVE_MODE);
		app.doProcess("/dev/ttyUSB0", PMS5003STDriver.PASSIVE_MODE);
    }
}

