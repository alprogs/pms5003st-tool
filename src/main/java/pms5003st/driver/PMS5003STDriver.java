package pms5003st.driver;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PMS5003STDriver implements AutoCloseable {

	// Constants --------
	private static final String PORT_NAME 	= "PORT_NAME";	

	private static final String BAUDRATE 	= "BAUDRATE";
	private static final String DATABITS 	= "DATABITS";
	private static final String STOPBITS 	= "STOPBITS";
	private static final String PARITYBITS 	= "PARITYBITS";
	private static final String TIMEOUT 	= "TIMEOUT";

	private static final String PORT_STATE 	= "PORT_STATE";
	// ------------------

	private static Properties config 	= new Properties();

	private static final int DEFAULT_TIMEOUT 	= 1000; 	// default read timout(msec)
	
	private final SerialPort serialPort;

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

	public static PMS5003STDriver getInstance(String port) {
		config.put( PORT_NAME, port );
		config.put( TIMEOUT, DEFAULT_TIMEOUT );

		return LazyHolder.INSTANCE;
	}

	public static PMS5003STDriver getInstance(String port, int timeout) {
		config.put( PORT_NAME, port );
		config.put( TIMEOUT, timeout );
		
		return LazyHolder.INSTANCE;
	}

	private PMS5003STDriver() {
		serialPort 	= SerialPort.getCommPort(Objects.requireNonNull((String) config.get( PORT_NAME )));

		config.put( BAUDRATE, 9600 );
		config.put( DATABITS, 8 );
		config.put( STOPBITS, SerialPort.ONE_STOP_BIT );
		config.put( PARITYBITS, SerialPort.NO_PARITY );
	}

	public boolean open() {
		if (isConnected()) {
			return true;
		}

		try {
			log.info("Initializing serial port.");

			if (!serialPort.openPort()) {
				throw new Exception("Failed to initializing serial port.");
			}

			serialPort.setBaudRate((int) config.get( BAUDRATE ));
			serialPort.setNumDataBits((int) config.get( DATABITS ));
			serialPort.setNumStopBits((int) config.get( STOPBITS ));
			serialPort.setParity((int) config.get( PARITYBITS ));
			serialPort.setComPortTimeouts(
				SerialPort.LISTENING_EVENT_DATA_WRITTEN, 
				(int)config.get( TIMEOUT ), 
				(int)config.get( TIMEOUT )
			);

			measurementBytesQueue = new ConcurrentLinkedDeque<>();
			serialPort.addDataListener(new SerialPortDataListener() {
				@Override
				public int getListeningEvents() {
					return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
				}

				@Override 
				public void serialEvent(SerialPortEvent event) {
					try {
						byte[] bytes = event.getReceivedData();

						if (bytes.length == PACKET_SIZE) {
							measurementBytesQueue.add(bytes);

							PMS5003STMeasurement data 	= genMeasurementResultFromLastQueue();
							log.info("\n{}", data.toString());
						} else {
							log.debug("Bytes received: {}", convertToHexString(bytes));
						}

					} catch (Exception e) {
						log.error("Failed to read bytes from event. {}", e.getMessage());
					}
				}
			});
			
			config.put(PORT_STATE, true);

			log.info("Serial port initialized with {}.", config.toString());
			
		} catch (Exception e) {
			log.error( e.getMessage() );

			return false;
		}

		return true;
	}

	@Override
	public void close() {
		try {
			if (serialPort.isOpen()) {
				log.info("Trying to close serial port{}.", config.get( PORT_NAME ));

				measurementBytesQueue.clear();
				if (!serialPort.closePort()) {
					throw new Exception("Failed to close serial port");
				}

				log.info("Serial port{} closed.", config.get( PORT_NAME ));

			} else {
				log.info("Serial port{} already closed.", config.get( PORT_NAME ));
			}

		} catch (Exception e) {
			log.error( e.getMessage() );
		}
	}

	public boolean setWakeUp() {
		if (!open()) {
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

	public boolean setSleep() {
		if (!open()) {
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

	public boolean setActiveMode() {
		return changeMode( ACTIVE_MODE );
	}

	public boolean setPassiveMode() {
		return changeMode( PASSIVE_MODE );
	}

	private boolean changeMode(String mode) {
		if (!open()) {
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

	public boolean sendMeasureCmdOnPassive() {
		if (!open()) {
			log.error("Can't measure, port not open.");
			return false;
		}

		if (!write( MEASURE_CMD_BYTES )) {
			log.error("Failed to send measure on passive command.");
			return false;
		}

		log.debug("Measuring.");
		return true;
	}

	public PMS5003STMeasurement measureOnPassive() {
		if (!open()) {
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

	public PMS5003STMeasurement measure() {
		if (!open()) {
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

	private PMS5003STMeasurement genMeasurementResultFromLastQueue() {
		byte[] bytes = measurementBytesQueue.pollLast();

		PMS5003STMeasurement measurement = new PMS5003STMeasurement();

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
		return ( (boolean)config.get(PORT_STATE) && serialPort.isOpen());
	}

	private int convertBytesToValue(byte[] bytes, int index) {
		return (Byte.toUnsignedInt(bytes[index]) << 8) + Byte.toUnsignedInt(bytes[index + 1]);
	}

	private boolean write(byte[] buffer) {
		try {
			int length 	= serialPort.bytesAvailable();
			if (length > 0) {
				byte[] unread 	= new byte[length];
				serialPort.readBytes( unread, length );
				log.info("Deleted unread buffer(length: {}).", length);
			}

			dump( buffer, "PMS5003ST Command Write: " );

			int ret 	= serialPort.writeBytes( buffer, buffer.length );
			if (ret == -1) {
				throw new Exception("Failed to write bytes.");
			}

		} catch (Exception e) {
			log.error(e.getMessage());

			return false;
		}

		return true;
	}

	/*
	private byte[] read(int size) {
		byte[] buffer 	= new byte[size];

		try {
			int ret = serialPort.readBytes( buffer, size );

			if (ret == -1) {
				throw new Exception("Failed to read bytes.");
			}

			dump( buffer, "PMS5003ST Command  Read: " );
			
		} catch (Exception e) {
			log.error( e.getMessage() );
		}

		return buffer;
	}
	*/

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

	private static class LazyHolder {
		private static final PMS5003STDriver INSTANCE 	= new PMS5003STDriver();
	}
}

