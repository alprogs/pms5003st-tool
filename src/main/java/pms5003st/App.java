package pms5003st;

import lombok.extern.slf4j.Slf4j;
import pms5003st.driver.PMS5003STDriver;
import pms5003st.driver.PMS5003STMeasurementResult;

@Slf4j
public class App {

	public void doProcess() {
		log.info("START");

		PMS5003STDriver driver = new PMS5003STDriver();

		// connect to PMS5003ST
		if (!driver.connect()) {
			log.error("connect failed.");
		}

		// change mode to passive 
		if (!driver.changeMode( PMS5003STDriver.PASSIVE_MODE )) {
			log.error("change mode failed.");
		}

		// read measurement result
		PMS5003STMeasurementResult result 	= driver.measureOnPassive();
		if (result == null) {
			log.error("measureOnPassive was failed.");
		}

		// print measurement result

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




