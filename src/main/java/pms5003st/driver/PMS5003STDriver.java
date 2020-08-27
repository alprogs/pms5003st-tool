package pms5003st.driver;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PMS5003STDriver {

	private static final String MY_SERIAL_DEVICE="/dev/serial0";
	private Serial serial;
	private SerialDataEventListener listener;
	private ConcurrentLinkedDeque<byte[]> measurementBytesQueue;

	private static final int PACKET_SIZE 	= 40;
	private static final byte START_BYTE_1 	= 0x42;
	private static final byte START_BYTE_2 	= 0x4D;
	
	// Read in Passive Mode
	private static final byte[] MEASURE_CMD_BYTES 	= { START_BYTE_1, START_BYTE_2, (byte) 0xE2, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x71 };

	// Change Mode
	public static final String ACTIVE_MODE 		= "ACTIVE_MODE";
	public static final String PASSIVE_MODE 		= "PASSIVE_MODE";
	private static final byte[] ACTIVE_CMD_BYTES 	= { START_BYTE_1, START_BYTE_2, (byte) 0xE1, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x71 };
	private static final byte[] PASSIVE_CMD_BYTES 	= { START_BYTE_1, START_BYTE_2, (byte) 0xE1, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x70 };

	// Sleep Set
	private static final byte[] SLEEP_CMD_BYTES 	= { START_BYTE_1, START_BYTE_2, (byte) 0xE4, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x74 };
	private static final byte[] WAKEUP_CMD_BYTES 	= { START_BYTE_1, START_BYTE_2, (byte) 0xE4, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x75 };

	private static final boolean isTraceEnabled 	= true;

	public boolean connect() {
		if (isConnected()) {
			return true;
		}

		measurementBytesQueue = new ConcurrentLinkedDeque<>();

		serial = SerialFactory.createInstance();

		serial.setBufferingDataReceived(false);

		SerialConfig config = new SerialConfig();

		config.device( MY_SERIAL_DEVICE )
			.baud(Baud._9600)
			.dataBits(DataBits._8)
			.parity(Parity.NONE)
			.stopBits(StopBits._1)
			.flowControl(FlowControl.NONE);

		listener = event -> {
			try {
				if (event.length() > 0) {
					byte[] bytes = event.getBytes();

					if (bytes.length == PACKET_SIZE)
						measurementBytesQueue.add(bytes);
					else
						log.debug("Bytes received: {}", convertToHexString(bytes));
				}
			} catch (IOException e) {
				log.error("Failed to read bytes from event. {}", e.getMessage());
			}
		};

		serial.addListener(listener);

		try {
			serial.open(config);

			log.debug("Opened port.");

		} catch (IOException e) {
			log.error("Failed to open port. {}", e.getMessage());
		}

		return isConnected();
	}

	public boolean disconnect() {
		if (!isConnected())
			return true;

		try {
			serial.removeListener(listener);

			serial.close();

			SerialFactory.shutdown();

			measurementBytesQueue.clear();

			log.debug("Closed port.");

		} catch (IOException e) {
			log.error("Failed to close port. {}", e.getMessage());
		}

		return !isConnected();
	}

	public boolean activate() {
		if (!connect()) {
			log.error("Can't activate, port not open.");
			return false;
		}

		if (!write(WAKEUP_CMD_BYTES)) {
			log.error("Failed to wake up.");
			return false;
		}

		log.debug("Activated.");

		return true;
	}

	public boolean deactivate() {
		if (!connect()) {
			log.error("Can't deactivate, port not open.");
			return false;
		}

		if (!write(SLEEP_CMD_BYTES)) {
			log.error("Failed to send to sleep.");
			return false;
		}

		log.debug("Deactivated.");

		measurementBytesQueue.clear();

		return true;
	}

	public boolean changeMode(String mode) {
		if (!connect()) {
			log.error("Can't change mode, port not open.");
			return false;
		}

		byte[] CMD_BYTES 	= mode.equals( ACTIVE_MODE ) ? ACTIVE_CMD_BYTES : PASSIVE_CMD_BYTES;
		if (!write( CMD_BYTES )) {
			log.error("Failed to send change mode command.");
			return false;
		}

		log.debug("Mode was changed to {}.", mode);

		measurementBytesQueue.clear();

		return true;
	}

	public PMS5003STMeasurementResult measureOnPassive() {
		if (!connect()) {
			log.error("Can't measure, port not open.");
			return null;
		}

		if (!write( MEASURE_CMD_BYTES )) {
			log.error("Failed to send measure on passive command.");
			return null;
		}
		
		log.debug("Measuring.");

		if (measurementBytesQueue.isEmpty()) {
			log.warn("No measurements available.");
			return null;
		}

		return genMeasurementResultFromLastQueue();
	}

	public PMS5003STMeasurementResult measure() {
		if (!connect()) {
			log.error("Can't measure, port not open.");
			return null;
		}

		log.debug("Measuring.");

		if (measurementBytesQueue.isEmpty()) {
			log.warn("No measurements available.");
			return null;
		}

		return genMeasurementResultFromLastQueue();
	}

	private PMS5003STMeasurementResult genMeasurementResultFromLastQueue() {
		byte[] bytes = measurementBytesQueue.pollLast();

		PMS5003STMeasurementResult measurement = new PMS5003STMeasurementResult();

		measurement.setTime(Instant.now());

		measurement.setPm1_0_cf1(convertBytesToValue(bytes, 4));
		measurement.setPm2_5_cf1(convertBytesToValue(bytes, 6));
		measurement.setPm10_0_cf1(convertBytesToValue(bytes, 8));

		measurement.setPm1_0_atmo(convertBytesToValue(bytes, 10));
		measurement.setPm2_5_atmo(convertBytesToValue(bytes, 12));
		measurement.setPm10_0_atmo(convertBytesToValue(bytes, 14));

		measurement.setPm0_3_count(convertBytesToValue(bytes, 16));
		measurement.setPm0_5_count(convertBytesToValue(bytes, 18));
		measurement.setPm1_0_count(convertBytesToValue(bytes, 20));
		measurement.setPm2_5_count(convertBytesToValue(bytes, 22));
		measurement.setPm5_0_count(convertBytesToValue(bytes, 24));
		measurement.setPm10_0_count(convertBytesToValue(bytes, 26));

		measurement.setFormaldehyde(convertBytesToValue(bytes, 28));

		measurement.setTemperature(convertBytesToValue(bytes, 30));

		measurement.setHumidity(convertBytesToValue(bytes, 32));

		return measurement;
	}

	public boolean isConnected() {
		return (serial != null && serial.isOpen());
	}

	private int convertBytesToValue(byte[] bytes, int index) {
		return (Byte.toUnsignedInt(bytes[index]) << 8) + Byte.toUnsignedInt(bytes[index + 1]);
	}

	private boolean write(byte[] bytes) {
		try {
			serial.write(bytes);

			return true;

		} catch (IOException e) {
			log.error("Failed to write bytes. {}", e.getMessage());
		}

		return false;
	}

	private String convertToHexString(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);

		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}

		return builder.toString();
	}

    private void dump(byte[] data, String tag) {
        if (isTraceEnabled) {
            StringBuffer sb = new StringBuffer();
            for (byte data1 : data) {
                sb.append(String.format("%02x ", data1));
            }
            log.debug("{}{}", tag, sb.toString().trim());
        }
    }

}

