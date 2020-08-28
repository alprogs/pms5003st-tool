package pms5003st;

import lombok.extern.slf4j.Slf4j;
import pms5003st.driver.PMS5003STDriver;
import pms5003st.driver.PMS5003STMeasurement;

@Slf4j
public class App {

	public void doProcess() {
		log.info("START");

		try (PMS5003STDriver driver = PMS5003STDriver.getInstance("/dev/ttyUSB0")) {
			driver.open();
				
			driver.setPassiveMode();

			while(true) {
			}

		} catch (Exception e) {
			log.error( e.toString() );
		}

		// connect to PMS5003ST
		if (!driver.connect()) {
			log.error("connect failed.");
		}

		// change mode to passive 
		if (!driver.changeMode( PMS5003STDriver.PASSIVE_MODE )) {
			log.error("change mode failed.");
		}

		// read measurement result
		PMS5003STMeasurement result 	= driver.measureOnPassive();
		if (result == null) {
			log.error("measureOnPassive was failed.");
		}

		// print measurement result
		log.info("{}{}", "\n", result.toString());	
	}

    public static void main(String[] args) {
		App app 	= new App();
		app.doProcess();
    }
}

/*
public static void main(String[] args) {
	PMS7003Driver driver = new PMS7003Driver();

	PMS7003MeasureTask task = new PMS7003MeasureTask(
			driver,
			Executors.newSingleThreadScheduledExecutor());

	ScheduledFuture<?> future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
			task, 0L, Duration.ofMinutes(5L).toMillis(), TimeUnit.MILLISECONDS);

	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		if (future != null && !future.isDone())
			future.cancel(true);

		driver.disconnect();
	}));
}
*/




