package pms5003st;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import pms5003st.driver.PMS5003STDriver;
import pms5003st.driver.PMS5003STMeasurement;

public class App {

	private static final Logger log 	= (Logger) LoggerFactory.getLogger( App.class );
		
	private static boolean isExternalToolMode 	= false;

	private void init(String[] args) {

		if (args.length > 0) {
			List<String> options 	= new ArrayList<>(Arrays.asList( args ));

			if (options.contains( "-ext.tool" )) {
				isExternalToolMode = true;

				Logger rootLogger 	= (Logger) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );
				rootLogger.setLevel( ch.qos.logback.classic.Level.WARN );
			}
		}
	}

	private void doProcess(String[] args, String serialDevice, String sensorMode) {
		init( args );
		
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
					if (!isExternalToolMode) {
						log.info( data.toString() );

						// interval
						Thread.sleep( 1 * 1000 );

					} else {
						System.out.println( data.toStringExtToolFormat() );
						break;
					}
				}
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
		app.doProcess(args, "/dev/ttyUSB0", PMS5003STDriver.PASSIVE_MODE);
    }
}

