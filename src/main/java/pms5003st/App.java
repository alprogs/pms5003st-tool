package pms5003st;

import lombok.extern.slf4j.Slf4j;
import pms5003st.driver.PMS5003STDriver;
import pms5003st.driver.PMS5003STMeasurement;

@Slf4j
public class App {

	public void doProcess() {
		log.info("START");

		try (PMS5003STDriver driver = PMS5003STDriver.getInstance("/dev/ttyUSB0")) {
			// connect to PMS5003ST
			driver.open();
				
			// change mode to passive 
			if (!driver.setPassiveMode()) {
				log.error("Failed to set passive mode.");
			}

			int i=0;
			while(true) {

				log.info("TICK -- {}", i++);

				PMS5003STMeasurement data 	= driver.measureOnPassive();
				if (data == null) {
					log.error("Received data is empty.");
				}

				log.info( data.toString() );

				Thread.sleep( 10 * 1000 );
			}

		} catch (Exception e) {
			log.error( e.toString() );
		}
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




