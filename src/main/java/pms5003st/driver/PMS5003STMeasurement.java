package pms5003st.driver;

import java.time.Instant;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PMS5003STMeasurement {
	private Instant time;

	private int pm1_0_cf1;
	private int pm2_5_cf1;
	private int pm10_0_cf1;
	private int pm1_0_atmo;
	private int pm2_5_atmo;
	private int pm10_0_atmo;
	private int pm0_3_count;
	private int pm0_5_count;
	private int pm1_0_count;
	private int pm2_5_count;
	private int pm5_0_count;
	private int pm10_0_count;	
	private float formaldehyde;
	private float temperature;
	private float humidity;

	public String toString() {
		StringBuffer sb 	= new StringBuffer();

		sb.append("\n");
		sb.append("PMS5003ST Measurement Result ==============").append("\n");
		sb.append("PM  1.0(CF1)  : ").append(pm1_0_cf1).append("\n");
		sb.append("PM  2.5(CF1)  : ").append(pm2_5_cf1).append("\n");
		sb.append("PM 10.0(CF1)  : ").append(pm10_0_cf1).append("\n");
		sb.append("PM  1.0(ATMO) : ").append(pm1_0_atmo).append("\n");
		sb.append("PM  2.5(ATMO) : ").append(pm2_5_atmo).append("\n");
		sb.append("PM 10.0(ATMO) : ").append(pm10_0_atmo).append("\n");
		sb.append("PM  0.3(COUNT): ").append(pm0_3_count).append("\n");
		sb.append("PM  0.5(COUNT): ").append(pm0_5_count).append("\n");
		sb.append("PM  1.0(COUNT): ").append(pm1_0_count).append("\n");
		sb.append("PM  2.5(COUNT): ").append(pm2_5_count).append("\n");
		sb.append("PM  5.0(COUNT): ").append(pm5_0_count).append("\n");
		sb.append("PM 10.0(COUNT): ").append(pm10_0_count).append("\n");
		sb.append("FORMALDEHYDE  : ").append(String.format("%.3f", formaldehyde/1000)).append("\n");
		sb.append("TEMPERATURE   : ").append(temperature/10).append("\n");
		sb.append("HUMIDITY      : ").append(humidity/10).append("\n");
		sb.append("===========================================").append("\n");

		return sb.toString();
	}
}

